package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrownActivityPresentationTest {
    @Test fun segmentedLabelsPreserveTheExistingLedgerFilterKeys() {
        assertEquals(
            listOf("All", "Money In", "Money Out", "Transfers", "Review"),
            brownActivityFilters.map { it.label }
        )
        assertEquals(
            listOf("All", "Money In", "Money Out", "Transfers", "Needs Review"),
            brownActivityFilters.map { it.key }
        )
    }

    @Test fun reviewTabReusesExistingAttentionMode() {
        assertEquals("Needs Review", brownActivityFilterKey("Review"))
        assertEquals("Transfers", brownActivityFilterKey("Transfers"))
        assertNull(brownActivityFilterKey("Invested"))
        assertNull(brownActivityFilterKey("Unknown"))
    }

    @Test fun theHeroNamesTheActualSelectedAccountingPeriod() {
        assertEquals("THIS MONTH", brownActivityPeriodCaption(false))
        assertEquals("THIS PAY CYCLE", brownActivityPeriodCaption(true))
    }

    @Test fun compactLedgerExposesThreePrimaryFiltersWithoutLosingAdvancedKinds() {
        assertEquals(listOf("All", "Money In", "Money Out"),
            brownActivityPrimaryFilters.map { it.label })
        assertEquals(listOf("Transfers", "Needs Review"),
            brownActivityAdvancedFilters.map { it.key })
        assertEquals(listOf("Transfers", "Needs Review", "Owed to me", "Repayments"),
            brownActivitySecondaryFilters.map { it.key })
    }

    @Test fun reviewNoticeIsOnlyVisibleWhenAttentionIsPending() {
        assertNull(brownActivityReviewNotice(0))
        assertEquals("1 item needs review", brownActivityReviewNotice(1))
        assertEquals("4 need review", brownActivityReviewNotice(4))
    }

    @Test fun compactOverviewKeepsOnlyIncomeAndRecordedBalance() {
        assertEquals(listOf("Money in", "Balance"),
            brownActivityHeroFooterLabels(partialBalance = false))
        assertEquals(listOf("Money in", "Partial balance"),
            brownActivityHeroFooterLabels(partialBalance = true))
    }
}
