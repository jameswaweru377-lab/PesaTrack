package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HomeSupplementalCacheTest {
    private fun snapshot() = ActivityLedgerSnapshot(
        transactions = emptyList(),
        summary = ActivitySummary(0, 0, 0, 0, 0, 0)
    )

    @Test fun historyIsReusedOnlyForSameLedgerRevisionMonthAndCalendarDay() {
        val home = HomeScreenCache()
        val selected = HomeHistorySnapshots(snapshot(), snapshot())
        home.saveHistory(revision = 7, monthOffset = 0, dayKey = 2026294, value = selected)
        assertSame(selected, home.historyFor(7, 0, 2026294))
        assertNull(home.historyFor(8, 0, 2026294))
        assertNull(home.historyFor(7, 1, 2026294))
        assertNull(home.historyFor(7, 0, 2026295))
    }

    @Test fun staleHistoricalComparisonCannotLeakAfterRevisionChange() {
        val home = HomeScreenCache()
        home.saveHistory(7, 0, 2026294, HomeHistorySnapshots(snapshot(), snapshot()))
        val newer = HomeHistorySnapshots(null, snapshot())
        home.saveHistory(8, 0, 2026294, newer)
        assertSame(newer, home.historyFor(8, 0, 2026294))
        assertNull(home.historyFor(7, 0, 2026294))
    }

    @Test fun emptyInvestmentSetCanBeCachedWithoutExtraRead() {
        val home = HomeScreenCache()
        home.saveInvestments(7, emptyList())
        assertSame(emptyList<Any>(), home.investmentsFor(7))
        assertNull(home.investmentsFor(8))
    }
}
