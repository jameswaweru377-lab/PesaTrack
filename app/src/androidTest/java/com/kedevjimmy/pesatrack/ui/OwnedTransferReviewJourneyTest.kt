package com.kedevjimmy.pesatrack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kedevjimmy.pesatrack.domain.evidence.*
import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidenceLevel
import com.kedevjimmy.pesatrack.ui.screens.OwnedTransferReviewCard
import com.kedevjimmy.pesatrack.ui.screens.OwnedTransferReviewSection
import com.kedevjimmy.pesatrack.ui.screens.TransactionDetailScreen
import com.kedevjimmy.pesatrack.data.local.*
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.content.ContentValues
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OwnedTransferReviewJourneyTest {
    @get:Rule val compose = createComposeRule()
    private val source = TransferReviewObservation(1, "bank", "BANK", "EXPENSE", 10000, 0, "KES", 1000,
        setOf("SYNTHETIC01"), "James Waweru", null, "source")
    private val destination = source.copy(id = 2, accountId = "account-mpesa", direction = "INCOME")
    private val candidate = TransferReviewCandidate(source, destination, 1, 1, SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW, false)
    @Test fun partialNamesNeverDecideWithoutUserActionAndAllThreeChoicesAreDistinct() {
        val decisions = mutableListOf<TransferPairDecision>()
        compose.setContent { MaterialTheme { OwnedTransferReviewCard(candidate, "Synthetic Bank", "M-Pesa", false) { decisions.add(it) } } }
        compose.onNodeWithText("Two names match your profile. Your confirmation is required.").assertExists()
        compose.runOnIdle { assertTrue(decisions.isEmpty()) }
        compose.onNodeWithText("Reject match").performClick()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Confirm transfer").performClick()
        compose.runOnIdle { assertEquals(listOf(TransferPairDecision.REJECTED, TransferPairDecision.DEFERRED, TransferPairDecision.CONFIRMED), decisions) }
    }
    @Test fun savingDisablesAllDecisions() {
        compose.setContent { MaterialTheme { OwnedTransferReviewCard(candidate, "Synthetic Bank", "M-Pesa", true) {} } }
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("Reject match").assertIsNotEnabled()
        compose.onNodeWithText("Not now").assertIsNotEnabled()
    }
    @Test fun detailOfMirrorOffersExplicitPairedUndoAndCancelKeepsTheMatch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        val prefs = context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_name", "James Mwangi Waweru").commit()
        try {
            val consent = AccountOwnershipConsentStore(context)
            val bank = consent.createOwnedAccount("Synthetic Bank", AccountType.BANK)
            consent.recordConsent(AccountStore.DEFAULT_MPESA_ID, true)
            val db = PesaTrackDatabase.get(context).writableDatabase
            listOf(bank.id, AccountStore.DEFAULT_MPESA_ID).forEachIndexed { index, account ->
                db.insertOrThrow("transactions", null, ContentValues().apply {
                    put("id", index + 101); put("fingerprint", "undo-ui-b2-$index"); put("transaction_code", "UNDOTXN01$index")
                    put("type", "OTHER"); put("amount_minor", 10000); put("fee_minor", 0)
                    put("counterparty", "James Mwangi Waweru"); put("occurred_at_epoch_ms", 1000)
                    put("received_at_epoch_ms", 1000); put("created_at_epoch_ms", 1000); put("source_kind", "statement_import")
                    put("direction", if (index == 0) "EXPENSE" else "INCOME"); put("category", "UNCATEGORIZED")
                    put("review_state", "NEEDS_REVIEW"); put("account_id", account)
                })
            }
            db.insertOrThrow("transaction_external_references", null, ContentValues().apply {
                put("transaction_id", 101); put("reference_value", "UNDOTXN011"); put("source_kind", "statement_import")
                put("observed_at_epoch_ms", 1000)
            })
            val store = OwnedTransferReviewStore(context)
            store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
            compose.setContent { MaterialTheme { TransactionDetailScreen(PaddingValues(), 102, onBack = {}) } }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Undo transfer match"))
            compose.onNodeWithText("Undo transfer match").performClick()
            compose.onNodeWithText("Cancel").performClick()
            compose.runOnIdle { assertTrue(store.isConfirmedPair(102)) }
            compose.onNodeWithText("Undo transfer match").performClick()
            compose.onNodeWithText("Undo both entries").performClick()
            compose.waitUntil(15_000) { !store.isConfirmedPair(102) && compose.onAllNodesWithText("Undo transfer match").fetchSemanticsNodes().isEmpty() }
            compose.runOnIdle {
                assertEquals("EXPENSE", TransactionStore.get(context).getById(101)?.direction)
                assertEquals("NEEDS_REVIEW", TransactionStore.get(context).getById(102)?.reviewState)
            }
        } finally {
            compose.runOnIdle { }
            prefs.edit().remove("profile_name").commit(); PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        }
    }
    @Test fun staleOwnershipRefreshesTheRealSectionWithoutChangingHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        val prefs = context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_name", "James Mwangi Waweru").commit()
        try {
            val consent = AccountOwnershipConsentStore(context)
            val bank = consent.createOwnedAccount("Synthetic Bank", AccountType.BANK)
            consent.recordConsent(AccountStore.DEFAULT_MPESA_ID, true)
            val db = PesaTrackDatabase.get(context).writableDatabase
            listOf(bank.id, AccountStore.DEFAULT_MPESA_ID).forEachIndexed { index, account ->
                db.insertOrThrow("transactions", null, ContentValues().apply {
                    put("id", index + 101); put("fingerprint", "ui-b2-$index"); put("transaction_code", "UITXN01$index")
                    put("type", "OTHER"); put("amount_minor", 10000); put("fee_minor", 0)
                    put("counterparty", "James Waweru");
                    put("occurred_at_epoch_ms", 1000); put("received_at_epoch_ms", 1000); put("created_at_epoch_ms", 1000)
                    put("source_kind", "statement_import"); put("direction", if (index == 0) "EXPENSE" else "INCOME")
                    put("category", "UNCATEGORIZED"); put("review_state", "NEEDS_REVIEW"); put("account_id", account)
                })
            }
            db.insertOrThrow("transaction_external_references", null, ContentValues().apply {
                put("transaction_id", 101); put("reference_value", "UITXN011"); put("source_kind", "statement_import")
                put("observed_at_epoch_ms", 1000)
            })
            compose.setContent { MaterialTheme { OwnedTransferReviewSection() } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Confirm transfer").fetchSemanticsNodes().isNotEmpty() }
            consent.recordConsent(bank.id, false) // Keep the already-rendered card deliberately stale.
            compose.onNodeWithText("Confirm transfer").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("This transfer changed. Check the refreshed details before deciding.").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Confirm transfer").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Confirm transfer").assertDoesNotExist()
            db.rawQuery("SELECT COUNT(*) FROM transactions WHERE direction='TRANSFER' OR review_state='EXCLUDED'", null).use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
        } finally {
            prefs.edit().remove("profile_name").commit()
            PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        }
    }
}
