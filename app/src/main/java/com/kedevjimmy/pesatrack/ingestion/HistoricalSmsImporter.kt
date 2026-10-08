package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.BaseColumns
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

data class HistoricalImportProgress(
    val scanned: Int,
    val total: Int,
    val matched: Int,
    val inserted: Int,
    val duplicates: Int,
    val ignored: Int,
    val resumed: Boolean = false
)

data class HistoricalImportResult(
    val scanned: Int,
    val matched: Int,
    val inserted: Int,
    val duplicates: Int,
    val ignored: Int,
    val understood: Int = 0,
    val stateRefreshed: Int = 0,
    val pendingReview: Int = 0,
    val cancelled: Boolean = false,
    val resumed: Boolean = false
)

class HistoricalSmsImporter(
    private val context: Context
) {
    fun importSince(
        sinceEpochMs: Long,
        rebuildPayCycles: Boolean = true,
        mode:
            HistoricalImportMode =
            HistoricalImportMode.IMPORT,
        onProgress:
            ((HistoricalImportProgress) -> Unit)? =
            null,
        isCancelled: () -> Boolean = { false },
        resumeKey: String? = null
    ): HistoricalImportResult {
        val rules = RuleStore.get(context).listRules(enabledOnly = true)
        val accounts = AccountStore.get(context).listAccounts()
        val store = TransactionStore.get(context)
        val checkpointStore =
            HistoricalImportCheckpointStore(
                context
            )
        val normalizedResumeKey =
            resumeKey
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        val checkpoint =
            if (
                mode ==
                    HistoricalImportMode.IMPORT &&
                normalizedResumeKey != null
            ) {
                checkpointStore.load(
                    normalizedResumeKey
                )
            } else {
                null
            }
        val resumed = checkpoint != null
        val effectiveSinceEpochMs =
            checkpoint?.sinceEpochMs
                ?: sinceEpochMs

        var scanned = 0
        var matched = 0
        var inserted = 0
        var duplicates = 0
        var ignored = 0
        var understood = 0
        var stateRefreshed = 0
        var pendingReview = 0
        var cancelled = false

        val projection = arrayOf(
            BaseColumns._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection: String
        val selectionArgs: Array<String>

        if (checkpoint != null) {
            selection =
                "(" +
                    Telephony.Sms.DATE +
                    " > ?) OR (" +
                    Telephony.Sms.DATE +
                    " = ? AND " +
                    BaseColumns._ID +
                    " > ?)"
            selectionArgs =
                arrayOf(
                    checkpoint
                        .lastSmsDateEpochMs
                        .toString(),
                    checkpoint
                        .lastSmsDateEpochMs
                        .toString(),
                    checkpoint
                        .lastSmsId
                        .toString()
                )
        } else {
            selection =
                Telephony.Sms.DATE +
                    " >= ?"
            selectionArgs =
                arrayOf(
                    effectiveSinceEpochMs
                        .toString()
                )
        }

        var lastProcessedSmsDate = 0L
        var lastProcessedSmsId = -1L

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DATE +
                " ASC, " +
                BaseColumns._ID +
                " ASC"
        )?.use { cursor ->
            val idIndex =
                cursor.getColumnIndexOrThrow(
                    BaseColumns._ID
                )
            val addressIndex =
                cursor.getColumnIndexOrThrow(
                    Telephony.Sms.ADDRESS
                )
            val bodyIndex =
                cursor.getColumnIndexOrThrow(
                    Telephony.Sms.BODY
                )
            val dateIndex =
                cursor.getColumnIndexOrThrow(
                    Telephony.Sms.DATE
                )
            val total = cursor.count.coerceAtLeast(0)

            fun publishProgress(force: Boolean = false) {
                if (
                    onProgress == null ||
                    (
                        !force &&
                        scanned != 1 &&
                        scanned % PROGRESS_INTERVAL != 0 &&
                        scanned != total
                    )
                ) {
                    return
                }

                onProgress.invoke(
                    HistoricalImportProgress(
                        scanned = scanned,
                        total = total,
                        matched = matched,
                        inserted = inserted,
                        duplicates = duplicates,
                        ignored = ignored,
                        resumed = resumed
                    )
                )
            }

            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    cancelled = true
                    break
                }

                scanned += 1

                val smsId =
                    cursor.getLong(idIndex)
                val address =
                    cursor.getString(
                        addressIndex
                    )?.trim()
                val body =
                    cursor.getString(
                        bodyIndex
                    )?.trim()
                val smsDate =
                    cursor.getLong(
                        dateIndex
                    )

                lastProcessedSmsDate =
                    smsDate
                lastProcessedSmsId =
                    smsId

                try {

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
                    if (didInsert) {
                        inserted += 1
                    } else {
                        duplicates += 1
                    }
                } finally {
                    if (
                        mode ==
                            HistoricalImportMode
                                .IMPORT &&
                        normalizedResumeKey !=
                            null &&
                        (
                            scanned %
                                CHECKPOINT_INTERVAL ==
                                0 ||
                            scanned == total
                        )
                    ) {
                        checkpointStore.save(
                            resumeKey =
                                normalizedResumeKey,
                            sinceEpochMs =
                                effectiveSinceEpochMs,
                            lastSmsDateEpochMs =
                                smsDate,
                            lastSmsId =
                                smsId
                        )
                    }
                    publishProgress()
                }
            }

            if (cancelled) {
                if (
                    mode ==
                        HistoricalImportMode
                            .IMPORT &&
                    normalizedResumeKey != null &&
                    lastProcessedSmsDate > 0L &&
                    lastProcessedSmsId >= 0L
                ) {
                    checkpointStore.save(
                        resumeKey =
                            normalizedResumeKey,
                        sinceEpochMs =
                            effectiveSinceEpochMs,
                        lastSmsDateEpochMs =
                            lastProcessedSmsDate,
                        lastSmsId =
                            lastProcessedSmsId
                    )
                }
                publishProgress(force = true)
            }
        } ?: error("SMS history is temporarily unavailable.")

        if (
            mode == HistoricalImportMode.IMPORT &&
            !cancelled
        ) {
            if (normalizedResumeKey != null) {
                checkpointStore.clear()
            }
            AutomaticHistoryRepair.recordImport(
                context,
                effectiveSinceEpochMs
            )
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
            understood = understood,
            stateRefreshed =
                stateRefreshed,
            pendingReview =
                pendingReview,
            cancelled = cancelled,
            resumed = resumed
        )
    }

    companion object {
        private const val PROGRESS_INTERVAL = 25
        private const val CHECKPOINT_INTERVAL = 25
        const val SOURCE_HISTORICAL_SMS = "historical_sms"
        const val SOURCE_HISTORICAL_FINANCIAL_MESSAGE =
            "historical_financial_message"
    }
}

