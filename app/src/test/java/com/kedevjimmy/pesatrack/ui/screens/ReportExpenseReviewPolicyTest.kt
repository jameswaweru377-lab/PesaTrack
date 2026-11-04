package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportExpenseReviewPolicyTest {
    @Test fun `largest review contains only eligible real expense legs and stable ties`() {
        val rows = listOf(
            row(7, 500), row(6, 800), row(3, 800),
            row(2, 900).copy(isExpense = false),
            row(9, 900).copy(isRefund = true),
            row(8, 950).copy(hasAllocations = true),
            row(5, 0)
        )
        assertEquals(listOf(3L, 6L, 7L), reportLargestExpenses(rows).map { it.id })
    }

    @Test fun `uncategorized review never silently includes Other or excluded money`() {
        val rows = listOf(
            row(1, 500).copy(category = "UNCATEGORIZED"),
            row(2, 900),
            row(3, 950).copy(category = "UNCATEGORIZED", isRefund = true),
            row(4, 300).copy(category = "UNCATEGORIZED")
        )
        assertEquals(listOf(1L, 4L), reportUncategorizedExpenses(rows).map { it.id })
        assertEquals(listOf(2L, 1L, 4L), reportBroadCategoryExpenses(rows).map { it.id })
    }

    private fun row(id: Long, amount: Long) = ReportReviewCandidate(
        id = id,
        counterparty = "Invented merchant",
        amountMinor = amount,
        category = "OTHER",
        isExpense = true,
        isRefund = false,
        hasAllocations = false
    )
}
