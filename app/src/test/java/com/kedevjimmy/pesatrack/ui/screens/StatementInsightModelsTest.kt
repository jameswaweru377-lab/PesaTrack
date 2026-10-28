package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.everyday.*
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementInsightModelsTest {
    private val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }

    @Test fun categoryTotalsAndNetAreRealClassifiedMoneyNotRawTransfers() {
        val time = requireNotNull(date.parse("2025-08-04")).time
        fun item(index: Int, flow: StatementFlow, amount: Long, category: String?) =
            StatementClassified("synthetic", StatementActivityLine(1, index,
                "AAA000000$index", time, "Synthetic", 0L, amount, 100_000L),
                flow, category)
        val rows = listOf(item(1, StatementFlow.EXPENSE, 500L, "FOOD"),
            item(2, StatementFlow.EXPENSE, 700L, "TRANSPORT"),
            item(3, StatementFlow.TRANSFER, 80_000L, "TRANSFER"))
        val model = StatementInsightModels.from("Personal wallet", StatementReportScope.AllHistory,
            rows, emptyList(), mapOf("FOOD" to "Groceries", "TRANSPORT" to "Travel"))
        assertEquals("Personal wallet", model.accountLabel)
        assertEquals(1_200L, model.metrics.spentMinor)
        assertEquals(81_200L, model.metrics.moneyOutMinor)
        assertEquals(1_200L, model.categories.sumOf { it.second })
        assertTrue(model.categories.any { it.first == "Groceries" && it.second == 500L })
    }

    @Test fun unknownInflowIsReportedAsUnknownInsteadOfSalary() {
        val time = requireNotNull(date.parse("2025-08-04")).time
        val rows = listOf(StatementClassified("synthetic", StatementActivityLine(
            1, 1, "AAA0000001", time, "Money in", 5_000L, 0L, 100_000L),
            StatementFlow.UNKNOWN))
        val model = StatementInsightModels.from("Wallet", StatementReportScope.AllHistory,
            rows, emptyList(), emptyMap())
        assertEquals(0L, model.metrics.incomeMinor)
        assertEquals(5_000L, model.metrics.moneyInMinor)
        assertTrue(model.hasUnclassifiedIncome)
    }

    @Test fun selectedMonthDoesNotEraseOtherImportedMonthFromYearChart() {
        val january = requireNotNull(date.parse("2025-01-04")).time
        val march = requireNotNull(date.parse("2025-03-04")).time
        fun item(time: Long, index: Int, amount: Long) = StatementClassified(
            "synthetic", StatementActivityLine(1, index, "AAA000000$index", time,
                "Synthetic", 0L, amount, 100_000L), StatementFlow.EXPENSE, "FOOD")
        fun selected(time: Long, month: Int) = StatementHistoryReport.SelectedMonth(
            StatementHistoryReport.SourceMonth("wallet", "pdf-$month", time, true,
                StatementMonthlyUsage.Month(2025, month, 1, 1, 0, 0L, 0L), time, time), 1)
        val jan = item(january, 1, 1_500L)
        val mar = item(march, 2, 2_600L)
        val model = StatementInsightModels.from("Wallet", StatementReportScope.Month("2025-03"),
            listOf(mar), listOf(selected(january, 1), selected(march, 3)), emptyMap(),
            accountRows = listOf(jan, mar))
        assertEquals(2_600L, model.metrics.spentMinor)
        assertEquals(1_500L, model.monthlySpending[0].spentMinor)
        assertEquals(2_600L, model.monthlySpending[2].spentMinor)
    }
}
