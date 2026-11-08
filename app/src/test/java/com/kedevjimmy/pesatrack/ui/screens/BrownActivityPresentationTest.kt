package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrownActivityPresentationTest {
    @Test fun segmentedLabelsPreserveTheExistingLedgerFilterKeys() {
        assertEquals(
            listOf("All", "Money In", "Money Out", "Transfers", "Review", "Invested"),
            brownActivityFilters.map { it.label }
        )
        assertEquals(
            listOf("All", "Money In", "Money Out", "Transfers", "Needs Review", "Invested"),
            brownActivityFilters.map { it.key }
        )
    }

    @Test fun reviewTabReusesExistingAttentionMode() {
        assertEquals("Needs Review", brownActivityFilterKey("Review"))
        assertEquals("Transfers", brownActivityFilterKey("Transfers"))
        assertEquals("Invested", brownActivityFilterKey("Invested"))
        assertNull(brownActivityFilterKey("Unknown"))
    }

    @Test fun theHeroNamesTheActualSelectedAccountingPeriod() {
        assertEquals("THIS MONTH", brownActivityPeriodCaption(false))
        assertEquals("THIS PAY CYCLE", brownActivityPeriodCaption(true))
    }
}
