package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMoneyDisplayTest {
    @Test fun wholeShillingsAreCompact() {
        assertEquals("KES 0", formatBudgetMoney(0L))
        assertEquals("KES 2,500", formatBudgetMoney(250_000L))
    }

    @Test fun centsStayVisibleAndAreNotRoundedAway() {
        assertEquals("KES 0.01", formatBudgetMoney(1L))
        assertEquals("KES 2,500.05", formatBudgetMoney(250_005L))
        assertEquals("-KES 2,500.05", formatBudgetMoney(-250_005L))
    }

    @Test fun extremeLongValuesRemainExact() {
        assertEquals("KES 92,233,720,368,547,758.07", formatBudgetMoney(Long.MAX_VALUE))
        assertEquals("-KES 92,233,720,368,547,758.08", formatBudgetMoney(Long.MIN_VALUE))
    }
}
