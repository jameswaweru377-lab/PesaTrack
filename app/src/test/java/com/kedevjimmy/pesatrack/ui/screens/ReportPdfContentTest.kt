package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfContentTest {
    @Test fun `pdf summary preserves authoritative ledger totals without recomputing money`() {
        val summary = ActivitySummary(
            incomeMinor = 600_000, spentMinor = 275_000, feesMinor = 1_500,
            transferMinor = 500_000, netMinor = -176_500, needsReviewCount = 2,
            refundMinor = 20_000
        )
        val result = buildReportPdfContent(
            periodLabel = "November 2026",
            summary = summary,
            categories = listOf("Rent" to 200_000L, "Food" to 75_000L),
            trend = listOf(ReportTrendValue("Oct", 300_000), ReportTrendValue("Nov", 275_000)),
            candidates = listOf(candidate(1, 130_000), candidate(2, 12_000).copy(isExpense = false))
        )
        assertEquals(summary.spentMinor, result.spentMinor)
        assertEquals(summary.incomeMinor, result.incomeMinor)
        assertEquals(summary.feesMinor, result.feesMinor)
        assertEquals(summary.transferMinor, result.transferMinor)
        assertEquals(1, result.largestExpenses.size)
        assertEquals(130_000L, result.largestExpenses.single().amountMinor)
        assertTrue(result.coverageNote.contains("trusted Activity"))
    }

    @Test fun `pdf content handles empty categories and no expense observations`() {
        val result = buildReportPdfContent(
            "Week", ActivitySummary(0, 0, 0, 0, 0, 0), emptyList(), emptyList(), emptyList()
        )
        assertEquals(emptyList<ReportPdfCategory>(), result.categories)
        assertEquals(emptyList<ReportPdfExpense>(), result.largestExpenses)
    }

    private fun candidate(id: Long, amount: Long) = ReportReviewCandidate(
        id, "Invented shop", amount, "OTHER", true, false, false
    )
}
