package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Invented amounts in minor units; Activity spending already includes linked refunds. */
class RecordedOutflowPolicyTest {
    @Test fun linkedRefundIsNotSubtractedTwice() {
        // Original purchase 1000, linked refund 200: Activity already reports 800.
        assertEquals(80_000L, RecordedOutflowPolicy.total(80_000L, 0L))
        assertEquals(83_000L, RecordedOutflowPolicy.total(80_000L, 3_000L))
    }

    @Test fun zeroSpendingAndFeesRemainZero() {
        assertEquals(0L, RecordedOutflowPolicy.total(0L, 0L))
    }

    @Test fun overflowingMoneyIsUnavailableRatherThanWrapped() {
        assertNull(RecordedOutflowPolicy.available(Long.MAX_VALUE, 1L))
    }
}
