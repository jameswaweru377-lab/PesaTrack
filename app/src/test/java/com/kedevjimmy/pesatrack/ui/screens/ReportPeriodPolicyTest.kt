package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReportPeriodPolicyTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")

    @Test fun `month scope uses local calendar boundaries and previous month`() {
        val range = reportPeriodRange(ReportPeriod.MONTH, epoch(2026, 11, 4), zone)
        assertEquals(epoch(2026, 11, 1), range.startEpochMs)
        assertEquals(epoch(2026, 12, 1), range.endExclusiveEpochMs)
        assertEquals(epoch(2026, 10, 1), range.previousStartEpochMs)
        assertEquals("November 2026", range.label)
    }

    @Test fun `six month scope includes selected calendar month and compares equal duration`() {
        val range = reportPeriodRange(ReportPeriod.SIX_MONTHS, epoch(2026, 11, 4), zone)
        assertEquals(epoch(2026, 6, 1), range.startEpochMs)
        assertEquals(epoch(2026, 12, 1), range.endExclusiveEpochMs)
        assertEquals(epoch(2025, 12, 1), range.previousStartEpochMs)
    }

    @Test fun `year and week follow calendar boundaries not fixed day durations`() {
        val year = reportPeriodRange(ReportPeriod.YEAR, epoch(2024, 2, 29), zone)
        assertEquals(epoch(2024, 1, 1), year.startEpochMs)
        assertEquals(epoch(2025, 1, 1), year.endExclusiveEpochMs)
        assertEquals(epoch(2023, 1, 1), year.previousStartEpochMs)

        val week = reportPeriodRange(ReportPeriod.WEEK, epoch(2026, 11, 4), zone)
        assertTrue(week.startEpochMs < epoch(2026, 11, 4))
        assertEquals(7L * 24 * 60 * 60 * 1000, week.endExclusiveEpochMs - week.startEpochMs)
        assertEquals(7L * 24 * 60 * 60 * 1000, week.startEpochMs - week.previousStartEpochMs)
    }

    private fun epoch(year: Int, month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

    @Test fun `comparison is unavailable without an actual previous spending baseline`() {
        assertNull(reportSpendingChangePercent(10_000, 0))
        assertEquals(-25, reportSpendingChangePercent(15_000, 20_000))
        assertEquals(50, reportSpendingChangePercent(30_000, 20_000))
    }

    @Test fun `trend buckets stay inside expected period and preserve six months`() {
        val months = reportTrendBuckets(ReportPeriod.SIX_MONTHS, epoch(2026, 11, 4), zone)
        assertEquals(6, months.size)
        assertEquals(epoch(2026, 6, 1), months.first().startEpochMs)
        assertEquals(epoch(2026, 12, 1), months.last().endExclusiveEpochMs)

        val year = reportTrendBuckets(ReportPeriod.YEAR, epoch(2024, 2, 29), zone)
        assertEquals(12, year.size)
        assertEquals(epoch(2024, 1, 1), year.first().startEpochMs)
        assertEquals(epoch(2025, 1, 1), year.last().endExclusiveEpochMs)
    }
}
