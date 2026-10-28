package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.everyday.*

/** Immutable screen input, deliberately unrelated to Android's lifecycle ViewModel. */
internal data class StatementInsightViewModel(
    val accountLabel: String,
    val scope: StatementReportScope,
    val metrics: StatementInsightMetrics,
    val chartYear: Int,
    val monthlySpending: List<StatementMonthSpend>,
    val categories: List<Pair<String, Long>>,
    val sourceMonths: List<StatementHistoryReport.SelectedMonth>,
    val hasUnclassifiedIncome: Boolean,
    val hasMissingMonths: Boolean
)

internal object StatementInsightModels {
    fun from(
        accountLabel: String,
        scope: StatementReportScope,
        visible: List<StatementClassified>,
        selectedMonths: List<StatementHistoryReport.SelectedMonth>,
        categoryLabels: Map<String, String>,
        accountRows: List<StatementClassified> = visible
    ): StatementInsightViewModel {
        val year = when (scope) {
            is StatementReportScope.Year -> scope.value
            is StatementReportScope.Month -> scope.key.substringBefore('-').toInt()
            else -> selectedMonths.maxOfOrNull { it.usage.year }
                ?: visible.maxOfOrNull {
                    StatementMonthlyUsage.monthKey(it.line.occurredAt).substringBefore('-').toInt()
                } ?: 2025
        }
        val metrics = StatementInsightProjection.metrics(visible)
        val bars = StatementInsightProjection.yearBars(accountRows, selectedMonths, year)
        return StatementInsightViewModel(
            accountLabel = accountLabel,
            scope = scope,
            metrics = metrics,
            chartYear = year,
            monthlySpending = bars,
            categories = StatementExpenseCatalog.breakdown(visible, categoryLabels),
            sourceMonths = selectedMonths,
            hasUnclassifiedIncome = metrics.unknownInMinor > 0L,
            hasMissingMonths = bars.any { !it.imported }
        )
    }
}
