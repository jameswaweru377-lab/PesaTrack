package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.ui.screens.MoneySetupScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** F09: invented local data verifies the actual preview and consent boundary. */
@RunWith(AndroidJUnit4::class)
class SmartRuleConsentUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        AccountStore.get(context).save("Invented bank", AccountType.BANK, "consent-test-bank")
    }

    @After fun cleanup() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun previewAndCancelKeepLedgerUnchangedAndApplyTouchesOnlyConsentedRule() {
        insert("CONSENT01", "Invented Internet", ReviewState.NEEDS_REVIEW)
        insert("CONSENT02", "Invented Internet", ReviewState.USER_CLASSIFIED)
        insert("CONSENT03", "Invented Other Shop", ReviewState.NEEDS_REVIEW)
        val rules = RuleStore.get(context)
        val unrelated = rules.save(
            name = "Existing shop rule", counterpartyContains = "Invented Other Shop",
            category = TransactionCategory.SHOPPING, direction = MoneyDirection.EXPENSE
        )
        val transactions = TransactionStore.get(context)
        val before = listOf("CONSENT01", "CONSENT02", "CONSENT03")
            .map { transactions.findByTransactionCode(it) }

        compose.setContent {
            PesaTrackTheme { MoneySetupScreen(PaddingValues(), initialSetup = false, onDone = {}) }
        }
        compose.onNodeWithText("Monthly salary").performScrollTo().performClick()
        compose.onNodeWithText("Internet", useUnmergedTree = true).performClick()
        compose.onNodeWithText("M-Pesa name").performScrollTo().performClick()
        compose.onNodeWithText("Recipient or merchant name").performScrollTo()
            .performTextInput("Invented Internet")
        compose.onNodeWithText("Preview rule").performScrollTo().performClick()
        compose.onNodeWithText("Review this rule").assertIsDisplayed()
        compose.onNodeWithText("1 eligible saved transactions", substring = true).assertIsDisplayed()
        assertEquals(listOf(unrelated.id), rules.listRules().map { it.id })
        assertEquals(before, listOf("CONSENT01", "CONSENT02", "CONSENT03")
            .map { transactions.findByTransactionCode(it) })

        compose.onNodeWithText("Keep editing").performClick()
        assertEquals(1, rules.listRules().size)
        compose.onNode(hasSetTextAction() and hasText("Invented Internet"))
            .performScrollTo().assertTextContains("Invented Internet")
        compose.onNodeWithText("Preview rule").performScrollTo().performClick()
        compose.onNodeWithText("Save and apply").performClick()
        compose.waitUntil(5_000) { rules.listRules().size == 2 }

        val saved = rules.listRules().single { it.id != unrelated.id }
        assertEquals("Invented Internet", saved.counterpartyContains)
        assertEquals(TransactionCategory.INTERNET, saved.category)
        assertEquals(saved.id, transactions.findByTransactionCode("CONSENT01")?.ruleId)
        assertEquals(TransactionCategory.INTERNET.name,
            transactions.findByTransactionCode("CONSENT01")?.category)
        assertEquals(before[1], transactions.findByTransactionCode("CONSENT02"))
        assertEquals(before[2], transactions.findByTransactionCode("CONSENT03"))
    }

    @Test fun notificationPreviewRequiresConsentAndDoesNotPromiseHistoricalReplay() {
        val rules = RuleStore.get(context)
        compose.setContent {
            PesaTrackTheme { MoneySetupScreen(PaddingValues(), initialSetup = false, onDone = {}) }
        }
        compose.onNodeWithText("Bank or sender name").performScrollTo()
            .performTextInput("Invented bank")
        compose.onNodeWithText("Recognizable phrase in the message").performScrollTo()
            .performTextInput("salary credited")
        compose.onNodeWithText("Preview rule").performScrollTo().performClick()
        compose.onNodeWithText("future notifications", substring = true).assertIsDisplayed()
        compose.onNodeWithText("cannot be checked", substring = true).assertIsDisplayed()
        assertTrue(rules.listRules().isEmpty())
        compose.onNodeWithText("Keep editing").performClick()
        assertTrue(rules.listRules().isEmpty())
        compose.onNodeWithText("Preview rule").performScrollTo().performClick()
        compose.onNodeWithText("Save and apply").performClick()
        compose.waitUntil(5_000) { rules.listRules().size == 1 }
        assertEquals("salary credited", rules.listRules().single().messageContains)
        assertEquals(0, TransactionStore.get(context).snapshot().transactionCount)
    }

    private fun insert(code: String, name: String, state: ReviewState) {
        val now = System.currentTimeMillis()
        check(TransactionStore.get(context).insertIfNew(
            parsed = ParsedMpesaTransaction(
                transactionCode = code, type = MpesaTransactionType.PAY_BILL,
                amountMinor = 300_000, counterparty = name, counterpartyPhone = null,
                feeMinor = 0, balanceMinor = null, occurredAtText = null,
                rawMessage = "Invented consent fixture $code"
            ),
            sourcePackage = null, sourceKind = "test", receivedAtEpochMs = now,
            rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = false, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, reviewStateOverride = state
        ))
    }
}
