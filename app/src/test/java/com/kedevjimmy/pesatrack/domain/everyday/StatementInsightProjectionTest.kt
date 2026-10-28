package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementInsightProjectionTest {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }
    private fun timestamp(date: String) = requireNotNull(fmt.parse(date)).time

    private fun row(date: String, flow: StatementFlow, debit: Long = 0L,
                    credit: Long = 0L, economic: Long? = null) = StatementClassified(
        "synthetic-batch", StatementActivityLine(1, 1, "AAA0000001", timestamp(date),
            "Synthetic evidence", credit, debit, 100_000L), flow,
        economicAmountMinor = economic
    )

    private fun source(date: String, account: String = "mpesa"):
        StatementHistoryReport.SelectedMonth {
        val year = date.substring(0,4).toInt()
        val month = date.substring(5,7).toInt()
        val time = timestamp("$date 09:00")
        return StatementHistoryReport.SelectedMonth(
            StatementHistoryReport.SourceMonth(
                accountId = account, batchId = "pdf-$date", recordedAt = time,
                independentlyVerified = true,
                usage = StatementMonthlyUsage.Month(year, month, 1, 1, 0, 0L, 0L),
                firstTransactionAt = time, lastTransactionAt = time
            ), 1
        )
    }

    @Test fun expenseExcludesOwnTransfersLoansAndUnknownDebits() {
        val rows = listOf(
            row("2025-01-02 12:00", StatementFlow.EXPENSE, 1_000L),
            row("2025-01-02 13:00", StatementFlow.TRANSFER, 90_000L),
            row("2025-01-02 14:00", StatementFlow.DEBT, 30_000L),
            row("2025-01-02 15:00", StatementFlow.UNKNOWN, 5_000L),
            row("2025-01-02 16:00", StatementFlow.INCOME, credit = 10_000L)
        )
        val metrics = StatementInsightProjection.metrics(rows)
        assertEquals(1_000L, metrics.spentMinor)
        assertEquals(10_000L, metrics.incomeMinor)
        assertEquals(126_000L, metrics.moneyOutMinor)
        assertEquals(5_000L, metrics.unknownOutMinor)
        assertEquals(10_000L, metrics.moneyInMinor)
    }

    @Test fun economicShareDoesNotInflateTheExpenseHero() {
        val row = row("2025-01-12 12:00", StatementFlow.EXPENSE,
            debit = 20_000L, economic = 9_000L)
        assertEquals(9_000L, StatementInsightProjection.metrics(listOf(row)).spentMinor)
    }

    @Test fun importedZeroAndMissingMonthsAreDifferent() {
        val observed = listOf(source("2025-01-02"), source("2025-03-11"))
        val rows = listOf(
            row("2025-01-02 12:00", StatementFlow.EXPENSE, 2_500L),
            row("2025-03-11 12:00", StatementFlow.TRANSFER, 30_000L)
        )
        val bars = StatementInsightProjection.yearBars(rows, observed, 2025)
        assertEquals(12, bars.size)
        assertEquals(2_500L, bars[0].spentMinor)
        assertNull(bars[1].spentMinor)
        assertFalse(bars[1].imported)
        assertEquals(0L, bars[2].spentMinor)
        assertTrue(bars[2].imported)
    }
}
