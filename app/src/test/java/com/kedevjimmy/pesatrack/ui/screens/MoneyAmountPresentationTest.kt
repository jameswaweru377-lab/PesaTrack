package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyAmountPresentationTest {
    @Test fun exactWholeShillingsRemainWhole() {
        assertEquals("KES 0", formatKesAmount(0L))
        assertEquals("KES 10,000", formatKesAmount(1_000_000L))
        assertEquals("-KES 1", formatKesAmount(-100L))
    }

    @Test fun centsAreNeverSilentlyRoundedAway() {
        assertEquals("KES 0.01", formatKesAmount(1L))
        assertEquals("KES 1,234.05", formatKesAmount(123_405L))
        assertEquals("-KES 1,234.05", formatKesAmount(-123_405L))
        assertEquals("-0.01", formatKesNumber(-1L))
    }

    @Test fun extremalMinorUnitsAvoidOverflowAndFloatingPointLoss() {
        assertEquals("KES 92,233,720,368,547,758.07", formatKesAmount(Long.MAX_VALUE))
        assertEquals("-KES 92,233,720,368,547,758.08", formatKesAmount(Long.MIN_VALUE))
    }
}
