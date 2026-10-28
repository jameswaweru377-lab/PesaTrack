package com.kedevjimmy.pesatrack.domain.everyday

/** A null total means the user has not imported evidence for that month. */
data class StatementMonthSpend(
    val year: Int,
    val month: Int,
    val spentMinor: Long?,
    val imported: Boolean
)

data class StatementInsightMetrics(
    val spentMinor: Long,
    val incomeMinor: Long,
    val moneyInMinor: Long,
    val moneyOutMinor: Long,
    val unknownInMinor: Long,
    val unknownOutMinor: Long
)

/** Pure read-only projection. Both inputs must belong to the same selected account. */
object StatementInsightProjection {
    fun metrics(rows: List<StatementClassified>): StatementInsightMetrics {
        val classified = StatementFinancialPolicy.totals(rows)
        val movement = StatementFinancialPolicy.sourceCashFlow(rows)
        return StatementInsightMetrics(
            spentMinor = classified.expenseMinor,
            incomeMinor = classified.incomeMinor,
            moneyInMinor = movement.moneyInMinor,
            moneyOutMinor = movement.moneyOutMinor,
            unknownInMinor = classified.unknownInMinor,
            unknownOutMinor = classified.unknownOutMinor
        )
    }

    /** A full calendar year, with unknown (null) instead of manufactured zeroes. */
    fun yearBars(
        rows: List<StatementClassified>,
        months: List<StatementHistoryReport.SelectedMonth>,
        year: Int
    ): List<StatementMonthSpend> {
        require(year in 1900..2200)
        val selected = months.filter { it.usage.year == year }
            .associateBy { it.usage.month }
        val spending = rows.asSequence()
            .filter { it.flow == StatementFlow.EXPENSE }
            .filter { StatementMonthlyUsage.monthKey(it.line.occurredAt).startsWith("$year-") }
            .groupBy { StatementMonthlyUsage.monthKey(it.line.occurredAt).substring(5).toInt() }
            .mapValues { (_, list) -> list.fold(0L) { acc, row ->
                Math.addExact(acc, row.reportAmountMinor)
            } }
        return (1..12).map { month ->
            val imported = month in selected
            StatementMonthSpend(year, month,
                spentMinor = if (imported) spending[month] ?: 0L else null,
                imported = imported)
        }
    }
}
