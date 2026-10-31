package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryForecastBillPolicyTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test fun weeklyBillCountsAllRemainingOccurrencesBeforePayday() {
        val from = day(2026, Calendar.OCTOBER, 13)
        val through = day(2026, Calendar.NOVEMBER, 4)
        val rows = SalaryForecastBillPolicy.occurrences(
            bill = SalaryForecastBill("wifi", "Weekly internet", 20_000L,
                SalaryForecastBillCadence.WEEKLY, dayOfWeek = Calendar.MONDAY, paidThisMonth = 2),
            nowEpochMs = from, throughEpochMs = through, timeZone = utc
        )
        assertEquals(3, rows.size) // Oct 19, Oct 26, Nov 2; Oct 5 and 12 paid
        assertEquals(60_000L, rows.sumOf { it.amountMinor })
        assertEquals(3, rows.distinctBy { it.id }.size)
    }

    @Test fun monthEndDayClampsAcrossFebruaryAndDoesNotDoubleCountPaidBill() {
        val rows = SalaryForecastBillPolicy.occurrences(
            bill = SalaryForecastBill("rent", "Rent", 1_000_000L,
                SalaryForecastBillCadence.MONTHLY, dayOfMonth = 31, paidThisMonth = 1),
            nowEpochMs = day(2027, Calendar.JANUARY, 25),
            throughEpochMs = day(2027, Calendar.MARCH, 1), timeZone = utc
        )
        assertEquals(1, rows.size)
        assertEquals(day(2027, Calendar.FEBRUARY, 28), rows.single().dueEpochMs)
    }

    @Test fun unpaidPastDueBillStillReducesAvailableMoney() {
        val now = day(2026, Calendar.OCTOBER, 20)
        val rows = SalaryForecastBillPolicy.occurrences(
            SalaryForecastBill("rent", "Rent", 1_000_000L,
                SalaryForecastBillCadence.MONTHLY, dayOfMonth = 10),
            nowEpochMs = now,
            throughEpochMs = day(2026, Calendar.OCTOBER, 31), timeZone = utc
        )
        assertEquals(1, rows.size)
        assertEquals(now, rows.single().dueEpochMs)
    }

    private fun day(year: Int, month: Int, day: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day)
    }.timeInMillis
}
