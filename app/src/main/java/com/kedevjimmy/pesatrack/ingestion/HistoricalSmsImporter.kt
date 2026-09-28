package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.Telephony
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.parser.ConfiguredMoneyParser
import com.kedevjimmy.pesatrack.domain.parser.MpesaParser
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import com.kedevjimmy.pesatrack.domain.rules.FinancialMessageEvidence
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceMatch
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceResolver

data class HistoricalImportResult(
    val scanned: Int,
    val matched: Int,
    val inserted: Int,
    val duplicates: Int,
    val ignored: Int,
    val fulizaMessages: Int = 0
)

class HistoricalSmsImporter(
    private val context: Context
) {
    fun importSince(sinceEpochMs: Long): HistoricalImportResult {
        val rules = RuleStore.get(context).listRules(enabledOnly = true)
        val accounts = AccountStore.get(context).listAccounts()
        val store = TransactionStore.get(context)

        var scanned = 0
        var matched = 0
        var inserted = 0
        var duplicates = 0
        var ignored = 0
        var fulizaMessages = 0

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
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                scanned += 1

                val smsId = cursor.getLong(idIndex)
                val address = cursor.getString(addressIndex)?.trim()
                val body = cursor.getString(bodyIndex)?.trim()
                val smsDate = cursor.getLong(dateIndex)

                if (body.isNullOrBlank() || smsDate <= 0L) {
                    ignored += 1
                    continue
                }

                val interpreted =
                    FinancialMessageIngestionCoordinator
                        .get(context)
                        .ingest(
                            title = address,
                            body = body,
                            sourcePackage = null,
                            sourceKind =
                                SOURCE_HISTORICAL_FINANCIAL_MESSAGE,
                            receivedAtEpochMs = smsDate,
                            rebuildPayCycles = false
                        )

                if (
                    interpreted
                        .consumesLegacyTransactionParsing
                ) {
                    matched += 1
                    inserted +=
                        interpreted.insertedTransactions +
                            interpreted.insertedFinancialEvents
                    if (
                        interpreted.insertedTransactions == 0 &&
                        interpreted.insertedFinancialEvents == 0 &&
                        interpreted.pendingReviewEvents == 0
                    ) {
                        duplicates += 1
                    }
                    continue
                }

                if (MpesaMessageDetector.isLikelyMpesa(address, body)) {
                    val parsed = MpesaParser.parse(body)
                    if (parsed == null) {
                        ignored += 1
                        continue
                    }

                    val evidence = if (parsed.counterparty.isNullOrBlank()) {
                        findNearbyEvidence(
                            smsId = smsId,
                            parsed = parsed,
                            smsDate = smsDate
                        )
                    } else {
                        null
                    }
                    val enriched = TransactionEvidenceResolver.enrich(parsed, evidence)
                    val rule = SmartRuleEngine.matchTransaction(enriched, rules)
                    val didInsert = store.insertIfNew(
                        parsed = enriched,
                        sourcePackage = null,
                        sourceKind = SOURCE_HISTORICAL_SMS,
                        receivedAtEpochMs = smsDate,
                        occurredAtEpochMs = TransactionDateResolver.resolve(
                            occurredAtText = parsed.occurredAtText,
                            fallbackEpochMs = smsDate
                        ),
                        matchedRule = rule,
                        rebuildPayCycles = false,
                        evidence = evidence
                    )

                    matched += 1
                    if (didInsert) inserted += 1 else duplicates += 1
                    continue
                }

                val configuredRule =
                    OwnAccountTransferDetector.matchConfiguredNotification(
                        title = address,
                        body = body,
                        accounts = accounts
                    )?.asSmartRule()
                        ?: SmartRuleEngine.matchNotification(
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
                    matchedRule = configuredRule,
                    rebuildPayCycles = false
                )

                matched += 1
                if (didInsert) inserted += 1 else duplicates += 1
            }
        }

        PayCycleStore.get(context).rebuild()

        return HistoricalImportResult(
            scanned = scanned,
            matched = matched,
            inserted = inserted,
            duplicates = duplicates,
            ignored = ignored,
            fulizaMessages = fulizaMessages
        )
    }

    private fun findNearbyEvidence(
        smsId: Long,
        parsed: com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction,
        smsDate: Long
    ): TransactionEvidenceMatch? {
        val candidates = mutableListOf<FinancialMessageEvidence>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val start = smsDate - TransactionEvidenceResolver.MAX_WINDOW_MS
        val end = smsDate + TransactionEvidenceResolver.MAX_WINDOW_MS

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            Telephony.Sms.DATE + " >= ? AND " + Telephony.Sms.DATE + " <= ?",
            arrayOf(start.toString(), end.toString()),
            Telephony.Sms.DATE + " ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                if (cursor.getLong(idIndex) == smsId) continue
                val address = cursor.getString(addressIndex)?.trim()
                val body = cursor.getString(bodyIndex)?.trim()
                val date = cursor.getLong(dateIndex)
                if (body.isNullOrBlank() || date <= 0L) continue
                if (MpesaMessageDetector.isLikelyMpesa(address, body)) continue

                TransactionEvidenceResolver.parseEvidence(
                    title = address,
                    body = body,
                    observedAtEpochMs = date,
                    sourceFallback = address
                )?.let(candidates::add)
            }
        }

        return TransactionEvidenceResolver.chooseBest(
            parsed = parsed,
            transactionObservedAtEpochMs = smsDate,
            evidence = candidates
        )
    }

    companion object {
        const val SOURCE_HISTORICAL_SMS = "historical_sms"
        const val SOURCE_HISTORICAL_FINANCIAL_MESSAGE =
            "historical_financial_message"
    }
}
