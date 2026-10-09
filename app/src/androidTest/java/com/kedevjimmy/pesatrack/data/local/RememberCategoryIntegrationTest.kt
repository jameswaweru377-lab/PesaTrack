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
        val automaticCategory = insert(512, category = "TRANSPORT")
        val repo = RememberCategoryRepository(context)
        assertEquals(506, repo.matchingCount(store.getById(source)!!))
        assertEquals(506, repo.remember(source, TransactionCategory.INVESTMENT.name))
        assertEquals("INVESTMENT", store.getById(automaticCategory)!!.category)
        assertEquals("INVESTMENT", store.getById(source)!!.category)
        assertEquals("TRANSFER", store.getById(source)!!.direction)
        assertEquals("GROCERIES", store.getById(manual)!!.category)
        listOf(incoming, refund, excluded, unrelated).forEach { assertEquals("OTHER", store.getById(it)!!.category) }
        val future = insert(511)
        val learning = PersonalLearningStore.get(context)
        assertEquals(1, learning.applyHighConfidenceTo(store.recentlyInsertedForLearning(limit = 1)))
        assertEquals("INVESTMENT", store.getById(future)!!.category)
        assertEquals(0, learning.applyHighConfidenceTo(store.recentlyInsertedForLearning(limit = 1)))
    }
    @Test fun rememberedEatingOutIsImmediatelyVisibleInActivityRows() {
        val source = insert(0, name = "Elizabeth", category = "UNCATEGORIZED")
        insert(1, name = "Elizabeth", category = "UNCATEGORIZED")
        insert(2, name = "Elizabeth", category = "UNCATEGORIZED")
        val repo = RememberCategoryRepository(context)
        assertEquals(2, repo.matchingCount(store.getById(source)!!))
        assertEquals(2, repo.remember(source, TransactionCategory.EATING_OUT.name))

        val snapshot = store.activitySnapshot(nowEpochMs = 1760000000000L)
        val elizabethRows = snapshot.transactions.filter { it.counterparty == "Elizabeth" }
        assertEquals(3, elizabethRows.size)
        assertTrue(elizabethRows.all { it.category == TransactionCategory.EATING_OUT.name })
        assertTrue(
            elizabethRows.all {
                CategoryStore.get(context).labelFor(it.category) == "Eating out"
            }
        )
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
    @Test fun inventedMerchantCorrectionHandlesUnicodeSpacingButRequiresRememberedConsent() {
        val source = insert(0, name = "SAMPLE WATERFALL MARKET")
        val spaced = insert(1, name = "SAMPLE\u00a0WATERFALL\u202fMARKET")
        val unrelated = insert(2, name = "SAMPLE WATERFALL MARKET SERVICES")
        store.classifyManually(source, "GROCERIES", MoneyDirection.EXPENSE)
        val learning = PersonalLearningStore.get(context)
        learning.applyHighConfidenceTo(store.categoryLearningCandidates())
        assertEquals("OTHER", store.getById(spaced)!!.category)
        val repo = RememberCategoryRepository(context)
        assertEquals(1, repo.matchingCount(store.getById(source)!!))
        assertEquals(1, repo.remember(source, "GROCERIES"))
        assertEquals("GROCERIES", store.getById(spaced)!!.category)
        assertEquals("OTHER", store.getById(unrelated)!!.category)
        val future = insert(3, name = "SAMPLE\u202fWATERFALL\u00a0MARKET")
        assertEquals(1, learning.applyHighConfidenceTo(store.recentlyInsertedForLearning(limit = 1)))
        assertEquals("GROCERIES", store.getById(future)!!.category)
    }

    @Test fun failedHistoryWriteRollsBackPreferenceAndSourceCategory() {
        val source = insert(0)
        val other = insert(1)
        db.execSQL("CREATE TRIGGER fail_learning BEFORE INSERT ON personal_learning_applications BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try {
            RememberCategoryRepository(context).remember(source, "GROCERIES")
            fail("Expected the failed write to roll back")
        } catch (_: RuntimeException) { }
        assertEquals("OTHER", store.getById(source)!!.category)
        assertEquals("OTHER", store.getById(other)!!.category)
        assertEquals(0, PersonalLearningStore.get(context).preferenceCount())
    }

    @Test fun debtRefreshKeepsUserDecisionAndRejectsSafaricomOverlap() {
        insert(0, "Safaricom")
        val reviews = RelationshipReviewStore.get(context)
        assertTrue(reviews.pendingReviews().none { it.kind == RelationshipReviewKind.LIABILITY_PAYMENT })
        db.execSQL("INSERT INTO debt_accounts(id,provider_key,name,created_at_epoch_ms,updated_at_epoch_ms) VALUES('other','OTHER_LOAN','Other loan',1,1)")
        db.execSQL("INSERT INTO debt_events(event_key,debt_account_id,event_type,amount_minor,funding_account_key,source_kind,occurred_at_epoch_ms,created_at_epoch_ms) VALUES('draw','other','DRAWDOWN',2000,'wallet','TEST',10000,10000)")
        db.execSQL("INSERT INTO debt_events(event_key,debt_account_id,event_type,amount_minor,funding_account_key,funding_link_user_decision,funding_link_reason,source_kind,occurred_at_epoch_ms,created_at_epoch_ms) VALUES('repay','debt-fuliza-mpesa','REPAYMENT',2000,'wallet','REJECTED','User decision','TEST',10000,10000)")
        DebtStore.get(context).refreshPendingRepaymentLinks()
        db.rawQuery("SELECT related_debt_event_id,funding_link_reason FROM debt_events WHERE event_key='repay'", null).use {
            assertTrue(it.moveToFirst()); assertTrue(it.isNull(0)); assertEquals("User decision", it.getString(1))
        }
    }

}
