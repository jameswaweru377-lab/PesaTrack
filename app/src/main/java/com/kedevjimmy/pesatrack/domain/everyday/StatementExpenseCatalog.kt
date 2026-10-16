package com.kedevjimmy.pesatrack.domain.everyday

/**
 * Expense categories are named individually. A roll-up such as Food & dining
 * may be useful on a high-level card, but MUST NOT hide Eating out or Groceries
 * in Categories or in the corresponding expense transaction drill-down.
 */
object StatementExpenseCatalog {
    fun name(id: String?, labels: Map<String, String>): String =
        when {
            id == null -> "Needs review"
            !labels[id].isNullOrBlank() -> requireNotNull(labels[id])
            else -> id.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }

    fun breakdown(
        rows: List<StatementClassified>, labels: Map<String, String>
    ): List<Pair<String, Long>> =
        rows.asSequence().filter { it.flow == StatementFlow.EXPENSE }
            .groupBy { name(it.categoryId, labels) }
            .map { (category, entries) ->
                category to entries.fold(0L) { total, row ->
                    Math.addExact(total, row.reportAmountMinor)
                }
            }.sortedByDescending { it.second }

    fun matchingExpenseRows(
        rows: List<StatementClassified>, category: String?,
        labels: Map<String, String>
    ): List<StatementClassified> = rows.filter {
        it.flow == StatementFlow.EXPENSE &&
            (category == null || name(it.categoryId, labels) == category)
    }
}
