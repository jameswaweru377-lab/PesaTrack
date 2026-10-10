package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingBarPolicyTest {
    @Test fun emptyAndAllZeroHaveNoBars() {
        assertEquals(emptyList<Int>(), spendingBarHeightsDp(emptyList()))
        assertEquals(listOf(0, 0, 0, 0, 0), spendingBarHeightsDp(List(5) { 0L }))
    }

    @Test fun mixedSpendingKeepsZeroInvisibleAndShowsRelativePositiveValues() {
        assertEquals(listOf(0, 44, 88, 0), spendingBarHeightsDp(listOf(0, 5_000, 10_000, 0)))
    }

    @Test fun negativeAdjustmentsDoNotBecomePositiveSpendingBars() {
        assertEquals(listOf(0, 0, 88), spendingBarHeightsDp(listOf(-5_000, 0, 9_000)))
        assertEquals(listOf(0, 0), spendingBarHeightsDp(listOf(-1, -9)))
    }

    @Test fun veryLargeValuesDoNotOverflowDuringNormalization() {
        assertEquals(listOf(88, 0), spendingBarHeightsDp(listOf(Long.MAX_VALUE, 0)))
        assertEquals(listOf(88, 44), spendingBarHeightsDp(listOf(Long.MAX_VALUE, Long.MAX_VALUE / 2)))
    }
}
