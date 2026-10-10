package com.kedevjimmy.pesatrack.ui

import android.content.ContentValues
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.screens.ReceivableHistoryReview
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Synthetic UI fixtures. Compiled by CI; executed only on explicit device-review request. */
@RunWith(AndroidJUnit4::class)
class ReceivableHistoryReviewJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private val db get() = PesaTrackDatabase.get(context).writableDatabase
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        compose.runOnIdle {}
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    private fun row(index: Int, direction: String, amount: Long = 10000): Long {
        val time = System.currentTimeMillis() - 600000 + index
        return db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","history-ui-" + index); put("transaction_code","HISTORYUI" + index)
            put("type",if (direction == "EXPENSE") "SENT_MONEY" else "RECEIVED")
            put("amount_minor",amount); put("fee_minor",0L); put("counterparty","Sample Borrower")
            put("counterparty_phone","0712000000"); put("occurred_at_epoch_ms",time)
            put("received_at_epoch_ms",time); put("created_at_epoch_ms",time)
            put("source_kind","synthetic_test"); put("direction",direction)
            put("category","EATING_OUT"); put("review_state","USER_CLASSIFIED")
            put("account_id",AccountStore.DEFAULT_MPESA_ID)
        })
    }
    private fun open() {
        compose.setContent {
            PesaTrackTheme {
                ReceivableHistoryReview(null,"Sample Borrower","0712000000",{}, {})
            }
        }
        compose.onNodeWithText("Search transactions").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("select-page-loans").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("receivable-history-list").performScrollToNode(hasTestTag(tag))
    }

    @Test fun defaultsStayUnselectedAndClosingDoesNotWriteDebt() {
        val loan = row(1,"EXPENSE")
        open(); scrollTo("loan-choice-" + loan)
        compose.onNodeWithTag("loan-choice-" + loan).assertIsNotChecked()
        compose.onNodeWithText("Close").performClick()
        assertTrue(PersonalReceivableStore(context).people().isEmpty())
    }

    @Test fun editedLoanGiftAndReceiptRequirePreviewBeforeSaving() {
        val loan = row(1,"EXPENSE",160000)
        val gift = row(2,"EXPENSE",10000)
        val receipt = row(3,"INCOME",30000)
        open()
        scrollTo("history-amount-" + loan)
        compose.onNodeWithTag("history-amount-" + loan).performTextReplacement("800")
        compose.onNodeWithTag("loan-choice-" + loan).performClick()
        scrollTo("gift-choice-" + gift)
        compose.onNodeWithTag("gift-choice-" + gift).performClick()
        scrollTo("history-repayments")
        compose.onNodeWithTag("history-repayments").performClick()
        scrollTo("loan-choice-" + receipt)
        compose.onNodeWithTag("loan-choice-" + receipt).assertIsNotChecked().performClick()
        compose.onNodeWithTag("history-preview").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Still owed: KES 500.00").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(PersonalReceivableStore(context).people().isEmpty())
        compose.onNodeWithText("Confirm records").performClick()
        compose.waitUntil(5000) { PersonalReceivableStore(context).people().size == 1 }
        val person = PersonalReceivableStore(context).people().single()
        assertEquals(50000L,person.outstandingMinor)
        assertEquals(setOf(gift),ReceivableHistoryRepository(context).exclusions(person.id))
        assertEquals(160000L,TransactionStore.get(context).getById(loan)!!.amountMinor)
    }

    @Test fun selectAllDoesNotIncludeUnseenHistoryPage() {
        val ids = (1..51).map { row(it,"EXPENSE") }
        open()
        compose.onNodeWithTag("select-page-loans").performScrollTo().performClick()
        compose.onNodeWithTag("history-preview").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Still owed: KES 5,000.00").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Confirm records").performClick()
        compose.waitUntil(5000) { PersonalReceivableStore(context).people().size == 1 }
        val person = PersonalReceivableStore(context).people().single()
        assertEquals(50,PersonalReceivableStore(context).events(person.id).size)
        assertEquals(null,PersonalReceivableStore(context).eventForTransaction(ids.first()))
    }
}
