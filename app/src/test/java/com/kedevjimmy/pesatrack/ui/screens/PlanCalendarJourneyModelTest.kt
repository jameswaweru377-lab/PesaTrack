package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PlanCalendarJourneyModelTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")

    private fun timestamp(day: Int, hour: Int = 12): Long = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, 0, 0)
    }.timeInMillis

    @Test fun weekStartsOnMondayAndMarksTodayAndScheduledDays() {
        val week = planCalendarJourneyWeek(
            todayEpochMs = timestamp(6),
            dueEpochMs = listOf(timestamp(8, 18), timestamp(12, 6)),
            locale = Locale.ENGLISH,
            timeZone = zone
        )
        assertEquals(listOf("2", "3", "4", "5", "6", "7", "8"),
            week.map { it.dayOfMonth })
        assertEquals("Mon", week.first().weekday)
        assertTrue(week[4].isToday)
        assertFalse(week[1].hasBill)
        assertTrue(week.last().hasBill)
        assertFalse(week[4].hasBill)
        assertEquals(1, week.count { it.isToday })
    }

    @Test fun weekCrossesMonthAndYearWithoutRepeatingDates() {
        val start = Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.DECEMBER, 30, 12, 0, 0)
        }.timeInMillis
        val week = planCalendarJourneyWeek(start, emptyList(), Locale.ENGLISH, zone)
        assertEquals(listOf("28", "29", "30", "31", "1", "2", "3"),
            week.map { it.dayOfMonth })
        assertEquals("Dec", week.first().month)
        assertEquals("Jan", week[4].month)
        assertTrue(week[2].isToday)
        assertTrue(week.none { it.hasBill })
    }

    @Test fun allocationVisualShowsProportionsAndWithholdsInvalidInput() {
        assertEquals(listOf(33, 17, 50),
            planJourneyDisplayedShares(1_800_000, 960_000, 2_740_000))
        assertEquals(null, planJourneyDisplayedShares(0, 0, 0))
        assertEquals(null, planJourneyDisplayedShares(-1, 10, 20))
        assertEquals(100, planJourneyDisplayedShares(Long.MAX_VALUE, Long.MAX_VALUE,
            Long.MAX_VALUE)?.sum())
    }

    @Test fun upcomingBillsRemainSeparateFromAllPlanningCommitments() {
        assertEquals(300_000L, planUpcomingBillsTotalMinor(listOf(100_000L, 200_000L)))
        assertEquals(0L, planUpcomingBillsTotalMinor(emptyList()))
        assertEquals(null, planUpcomingBillsTotalMinor(listOf(0L, -1L)))
        assertEquals(null, planUpcomingBillsTotalMinor(listOf(Long.MAX_VALUE, 1L)))
    }
}
