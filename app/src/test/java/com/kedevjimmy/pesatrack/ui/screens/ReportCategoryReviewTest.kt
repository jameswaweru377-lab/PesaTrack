package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportCategoryReviewTest {
    @Test fun `highest value broad category expenses come first with stable ties`() {
        val rows = listOf(candidate(3, 900), candidate(2, 900), candidate(1, 100), candidate(4, 200))
        assertEquals(listOf(2L, 3L, 4L), reportCategoryReview(rows).map { it.id })
    }
    @Test fun `specific categories refunds transfers allocations and nonpositive amounts are omitted`() {
        val rows = listOf(candidate(1, 100), candidate(2, 900).copy(category = "FOOD"),
            candidate(3, 900).copy(isExpense = false), candidate(4, 900).copy(isRefund = true),
            candidate(5, 900).copy(hasAllocations = true), candidate(6, 0), candidate(7, -10))
        assertEquals(listOf(1L), reportCategoryReview(rows).map { it.id })
    }
    @Test fun `uncategorized rows are eligible and empty input remains empty`() {
        assertEquals(listOf(1L), reportCategoryReview(listOf(candidate(1, 100).copy(category = "UNCATEGORIZED"))).map { it.id })
        assertEquals(emptyList<ReportReviewCandidate>(), reportCategoryReview(emptyList()))
    }
    private fun candidate(id: Long, amount: Long) = ReportReviewCandidate(id, "Synthetic shop", amount, "OTHER", true, false, false)
}
