package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.Telephony
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.parser.ConfiguredMoneyParser
import com.kedevjimmy.pesatrack.domain.parser.MpesaParser
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine

data class HistoricalImportResult(
    val scanned: Int,
    val matched: Int,
    val inserted: Int,
    val duplicates: Int,
    val ignored: Int
)

class HistoricalSmsImporter(
    private val context: Context
) {
    fun importSince(sinceEpochMs: Long): HistoricalImportResult {
        val rules = RuleStore.get(context).listRules(enabledOnly = true)
        val store = TransactionStore.get(context)

        var scanned = 0
        var matched = 0
        var inserted = 0
        var duplicates = 0
        var ignored = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            Telephony.Sms.DATE + " >= ?",
            arrayOf(sinceEpochMs.toString()),
            Telephony.Sms.DATE + " ASC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                scanned += 1

                val address = cursor.getString(addressIndex)?.trim()
                val body = cursor.getString(bodyIndex)?.trim()
                val smsDate = cursor.getLong(dateIndex)

                if (body.isNullOrBlank() || smsDate <= 0L) {
                    ignored += 1
                    continue
                }

                if (MpesaMessageDetector.isLikelyMpesa(address, body)) {
                    val parsed = MpesaParser.parse(body)
                    if (parsed == null) {
                        ignored += 1
                        continue
                    }

                    val rule = SmartRuleEngine.matchTransaction(parsed, rules)
                    val didInsert = store.insertIfNew(
                        parsed = parsed,
                        sourcePackage = null,
                        sourceKind = SOURCE_HISTORICAL_SMS,
                        receivedAtEpochMs = smsDate,
                        occurredAtEpochMs = TransactionDateResolver.resolve(
                            occurredAtText = parsed.occurredAtText,
                            fallbackEpochMs = smsDate
                        ),
                        matchedRule = rule
                    )

                    matched += 1
                    if (didInsert) inserted += 1 else duplicates += 1
                    continue
                }

                val configuredRule = SmartRuleEngine.matchNotification(
                    title = address,
                    body = body,
                    rules = rules
                )

                if (configuredRule == null) {
                    ignored += 1
                    continue
                }

                val parsed = ConfiguredMoneyParser.parse(
                    title = address,
                    body = body,
                    rule = configuredRule
                )

                if (parsed == null) {
                    ignored += 1
                    continue
                }

                val didInsert = store.insertIfNew(
                    parsed = parsed,
                    sourcePackage = null,
                    sourceKind = SOURCE_HISTORICAL_SMS,
                    receivedAtEpochMs = smsDate,
                    occurredAtEpochMs = smsDate,
                    matchedRule = configuredRule
                )

                matched += 1
                if (didInsert) inserted += 1 else duplicates += 1
            }
        }

        return HistoricalImportResult(
            scanned = scanned,
            matched = matched,
            inserted = inserted,
            duplicates = duplicates,
            ignored = ignored
        )
    }

    companion object {
        const val SOURCE_HISTORICAL_SMS = "historical_sms"
    }
}
