package com.kedevjimmy.pesatrack.ui

import android.content.ContentValues
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.screens.ReceivableEntryDialog
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Compiled fixtures; execution remains opt-in. Invented records only. */
@RunWith(AndroidJUnit4::class)
class SharedBillEntryJourneyTest {
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
    private fun row(index: Int,name: String,phone: String,amount: Long): StoredTransaction {
        val time = System.currentTimeMillis() - 600000 + index
        val id = db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","shared-ui-" + index); put("transaction_code","SHAREDUI" + index)
            put("type","SENT_MONEY"); put("amount_minor",amount); put("fee_minor",0L)
            put("counterparty",name); put("counterparty_phone",phone)
            put("occurred_at_epoch_ms",time); put("received_at_epoch_ms",time); put("created_at_epoch_ms",time)
            put("source_kind","synthetic_test"); put("direction","EXPENSE")
            put("category","EATING_OUT"); put("review_state","USER_CLASSIFIED")
            put("account_id",AccountStore.DEFAULT_MPESA_ID)
        })
        return TransactionStore.get(context).getById(id)!!
    }
    private fun open(source: StoredTransaction,shared: Boolean,onSaved: () -> Unit = {}) {
        val people = PersonalReceivableStore(context).people()
        compose.setContent {
            PesaTrackTheme {
                ReceivableEntryDialog(people,null,"LENT",source,shared,onDismiss = {},onSaved = onSaved)
            }
        }
    }
    private fun text(tag: String) =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    @Test fun merchantIsNeverPrefilledAndFullOrExcessShareCannotBeSaved() {
        val merchant = row(1,"Sample Restaurant","0712345678",20000)
        open(merchant,true)
        assertEquals("",text("shared-person-name"))
        assertEquals("",text("receivable-entry-amount"))
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("shared-person-name").performTextInput("Norman")
        listOf("0","-1","200","300").forEach {
            compose.onNodeWithTag("receivable-entry-amount").performTextReplacement(it)
            compose.onNodeWithText("Save").assertIsNotEnabled()
        }
        assertTrue(PersonalReceivableStore(context).people().isEmpty())
    }

    @Test fun selectingHistoryIdentityFillsFullNameAndPhoneThenSavesOnlyTheShare() {
        val friend = row(1,"Jeffrey Kirongo","0712000000",10000)
        val merchant = row(2,"Sample Restaurant","0712345678",20000)
        var saved = false
        open(merchant,true) { saved = true }
        compose.onNodeWithTag("shared-person-name").performTextInput("Jeffrey")
        compose.waitUntil(5000) { compose.onAllNodesWithTag("shared-identity-" + friend.id).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("shared-identity-" + friend.id).performScrollTo().performClick()
        assertEquals("Jeffrey Kirongo",text("shared-person-name"))
        compose.onNodeWithTag("receivable-entry-amount").performTextInput("120")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { saved }
        val store = PersonalReceivableStore(context)
        assertEquals("Jeffrey Kirongo",store.people().single().name)
        assertEquals("0712000000",store.people().single().phone)
        assertEquals(12000L,store.people().single().outstandingMinor)
        assertTrue(store.eventForTransaction(merchant.id)!!.sharedBill)
        assertEquals(null,store.eventForTransaction(friend.id))
        assertEquals(20000L,TransactionStore.get(context).getById(merchant.id)!!.amountMinor)
    }

    @Test fun moneyLentUsesTransactionRecipientAndFullAmount() {
        val source = row(3,"Norman Sample","0712000000",20000)
        var saved = false
        open(source,false) { saved = true }
        assertEquals("200.00",text("receivable-entry-amount"))
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5000) { saved }
        val store = PersonalReceivableStore(context)
        assertEquals("Norman Sample",store.people().single().name)
        assertEquals("0712000000",store.people().single().phone)
        assertEquals(20000L,store.people().single().outstandingMinor)
        assertFalse(store.eventForTransaction(source.id)!!.sharedBill)
    }
}
