package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class CrossSourceTotalsPreviewPolicyTest {
    @Test fun independentScopesAreNeverNetOrCombined() {
        val preview = CrossSourceTotalsPreviewPolicy.project(
            CrossSourceTotalsPreviewPolicy.ActivityTotals(12_000, 8_000),
            StatementUnifiedOverviewPolicy.CashSummary(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, 17_000, 15_000),
            2, 1)
        assertEquals(12_000L, preview.activity?.incomeMinor)
        assertEquals(17_000L, preview.statementMoneyInMinor)
        assertEquals(1, preview.unresolvedCount)
        assertFalse(preview.comparable)
    }

    @Test fun unknownCoverageRemainsUnknownNotZero() {
        val preview = CrossSourceTotalsPreviewPolicy.project(null,
            StatementUnifiedOverviewPolicy.CashSummary(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW), 0, 0)
        assertNull(preview.activity)
        assertNull(preview.statementMoneyInMinor)
        assertNull(preview.statementMoneyOutMinor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAmountsCannotAppearAsValidTotals() {
        CrossSourceTotalsPreviewPolicy.project(null,
            StatementUnifiedOverviewPolicy.CashSummary(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, -1, 0), 0, 0)
    }
}
