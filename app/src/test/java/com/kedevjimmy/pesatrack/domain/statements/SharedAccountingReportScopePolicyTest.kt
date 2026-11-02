package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class SharedAccountingReportScopePolicyTest {
    private val view = SharedAccountingViewPolicy.View(
        year = 2026, activityIncomeMinor = 120_000, activitySpendingMinor = 75_000,
        statementGrossInMinor = 150_000, statementGrossOutMinor = 90_000,
        reviewedInternalTransfersMinor = 40_000, confirmedUniqueCashLegs = 2,
        confirmedInternalTransfers = 1, confirmedSameLegGroups = 0,
        unresolvedEvidenceCount = 1, status = SharedAccountingViewPolicy.Status.REVIEW_REQUIRED
    )

    @Test fun onlyAnnualReportsMayShowAnnualEvidence() {
        assertNull(SharedAccountingReportScopePolicy.forPeriod("Month", 2026, view))
        assertNull(SharedAccountingReportScopePolicy.forPeriod("Week", 2026, view))
        assertSame(view, SharedAccountingReportScopePolicy.forPeriod("Year", 2026, view))
    }

    @Test fun aDifferentYearOrMissingEvidenceMustBeWithheld() {
        assertNull(SharedAccountingReportScopePolicy.forPeriod("Year", 2025, view))
        assertNull(SharedAccountingReportScopePolicy.forPeriod("Year", 2026, null))
    }

    @Test fun evidenceNeverBecomesAnApprovedCombinedTotal() {
        val annual = SharedAccountingReportScopePolicy.forPeriod("Year", 2026, view)!!
        assertFalse(annual.postingAllowed)
        assertFalse(annual.comparableTotals)
        assertEquals(1, annual.unresolvedEvidenceCount)
        assertEquals(SharedAccountingViewPolicy.Status.REVIEW_REQUIRED, annual.status)
    }
}
