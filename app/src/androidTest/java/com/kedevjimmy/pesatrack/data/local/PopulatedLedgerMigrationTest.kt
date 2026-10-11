package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Invented, relationship-rich persisted ledger; this tests preservation, not a new migration feature. */
@RunWith(AndroidJUnit4::class)
class PopulatedLedgerMigrationTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun version33UpgradePreservesRelationshipsCorrectionsAndTrustedTotals() {
        val legacy = PesaTrackDatabase(context)
        try {
            val db = legacy.writableDatabase
            removeV34DebtProvenanceColumn(db)
            populate(db)
            db.version = 33
        } finally { legacy.close() }

        val upgraded = PesaTrackDatabase.get(context).writableDatabase
        assertEquals(38, upgraded.version)
        upgraded.rawQuery("""
            SELECT transaction_code, amount_minor, fee_minor, category, review_state,
                   account_id, rule_id, occurred_at_epoch_ms, planning_month
            FROM transactions WHERE id=101
        """.trimIndent(), null).use {
            assertTrue(it.moveToFirst())
            assertEquals("FIXTURE-EXPENSE", it.getString(0))
            assertEquals(10000L, it.getLong(1))
            assertEquals(100L, it.getLong(2))
            assertEquals("GROCERIES", it.getString(3))
            assertEquals("USER_CLASSIFIED", it.getString(4))
            assertEquals(AccountStore.DEFAULT_MPESA_ID, it.getString(5))
            assertEquals("fixture-rule", it.getString(6))
            assertEquals(EPOCH, it.getLong(7))
            assertEquals("2026-09", it.getString(8))
        }
        upgraded.rawQuery("SELECT relation_type, related_transaction_id, review_state FROM transactions WHERE id=102", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("REFUND", it.getString(0))
            assertEquals(101L, it.getLong(1))
            assertEquals("USER_CLASSIFIED", it.getString(2))
        }
        upgraded.rawQuery("""
            SELECT id, event_key, linked_transaction_id, related_debt_event_id,
                   funding_account_id, funding_link_user_decision, amount_is_net_proceeds
            FROM debt_events WHERE id=202
        """.trimIndent(), null).use {
            assertTrue(it.moveToFirst())
            assertEquals(202L, it.getLong(0))
            assertEquals("fixture-repayment", it.getString(1))
            assertEquals(103L, it.getLong(2))
            assertEquals(201L, it.getLong(3))
            assertEquals(AccountStore.DEFAULT_MPESA_ID, it.getString(4))
            assertEquals("USER_LINKED", it.getString(5))
            assertTrue("Old events must not invent net-proceeds provenance", it.isNull(6))
        }
        upgraded.rawQuery("SELECT amount_minor, outstanding_after_minor, amount_is_net_proceeds FROM debt_events WHERE id=201", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(50000L, it.getLong(0))
            assertEquals(50000L, it.getLong(1))
            assertTrue(it.isNull(2))
        }
        upgraded.rawQuery("SELECT outstanding_minor, balance_quality FROM debt_accounts WHERE id='fixture-debt'", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(47000L, it.getLong(0))
            assertEquals("OBSERVED", it.getString(1))
        }
        upgraded.rawQuery("SELECT category, limit_minor FROM category_budgets", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("GROCERIES", it.getString(0))
            assertEquals(80000L, it.getLong(1))
        }
        upgraded.rawQuery("""
            SELECT p.identity_id, p.category_id, p.confirmation_count, p.source_transaction_id,
                   o.transaction_id, o.account_id, n.canonical_name, n.user_confirmed
            FROM identity_category_preferences p
            JOIN identity_nodes n ON n.identity_id=p.identity_id
            JOIN identity_observations o ON o.identity_id=p.identity_id
        """.trimIndent(), null).use {
            assertTrue(it.moveToFirst())
            assertEquals("fixture-merchant", it.getString(0))
            assertEquals("GROCERIES", it.getString(1))
            assertEquals(4, it.getInt(2))
            assertEquals(101L, it.getLong(3))
            assertEquals(101L, it.getLong(4))
            assertEquals(AccountStore.DEFAULT_MPESA_ID, it.getString(5))
            assertEquals("Invented Grocer", it.getString(6))
            assertEquals(1, it.getInt(7))
        }
        upgraded.rawQuery("SELECT transaction_id, identity_id, category_id, confirmation_count FROM personal_learning_applications", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(101L, it.getLong(0))
            assertEquals("fixture-merchant", it.getString(1))
            assertEquals("GROCERIES", it.getString(2))
            assertEquals(4, it.getInt(3))
        }
        upgraded.rawQuery("SELECT identity_id, normalized_value, user_confirmed FROM identity_aliases WHERE id=301", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("fixture-merchant", it.getString(0))
            assertEquals("INVENTED GROCER", it.getString(1))
            assertEquals(1, it.getInt(2))
        }
        val identity = IdentityGraphStore.get(context).resolve("Invented Grocer", null)
        assertNotNull(identity)
        assertEquals("fixture-merchant", identity!!.node.identityId)
        val application = PersonalLearningStore.get(context).applicationFor(101)
        assertNotNull(application)
        assertEquals("fixture-merchant", application!!.identityId)
        assertEquals("GROCERIES", application.categoryId)
        assertEquals(4, application.confirmationCount)
        val snapshot = TransactionStore.get(context).activitySnapshotBetween(
            EPOCH - 1, EPOCH + DAY, nowEpochMs = EPOCH + DAY, performMaintenance = false
        )
        assertEquals(3, snapshot.transactions.size)
        assertEquals(8000L, snapshot.summary.spentMinor)
        assertEquals(2000L, snapshot.summary.refundMinor)
        assertEquals(100L, snapshot.summary.feesMinor)
        assertEquals(3000L, snapshot.summary.transferMinor)
        assertEquals(-8100L, snapshot.summary.netMinor)

        // Preserve the functional relationship, not only stored foreign-key-like integers.
        upgraded.execSQL("UPDATE transactions SET category='TRANSPORT' WHERE id=101")
        upgraded.rawQuery("SELECT category FROM transactions WHERE id=102", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("TRANSPORT", it.getString(0))
        }
    }

    private fun populate(db: SQLiteDatabase) {
        db.execSQL("""
            INSERT INTO smart_rules(id,name,counterparty_contains,category,direction,enabled,created_at_epoch_ms,updated_at_epoch_ms)
            VALUES('fixture-rule','Invented groceries','Invented Grocer','GROCERIES','EXPENSE',1,$EPOCH,$EPOCH)
        """.trimIndent())
        insertTransaction(db, 101, "FIXTURE-EXPENSE", 10000, 100, "EXPENSE", EPOCH)
        insertTransaction(db, 102, "FIXTURE-REFUND", 2000, 0, "TRANSFER", EPOCH + 3600000)
        insertTransaction(db, 103, "FIXTURE-REPAY", 3000, 0, "TRANSFER", EPOCH + 7200000)
        db.execSQL("UPDATE transactions SET relation_type='REFUND', related_transaction_id=101 WHERE id=102")
        db.execSQL("UPDATE transactions SET rule_id='fixture-rule', planning_month='2026-09' WHERE id=101")
        db.execSQL("""
            INSERT INTO debt_accounts(id,provider_key,name,outstanding_minor,balance_quality,created_at_epoch_ms,updated_at_epoch_ms)
            VALUES('fixture-debt','INVENTED_LENDER','Invented lender',47000,'OBSERVED',$EPOCH,$EPOCH)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO debt_events(id,event_key,debt_account_id,event_type,amount_minor,outstanding_after_minor,source_kind,occurred_at_epoch_ms,created_at_epoch_ms)
            VALUES(201,'fixture-drawdown','fixture-debt','DRAWDOWN',50000,50000,'SYNTHETIC_TEST',$EPOCH,$EPOCH)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO debt_events(id,event_key,debt_account_id,event_type,amount_minor,outstanding_after_minor,linked_transaction_id,related_debt_event_id,funding_account_id,funding_link_user_decision,source_kind,occurred_at_epoch_ms,created_at_epoch_ms)
            VALUES(202,'fixture-repayment','fixture-debt','REPAYMENT',3000,47000,103,201,?,'USER_LINKED','SYNTHETIC_TEST',?,?)
        """.trimIndent(), arrayOf<Any>(AccountStore.DEFAULT_MPESA_ID, EPOCH + 7200000, EPOCH + 7200000))
        db.execSQL("INSERT INTO category_budgets VALUES('GROCERIES',80000,$EPOCH,$EPOCH)")
        db.execSQL("""
            INSERT INTO identity_nodes(identity_id,kind,canonical_name,ownership,confidence,user_confirmed,first_seen_epoch_ms,last_seen_epoch_ms,updated_at_epoch_ms)
            VALUES('fixture-merchant','BUSINESS','Invented Grocer','EXTERNAL','USER_CONFIRMED',1,$EPOCH,$EPOCH,$EPOCH)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO identity_aliases(id,identity_id,alias_type,normalized_value,display_value,source,confidence,user_confirmed,first_seen_epoch_ms,last_seen_epoch_ms)
            VALUES(301,'fixture-merchant','NAME','INVENTED GROCER','Invented Grocer','USER_CONFIRMATION','USER_CONFIRMED',1,$EPOCH,$EPOCH)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO identity_observations(transaction_id,identity_id,account_id,direction,observed_name,occurred_at_epoch_ms)
            VALUES(101,'fixture-merchant',?,'EXPENSE','Invented Grocer',?)
        """.trimIndent(), arrayOf<Any>(AccountStore.DEFAULT_MPESA_ID, EPOCH))
        db.execSQL("""
            INSERT INTO identity_category_preferences(identity_id,category_id,direction,confirmation_count,source_transaction_id,created_at_epoch_ms,updated_at_epoch_ms)
            VALUES('fixture-merchant','GROCERIES','EXPENSE',4,101,$EPOCH,$EPOCH)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO personal_learning_applications(transaction_id,identity_id,category_id,direction,confidence,confirmation_count,applied_at_epoch_ms)
            VALUES(101,'fixture-merchant','GROCERIES','EXPENSE','HIGH',4,$EPOCH)
        """.trimIndent())
    }

    private fun insertTransaction(db: SQLiteDatabase, id: Long, code: String, amount: Long,
        fee: Long, direction: String, occurred: Long) {
        db.insertOrThrow("transactions", null, ContentValues().apply {
            put("id", id); put("transaction_code", code); put("fingerprint", "synthetic-$code")
            put("type", "BUY_GOODS"); put("amount_minor", amount); put("fee_minor", fee)
            put("counterparty", "Invented Grocer $id"); put("source_kind", "SYNTHETIC_TEST")
            put("occurred_at_epoch_ms", occurred); put("received_at_epoch_ms", occurred)
            put("created_at_epoch_ms", occurred); put("direction", direction)
            put("category", "GROCERIES"); put("review_state", "USER_CLASSIFIED")
            put("account_id", AccountStore.DEFAULT_MPESA_ID)
        })
    }

    companion object {
        private const val EPOCH = 1788220800000L
        private const val DAY = 86400000L
    }
}




