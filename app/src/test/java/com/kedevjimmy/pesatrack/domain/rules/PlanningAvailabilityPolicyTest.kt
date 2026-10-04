package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanningAvailabilityPolicyTest {
    @Test
    fun positiveAvailabilityRemainsSpendable() {
        val result =
            PlanningAvailabilityPolicy.from(
                12_500L
            )

        assertEquals(12_500L, result.availableMinor)
        assertEquals(0L, result.shortfallMinor)
    }

    @Test
    fun negativeAvailabilityBecomesShortfallNotNegativeMoney() {
        val result =
            PlanningAvailabilityPolicy.from(
                -9_500_000L
            )

        assertEquals(0L, result.availableMinor)
        assertEquals(9_500_000L, result.shortfallMinor)
    }
}
