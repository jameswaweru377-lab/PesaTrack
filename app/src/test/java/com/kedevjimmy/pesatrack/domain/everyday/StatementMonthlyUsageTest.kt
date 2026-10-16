package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementMonthlyUsageTest {
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }

    private fun row(
        time: String,
        reference: String = "QAB1234569",
        paidIn: Long = 0,
        paidOut: Long = 0
    ) = StatementActivityLine(
        page = 1, row = 1, reference = reference,
        occurredAt = requireNotNull(format.parse(time)).time,
        description = if (paidIn == 0L && paidOut == 0L) "M-Shwari Loan Request" else "Source cash movement",
        paidInMinor = paidIn, withdrawnMinor = paidOut, balanceMinor = 100000
    )

    @Test fun monthsAreSortedNewestFirstAndTotalsAreExact() {
        val source = listOf(
            row("2026-03-14 10:00:00", paidOut = 3000),
            row("2026-05-15 10:00:00", paidIn = 50000),
            row("2026-04-01 00:00:00", paidIn = 10000),
            row("2026-04-03 12:00:00", paidOut = 7000),
            row("2026-04-03 12:00:00")
        )
        val months = StatementMonthlyUsage.summarize(source)
        assertEquals(listOf("2026-05", "2026-04", "2026-03"), months.map { it.key })
        assertEquals("April 2026", months[1].label)
        assertEquals(10000L, months[1].paidInMinor)
        assertEquals(7000L, months[1].paidOutMinor)
        assertEquals(3000L, months[1].netMinor)
        assertEquals(2, months[1].financialMovements)
        assertEquals(1, months[1].informationalRows)
        assertEquals(3, months[1].sourceRows)
        assertEquals(5, months.sumOf { it.sourceRows })
        assertEquals(60000L, months.sumOf { it.paidInMinor })
        assertEquals(10000L, months.sumOf { it.paidOutMinor })
    }

    @Test fun monthBoundaryUsesNairobiTimeNotDeviceTimeZone() {
        val observed = row("2025-12-31 23:59:59").occurredAt
        val first = row("2026-01-01 00:00:00").occurredAt
        assertEquals("2025-12", StatementMonthlyUsage.monthKey(observed))
        assertEquals("2026-01", StatementMonthlyUsage.monthKey(first))
        val source = listOf(row("2025-12-31 23:59:59", paidOut = 100),
            row("2026-01-01 00:00:00", paidIn = 200))
        assertEquals(listOf("2026-01", "2025-12"),
            StatementMonthlyUsage.summarize(source).map { it.key })
    }

    @Test fun separateCashLegsOnSameReceiptAreNotDeduplicated() {
        val source = listOf(
            row("2026-05-04 13:00:00", reference = "QAB1234569", paidOut = 20000),
            row("2026-05-04 13:00:00", reference = "QAB1234569", paidOut = 500)
        )
        val result = StatementMonthlyUsage.summarize(source).single()
        assertEquals(2, result.financialMovements)
        assertEquals(20500L, result.paidOutMinor)
        assertEquals(-20500L, result.netMinor)
    }

    @Test fun emptyArchiveDoesNotInventUsage() {
        assertTrue(StatementMonthlyUsage.summarize(emptyList()).isEmpty())
    }

    @Test fun priorStatementRowsAreNotChangedBySummary() {
        val source = listOf(row("2026-05-04 13:00:00", paidOut = 200),
            row("2026-05-03 13:00:00", paidIn = 1000))
        val snapshot = source.toList()
        StatementMonthlyUsage.summarize(source)
        assertEquals(snapshot, source)
    }
}
