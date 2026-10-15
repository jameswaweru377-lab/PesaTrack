package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementHistoryReportTest {
    private val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }

    private fun month(
        account: String,
        batch: String,
        date: String,
        income: Long = 0L,
        out: Long = 0L,
        rows: Int = 1,
        verified: Boolean = false,
        recordedAt: Long = 100L
    ): StatementHistoryReport.SourceMonth {
        val timestamp = requireNotNull(dateParser.parse(date)).time
        val year = date.substring(0, 4).toInt()
        val month = date.substring(5, 7).toInt()
        return StatementHistoryReport.SourceMonth(
            accountId = account,
            batchId = batch,
            recordedAt = recordedAt,
            independentlyVerified = verified,
            usage = StatementMonthlyUsage.Month(
                year = year, month = month,
                sourceRows = rows, financialMovements = rows,
                informationalRows = 0, paidInMinor = income, paidOutMinor = out
            ),
            firstTransactionAt = timestamp,
            lastTransactionAt = timestamp + 60_000L
        )
    }

    @Test fun overlappingImportsUseOneSourceNeverSumDuplicates() {
        val a = month("mpesa", "pdf1", "2026-08-10 09:00:00",
            income = 100_00L, out = 3_000_00L, rows = 100)
        val b = month("mpesa", "pdf2", "2026-08-10 09:00:00",
            income = 100_00L, out = 3_000_00L, rows = 105)
        val selected = StatementHistoryReport.selectForAccount(listOf(a,b), "mpesa")
        assertEquals(1, selected.size)
        assertEquals(2, selected.single().sourceCount)
        assertEquals("pdf2", selected.single().source.batchId)
        assertEquals(3_000_00L, StatementHistoryReport.year(selected, 2026).paidOutMinor)
    }

    @Test fun separateAccountsCannotBeAccidentallySummed() {
        val wallet = month("wallet","one","2025-01-12 08:00:00",out=20_000L)
        val bank = month("bank","two","2025-01-12 08:00:00",out=40_000L)
        val chosen = StatementHistoryReport.selectForAccount(listOf(wallet,bank),"wallet")
        assertEquals(20_000L, chosen.single().usage.paidOutMinor)
        assertEquals("one", chosen.single().source.batchId)
    }

    @Test fun historicalYearHasMissingMonthsAndCorrectRecordedAverage() {
        val rows = listOf(
            month("wallet","jan","2023-01-02 12:00:00",out=10_001L),
            month("wallet","dec","2023-12-31 23:59:00",income=23_000L,out=20_000L),
            month("wallet","may","2024-05-01 00:00:00",out=99_000L),
            month("wallet","new","2026-09-01 00:00:00",out=10L)
        )
        val selected = StatementHistoryReport.selectForAccount(rows,"wallet")
        assertEquals(listOf(2026, 2024, 2023), StatementHistoryReport.years(selected))
        val year = StatementHistoryReport.year(selected,2023)
        assertEquals(2, year.importedMonthCount)
        assertEquals(10, year.missingMonthCount)
        assertEquals(30_001L, year.paidOutMinor)
        assertEquals(15_000L, year.averageOutMinor)
        assertEquals(23_000L, year.paidInMinor)
        assertEquals(-7_001L, year.netMinor)
    }

    @Test fun selectionDoesNotMutateSourceAndEmptyYearIsMissingNotZeroUsage() {
        val source = listOf(month("wallet","jan","2024-01-04 09:00:00",out=199L))
        val before = source.toList()
        val selected = StatementHistoryReport.selectForAccount(source,"wallet")
        val emptyYear = StatementHistoryReport.year(selected,2023)
        assertEquals(12, emptyYear.missingMonthCount)
        assertEquals(0, emptyYear.importedMonthCount)
        assertEquals(0L, emptyYear.averageOutMinor)
        assertEquals(before, source)
        assertTrue(StatementHistoryReport.selectForAccount(source,"other").isEmpty())
    }

    @Test fun sameRowCoveragePrefersVerifiedSourceAndLatestWhenEqual() {
        val old = month("wallet","old","2024-05-02 00:00:00",rows=2)
        val verified = month("wallet","verified","2024-05-02 00:00:00",
            rows=2,verified=true,recordedAt=101)
        assertEquals("verified",
            StatementHistoryReport.selectForAccount(listOf(old,verified),"wallet")
                .single().source.batchId)
    }
}
