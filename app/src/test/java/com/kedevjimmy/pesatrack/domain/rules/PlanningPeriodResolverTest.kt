package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PlanningPeriodResolverTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun monthEndSalaryCanFundFollowingMonth() {
        val calendar = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 29, 18, 0, 0)
        }

        assertEquals(
            "2026-10",
            PlanningPeriodResolver.resolve(
                observedAtEpochMs = calendar.timeInMillis,
                monthOffset = 1,
                timeZone = utc
            )
        )
    }

    @Test
    fun sameMonthIncomeStaysInObservedMonth() {
        val calendar = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 29, 18, 0, 0)
        }

        assertEquals(
            "2026-09",
            PlanningPeriodResolver.resolve(
                observedAtEpochMs = calendar.timeInMillis,
                monthOffset = 0,
                timeZone = utc
            )
        )
    }
}
