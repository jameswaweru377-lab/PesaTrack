package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PlanningPeriodResolverTest {
    @Test
    fun monthEndSalaryCanFundFollowingMonth() {
        val zone = TimeZone.getTimeZone("Africa/Nairobi")
        val calendar = Calendar.getInstance(zone).apply {
            set(2026, Calendar.SEPTEMBER, 28, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals(
            "2026-10",
            PlanningPeriodResolver.resolve(
                observedAtEpochMs = calendar.timeInMillis,
                monthOffset = 1,
                timeZone = zone
            )
        )
    }

    @Test
    fun sameMonthRuleKeepsObservedMonth() {
        val zone = TimeZone.getTimeZone("Africa/Nairobi")
        val calendar = Calendar.getInstance(zone).apply {
            set(2026, Calendar.SEPTEMBER, 28, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals(
            "2026-09",
            PlanningPeriodResolver.resolve(
                observedAtEpochMs = calendar.timeInMillis,
                monthOffset = 0,
                timeZone = zone
            )
        )
    }
}
