package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OnboardingHistoryWindowTest {
    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")
    private val utc = TimeZone.getTimeZone("UTC")

    private fun epoch(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    @Test fun octoberSelectedPreviousMonthsIncludeCurrentWithoutDroppingJuly() {
        val now = epoch(nairobi, 2026, Calendar.OCTOBER, 10, 20)
        assertEquals(
            epoch(nairobi, 2026, Calendar.SEPTEMBER, 1),
            OnboardingHistoryWindow.importStartEpochMs(1, now, nairobi)
        )
        assertEquals(
            epoch(nairobi, 2026, Calendar.AUGUST, 1),
            OnboardingHistoryWindow.importStartEpochMs(2, now, nairobi)
        )
        assertEquals(
            epoch(nairobi, 2026, Calendar.JULY, 1),
            OnboardingHistoryWindow.importStartEpochMs(3, now, nairobi)
        )
    }

    @Test fun optingOutDoesNotScanEvenCurrentMonth() {
        assertNull(OnboardingHistoryWindow.importStartEpochMs(0))
    }

    @Test fun januaryCrossesCalendarYearAndNormalizesToMidnight() {
        val now = epoch(nairobi, 2027, Calendar.JANUARY, 29, 15)
        assertEquals(
            epoch(nairobi, 2026, Calendar.OCTOBER, 1),
            OnboardingHistoryWindow.importStartEpochMs(3, now, nairobi)
        )
    }

    @Test fun leapFebruaryAndTimezoneBoundariesUseCalendarArithmeticNotThirtyDayEstimates() {
        val now = epoch(utc, 2024, Calendar.MARCH, 31, 23)
        assertEquals(
            epoch(utc, 2024, Calendar.FEBRUARY, 1),
            OnboardingHistoryWindow.importStartEpochMs(1, now, utc)
        )
        assertEquals(
            epoch(utc, 2023, Calendar.DECEMBER, 1),
            OnboardingHistoryWindow.importStartEpochMs(3, now, utc)
        )
    }

    @Test fun priorMonthChoiceCannotExceedProductLimit() {
        for (bad in listOf(-1, 4, 12)) {
            assertThrows(IllegalArgumentException::class.java) {
                OnboardingHistoryWindow.importStartEpochMs(bad)
            }
        }
    }

    @Test fun olderHistoryWindowKeepsItsExistingCurrentMonthInclusiveMeaning() {
        val now = epoch(nairobi, 2026, Calendar.OCTOBER, 10, 15)
        assertEquals(
            epoch(nairobi, 2026, Calendar.AUGUST, 1),
            HistoryImportWindow.importStartEpochMs(3, now, nairobi)
        )
        assertEquals(
            epoch(nairobi, 2026, Calendar.JULY, 1),
            OnboardingHistoryWindow.importStartEpochMs(3, now, nairobi)
        )
    }
}
