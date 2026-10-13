package com.kedevjimmy.pesatrack.ui

import android.content.Context
import android.content.ContentValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.planning.SmartNoticePreferences
import com.kedevjimmy.pesatrack.ui.screens.SmartNoticeReviewScreen
import com.kedevjimmy.pesatrack.ui.screens.SmartPaymentReviewHint
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import androidx.compose.foundation.layout.PaddingValues
import org.junit.*
import org.junit.Assert.*

/** Synthetic UI coverage compiled in normal CI; device execution remains paused. */
class SmartNoticeJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("review_attention_reminders",Context.MODE_PRIVATE).edit().clear().commit()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        compose.runOnIdle {}
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @Test fun stalePaymentShowsResolvedStateWithoutFinancialDialog() {
        compose.setContent { PesaTrackTheme {
            SmartNoticeReviewScreen(PaddingValues(),"payment:deleted","entry",{}, {})
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("smart-notice-resolved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("This item no longer needs this review.").assertIsDisplayed()
        compose.onNodeWithText("Record money lent").assertDoesNotExist()
    }
    @Test fun cleanSlateDiscoveryOpensManualEntryAndCompletesTip() {
        compose.setContent { PesaTrackTheme {
            SmartNoticeReviewScreen(PaddingValues(),"discovery","review",{}, {})
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Record money lent").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Record money lent").assertIsDisplayed()
        compose.onNodeWithText("Find in transactions").assertIsDisplayed()
        assertFalse(SmartNoticePreferences(context).discoveryPending())
    }
    @Test fun inlinePaymentReviewIsAvailableWithoutPostingAndRequiresExplicitFinancialSave() {
        val time = System.currentTimeMillis()
        val db = PesaTrackDatabase.get(context).writableDatabase
        val journal = PersonalReceivableStore(context)
        journal.record(null,"Sample Borrower","0712000000","LENT",200_000,time - 2 * 86_400_000L)
        db.update("personal_receivable_events",ContentValues().apply { put("created_at_epoch_ms",time - 2 * 86_400_000L) },null,null)
        context.getSharedPreferences("review_attention_reminders",Context.MODE_PRIVATE).edit()
            .putBoolean("payment_enabled",true).putLong("payment_enabled_at",time - 86_400_000L).commit()
        val id = db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","inline-notice-synthetic"); put("transaction_code","INLINETEST")
            put("type","RECEIVED"); put("amount_minor",50_000L); put("fee_minor",0L)
            put("counterparty","Sample Borrower"); put("counterparty_phone","0712000000")
            put("occurred_at_epoch_ms",time - 3 * 3_600_000L); put("received_at_epoch_ms",time - 3 * 3_600_000L)
            put("created_at_epoch_ms",time - 3 * 3_600_000L); put("source_kind","financial_notification")
            put("direction","INCOME"); put("category","INCOME"); put("review_state","AUTO_CLASSIFIED")
            put("account_id",AccountStore.DEFAULT_MPESA_ID)
        })
        val source = TransactionStore.get(context).getById(id)!!
        compose.setContent { PesaTrackTheme { SmartPaymentReviewHint(source) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("they-owe-me-payment-hint").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Review options").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Review repayment").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(200_000L,journal.people().single().outstandingMinor)
        assertEquals(source,TransactionStore.get(context).getById(id))
    }
}
