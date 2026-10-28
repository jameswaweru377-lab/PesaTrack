package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementReportScopePolicyTest {
    private val dates = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }

    private fun at(value: String) = requireNotNull(dates.parse(value)).time

    private fun row(value: String, receipt: String) = StatementClassified(
        batchId = "test-pdf",
        line = StatementActivityLine(1, 1, receipt, at(value), "Synthetic groceries",
            0L, 400L, 100_000L),
        flow = StatementFlow.EXPENSE,
        categoryId = "GROCERIES"
    )

    @Test fun yearAndMonthDoNotLeakOtherYears() {
        val rows = listOf(
            row("2024-12-31 23:55", "AAA0000001"),
            row("2025-01-01 00:05", "AAA0000002"),
            row("2025-11-05 14:00", "AAA0000003")
        )
        assertEquals(listOf("AAA0000002", "AAA0000003"),
            StatementReportScopePolicy.selectRows(rows, StatementReportScope.Year(2025))
                .map { it.line.reference })
        assertEquals(listOf("AAA0000002"),
            StatementReportScopePolicy.selectRows(rows, StatementReportScope.Month("2025-01"))
                .map { it.line.reference })
    }

    @Test fun mondayWeekCanCrossCalendarMonthAndDayDoesNot() {
        val rows = listOf(
            row("2025-09-29 10:00", "AAA0000001"),
            row("2025-10-05 22:00", "AAA0000002"),
            row("2025-10-06 10:00", "AAA0000003")
        )
        assertEquals(2, StatementReportScopePolicy.selectRows(
            rows, StatementReportScope.Week(at("2025-10-01 12:00"))).size)
        assertEquals(1, StatementReportScopePolicy.selectRows(
            rows, StatementReportScope.Day(at("2025-10-06 01:00"))).size)
    }

    @Test fun allHistoryRetainsEveryImportedRow() {
        val rows = listOf(
            row("2023-01-01 12:00", "AAA0000001"),
            row("2025-01-01 12:00", "AAA0000002")
        )
        assertEquals(rows,
            StatementReportScopePolicy.selectRows(rows, StatementReportScope.AllHistory))
    }
}
