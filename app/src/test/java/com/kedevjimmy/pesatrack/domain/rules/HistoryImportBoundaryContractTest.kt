package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** P0/A1: calendar boundaries must never silently omit or shift SMS history. */
class HistoryImportBoundaryContractTest {
    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun oneAndThreeMonthImportsUseExactLocalCalendarStartNotThirtyDayEstimates() {
        val now = date(2026, Calendar.OCTOBER, 8, 19)
        val one = HistoryImportWindow.months(1, now, nairobi, Locale.ENGLISH)
        val three = HistoryImportWindow.months(3, now, nairobi, Locale.ENGLISH)

        assertEquals(listOf("October 2026"), one.map { it.label })
        assertEquals(
            listOf("October 2026", "September 2026", "August 2026"),
            three.map { it.label }
        )
        assertEquals(date(2026, Calendar.OCTOBER, 1, 0), HistoryImportWindow.importStartEpochMs(1, now, nairobi))
        assertEquals(date(2026, Calendar.AUGUST, 1, 0), HistoryImportWindow.importStartEpochMs(3, now, nairobi))
        assertEquals(date(2026, Calendar.NOVEMBER, 1, 0), one.first().endEpochMs)

        // Half-open range [start, end): midnight on the following month is excluded.
        assertFalse(date(2026, Calendar.JULY, 31, 23) >= three.last().startEpochMs)
        assertTrue(date(2026, Calendar.AUGUST, 1, 0) >= three.last().startEpochMs)
        assertFalse(date(2026, Calendar.NOVEMBER, 1, 0) < three.first().endEpochMs)
    }

    @Test
    fun threeMonthWindowCrossesYearBoundaryWithoutSkips() {
        val now = date(2027, Calendar.JANUARY, 15, 11)
        val months = HistoryImportWindow.months(3, now, nairobi, Locale.ENGLISH)
        assertEquals(
            listOf("January 2027", "December 2026", "November 2026"),
            months.map { it.label }
        )
        assertEquals(date(2026, Calendar.NOVEMBER, 1, 0), months.last().startEpochMs)
        assertEquals(date(2027, Calendar.FEBRUARY, 1, 0), months.first().endEpochMs)
        assertEquals(
            months.last().startEpochMs,
            HistoryImportWindow.importStartEpochMs(3, now, nairobi)
        )
    }

    @Test
    fun leapFebruaryIsEntireCalendarMonthAndInvalidDayIsRejected() {
        val now = date(2024, Calendar.MARCH, 2, 12)
        val months = HistoryImportWindow.months(3, now, nairobi, Locale.ENGLISH)
        val feb = months.single { it.month == Calendar.FEBRUARY }
        assertEquals(date(2024, Calendar.FEBRUARY, 1, 0), feb.startEpochMs)
        assertEquals(date(2024, Calendar.MARCH, 1, 0), feb.endEpochMs)
        assertEquals(date(2024, Calendar.FEBRUARY, 29, 12), HistoryImportWindow.epochForDay(feb, 29, nairobi))
        assertEquals(null, HistoryImportWindow.epochForDay(feb, 30, nairobi))
    }

    @Test
    fun unsupportedImportMonthCountsAreRejected() {
        for (count in listOf(-1, 4, 12)) {
            var rejected = false
            try {
                HistoryImportWindow.months(count, date(2026, Calendar.OCTOBER, 8, 12), nairobi)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue("Unexpected accepted month count: $count", rejected)
        }
    }

    private fun date(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(nairobi).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}
