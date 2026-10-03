package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.Telephony
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.rules.RuleBackedFinancialResolver
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine

enum class HistoricalImportMode {
    IMPORT,
    REPROCESS
}

data class HistoricalImportResult(
    val scanned: Int,
    val matched: Int,
    val inserted: Int,
    val duplicates: Int,
    val ignored: Int,
    val fulizaMessages: Int = 0,
    val understood: Int = 0,
    val stateRefreshed: Int = 0,
    val pendingReview: Int = 0
)

class HistoricalSmsImporter(
    private val context: Context
) {
    fun importSince(
        sinceEpochMs: Long,
        rebuildPayCycles: Boolean = true,
        mode:
            HistoricalImportMode =
            HistoricalImportMode.IMPORT
    ): HistoricalImportResult {
        val rules = RuleStore.get(context).listRules(enabledOnly = true)
        val accounts = AccountStore.get(context).listAccounts()
        val store = TransactionStore.get(context)

        var scanned = 0
        var matched = 0
        var inserted = 0
        var duplicates = 0
        var ignored = 0
        var fulizaMessages = 0
        var understood = 0
        var stateRefreshed = 0
        var pendingReview = 0

        val projection = arrayOf(
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
                            rebuildPayCycles = false,
                            diagnosticMode =
                                if (
                                    mode ==
                                    HistoricalImportMode
                                        .REPROCESS
                                ) {
                                    FinancialDiagnosticMode
                                        .RECONCILE_ONLY
                                } else {
                                    FinancialDiagnosticMode
                                        .RECORD
                                }
                        )

                val understoodByCurrentBrain =
                    interpreted.recognized ||
                        interpreted
                            .semanticallyUnderstood ||
                        interpreted
                            .consumesLegacyTransactionParsing ||
                        interpreted
                            .handledWithoutLedgerMovement

                if (understoodByCurrentBrain) {
                    understood += 1
                }

                pendingReview +=
                    interpreted.pendingReviewEvents

                if (
                    interpreted.changed &&
                    interpreted.insertedTransactions == 0 &&
                    interpreted.insertedFinancialEvents == 0
                ) {
                    stateRefreshed += 1
                }

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
                        interpreted.pendingReviewEvents == 0 &&
                        !interpreted
                            .handledWithoutLedgerMovement
                    ) {
                        duplicates += 1
                    }
                    continue
                }

                if (
                    interpreted.recognized ||
                    interpreted
                        .semanticallyUnderstood ||
                    interpreted
                        .handledWithoutLedgerMovement
                ) {
                    matched += 1
                    inserted +=
                        interpreted.insertedTransactions +
                            interpreted.insertedFinancialEvents
                    continue
                }

                if (
                    MpesaMessageDetector.isLikelyMpesa(
                        address,
                        body
                    )
                ) {
                    // Generic interpretation is authoritative
                    // for M-PESA semantics. Unknown wording is
                    // ignored rather than converted into an
                    // arbitrary OTHER transaction.
                    ignored += 1
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

                val parsed = RuleBackedFinancialResolver.resolve(
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
                understood += 1
                if (didInsert) inserted += 1 else duplicates += 1
            }
        }

        if (rebuildPayCycles) {
            PayCycleStore.get(context).rebuild()
        }

        return HistoricalImportResult(
            scanned = scanned,
            matched = matched,
            inserted = inserted,
            duplicates = duplicates,
            ignored = ignored,
            fulizaMessages = fulizaMessages,
            understood = understood,
            stateRefreshed =
                stateRefreshed,
            pendingReview =
                pendingReview
        )
    }

    companion object {
        const val SOURCE_HISTORICAL_SMS = "historical_sms"
        const val SOURCE_HISTORICAL_FINANCIAL_MESSAGE =
            "historical_financial_message"
    }
}
