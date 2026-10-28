package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementOverviewDashboardUiTest {
    @get:Rule val compose = createComposeRule()

    private val model = StatementInsightViewModel(
        accountLabel = "Personal wallet",
        scope = StatementReportScope.Year(2025),
        metrics = StatementInsightMetrics(12_345_00L, 8_000_00L,
            32_000_00L, 18_000_00L, 0L, 2_000_00L),
        chartYear = 2025,
        monthlySpending = (1..12).map { month ->
            StatementMonthSpend(2025, month, if (month == 1) 12_345_00L else null,
                imported = month == 1)
        },
        categories = listOf("Groceries" to 12_345_00L),
        sourceMonths = emptyList(),
        hasUnclassifiedIncome = false,
        hasMissingMonths = true
    )

    @Test fun showsRealPassedAmountsWithoutFakeAllAccounts() {
        compose.setContent { PesaTrackTheme {
            StatementOverviewPage(model, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
        } }
        compose.onNodeWithText("M-Pesa").assertExists()
        compose.onNodeWithText("KES 12,345").assertExists()
        compose.onNodeWithTag("statements-overview").performScrollToNode(hasText("Groceries"))
        compose.onNodeWithText("Monthly spending").assertExists()
        compose.onNodeWithText("Groceries").assertExists()
        compose.onNodeWithText("All accounts").assertDoesNotExist()
        compose.onNodeWithText("KCB").assertDoesNotExist()
        compose.onNodeWithText("Equity").assertDoesNotExist()
    }

    @Test fun archiveAndCoverageButtonsOpenRealDestinations() {
        var archive = 0
        var coverage = 0
        compose.setContent { PesaTrackTheme {
            StatementOverviewPage(model, {}, {}, {}, {}, { archive++ }, { coverage++ }, {}, { _, _ -> }, {}, {})
        } }
        compose.onNodeWithText("Archive").performClick()
        compose.onNodeWithTag("statements-overview").performScrollToNode(hasText("View coverage"))
        compose.onNodeWithText("View coverage").performClick()
        assertEquals(1, archive)
        assertEquals(1, coverage)
    }

    @Test fun cashMovementCardsOpenDifferentSourceDirections() {
        val selected = mutableListOf<Boolean>()
        compose.setContent { PesaTrackTheme {
            StatementOverviewPage(model, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
                onOpenCashMovement = { selected.add(it) })
        } }
        compose.onNodeWithText("Money in").performClick()
        compose.onNodeWithText("Money out").performClick()
        assertEquals(listOf(true, false), selected)
    }
}
