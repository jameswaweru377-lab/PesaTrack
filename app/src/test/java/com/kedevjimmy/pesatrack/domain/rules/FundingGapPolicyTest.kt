package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FundingGapPolicyTest {
    @Test
    fun unrepresentableRecognizedFundingCannotBecomeAFabricatedGap() {
        assertThrows(ArithmeticException::class.java) {
            FundingGapPolicy.unexplainedGap(Long.MAX_VALUE, 1L, 100L, 0L)
        }
    }

    @Test
    fun unrepresentableOutflowCannotBeReportedAsNoGap() {
        assertThrows(ArithmeticException::class.java) {
            FundingGapPolicy.unexplainedGap(0L, 0L, Long.MAX_VALUE, 1L)
        }
    }

    @Test
    fun detectsSpendingAboveRecognizedFunding() {
        assertEquals(
            700_000L,
            FundingGapPolicy.unexplainedGap(
                startingFundsMinor = 5_800_000L,
                additionalIncomeMinor = 0L,
                spentMinor = 6_300_000L,
                feesMinor = 200_000L
            )
        )
    }

    @Test
    fun additionalIncomeCanExplainTheDifference() {
        assertEquals(
            0L,
            FundingGapPolicy.unexplainedGap(
                startingFundsMinor = 5_800_000L,
                additionalIncomeMinor = 700_000L,
                spentMinor = 6_300_000L,
                feesMinor = 200_000L
            )
        )
    }

    @Test
    fun negativeCarryoverDoesNotInventAGapWithoutCurrentOutflow() {
        assertEquals(
            0L,
            FundingGapPolicy.unexplainedGap(
                startingFundsMinor = -500_000L,
                additionalIncomeMinor = 0L,
                spentMinor = 0L,
                feesMinor = 0L
            )
        )
    }

    @Test
    fun spendingWithinRecognizedFundingHasNoGap() {
        assertEquals(
            0L,
            FundingGapPolicy.unexplainedGap(
                startingFundsMinor = 5_800_000L,
                additionalIncomeMinor = 200_000L,
                spentMinor = 4_500_000L,
                feesMinor = 100_000L
            )
        )
    }
}
