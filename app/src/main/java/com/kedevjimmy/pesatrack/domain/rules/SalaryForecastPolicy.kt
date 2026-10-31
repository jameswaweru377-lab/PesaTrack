package com.kedevjimmy.pesatrack.domain.rules

import java.math.BigInteger
import java.util.concurrent.TimeUnit

data class SalarySpendingCycle(
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val variableSpendingMinor: Long,
    val unreviewedCount: Int = 0
)

data class SalaryForecastCommitment(
    val id: String,
    val label: String,
    val amountMinor: Long,
    val dueEpochMs: Long?
)

data class SalaryForecastFacts(
    val nowEpochMs: Long,
    val confirmedPaydayEpochMs: Long?,
    val confirmedSalaryAmountMinor: Long?,
    val expectedWindow: ExpectedPayWindow?,
    val balanceMinor: Long?,
    val balanceAsOfEpochMs: Long?,
    val cycles: List<SalarySpendingCycle>,
    val commitments: List<SalaryForecastCommitment>,
    val hasUnscheduledDebt: Boolean = false,
    val hasUnscheduledBills: Boolean = false,
    val balanceLabel: String = "Spendable balance"
)

enum class SalaryForecastStatus { NEEDS_DATA, CONFIRMED, ESTIMATED }

data class SalaryForecastResult(
    val status: SalaryForecastStatus,
    val paydayStartEpochMs: Long?,
    val paydayEndEpochMs: Long?,
    val lowMinor: Long?,
    val highMinor: Long?,
    val included: List<String>,
    val excluded: List<String>,
    val missing: List<String>
)

/** Pre-payday cash estimate; a future salary never enters the calculation. */
object SalaryForecastPolicy {
    private val dayMs = TimeUnit.DAYS.toMillis(1)

    fun evaluate(facts: SalaryForecastFacts): SalaryForecastResult {
        val confirmed = facts.confirmedPaydayEpochMs != null &&
            (facts.confirmedSalaryAmountMinor ?: 0L) > 0L
        val from = if (confirmed) facts.confirmedPaydayEpochMs else facts.expectedWindow?.startEpochMs
        val to = if (confirmed) facts.confirmedPaydayEpochMs else facts.expectedWindow?.endEpochMs
        val status = if (confirmed) SalaryForecastStatus.CONFIRMED else SalaryForecastStatus.ESTIMATED
        val missing = missingEvidence(facts, from, to)
        val excluded = listOf(
            "Future salary is excluded until it is actually received.",
            "Protected savings, uncertain recurring suggestions and unconfirmed income are excluded."
        )
        if (missing.isNotEmpty()) return SalaryForecastResult(
            SalaryForecastStatus.NEEDS_DATA, from, to, null, null,
            emptyList(), excluded, missing
        )

        return try {
            project(facts, status, requireNotNull(from), requireNotNull(to), excluded)
        } catch (_: ArithmeticException) {
            SalaryForecastResult(SalaryForecastStatus.NEEDS_DATA, from, to, null, null,
                emptyList(), excluded, listOf("A money amount is too large to forecast safely."))
        }
    }

    private fun missingEvidence(facts: SalaryForecastFacts, from: Long?, to: Long?): List<String> =
        buildList {
            if (from == null || to == null) add("Confirm the next payday or set a usual payday window.")
            else if (from > to || to < facts.nowEpochMs) add("The payday has passed. Confirm a new payday or salary receipt.")
            val age = facts.balanceAsOfEpochMs?.let { facts.nowEpochMs - it }
            if (facts.balanceMinor == null || age == null || age < 0L || age > dayMs) {
                add("Confirm a complete fresh spendable balance (within 24 hours).")
            }
            if (facts.cycles.size != 3 || facts.cycles.any { !completeCycle(it, facts.nowEpochMs) }) {
                add("Three complete, reviewed salary cycles of spending are needed.")
            }
            if (facts.hasUnscheduledDebt) add("Confirm loan/Fuliza repayment amounts and due dates.")
            if (facts.hasUnscheduledBills) add("Schedule remaining recurring bills before forecasting.")
            if (facts.commitments.any { it.amountMinor < 0L || it.id.isBlank() }) {
                add("Some commitments are not valid or identifiable.")
            }
        }

    private fun completeCycle(cycle: SalarySpendingCycle, now: Long): Boolean {
        val end = cycle.endEpochMs ?: return false
        val days = TimeUnit.MILLISECONDS.toDays(end - cycle.startEpochMs)
        return days in 14..45 && end <= now && cycle.unreviewedCount == 0 &&
            cycle.variableSpendingMinor >= 0L
    }

    private fun project(
        facts: SalaryForecastFacts,
        status: SalaryForecastStatus,
        from: Long,
        to: Long,
        excluded: List<String>
    ): SalaryForecastResult {
        val commitments = facts.commitments.groupBy { it.id }.values.map { entries ->
            entries.maxBy { it.amountMinor }
        }
        val highDays = daysUntil(facts.nowEpochMs, maxOf(from, facts.nowEpochMs))
        val lowDays = daysUntil(facts.nowEpochMs, maxOf(to, facts.nowEpochMs))
        val minVariable = facts.cycles.minOf { spendUntil(it, highDays, roundUp = false) }
        val maxVariable = facts.cycles.maxOf { spendUntil(it, lowDays, roundUp = true) }
        val earlyCommitments = sumCommitments(commitments, maxOf(from, facts.nowEpochMs))
        val lateCommitments = sumCommitments(commitments, maxOf(to, facts.nowEpochMs))
        val low = Math.subtractExact(Math.subtractExact(requireNotNull(facts.balanceMinor), lateCommitments), maxVariable)
        val high = Math.subtractExact(Math.subtractExact(requireNotNull(facts.balanceMinor), earlyCommitments), minVariable)
        val included = buildList {
            add(facts.balanceLabel)
            add("Variable spending and fees: three completed salary cycles")
            commitments.filter { it.dueEpochMs == null || it.dueEpochMs in facts.nowEpochMs..to }
                .forEach { add(it.label) }
        }
        return SalaryForecastResult(status, from, to, low, high, included, excluded, emptyList())
    }

    private fun daysUntil(now: Long, target: Long): Long =
        ((target - now).coerceAtLeast(0L) + dayMs - 1L) / dayMs

    private fun spendUntil(cycle: SalarySpendingCycle, days: Long, roundUp: Boolean): Long {
        val cycleDays = TimeUnit.MILLISECONDS.toDays(requireNotNull(cycle.endEpochMs) - cycle.startEpochMs)
        val numerator = BigInteger.valueOf(cycle.variableSpendingMinor).multiply(BigInteger.valueOf(days))
        val adjusted = if (roundUp) numerator.add(BigInteger.valueOf(cycleDays - 1L)) else numerator
        return adjusted.divide(BigInteger.valueOf(cycleDays)).longValueExact()
    }

    private fun sumCommitments(items: List<SalaryForecastCommitment>, dueThrough: Long): Long =
        items.filter { it.dueEpochMs == null || it.dueEpochMs <= dueThrough }
            .fold(0L) { sum, item -> Math.addExact(sum, item.amountMinor) }
}
