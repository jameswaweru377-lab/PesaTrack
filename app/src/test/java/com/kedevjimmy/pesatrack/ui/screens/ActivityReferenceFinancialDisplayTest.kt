package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityReferenceFinancialDisplayTest {
    @Test fun wholeShillingAmountsRemainReadable() {
        assertEquals("KES 0", activityKesLabel(0))
        assertEquals("KES 100", activityKesLabel(10_000))
        assertEquals("KES 9,999", activityKesLabel(999_900))
        assertEquals("KES 51,057", activityKesLabel(5_105_700))
        assertEquals("KES 999,999", activityKesLabel(99_999_900))
        assertEquals("KES 1,250,000", activityKesLabel(125_000_000))
    }

    @Test fun minorUnitSubtotalsReconcileExactlyWhenCentsArePresent() {
        // Individually truncated labels would incorrectly sum to KES 51,056.
        val parts = listOf(4_238_225L, 757_025L, 60_425L, 50_025L)
        val total = parts.sum()
        assertEquals(5_105_700L, total)
        assertEquals("KES 42,382.25", activityKesLabel(parts[0], true))
        assertEquals("KES 7,570.25", activityKesLabel(parts[1], true))
        assertEquals("KES 604.25", activityKesLabel(parts[2], true))
        assertEquals("KES 500.25", activityKesLabel(parts[3], true))
        assertEquals("KES 51,057.00", activityKesLabel(total, true))
    }

    @Test fun lowButNonzeroPercentageIsNotMisreportedAsZero() {
        assertEquals("0%", activityShareLabel(0L, 10_000L))
        assertEquals("<1%", activityShareLabel(1L, 10_000L))
        assertEquals("1%", activityShareLabel(100L, 10_000L))
        assertEquals("100%", activityShareLabel(10_000L, 10_000L))
    }
}
