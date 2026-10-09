package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtStore
import java.util.Calendar

data class DebtBalanceReconciliation(
    val accountId: String,
    val name: String,
    val beforeOutstandingMinor: Long?,
    val afterOutstandingMinor: Long,
    val observedAfter: Boolean
)

data class FinancialHistoryReprocessReport(
    val months: Int,
    val sinceEpochMs: Long,
    val importResult: HistoricalImportResult,
    val retiredUnsupportedOccurrences: Int,
    val debtBalanceChanges:
        List<DebtBalanceReconciliation>,
    val totalOutstandingBeforeMinor: Long,
    val totalOutstandingAfterMinor: Long
)

class FinancialHistoryReprocessor(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val stateStore =
        FinancialHistoryReprocessStateStore(
            appContext
        )

    fun reprocess(
        months: Int,
        nowEpochMs: Long =
            System.currentTimeMillis(),
        onProgress: ((HistoricalImportProgress) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): FinancialHistoryReprocessReport {
        val safeMonths =
            months.coerceIn(
                MIN_MONTHS,
                MAX_MONTHS
            )
        val sinceEpochMs =
            FinancialHistoryReprocessAnalysis
                .windowStartEpochMs(
                    nowEpochMs =
                        nowEpochMs,
                    months =
                        safeMonths
                )

        val debtStore =
            DebtStore.get(appContext)
        val messageLabStore =
            MessageLabStore.get(
                appContext
            )

        val beforeDebt =
            debtStore.accounts()
        val retiredBefore =
            messageLabStore
                .retiredOccurrenceCount()

        val importResult =
            HistoricalSmsImporter(
                appContext
            ).importSince(
                sinceEpochMs =
                    sinceEpochMs,
                rebuildPayCycles = true,
                mode =
                    HistoricalImportMode
                        .REPROCESS,
                onProgress = onProgress,
                isCancelled = isCancelled
            ).let { result ->
                // REPROCESS restarts the window; no import cursor is reused.
                // A cancellation arriving after the final row is still not success.
                if (isCancelled()) result.copy(cancelled = true) else result
            }

        val afterDebt =
            debtStore.accounts()
        val retiredAfter =
            messageLabStore
                .retiredOccurrenceCount()

        val report =
            FinancialHistoryReprocessReport(
                months = safeMonths,
                sinceEpochMs =
                    sinceEpochMs,
                importResult =
                    importResult,
                retiredUnsupportedOccurrences =
                    (
                        retiredAfter -
                            retiredBefore
                        ).coerceAtLeast(0),
                debtBalanceChanges =
                    FinancialHistoryReprocessAnalysis
                        .debtBalanceChanges(
                            before = beforeDebt,
                            after = afterDebt
                        ),
                totalOutstandingBeforeMinor =
                    beforeDebt.sumOf {
                        it.outstandingMinor
                    },
                totalOutstandingAfterMinor =
                    afterDebt.sumOf {
                        it.outstandingMinor
                    }
            )

        val finalReport = if (isCancelled() && !report.importResult.cancelled) {
            report.copy(importResult = report.importResult.copy(cancelled = true))
        } else report

        if (!finalReport.importResult.cancelled) {
            stateStore.recordSuccess(
                atEpochMs = nowEpochMs,
                months = safeMonths
            )
        }

        return finalReport
    }

    companion object {
        const val MIN_MONTHS = 1
        const val MAX_MONTHS = 3
    }
}

object FinancialHistoryReprocessAnalysis {
    fun windowStartEpochMs(
        nowEpochMs: Long,
        months: Int
    ): Long =
        Calendar.getInstance().run {
            timeInMillis =
                nowEpochMs
            add(
                Calendar.MONTH,
                -months.coerceIn(
                    FinancialHistoryReprocessor
                        .MIN_MONTHS,
                    FinancialHistoryReprocessor
                        .MAX_MONTHS
                )
            )
            timeInMillis
        }

    fun debtBalanceChanges(
        before:
            List<DebtAccountSnapshot>,
        after:
            List<DebtAccountSnapshot>
    ): List<DebtBalanceReconciliation> {
        val beforeById =
            before.associateBy {
                it.id
            }

        return after.mapNotNull {
                current ->
            val previous =
                beforeById[current.id]
            val changed =
                previous == null ||
                    previous
                        .outstandingMinor !=
                    current
                        .outstandingMinor ||
                    previous
                        .balanceQuality !=
                    current
                        .balanceQuality ||
                    previous
                        .observedAtEpochMs !=
                    current
                        .observedAtEpochMs

            if (!changed) {
                null
            } else {
                DebtBalanceReconciliation(
                    accountId =
                        current.id,
                    name =
                        current.name,
                    beforeOutstandingMinor =
                        previous
                            ?.outstandingMinor,
                    afterOutstandingMinor =
                        current
                            .outstandingMinor,
                    observedAfter =
                        current
                            .observedAtEpochMs !=
                        null
                )
            }
        }.sortedWith(
            compareByDescending<
                DebtBalanceReconciliation
            > {
                kotlin.math.abs(
                    (
                        it.beforeOutstandingMinor
                            ?: 0L
                        ) -
                        it.afterOutstandingMinor
                )
            }.thenBy {
                it.name.lowercase()
            }
        )
    }
}
