package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.everyday.StatementCashFlowTotals
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementOverviewDashboardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun summaryExplainsMissingTrendDataAndKeepsImportActionAvailable() {
        var imports = 0
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                StatementOverviewPage(
                    sourceMonths = emptyList(),
                    visible = emptyList(),
                    sourceCashFlow = StatementCashFlowTotals(0L, 0L, 0L),
                    breakdown = emptyList(),
                    categoryLabels = emptyMap(),
                    reconciliationExpanded = false,
                    onReconciliationToggle = {},
                    onNavigate = { _, _, _ -> },
                    onImport = { imports++ },
                    onArchive = {},
                    onSelectMonth = {}
                )
            }
        }

        compose.onNodeWithText("Income vs spending trend").assertIsDisplayed()
        compose.onNodeWithText("Import a statement to compare recognized income and spending.")
            .assertIsDisplayed()
        compose.onNodeWithText("Import statement (PDF or CSV)").assertIsDisplayed()
        compose.onNodeWithText("Import statement").performClick()
        assertEquals(1, imports)
    }
}
