package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryHistoryMonthPolicyTest {
    private val utc =
        TimeZone.getTimeZone("UTC")

    @Test
    fun firstDaysOfOctoberAnchorToSeptemberPaydayMonth() {
        val now =
            Calendar.getInstance(utc)
                .apply {
                    clear()
                    set(
                        2026,
                        Calendar.OCTOBER,
                        1,
                        1,
                        0,
                        0
                    )
                }
                .timeInMillis

        val anchor =
            SalaryHistoryMonthPolicy
                .anchorCalendar(
                    now,
                    utc
                )

        assertEquals(
            2026,
            anchor.get(
                Calendar.YEAR
            )
        )
        assertEquals(
            Calendar.SEPTEMBER,
            anchor.get(
                Calendar.MONTH
            )
        )
    }

    @Test
    fun lateOctoberCanAnchorToOctoberPaydayMonth() {
        val now =
            Calendar.getInstance(utc)
                .apply {
                    clear()
                    set(
                        2026,
                        Calendar.OCTOBER,
                        25,
                        12,
                        0,
                        0
                    )
                }
                .timeInMillis

        val anchor =
            SalaryHistoryMonthPolicy
                .anchorCalendar(
                    now,
                    utc
                )

        assertEquals(
            Calendar.OCTOBER,
            anchor.get(
                Calendar.MONTH
            )
        )
    }
}
