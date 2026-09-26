package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PayCyclePolicyTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun monthEndWindowAllows24thThrough1stAcrossMonthBoundary() {
        assertTrue(PayCyclePolicy.dayInWindow(24, 24, 1))
        assertTrue(PayCyclePolicy.dayInWindow(31, 24, 1))
        assertTrue(PayCyclePolicy.dayInWindow(1, 24, 1))
    }

    @Test
    fun irregularNormalPaydayDoesNotNeedFixedDate() {
        val previous = date(2026, Calendar.SEPTEMBER, 24)
        val current = date(2026, Calendar.OCTOBER, 31)

        assertNull(
            PayCyclePolicy.candidateReason(
                previousCycleStartEpochMs = previous,
                currentSalaryEpochMs = current,
                payWindowStartDay = 24,
                payWindowEndDay = 1,
                timeZone = utc
            )
        )
    }

    @Test
    fun decemberEarlySalaryCanBeFlaggedByOptionalWindow() {
        val previous = date(2026, Calendar.NOVEMBER, 28)
        val current = date(2026, Calendar.DECEMBER, 21)

        assertEquals(
            "This salary arrived outside your usual pay window.",
            PayCyclePolicy.candidateReason(
                previousCycleStartEpochMs = previous,
                currentSalaryEpochMs = current,
                payWindowStartDay = 24,
                payWindowEndDay = 1,
                timeZone = utc
            )
        )
    }

    @Test
    fun earlyDecemberIsAcceptedWhenUserChoosesNoFixedWindow() {
        val previous = date(2026, Calendar.NOVEMBER, 28)
        val current = date(2026, Calendar.DECEMBER, 21)

        assertNull(
            PayCyclePolicy.candidateReason(
                previousCycleStartEpochMs = previous,
                currentSalaryEpochMs = current,
                payWindowStartDay = null,
                payWindowEndDay = null,
                timeZone = utc
            )
        )
    }

    @Test
    fun secondSalaryLikeCreditWithinTwoDaysNeedsConfirmation() {
        val previous = date(2026, Calendar.OCTOBER, 25)
        val current = date(2026, Calendar.OCTOBER, 27)

        assertTrue(
            PayCyclePolicy.candidateReason(
                previousCycleStartEpochMs = previous,
                currentSalaryEpochMs = current,
                payWindowStartDay = null,
                payWindowEndDay = null,
                timeZone = utc
            )?.contains("2 days") == true
        )
    }

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
