package com.kedevjimmy.pesatrack.domain.intelligence

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class OfflineMoneyPolicyTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")
    private fun date(month: Int, day: Int = 1): Long = Calendar.getInstance(zone).apply {
        clear(); set(2026, month - 1, day, 12, 0, 0)
    }.timeInMillis

    @Test fun completedCalendarMonthsIncludeGapsAndWithdrawalsButExcludePartialMonth() {
        val history = OfflineMoneyPolicy.completedSavings(listOf(
            SavingObservation(date(7), 900L), SavingObservation(date(9), 700L),
            SavingObservation(date(9, 20), -200L), SavingObservation(date(10), 10000L)
        ), date(10, 9), zone)
        assertEquals(listOf(900L, 0L, 500L), history)
    }
    @Test fun oldObservationEstablishesOnlyLastSixCompletedMonths() {
        assertEquals(List(6) { 0L }, OfflineMoneyPolicy.completedSavings(
            listOf(SavingObservation(date(1), 100L)), date(10), zone))
    }
    @Test fun newGoalHasNoInventedHistory() {
        assertTrue(OfflineMoneyPolicy.completedSavings(listOf(SavingObservation(date(10), 100L)), date(10, 9), zone).isEmpty())
        assertEquals(ForecastStatus.INSUFFICIENT_HISTORY, OfflineMoneyPolicy.forecast(1000, 100, listOf(100, 200)).status)
    }
    @Test fun usesObservedMedianWithExplicitRange() {
        val forecast = OfflineMoneyPolicy.forecast(1000, 100, listOf(100, 200, 300))
        assertEquals(SavingForecast(ForecastStatus.ESTIMATE, 5, 3, 9, 3), forecast)
    }
    @Test fun stalledMonthsDoNotDisappear() {
        assertEquals(ForecastStatus.NO_RELIABLE_PROGRESS, OfflineMoneyPolicy.forecast(1000, 100, listOf(-100, 0, 500)).status)
        assertNull(OfflineMoneyPolicy.forecast(1000, 100, listOf(0, 200, 300)).slowestMonths)
    }
    @Test fun reachedGoalDoesNotNeedHistoryAndInvalidTargetsDoNotBecomeReached() {
        assertEquals(ForecastStatus.REACHED, OfflineMoneyPolicy.forecast(100, 200, emptyList()).status)
        assertEquals(ForecastStatus.INSUFFICIENT_HISTORY, OfflineMoneyPolicy.forecast(0, 0, emptyList()).status)
    }
    @Test fun exactArithmeticHandlesLargeAmountsAndBoundsImplausibleHorizons() {
        assertEquals(1, OfflineMoneyPolicy.forecast(Long.MAX_VALUE, 0, List(4) { Long.MAX_VALUE }).typicalMonths)
        assertEquals(ForecastStatus.NO_RELIABLE_PROGRESS, OfflineMoneyPolicy.forecast(Long.MAX_VALUE, 0, listOf(1, 1, 1)).status)
        assertTrue(OfflineMoneyPolicy.completedSavings(listOf(SavingObservation(date(7), Long.MAX_VALUE),
            SavingObservation(date(7), 1)), date(10), zone).isEmpty())
    }
    @Test fun comparisonsRequireMeaningfulObservedIncrease() {
        assertNull(OfflineMoneyPolicy.spendingIncrease(0, 20000))
        assertNull(OfflineMoneyPolicy.spendingIncrease(100000, 110000))
        assertEquals(20000L, OfflineMoneyPolicy.spendingIncrease(100000, 120000))
        assertNull(OfflineMoneyPolicy.spendingIncrease(Long.MAX_VALUE - 10000, Long.MAX_VALUE))
    }
}
