package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HistoryImportWindowTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun threeMonthsStartsWithCurrentMonthAndMovesBackward() {
        val now = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 27, 8, 0, 0)
        }.timeInMillis

        val months = HistoryImportWindow.months(
            count = 3,
            nowEpochMs = now,
            timeZone = utc,
            locale = Locale.ENGLISH
        )

        assertEquals(
            listOf("September 2026", "August 2026", "July 2026"),
            months.map { it.label }
        )
    }

    @Test
    fun oneMonthContainsOnlyCurrentMonth() {
        val now = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.APRIL, 10, 8, 0, 0)
        }.timeInMillis

        assertEquals(
            listOf("April 2026"),
            HistoryImportWindow.months(1, now, utc, Locale.ENGLISH).map { it.label }
        )
    }

    @Test
    fun zeroMonthsMeansNoHistoryImport() {
        assertEquals(emptyList<HistoryMonth>(), HistoryImportWindow.months(0))
        assertNull(HistoryImportWindow.importStartEpochMs(0))
    }

    @Test
    fun manualSalaryDayMustExistInsideSelectedMonth() {
        val february = HistoryImportWindow.months(
            count = 1,
            nowEpochMs = Calendar.getInstance(utc).apply {
                clear()
                set(2026, Calendar.FEBRUARY, 10)
            }.timeInMillis,
            timeZone = utc,
            locale = Locale.ENGLISH
        ).first()

        assertNull(HistoryImportWindow.epochForDay(february, 30, utc))
    }
}
