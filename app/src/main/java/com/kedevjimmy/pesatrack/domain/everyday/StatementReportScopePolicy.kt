package com.kedevjimmy.pesatrack.domain.everyday

/** Explicit time selection for one already-selected statement account. */
sealed interface StatementReportScope {
    data object AllHistory : StatementReportScope
    data class Year(val value: Int) : StatementReportScope
    data class Month(val key: String) : StatementReportScope
    data class Week(val anchorMs: Long) : StatementReportScope
    data class Day(val anchorMs: Long) : StatementReportScope
}

object StatementReportScopePolicy {
    private val monthPattern = Regex("[0-9]{4}-(0[1-9]|1[0-2])")

    /** No account combination occurs here: [rows] must already come from StatementReportReader. */
    fun selectRows(
        rows: List<StatementClassified>,
        scope: StatementReportScope
    ): List<StatementClassified> = when (scope) {
        StatementReportScope.AllHistory -> rows
        is StatementReportScope.Year -> {
            require(scope.value in 1900..2200)
            rows.filter { StatementMonthlyUsage.monthKey(it.line.occurredAt)
                .startsWith("${scope.value}-") }
        }
        is StatementReportScope.Month -> {
            require(monthPattern.matches(scope.key))
            rows.filter { StatementMonthlyUsage.monthKey(it.line.occurredAt) == scope.key }
        }
        is StatementReportScope.Week -> rows.filter {
            StatementFinancialPolicy.within(it.line.occurredAt, scope.anchorMs,
                StatementFinancialPolicy.Period.WEEK)
        }
        is StatementReportScope.Day -> rows.filter {
            StatementFinancialPolicy.within(it.line.occurredAt, scope.anchorMs,
                StatementFinancialPolicy.Period.DAY)
        }
    }
}
