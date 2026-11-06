package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SelectedPlanSubpagePolicyTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")

    private fun date(month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(2026, month, day, 12, 0, 0)
    }.timeInMillis

    @Test fun budgetRingRoundsPercentageAndDoesNotPretendZeroLimitIsSpent() {
        assertEquals(59, selectedBudgetSpentPercent(5_300_000, 3_120_000))
        assertEquals(100, selectedBudgetSpentPercent(1_000, 1_200))
        assertNull(selectedBudgetSpentPercent(0, 0))
        assertNull(selectedBudgetSpentPercent(-1, 0))
        assertNull(selectedBudgetSpentPercent(1_000, -1))
    }

    @Test fun milestonesAreBasedOnlyOnStoredGoalTargetAndRecordedSavings() {
        assertEquals(5_000_000L, selectedGoalNextMilestone(10_000_000, 4_600_000))
        assertEquals(10_000_000L, selectedGoalNextMilestone(10_000_000, 8_500_000))
        assertNull(selectedGoalNextMilestone(0, 10))
        assertNull(selectedGoalNextMilestone(10_000_000, 10_000_000))
    }

    @Test fun billCalendarStartsMondayAndMarksActualDueDaysOnly() {
        val dates = selectedBillsFortnight(
            viewedMonthEpochMs = date(Calendar.NOVEMBER, 1),
            todayEpochMs = date(Calendar.NOVEMBER, 6),
            dueEpochMs = listOf(date(Calendar.NOVEMBER, 8), date(Calendar.NOVEMBER, 12)),
            locale = Locale.ENGLISH,
            timeZone = zone
        )
        assertEquals(14, dates.size)
        assertEquals("Mon", dates.first().weekday)
        assertEquals("2", dates.first().dayOfMonth)
        assertTrue(dates[4].isToday)
        assertTrue(dates[6].hasBill)
        assertTrue(dates[10].hasBill)
        assertFalse(dates[7].hasBill)
    }

    @Test fun archivedBillMonthDoesNotHighlightToday() {
        val dates = selectedBillsFortnight(
            viewedMonthEpochMs = date(Calendar.DECEMBER, 1),
            todayEpochMs = date(Calendar.NOVEMBER, 6),
            dueEpochMs = emptyList(),
            locale = Locale.ENGLISH,
            timeZone = zone
        )
        assertEquals("30", dates.first().dayOfMonth)
        assertTrue(dates.none { it.isToday || it.hasBill })
    }

    @Test fun billDueLabelUsesCalendarDaysNotElapsedTwentyFourHours() {
        val due = date(Calendar.NOVEMBER, 8)
        val lateOnSixth = date(Calendar.NOVEMBER, 6) + 10 * 60 * 60 * 1_000L
        assertEquals(2L, selectedBillDaysUntil(due, lateOnSixth, zone))
        assertEquals(0L, selectedBillDaysUntil(due, due + 5_000L, zone))
    }
}
