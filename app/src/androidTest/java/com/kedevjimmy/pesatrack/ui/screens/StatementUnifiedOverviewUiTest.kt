package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.statements.StatementGrandTotalReconciledPolicy
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

    @Test fun allAccountLandingShowsCoverageAndWithholdsUncertainCashTotals() {
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
        compose.onNodeWithText("Money in").assertExists()
        compose.onNodeWithText("Money out").assertExists()
        compose.onNodeWithText("Amounts on hold · review overlaps").assertExists()
        compose.onNodeWithText("PesaTrack").assertDoesNotExist()
        compose.onNodeWithContentDescription("Equity Bank, Aug 2026: Needs review").assertExists()
        compose.onNodeWithContentDescription("Equity Bank, Aug 2026: Needs review").performClick()
        assertEquals("equity", selected)
    }

    @Test fun allAccountLandingShowsVerifiedCashMovementsFromImportsOnly() {
        val overview = StatementUnifiedOverviewPolicy.project(2026, listOf(
            coverage("wallet", "2026-08"), coverage("equity", "2026-09")
        ))
        compose.setContent { PesaTrackTheme {
            StatementUnifiedOverviewPage(overview, listOf(2026), emptyMap(), setOf("equity"),
                onSelectYear = {}, onSelectAccount = {}, onImport = {}, onBack = {})
        } }
        compose.onNodeWithText("Money in").assertExists()
        compose.onNodeWithText("Money out").assertExists()
        compose.onNodeWithText("KES 400").assertExists()
        compose.onNodeWithText("KES 60").assertExists()
        compose.onNodeWithText("Gross account movements · includes own transfers").assertExists()
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

    @Test fun limitedOnlySavedEquityStatementHasAVisibleAccountEntry() {
        val overview = StatementUnifiedOverviewPolicy.project(2026, emptyList())
        var selected = ""
        compose.setContent { PesaTrackTheme {
            StatementUnifiedOverviewPage(
                overview, listOf(2026), mapOf("invented-equity" to "Equity Bank"), emptySet(),
                onSelectYear = {}, onSelectAccount = { selected = it },
                onImport = {}, onBack = {},
                limitedSources = listOf(StatementBankArchiveStore.Source(
                    id = "a".repeat(64), accountId = "invented-equity",
                    providerKey = "KENYAN_BANK_EQUITY", currency = "KES", format = "PDF",
                    importedAt = 1_780_000_000_000L, firstAt = 1_780_000_000_000L,
                    lastAt = 1_780_000_060_000L, rows = 2,
                    moneyInMinor = 1_000L, moneyOutMinor = 250L
                ))
            )
        } }
        compose.onNodeWithText("Limited assurance statements · all years").assertExists()
        compose.onNodeWithText("1 account on file").assertExists()
        compose.onNodeWithText("No statements imported yet").assertDoesNotExist()
        compose.onNodeWithText("Equity Bank").performClick()
        assertEquals("invented-equity", selected)
    }

    @Test fun accountPickerIncludesWalletAndLimitedBankWithoutChangingYearTotals() {
        val overview = StatementUnifiedOverviewPolicy.project(2026, listOf(coverage("wallet", "2026-08")))
        val source = StatementBankArchiveStore.Source(
            id = "c".repeat(64), accountId = "equity", providerKey = "KENYAN_BANK_EQUITY",
            currency = "KES", format = "PDF", importedAt = 1_780_000_000_000L,
            firstAt = 1_780_000_000_000L, lastAt = 1_780_000_060_000L, rows = 2,
            moneyInMinor = 1_000L, moneyOutMinor = 250L
        )
        var selected = ""
        compose.setContent { PesaTrackTheme {
            StatementUnifiedOverviewPage(
                overview, listOf(2026), mapOf("wallet" to "M-Pesa", "equity" to "Equity Bank"),
                setOf("equity"), onSelectYear = {}, onSelectAccount = { selected = it },
                onImport = {}, onBack = {}, limitedSources = listOf(source),
                reportAccountIds = listOf("wallet")
            )
        } }
        compose.onNodeWithText("2 accounts on file").assertExists()
        compose.onNodeWithContentDescription("Choose account").performClick()
        compose.onNodeWithText("M-Pesa").assertExists()
        compose.onNodeWithText("Equity Bank · Limited").performClick()
        assertEquals("equity", selected)
    }

    @Test fun limitedOnlyBankPageOpensTheSavedSourceReview() {
        val saved = StatementBankArchiveStore.Source(
            id = "b".repeat(64), accountId = "invented-equity",
            providerKey = "KENYAN_BANK_EQUITY", currency = "KES", format = "PDF",
            importedAt = 1_780_000_000_000L, firstAt = 1_780_000_000_000L,
            lastAt = 1_780_000_060_000L, rows = 2,
            moneyInMinor = 1_000L, moneyOutMinor = 250L
        )
        compose.setContent { PesaTrackTheme {
            StatementLimitedBankOverviewPage(
                accountLabel = "Invented Equity", sources = listOf(saved),
                onBack = {}, onImport = {}
            )
        } }
        compose.onNodeWithText("Grand-total reconciled · source only").assertExists()
        compose.onNodeWithText("Grand-total reconciled · 2 rows").performClick()
        compose.onNodeWithText("Not a fully verified bank report").assertExists()
    }

    @Test fun reconciledSourceCardDoesNotShowUnvalidatedStoredTotals() {
        val source = StatementBankArchiveStore.Source(
            id = "synthetic-source", accountId = "invented-equity",
            providerKey = "KENYAN_BANK_EQUITY", currency = "KES", format = "PDF",
            importedAt = 1_780_000_000_000L, firstAt = 1_780_000_000_000L,
            lastAt = 1_780_000_060_000L, rows = 2,
            moneyInMinor = 1_000L, moneyOutMinor = 250L
        )
        val rows = listOf(
            StatementGrandTotalReconciledPolicy.Row(source.id, 0, source.firstAt, 1_000L),
            StatementGrandTotalReconciledPolicy.Row(source.id, 1, source.lastAt, -250L)
        )
        val verified = StatementGrandTotalReconciledPolicy.project(source.id, 2, 1_000L, 250L, rows)!!
        compose.setContent { PesaTrackTheme {
            StatementGrandTotalReconciledSourceCardContent(source, verified, checked = true, onSelect = {})
        } }
        compose.onNodeWithText("Money in KES 10 · Money out KES 2.5").assertExists()
    }

    @Test fun corruptedSavedSourceCardWithholdsUnreconciledAmounts() {
        val source = StatementBankArchiveStore.Source(
            id = "synthetic-corrupt-source", accountId = "invented-equity",
            providerKey = "KENYAN_BANK_EQUITY", currency = "KES", format = "PDF",
            importedAt = 1_780_000_000_000L, firstAt = 1_780_000_000_000L,
            lastAt = 1_780_000_060_000L, rows = 2,
            moneyInMinor = 1_000L, moneyOutMinor = 250L
        )
        val missingSecondRow = listOf(StatementGrandTotalReconciledPolicy.Row(
            source.id, 0, source.firstAt, 1_000L
        ))
        val rejected = StatementGrandTotalReconciledPolicy.project(source.id, source.rows, source.moneyInMinor,
            source.moneyOutMinor, missingSecondRow)
        compose.setContent { PesaTrackTheme {
            StatementGrandTotalReconciledSourceCardContent(source, rejected, checked = true,
                onSelect = {})
        } }
        compose.onNodeWithText("Cash totals unavailable — saved rows need review").assertExists()
        compose.onNodeWithText("Money in KES 10 · Money out KES 2.5").assertDoesNotExist()
    }

    @Test fun reconciledSourceSummaryShowsGrossTotalsAndMonthlyBreakdown() {
        val report = StatementGrandTotalReconciledPolicy.Report(
            rows = 2, moneyInMinor = 1_000L, moneyOutMinor = 250L, netMinor = 750L,
            months = listOf(StatementGrandTotalReconciledPolicy.Month(
                "2026-09", 2, 1_000L, 250L
            ))
        )
        compose.setContent { PesaTrackTheme {
            StatementGrandTotalReconciledSummary(report)
        } }
        compose.onNodeWithText("Money in · KES 10").assertExists()
        compose.onNodeWithText("Money out · KES 2.5").assertExists()
        compose.onNodeWithText("Net movement · KES 7.5").assertExists()
        compose.onNodeWithText("2026-09 · 2 rows").assertExists()
    }
}
