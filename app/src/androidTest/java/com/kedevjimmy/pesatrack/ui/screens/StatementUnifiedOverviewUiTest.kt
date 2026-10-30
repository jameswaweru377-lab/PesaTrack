package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceCoveragePolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementUnifiedOverviewPolicy
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementUnifiedOverviewUiTest {
    @get:Rule val compose = createComposeRule()

    private fun coverage(account: String, month: String, review: Boolean = false) =
        StatementSourceCoveragePolicy.MonthCoverage(
            accountId = account, currency = "KES", monthKey = month,
            sourceCount = if (review) 2 else 1, observedLegCount = 3,
            remainingObservedLegCount = 3, confirmedRepeatCopies = 0,
            unresolvedCandidateCount = if (review) 2 else 0,
            status = if (review) StatementSourceCoveragePolicy.Status.NEEDS_REVIEW
                else StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
            safeMoneyInMinor = if (review) null else 20_000L,
            safeMoneyOutMinor = if (review) null else 3_000L
        )

    @Test fun allAccountLandingShowsCoverageNotInventedMoneyTotals() {
        val overview = StatementUnifiedOverviewPolicy.project(2026, listOf(
            coverage("wallet", "2026-08"), coverage("equity", "2026-08", review = true)
        ))
        var selected = ""
        compose.setContent { PesaTrackTheme {
            StatementUnifiedOverviewPage(
                overview = overview, years = listOf(2026),
                accountLabels = mapOf("wallet" to "M-Pesa", "equity" to "Equity Bank"),
                bankAccountIds = setOf("equity"), onSelectYear = {},
                onSelectAccount = { selected = it }, onImport = {},
                onBack = {}
            )
        } }
        compose.onNodeWithTag("statements-unified-overview").assertExists()
        compose.onNodeWithText("2 accounts on file").assertExists()
        compose.onNodeWithText("Money in").assertDoesNotExist()
        compose.onNodeWithText("Money out").assertDoesNotExist()
        compose.onNodeWithContentDescription("Equity Bank, Aug 2026: Needs review").assertExists()
        compose.onNodeWithContentDescription("Equity Bank, Aug 2026: Needs review").performClick()
        assertEquals("equity", selected)
    }

    @Test fun emptyHistoryInvitesImportWithoutMockFinancialValues() {
        val overview = StatementUnifiedOverviewPolicy.project(2026, emptyList())
        var imports = 0
        compose.setContent { PesaTrackTheme {
            StatementUnifiedOverviewPage(overview, listOf(2026), emptyMap(), emptySet(),
                onSelectYear = {}, onSelectAccount = {}, onImport = { imports++ }, onBack = {})
        } }
        compose.onNodeWithText("No statements imported yet").assertExists()
        compose.onNodeWithText("Import statement").performClick()
        assertEquals(1, imports)
    }
}
