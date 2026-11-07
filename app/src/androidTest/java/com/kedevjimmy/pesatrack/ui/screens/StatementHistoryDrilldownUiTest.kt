package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class StatementHistoryDrilldownUiTest {
    @get:Rule val compose = createComposeRule()

    private val date: Long = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
        set(2025, Calendar.MARCH, 14, 16, 32)
    }.timeInMillis
    private val line = StatementActivityLine(1, 2, "AAA0000001", date,
        "Synthetic supermarket", 0L, 3_850_00L, 96_150_00L)
    private val classified = StatementClassified("test-pdf", line, StatementFlow.EXPENSE,
        categoryId = "GROCERIES")
    private val month = StatementHistoryReport.SelectedMonth(
        StatementHistoryReport.SourceMonth("wallet", "test-pdf", date, true,
            StatementMonthlyUsage.Month(2025, 3, 1, 1, 0, 0L, 3_850_00L), date, date), 1)

    @Test fun monthShowsSourceEvidenceAndOpensExactlyThatRow() {
        var clicked: StatementClassified? = null
        compose.setContent { PesaTrackTheme {
            StatementPeriodPage(month, listOf(classified), { clicked = it }, {})
        } }
        compose.onNodeWithText("March 2025").assertExists()
        compose.onNodeWithText("Synthetic supermarket").performClick()
        assertEquals("AAA0000001", clicked?.line?.reference)
    }

    @Test fun coverageShowsMissingMonthNotZeroSpending() {
        var opened: String? = null
        compose.setContent { PesaTrackTheme {
            StatementCoveragePage(2025, listOf(month), { opened = it }, {}, {})
        } }
        // March is imported: the other eleven months remain explicitly missing.
        compose.onAllNodesWithText("Not imported", substring = true).assertCountEquals(11)
        compose.onNodeWithText("Mar").performClick()
        assertEquals("2025-03", opened)
    }
}
