package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.BudgetSpendingProjection
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.EverydayMoneyStore
import com.kedevjimmy.pesatrack.data.local.PersonalReceivableStore
import com.kedevjimmy.pesatrack.data.local.StatementMatchDecisionStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.statements.SharedAccountingReportScopePolicy
import com.kedevjimmy.pesatrack.domain.statements.SharedAccountingViewPolicy
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

internal data class ReportsInsightData(
    val range: ReportPeriodRange,
    val current: ActivityLedgerSnapshot,
    val previous: ActivityLedgerSnapshot,
    val spending: List<Pair<String, Long>>,
    val reviewCandidates: List<ReportReviewCandidate>,
    val trend: List<ReportTrendValue>,
    val annualAccounting: SharedAccountingViewPolicy.View?
) {
    val currentSpent: Long get() = current.summary.spentMinor
    val spendingChange: Int? get() = reportSpendingChangePercent(currentSpent, previous.summary.spentMinor)
    val topCategory: Pair<String, Long>? get() = spending.firstOrNull()
    val pdfContent: ReportPdfContent get() = buildReportPdfContent(
        range.label, current.summary, spending, trend, reviewCandidates
    )
}

internal fun reportSpendingChangePercent(current: Long, previous: Long): Int? {
    if (previous <= 0L) return null
    val percent = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
        .multiply(BigDecimal.valueOf(100L))
        .divide(BigDecimal.valueOf(previous), 0, RoundingMode.DOWN)
    return percent.coerceIn(BigDecimal.valueOf(Int.MIN_VALUE.toLong()),
        BigDecimal.valueOf(Int.MAX_VALUE.toLong())).toInt()
}

/** Existing Activity projection and categorization are read-only sources of truth. */
internal class ReportsInsightsReader(private val context: Context) {
    fun load(period: ReportPeriod, selectedEpochMs: Long): ReportsInsightData {
        val now = System.currentTimeMillis()
        val range = reportPeriodRange(period, selectedEpochMs)
        val store = TransactionStore.get(context)
        val current = store.activitySnapshotBetween(range.startEpochMs, range.endExclusiveEpochMs, now)
        val previous = store.activitySnapshotBetween(range.previousStartEpochMs, range.startEpochMs, now)
        val categoryStore = CategoryStore.get(context)
        val allocations = EverydayMoneyStore(context).allAllocations()
        val shares = PersonalReceivableStore(context).linkedShares()
        val totals = BudgetSpendingProjection.calculate(
            current.trustedTransactions.map { it.copy(feeMinor = 0L) },
            store::trustedRefundOriginal,
            categoryStore::canonicalId,
            receivableLent = { shares[it]?.lentMinor ?: 0L },
            allocations = { allocations[it].orEmpty() }
        )
        val labels = categoryStore.all().associate { it.id to it.label }
        val spending = totals.filterValues { it > 0L }
            .map { (id, amount) -> (labels[id] ?: id.replace('_', ' ')) to amount }
            .sortedByDescending { it.second }
        val candidates = current.trustedTransactions.map { row ->
            ReportReviewCandidate(
                id = row.id,
                counterparty = row.counterparty,
                amountMinor = Math.subtractExact(row.amountMinor, shares[row.id]?.lentMinor ?: 0L),
                category = categoryStore.canonicalId(row.category),
                isExpense = row.direction == MoneyDirection.EXPENSE.name,
                isRefund = row.relationType == TransactionStore.RELATION_REFUND,
                hasAllocations = allocations[row.id].orEmpty().isNotEmpty()
            )
        }
        val trend = reportTrendBuckets(period, selectedEpochMs).map { bucket ->
            ReportTrendValue(bucket.label,
                store.activitySnapshotBetween(bucket.startEpochMs, bucket.endExclusiveEpochMs, now)
                    .summary.spentMinor)
        }
        val annualAccounting = if (period == ReportPeriod.YEAR) {
            val year = Calendar.getInstance().apply { timeInMillis = selectedEpochMs }.get(Calendar.YEAR)
            try {
                SharedAccountingReportScopePolicy.forPeriod(
                    "Year", year, StatementMatchDecisionStore(context).read(year).sharedAccounting
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.w("PesaTrackReports", "Annual accounting evidence unavailable")
                null
            }
        } else null
        return ReportsInsightData(range, current, previous, spending, candidates, trend, annualAccounting)
    }
}
