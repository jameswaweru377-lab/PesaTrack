package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ActivityPremiumPeriodPolicyTest {
    private fun millis(year:Int, month:Int, day:Int):Long =
        Calendar.getInstance().apply {
            clear();set(year,month-1,day,12,0)
        }.timeInMillis

    @Test fun noRemainingDayClaimForHistoricalMonths() {
        assertNull(monthDaysRemaining(millis(2026,9,1),millis(2026,10,17)))
        assertNull(monthDaysRemaining(millis(2025,10,1),millis(2026,10,17)))
    }
    @Test fun remainingDaysUsesCalendarLength() {
        assertEquals(14,monthDaysRemaining(millis(2026,10,1),millis(2026,10,17)))
        assertEquals(0,monthDaysRemaining(millis(2026,10,1),millis(2026,10,31)))
        assertEquals(1,monthDaysRemaining(millis(2028,2,1),millis(2028,2,28)))
    }
    @Test fun chosenCalendarOrSalaryPeriodIsDisplayedWithoutADropdown() {
        assertEquals("October 2026",
            approvedMoneyCardPeriodLabel("October 2026", salaryMode = false))
        assertEquals("November 2025 salary cycle",
            approvedMoneyCardPeriodLabel("November 2025", salaryMode = true))
        assertEquals("September 2026 salary cycle",
            approvedMoneyCardPeriodLabel("September 2026", salaryMode = true))
    }

    @Test fun approvedCardScalesToReferenceImageProportions() {
        // Image measurement: roughly 1,277 px wide by 620 px high.
        assertEquals(177f, approvedMoneyCardHeight(365f), 1f)
        assertEquals(166f, approvedMoneyCardHeight(300f), .001f)
        assertEquals(206f, approvedMoneyCardHeight(500f), .001f)
        assertTrue(APPROVED_MONEY_CARD_ASPECT_RATIO > 2f)
    }

    @Test fun changeComparisonDoesNotInventPriorData() {
        assertNull(periodPercentChange(500,0))
        assertNull(periodPercentChange(500,-1))
        assertEquals(25,periodPercentChange(12500,10000))
        assertEquals(-20,periodPercentChange(8000,10000))
        assertEquals(0,periodPercentChange(10000,10000))
    }
}