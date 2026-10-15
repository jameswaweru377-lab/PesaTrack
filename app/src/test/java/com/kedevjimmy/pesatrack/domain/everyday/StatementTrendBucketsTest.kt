package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementTrendBucketsTest {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }
    private fun item(time: String, flow: StatementFlow, amount: Long): StatementClassified {
        val row = StatementActivityLine(
            page=1, row=1, reference="QAB1234569",
            occurredAt = requireNotNull(fmt.parse(time)).time,
            description="Invented synthetic",
            paidInMinor = 0L, withdrawnMinor = amount,
            balanceMinor=1000L)
        return StatementClassified("fake",row,flow)
    }

    @Test fun yearBarsOnlyCountExpensesAndDoNotInventMissingMonths() {
        val values = listOf(
            item("2025-01-02 11:00:00",StatementFlow.EXPENSE,1_000),
            item("2025-01-05 11:00:00",StatementFlow.TRANSFER,50_000),
            item("2025-03-10 10:00:00",StatementFlow.EXPENSE,2_500))
        val bars = StatementTrendBuckets.build(values,StatementFinancialPolicy.Period.YEAR,
            importedMonths=setOf(1,3))
        assertEquals(12,bars.size)
        assertEquals(1_000L,bars[0].amountMinor)
        assertEquals(2_500L,bars[2].amountMinor)
        assertEquals(0L,bars[1].amountMinor)
        assertTrue(bars[0].observed)
        assertFalse(bars[1].observed)
    }

    @Test fun weekStartsOnMondayAndDayUsesNairobiHours() {
        val monday = item("2026-09-14 02:30:00",StatementFlow.EXPENSE,200)
        val sunday = item("2026-09-20 22:15:00",StatementFlow.EXPENSE,400)
        val week = StatementTrendBuckets.build(listOf(monday,sunday),
            StatementFinancialPolicy.Period.WEEK)
        assertEquals("Mon",week[0].label)
        assertEquals(200L,week[0].amountMinor)
        assertEquals(400L,week[6].amountMinor)
        assertTrue(week[6].observed)
        val day = StatementTrendBuckets.build(listOf(monday),
            StatementFinancialPolicy.Period.DAY)
        assertEquals(200L,day[0].amountMinor)
        assertFalse(day[1].observed)
    }

    @Test fun monthGroupsFiveDateBucketsAndExcludesUnknown() {
        val a = item("2026-09-01 10:00:00",StatementFlow.EXPENSE,100)
        val b = item("2026-09-29 10:00:00",StatementFlow.UNKNOWN,400)
        val bars = StatementTrendBuckets.build(listOf(a,b),
            StatementFinancialPolicy.Period.MONTH)
        assertEquals(5,bars.size)
        assertEquals(100L,bars[0].amountMinor)
        assertTrue(bars[4].observed)
        assertEquals(0L,bars[4].amountMinor)
    }

    @Test fun fulizaSpecificRepaymentDoesNotInventFullSettlement() {
        val item = item("2026-09-01 10:00:00",StatementFlow.EXPENSE,200)
            .line.copy(description="Fuliza repayment", withdrawnMinor=200)
        val classified = StatementFinancialPolicy.classify("fake", item)
        assertEquals(StatementFlow.DEBT,classified.flow)
        assertEquals(StatementDebtAction.REPAID,classified.debtAction)
    }
}
