package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Invented shilling minor units; scope is presentation-only. */
class HomeSummaryScopeTest {
    @Test fun historicalMonthRetainsCurrentConfirmedBalanceWhileUpdatingFlow() {
        val current = homeSummaryScope(
            confirmedCycle = true,
            currentAvailableMinor = 1_925_000L,
            selectedNetFlowMinor = 120_000L,
            monthLabel = "November 2026"
        )
        val historical = homeSummaryScope(
            confirmedCycle = true,
            currentAvailableMinor = 1_925_000L,
            selectedNetFlowMinor = -40_000L,
            monthLabel = "October 2026"
        )

        assertEquals(1_925_000L, current.currentAvailableMinor)
        assertEquals(current.currentAvailableMinor, historical.currentAvailableMinor)
        assertEquals(120_000L, current.selectedNetFlowMinor)
        assertEquals(-40_000L, historical.selectedNetFlowMinor)
        assertEquals("net recorded flow · October 2026", historical.netFlowCaption)
    }

    @Test fun verifiedCurrentBalanceIsNotHiddenWhenSalaryCycleIsMissing() {
        val known = homeSummaryScope(
            confirmedCycle = false,
            currentAvailableMinor = 1_925_000L,
            selectedNetFlowMinor = null,
            monthLabel = "September 2026"
        )

        assertEquals(1_925_000L, known.currentAvailableMinor)
        assertNull(known.selectedNetFlowMinor)
        assertEquals("net recorded flow · September 2026", known.netFlowCaption)
    }
}
