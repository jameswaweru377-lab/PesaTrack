package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class PaydayCarryoverPolicyTest {
    @Test
    fun firstCycleUsesKnownPaydayCashEvenWhenPictureIsPartial() {
        val result =
            PaydayCarryoverPolicy.resolve(
                cycleIndex = 0,
                payday =
                    PaydayCarryoverEvidence(
                        knownLiquidMinor = 4_580_00L,
                        knownLiquidAccountCount = 2,
                        unknownLiquidAccountCount = 1
                    ),
                previousTrackedEndingMinor = null
            )

        assertEquals(
            4_580_00L,
            result
        )
    }

    @Test
    fun completePaydayPictureOverridesLedgerCarryover() {
        val result =
            PaydayCarryoverPolicy.resolve(
                cycleIndex = 2,
                payday =
                    PaydayCarryoverEvidence(
                        knownLiquidMinor = 18_000_00L,
                        knownLiquidAccountCount = 2,
                        unknownLiquidAccountCount = 0
                    ),
                previousTrackedEndingMinor =
                    13_500_00L
            )

        assertEquals(
            18_000_00L,
            result
        )
    }

    @Test
    fun laterPartialPictureKeepsLedgerCarryover() {
        val result =
            PaydayCarryoverPolicy.resolve(
                cycleIndex = 2,
                payday =
                    PaydayCarryoverEvidence(
                        knownLiquidMinor = 8_000_00L,
                        knownLiquidAccountCount = 1,
                        unknownLiquidAccountCount = 1
                    ),
                previousTrackedEndingMinor =
                    11_200_00L
            )

        assertEquals(
            11_200_00L,
            result
        )
    }

    @Test
    fun missingEvidenceFallsBackToZeroOnlyForUnseededFirstCycle() {
        val result =
            PaydayCarryoverPolicy.resolve(
                cycleIndex = 0,
                payday = null,
                previousTrackedEndingMinor = null
            )

        assertEquals(0L, result)
    }

    @Test
    fun negativeKnownCarryoverIsPreserved() {
        val result =
            PaydayCarryoverPolicy.resolve(
                cycleIndex = 0,
                payday =
                    PaydayCarryoverEvidence(
                        knownLiquidMinor = -2_000_00L,
                        knownLiquidAccountCount = 1,
                        unknownLiquidAccountCount = 0
                    ),
                previousTrackedEndingMinor = null
            )

        assertEquals(
            -2_000_00L,
            result
        )
    }
}
