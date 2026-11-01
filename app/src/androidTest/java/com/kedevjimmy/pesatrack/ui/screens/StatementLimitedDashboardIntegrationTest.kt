package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.AccountOwnershipConsentStore
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.StatementAdmissionResult
import com.kedevjimmy.pesatrack.domain.statements.StatementBankAdmission
import com.kedevjimmy.pesatrack.domain.statements.StatementDocumentAssurance
import com.kedevjimmy.pesatrack.domain.statements.StatementDocumentProof
import com.kedevjimmy.pesatrack.domain.statements.StatementPrintedTotal
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceProfileRegistry
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses a disposable emulator database and invented source rows only. */
@RunWith(AndroidJUnit4::class)
class StatementLimitedDashboardIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: Context

    @Before fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    @Test fun savedLimitedSourceOpensDashboardAndLeavesActivityUntouched() {
        val bank = AccountOwnershipConsentStore(app).createOwnedAccount("Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(bank.id, "001234567890", null)
        val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
        val pages = listOf(StatementTablePage(1,
            listOf("Transaction Details", "Payment reference", "Date", "Credit", "Debit", "Balance"),
            listOf(listOf("Invented deposit", "EXAMPLE00001", "01/09/2026", "100.00", "", "100.00"),
                listOf("Invented payment", "EXAMPLE00002", "02/09/2026", "", "20.00", "80.00"))))
        val proof = StatementDocumentProof(profile.providerKey, "001234567890", "KES", "PDF",
            true, 1, emptyMap(), StatementPrintedTotal(2_000, 10_000), null,
            StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY)
        val admitted = StatementBankAdmission.verify(profile,
            AccountStore.get(app).listAccounts().first { it.id == bank.id }, setOf(bank.id),
            profile.providerKey, proof, pages) as StatementAdmissionResult.Accepted
        val store = StatementBankArchiveStore(app)
        store.save(admitted, pages)
        val source = store.limitedSourcesFor(bank.id).single()
        var backCount = 0
        compose.setContent { PesaTrackTheme {
            StatementLimitedBankOverviewPage("Invented Equity", listOf(source),
                onBack = { backCount++ }, onImport = {})
        } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Monthly money movement").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Not a fully verified bank report").assertExists()
        compose.onNodeWithTag("limited-bank-dashboard")
            .performScrollToNode(hasText("Monthly money movement"))
        compose.onNodeWithText("Monthly money movement").assertExists()
        compose.onNodeWithTag("limited-bank-dashboard")
            .performScrollToNode(hasText("Largest money-out movements"))
        assertTrue(compose.onAllNodesWithText("Invented payment").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("limited-bank-dashboard")
            .performScrollToNode(hasText("Statement insights · 2 locally reconciled rows"))
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase
            .compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }
}
