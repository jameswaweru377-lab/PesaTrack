package com.kedevjimmy.pesatrack.ui

import android.view.View
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidence
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidenceResult
import com.kedevjimmy.pesatrack.ui.screens.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDetailJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.getSharedPreferences("transaction_annotations", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.getSharedPreferences("transaction_annotations", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun payment(): StoredTransaction {
        val store = TransactionStore.get(context)
        val now = System.currentTimeMillis()
        store.insertIfNew(
            ParsedMpesaTransaction("UITEST0001", MpesaTransactionType.BUY_GOODS, 200000L,
                "Invented grocer", null, 2300L, null, null, "Invented UI test payment"),
            sourcePackage = null, sourceKind = "test", receivedAtEpochMs = now,
            rebuildPayCycles = false, runRelationshipReconciliation = false, runMirrorCorrelation = false,
            directionOverride = MoneyDirection.EXPENSE, categoryOverride = TransactionCategory.GROCERIES,
            reviewStateOverride = ReviewState.USER_CLASSIFIED
        )
        return store.snapshot().latestTransaction!!
    }

    @Test fun categoryPickerScrollsAndNoteActionPersistsWithoutChangingMoney() {
        val payment = payment()
        compose.setContent { PesaTrackTheme { TransactionDetailScreen(PaddingValues(), payment.id, {}) } }
        val list = compose.onNodeWithTag("transaction-details-list")
        list.performScrollToNode(hasText("Transaction facts"))
        compose.onNodeWithText("Transaction facts").performClick()
        list.performScrollToNode(hasText("Total outflow"))
        compose.onNodeWithText("KES 2,023.00").assertIsDisplayed()
        list.performScrollToNode(hasText("Balance unknown"))
        compose.onNodeWithText("Balance unknown").assertIsDisplayed()
        // Current design opens the category sheet from the review action.
        list.performScrollToNode(hasText("No, choose a different category"))
        compose.onNodeWithText("No, choose a different category").performClick()
        compose.onNodeWithText("Choose a category").assertIsDisplayed()
        compose.onNodeWithTag("category-selection-list").performScrollToNode(hasText("Transport"))
        compose.onNodeWithText("Transport").performClick()
        compose.onNodeWithText("Save Category").performClick()
        compose.waitUntil(5000) {
            TransactionStore.get(context).getById(payment.id)?.category ==
                CategoryStore.get(context).canonicalId(TransactionCategory.TRANSPORT.name)
        }
        // The note/receipt action lives in the transaction header's overflow
        // menu in the approved redesign, not in the details list.
        list.performScrollToIndex(0)
        compose.onNodeWithContentDescription("Transaction menu").performClick()
        compose.onNodeWithText("Add note or receipt").performClick()
        compose.onNodeWithText("Personal note").performTextInput("Weekly shopping")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { TransactionAnnotationStore.get(context).get(payment.id).note == "Weekly shopping" }
        assertEquals(200000L, TransactionStore.get(context).getById(payment.id)!!.amountMinor)
        assertEquals(2300L, TransactionStore.get(context).getById(payment.id)!!.feeMinor)
    }

    private fun selectSpecialTreatment(label: String) {
        val list = compose.onNodeWithTag("transaction-details-list")
        list.performScrollToNode(hasText("No, choose a different category"))
        compose.onNodeWithText("No, choose a different category").performClick()
        compose.onNodeWithTag("category-selection-list").performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
    }

    @Test fun transferCorrectionRequiresConfirmationAndDoesNotAlterAmount() {
        val payment = payment()
        val store = TransactionStore.get(context)
        compose.setContent { PesaTrackTheme {
            TransactionDetailScreen(PaddingValues(), payment.id, {})
        } }
        selectSpecialTreatment("Mark as own-account transfer")
        compose.onNodeWithText("Mark as own-account transfer?").assertIsDisplayed()
        assertEquals(MoneyDirection.EXPENSE.name, store.getById(payment.id)!!.direction)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(MoneyDirection.EXPENSE.name, store.getById(payment.id)!!.direction)

        selectSpecialTreatment("Mark as own-account transfer")
        compose.onNodeWithText("Mark as transfer").performClick()
        compose.waitUntil(5_000L) {
            store.getById(payment.id)?.direction == MoneyDirection.TRANSFER.name
        }
        val updated = store.getById(payment.id)!!
        assertEquals(TransactionCategory.TRANSFER.name, updated.category)
        assertEquals(200_000L, updated.amountMinor)
        assertEquals(2_300L, updated.feeMinor)
    }

    @Test fun exclusionWarningExplainsActivityImpactAndCancelIsSafe() {
        val payment = payment()
        val store = TransactionStore.get(context)
        compose.setContent { PesaTrackTheme {
            TransactionDetailScreen(PaddingValues(), payment.id, {})
        } }
        selectSpecialTreatment("Exclude from tracked activity")
        compose.onNodeWithText("Exclude from tracked activity?").assertIsDisplayed()
        compose.onNodeWithText(
            "This is more than excluding a budget category.", substring = true
        ).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(ReviewState.USER_CLASSIFIED.name, store.getById(payment.id)!!.reviewState)

        selectSpecialTreatment("Exclude from tracked activity")
        compose.onNodeWithText("Exclude entry").performClick()
        compose.waitUntil(5_000L) {
            store.getById(payment.id)?.reviewState == ReviewState.EXCLUDED.name
        }
        assertEquals(200_000L, store.getById(payment.id)!!.amountMinor)
    }

    @Test fun transactionFactsAreCollapsedUntilUserExpandsThem() {
        val payment = payment()
        compose.setContent { PesaTrackTheme {
            TransactionDetailScreen(PaddingValues(), payment.id, {})
        } }
        compose.onNodeWithTag("transaction-facts-approved").assertDoesNotExist()
        val list = compose.onNodeWithTag("transaction-details-list")
        list.performScrollToNode(hasText("Transaction facts"))
        compose.onNodeWithText("Transaction facts").performClick()
        list.performScrollToNode(hasTestTag("transaction-facts-approved"))
        compose.onNodeWithTag("transaction-facts-approved").assertExists()
    }

    @Test fun merchantHistoryRemainsAvailableAfterRemovingCorrectionsCard() {
        val payment = payment()
        compose.setContent { PesaTrackTheme {
            TransactionDetailScreen(PaddingValues(), payment.id, {})
        } }
        val list = compose.onNodeWithTag("transaction-details-list")
        list.performScrollToNode(hasTestTag("transaction-merchant-history-card"))
        compose.onNodeWithText("Merchant history").performClick()
        compose.onNodeWithText("No other recent transactions with this merchant.").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("View M-Pesa").assertDoesNotExist()
        compose.onNodeWithText("Hide additional actions").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("remember-category-action"))
        compose.onNodeWithText("Remember category in Activity and Statements").assertIsDisplayed()
        compose.onNodeWithTag("record-money-lent").assertDoesNotExist()
        compose.onNodeWithTag("record-shared-bill").assertDoesNotExist()
    }

    @Test fun originalMessageExpandsAndReferenceCopiesInDarkTheme() {
        val payment = payment()
        val body = "UITEST0001 Confirmed. Ksh2,000.00 paid to Invented grocer."
        val evidence = TransactionMessageEvidenceResult(TransactionMessageEvidence("M-PESA", body,
            payment.receivedAtEpochMs, payment.transactionCode, listOf(payment.amountMinor)))
        lateinit var screenClipboard: ClipboardManager
        lateinit var screenView: View
        compose.setContent {
            screenClipboard = LocalClipboardManager.current
            screenView = LocalView.current
            PesaTrackTheme(darkTheme = true) {
                var expanded by remember { mutableStateOf(true) }
                LazyColumn(Modifier.testTag("evidence-test-list")) {
                    item { TransactionDetailMessage(payment, evidence, expanded, { expanded = !expanded }, false, {}) }
                }
            }
        }
        compose.onNodeWithText(body).assertIsDisplayed()
        compose.onNodeWithText("SMS (M-PESA)").assertIsDisplayed()
        compose.onNodeWithText("Message evidence").performClick()
        compose.onNodeWithText(body).assertDoesNotExist()
        compose.onNodeWithText("Add note or receipt").assertIsDisplayed()
        compose.onNodeWithText("Message evidence").performClick()
        // API29+ clipboard access belongs to the focused screen. Read the same
        // real CompositionLocal service used by its copy action, not a background context.
        // View/window state must be read on the main thread, including polling.
        compose.waitUntil(5_000) { compose.runOnIdle { screenView.rootView.hasWindowFocus() } }
        compose.runOnIdle {
            screenClipboard.setText(AnnotatedString(""))
            assertNotEquals(payment.transactionCode, screenClipboard.getText()?.text)
        }
        compose.onNodeWithContentDescription("Copy reference").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.runOnIdle { screenClipboard.getText()?.text == payment.transactionCode } }
        compose.runOnIdle {
            assertEquals(payment.transactionCode, screenClipboard.getText()?.text)
        }
    }
}
