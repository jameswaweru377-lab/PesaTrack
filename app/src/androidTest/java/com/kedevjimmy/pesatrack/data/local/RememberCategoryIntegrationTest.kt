package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberCategoryIntegrationTest {
    private lateinit var context: Context
    private val db get() = PesaTrackDatabase.get(context).writableDatabase
    private val store get() = TransactionStore.get(context)
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    @After fun cleanup() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    private fun insert(index: Int, name: String = "ZD", phone: String? = null, category: String = "OTHER",
        review: String = "AUTO_CLASSIFIED", direction: String = "EXPENSE", relation: String? = null): Long {
        val time = 1760000000000L + index * 86400000L
        return db.insertOrThrow("transactions", null, ContentValues().apply {
            put("fingerprint", "remember-$index"); put("transaction_code", "R$index"); put("type", "SEND_MONEY")
            put("amount_minor", 2000L); put("fee_minor", 0L); put("counterparty", name); put("counterparty_phone", phone)
            put("occurred_at_epoch_ms", time); put("received_at_epoch_ms", time); put("created_at_epoch_ms", time)
            put("source_kind", "TEST"); put("direction", direction); put("category", category); put("review_state", review)
            put("account_id", AccountStore.DEFAULT_MPESA_ID); put("relation_type", relation)
        })
    }
    @Test fun rememberInvestmentUpdatesAllMonthsBeyondFiveHundredAndFutureImports() {
        val source = insert(0)
        repeat(505) { insert(it + 1) }
        val manual = insert(506, category = "GROCERIES", review = "USER_CLASSIFIED")
        val incoming = insert(507, direction = "INCOME")
        val refund = insert(508, relation = "REFUND")
        val excluded = insert(509, review = "EXCLUDED")
        val unrelated = insert(510, name = "ZD OTHER")
        val repo = RememberCategoryRepository(context)
        assertEquals(505, repo.matchingCount(store.getById(source)!!))
        assertEquals(505, repo.remember(source, TransactionCategory.INVESTMENT.name))
        assertEquals("INVESTMENT", store.getById(source)!!.category)
        assertEquals("TRANSFER", store.getById(source)!!.direction)
        assertEquals("GROCERIES", store.getById(manual)!!.category)
        listOf(incoming, refund, excluded, unrelated).forEach { assertEquals("OTHER", store.getById(it)!!.category) }
        val future = insert(511)
        val learning = PersonalLearningStore.get(context)
        assertEquals(1, learning.applyHighConfidenceTo(store.categoryLearningCandidates()))
        assertEquals("INVESTMENT", store.getById(future)!!.category)
        assertEquals(0, learning.applyHighConfidenceTo(store.categoryLearningCandidates()))
    }
    @Test fun manualChoiceDoesNotTeachAndConflictingPhonesStaySeparate() {
        val source = insert(0, "James", "0712345678")
        val same = insert(1, "James", "+254712345678")
        val other = insert(2, "James", "0798765432")
        val missingPhone = insert(3, "James")
        val repo = RememberCategoryRepository(context)
        assertEquals(1, repo.matchingCount(store.getById(source)!!))
        assertEquals(1, repo.remember(source, "GROCERIES"))
        assertEquals("GROCERIES", store.getById(same)!!.category)
        assertEquals("OTHER", store.getById(other)!!.category)
        assertEquals("OTHER", store.getById(missingPhone)!!.category)
        val single = insert(4, "Mwengo")
        val untouched = insert(5, "Mwengo")
        store.classifyManually(single, "GROCERIES", MoneyDirection.EXPENSE)
        PersonalLearningStore.get(context).applyHighConfidenceTo(store.categoryLearningCandidates())
        assertEquals("OTHER", store.getById(untouched)!!.category)
    }
}
