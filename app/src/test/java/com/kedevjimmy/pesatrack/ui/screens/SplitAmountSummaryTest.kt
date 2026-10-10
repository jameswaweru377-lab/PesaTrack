package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

class SplitAmountSummaryTest {
    @Test fun exactAllocationCanSave() {
        val result = SplitAmountSummary.from(10000, listOf("food" to "60", "travel" to "40"))
        assertTrue(result.canSave)
        assertEquals(BigInteger.ZERO, result.remainingMinor)
    }
    @Test fun incompleteAndMalformedAllocationsCannotSave() {
        for (amount in listOf("", "x", "40.001", "-40", "0")) {
            assertFalse(SplitAmountSummary.from(10000, listOf("food" to "60", "travel" to amount)).canSave)
        }
        assertEquals(BigInteger.valueOf(4000), SplitAmountSummary.from(10000, listOf("food" to "60", "travel" to "")).remainingMinor)
    }
    @Test fun duplicatesAndOverAllocationCannotSave() {
        assertFalse(SplitAmountSummary.from(10000, listOf("food" to "60", "food" to "40")).canSave)
        val excess = SplitAmountSummary.from(10000, listOf("food" to "60", "travel" to "50"))
        assertFalse(excess.canSave)
        assertEquals(BigInteger.valueOf(-1000), excess.remainingMinor)
    }
    @Test fun largeAllocationsDoNotWrapIntoValidTotals() {
        val result = SplitAmountSummary.from(100, listOf("food" to "92233720368547758.07", "travel" to "92233720368547758.07"))
        assertFalse(result.canSave)
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO), result.allocatedMinor)
    }
}
