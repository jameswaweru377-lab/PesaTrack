package com.kedevjimmy.pesatrack.domain.statements

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

/** Invented balances and credits only; no customer statement material. */
class StatementGrandTotalReconciledPolicyTest {
    private val source = "invented-equity-pdf"

    private fun at(month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
            clear()
            set(2026, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun row(number: Int, at: Long, movement: Long, id: String = source) =
        StatementGrandTotalReconciledPolicy.Row(id, number, at, movement)

    @Test fun printedGrandTotalsReconcilePerSourceAcrossNairobiMonthBoundary() {
        val report = StatementGrandTotalReconciledPolicy.project(source, 3, 10_000, 500, listOf(
            row(0, at(9, 30, 23, 59), 2_500),
            row(1, at(10, 1, 0, 1), -500),
            row(2, at(10, 3), 7_500)
        ))!!
        assertEquals(3, report.rows)
        assertEquals(10_000L, report.moneyInMinor)
        assertEquals(500L, report.moneyOutMinor)
        assertEquals(9_500L, report.netMinor)
        assertEquals(listOf("2026-10", "2026-09"), report.months.map { it.key })
        assertEquals(2, report.months.first().rows)
        assertEquals(7_500L, report.months.first().moneyInMinor)
        assertEquals(500L, report.months.first().moneyOutMinor)
        assertEquals(2_500L, report.months.last().moneyInMinor)
    }

    @Test fun mismatchedGrandTotalOrMissingSourceLegCannotProduceAReport() {
        val saved = listOf(row(0, at(9, 1), 1_000), row(1, at(9, 2), -250))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 3, 1_000, 250, saved))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, 1_001, 250, saved))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, 1_000, 251, saved))
    }

    @Test fun mixedSourcesAndDuplicateOrMissingLegNumbersAreNotAggregated() {
        val good = row(0, at(9, 1), 1_000)
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, 2_000, 0,
            listOf(good, row(1, at(9, 2), 1_000, "another-bank-file"))))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, 2_000, 0,
            listOf(good, row(0, at(9, 2), 1_000))))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, 2_000, 0,
            listOf(good, row(2, at(9, 2), 1_000))))
    }

    @Test fun invalidTimestampZeroCashAndCheckedArithmeticOverflowStayUnavailable() {
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 1, 1, 0,
            listOf(row(0, 0, 1))))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 1, 0, 0,
            listOf(row(0, at(9, 1), 0))))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 1, 0, Long.MAX_VALUE,
            listOf(row(0, at(9, 1), Long.MIN_VALUE))))
        assertNull(StatementGrandTotalReconciledPolicy.project(source, 2, Long.MAX_VALUE, 0,
            listOf(row(0, at(9, 1), Long.MAX_VALUE), row(1, at(9, 2), 1))))
        val largeButValid = StatementGrandTotalReconciledPolicy.project(source, 2, Long.MAX_VALUE, 1,
            listOf(row(0, at(9, 1), Long.MAX_VALUE), row(1, at(9, 2), -1)))
        assertEquals(Long.MAX_VALUE - 1L, largeButValid?.netMinor)
    }
}
