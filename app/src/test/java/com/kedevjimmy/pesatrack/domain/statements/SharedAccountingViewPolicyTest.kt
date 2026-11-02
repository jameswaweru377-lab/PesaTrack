package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class SharedAccountingViewPolicyTest {
    private fun cash(status: StatementUnifiedOverviewPolicy.CashStatus,
                     incoming: Long? = null, outgoing: Long? = null) =
        CrossSourceTotalsPreviewPolicy.project(
            CrossSourceTotalsPreviewPolicy.ActivityTotals(120_000, 70_000),
            StatementUnifiedOverviewPolicy.CashSummary(status, incoming, outgoing), 2, 0)

    private fun verifiedTransfer() = StatementReconciledLegProjectionPolicy.Projection(
        observationCount = 3, uniqueCashLegCount = 2, confirmedSameLegGroups = 0,
        confirmedInternalTransfers = 1, confirmedInternalTransferMinor = 50_000,
        ambiguousObservationCount = 0)

    @Test fun commonViewKeepsActivityAndStatementMoneyInSeparateScopes() {
        val shared = SharedAccountingViewPolicy.project(2026,
            cash(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, 150_000, 110_000),
            verifiedTransfer())
        assertEquals(120_000L, shared.activityIncomeMinor)
        assertEquals(70_000L, shared.activitySpendingMinor)
        assertEquals(150_000L, shared.statementGrossInMinor)
        assertEquals(110_000L, shared.statementGrossOutMinor)
        assertEquals(50_000L, shared.reviewedInternalTransfersMinor)
        assertEquals(2, shared.confirmedUniqueCashLegs)
        assertFalse(shared.postingAllowed)
        assertFalse(shared.comparableTotals)
    }

    @Test fun withholdUnverifiedStatementMoneyWithoutInventingZero() {
        val shared = SharedAccountingViewPolicy.project(2026,
            cash(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW), verifiedTransfer())
        assertNull(shared.statementGrossInMinor)
        assertNull(shared.statementGrossOutMinor)
        assertEquals(SharedAccountingViewPolicy.Status.REVIEW_REQUIRED, shared.status)
        assertEquals(120_000L, shared.activityIncomeMinor)
    }

    @Test fun missingActivityDoesNotUseStatementsAsReplacementIncome() {
        val totals = CrossSourceTotalsPreviewPolicy.project(null,
            StatementUnifiedOverviewPolicy.CashSummary(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE,
                100_000, 25_000), 0, 0)
        val shared = SharedAccountingViewPolicy.project(2026, totals, verifiedTransfer())
        assertNull(shared.activityIncomeMinor)
        assertNull(shared.activitySpendingMinor)
        assertEquals(SharedAccountingViewPolicy.Status.REVIEW_REQUIRED, shared.status)
    }

    @Test fun undoResetsSharedTransferPreviewWithoutChangingActivity() {
        val totals = cash(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, 150_000, 110_000)
        val before = SharedAccountingViewPolicy.project(2026, totals, verifiedTransfer())
        val after = SharedAccountingViewPolicy.project(2026, totals, verifiedTransfer().copy(
            confirmedInternalTransfers = 0, confirmedInternalTransferMinor = 0,
            uniqueCashLegCount = 0, observationCount = 0))
        assertEquals(50_000L, before.reviewedInternalTransfersMinor)
        assertEquals(0L, after.reviewedInternalTransfersMinor)
        assertEquals(before.activityIncomeMinor, after.activityIncomeMinor)
        assertEquals(before.statementGrossOutMinor, after.statementGrossOutMinor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidYear() {
        SharedAccountingViewPolicy.project(1969, cash(StatementUnifiedOverviewPolicy.CashStatus.NO_IMPORT), verifiedTransfer())
    }
}
