package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SalaryExpectationPolicyTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun nextWindow_movesNormalLateMonthPaydayForwardOneMonth() {
        val window = SalaryExpectationPolicy.nextWindow(
            lastSalaryEpochMs = epoch(2026, Calendar.AUGUST, 25),
            payWindowStartDay = 24,
            payWindowEndDay = 28,
            timeZone = utc
        )

        assertNotNull(window)
        assertEquals(
            epoch(2026, Calendar.SEPTEMBER, 24),
            window!!.startEpochMs
        )
        assertEquals(
            epochEnd(2026, Calendar.SEPTEMBER, 28),
            window.endEpochMs
        )
    }

    @Test
    fun crossMonthWindow_handlesSalaryPaidOnFirstOfFollowingMonth() {
        val window = SalaryExpectationPolicy.nextWindow(
            lastSalaryEpochMs = epoch(2026, Calendar.SEPTEMBER, 1),
            payWindowStartDay = 24,
            payWindowEndDay = 1,
            timeZone = utc
        )

        assertNotNull(window)
        assertEquals(
            epoch(2026, Calendar.SEPTEMBER, 24),
            window!!.startEpochMs
        )
        assertEquals(
            epochEnd(2026, Calendar.OCTOBER, 1),
            window.endEpochMs
        )
    }

    @Test
    fun crossMonthWindow_handlesSalaryPaidNearWindowStart() {
        val window = SalaryExpectationPolicy.nextWindow(
            lastSalaryEpochMs = epoch(2026, Calendar.AUGUST, 25),
            payWindowStartDay = 24,
            payWindowEndDay = 1,
            timeZone = utc
        )

        assertNotNull(window)
        assertEquals(
            epoch(2026, Calendar.SEPTEMBER, 24),
            window!!.startEpochMs
        )
        assertEquals(
            epochEnd(2026, Calendar.OCTOBER, 1),
            window.endEpochMs
        )
    }

    @Test
    fun overdueWindow_waitsUntilExpectedWindowActuallyEnds() {
        val beforeEnd = SalaryExpectationPolicy.overdueWindow(
            lastSalaryEpochMs = epoch(2026, Calendar.AUGUST, 25),
            payWindowStartDay = 24,
            payWindowEndDay = 28,
            nowEpochMs = epoch(2026, Calendar.SEPTEMBER, 27),
            timeZone = utc
        )
        val afterEnd = SalaryExpectationPolicy.overdueWindow(
            lastSalaryEpochMs = epoch(2026, Calendar.AUGUST, 25),
            payWindowStartDay = 24,
            payWindowEndDay = 28,
            nowEpochMs = epoch(2026, Calendar.SEPTEMBER, 29),
            timeZone = utc
        )

        assertNull(beforeEnd)
        assertNotNull(afterEnd)
    }

    @Test
    fun noFixedPayWindow_neverCreatesExpectedSalaryDeadline() {
        assertNull(
            SalaryExpectationPolicy.overdueWindow(
                lastSalaryEpochMs = epoch(2026, Calendar.AUGUST, 25),
                payWindowStartDay = null,
                payWindowEndDay = null,
                nowEpochMs = epoch(2026, Calendar.SEPTEMBER, 30),
                timeZone = utc
            )
        )
    }

    private fun epoch(
        year: Int,
        month: Int,
        day: Int
    ): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun epochEnd(
        year: Int,
        month: Int,
        day: Int
    ): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
}
