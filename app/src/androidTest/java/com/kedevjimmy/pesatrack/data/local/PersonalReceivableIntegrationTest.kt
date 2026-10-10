package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Invented records only. No user SMS or financial accounts are accessed. */
@RunWith(AndroidJUnit4::class)
class PersonalReceivableIntegrationTest {
    private lateinit var context: Context
    private val db get() = PesaTrackDatabase.get(context).writableDatabase
    private val receivables get() = PersonalReceivableStore(context)
    private val transactions get() = TransactionStore.get(context)
    private val now = 1_790_000_000_000L

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }
    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    private fun transaction(index: Int, amount: Long, direction: String): Long =
        db.insertOrThrow("transactions", null, ContentValues().apply {
            put("fingerprint", "receivable-invented-" + index)
            put("transaction_code", "TESTLOAN" + index)
            put("type", if (direction == "EXPENSE") "SENT_MONEY" else "RECEIVED")
            put("amount_minor", amount); put("fee_minor", 0L)
            put("counterparty", "SAMPLE BORROWER")
            put("counterparty_phone", "0712000000")
            put("occurred_at_epoch_ms", now - 600000L + index)
            put("received_at_epoch_ms", now - 600000L + index)
            put("created_at_epoch_ms", now - 600000L + index)
            put("source_kind", "synthetic_test")
            put("direction", direction); put("category", "EATING_OUT")
            put("review_state", "USER_CLASSIFIED")
            put("account_id", AccountStore.DEFAULT_MPESA_ID)
        })

    @Test fun linkedRestaurantShareAndRepaymentPreserveCashWithoutFalseIncome() {
        val outgoing = transaction(1, 160_000L, "EXPENSE")
        val incoming = transaction(2, 30_000L, "INCOME")
        val date = System.currentTimeMillis() - 60_000L
        receivables.record(null, "Sample Borrower", "0712000000",
            PersonalReceivableStore.LENT, 80_000L, date, outgoing)
        val person = receivables.people().single()
        assertEquals(80_000L, person.outstandingMinor)

        receivables.record(person.id, person.name, person.phone,
            PersonalReceivableStore.REPAID, 30_000L, date, incoming)
        assertEquals(50_000L, receivables.people().single().outstandingMinor)
        assertEquals(80_000L, receivables.linkedShares()[outgoing]?.lentMinor)
        assertEquals(30_000L, receivables.linkedShares()[incoming]?.repaidMinor)
        assertEquals(160_000L, transactions.getById(outgoing)!!.amountMinor)
        assertEquals(30_000L, transactions.getById(incoming)!!.amountMinor)

        val rows = listOf(transactions.getById(outgoing)!!, transactions.getById(incoming)!!)
        val projected = BudgetSpendingProjection.calculate(
            rows, { null }, { it },
            receivableLent = { receivables.linkedShares()[it]?.lentMinor ?: 0L }
        )
        assertEquals(80_000L, projected["EATING_OUT"])
        val snapshot = transactions.activitySnapshotBetween(now - 700000L, now,
            performMaintenance = false)
        assertEquals(0L, snapshot.summary.incomeMinor)
        assertEquals(80_000L, snapshot.summary.spentMinor)
        assertEquals(-130_000L, snapshot.summary.netMinor)
    }

    @Test fun linkedLoanCannotExceedPrincipalOrBeLinkedTwice() {
        val payment = transaction(3, 50_000L, "EXPENSE")
        val date = System.currentTimeMillis() - 60_000L
        try {
            receivables.record(null, "Borrower A", null,
                PersonalReceivableStore.LENT, 60_000L, date, payment)
            throw AssertionError("Over-allocation was allowed")
        } catch (_: IllegalArgumentException) {}
        assertTrue(receivables.people().isEmpty())
        receivables.record(null, "Borrower A", null,
            PersonalReceivableStore.LENT, 50_000L, date, payment)
        try {
            receivables.record(null, "Borrower B", null,
                PersonalReceivableStore.LENT, 1_000L, date, payment)
            throw AssertionError("Duplicate allocation was allowed")
        } catch (_: IllegalArgumentException) {}
        assertEquals(1, receivables.people().size)
    }

    @Test fun manualRecordCannotCreateCashAndOverpaymentIsRejected() {
        val date = System.currentTimeMillis() - 60_000L
        receivables.record(null, "Borrower A", null,
            PersonalReceivableStore.LENT, 50_000L, date, memo = "cash lent outside bank records")
        val person = receivables.people().single()
        assertEquals(50_000L, person.outstandingMinor)
        assertTrue(receivables.linkedShares().isEmpty())
        try {
            receivables.record(person.id, person.name, person.phone,
                PersonalReceivableStore.REPAID, 60_000L, date)
            throw AssertionError("Overpayment accepted")
        } catch (_: IllegalArgumentException) {}
        assertEquals(50_000L, receivables.people().single().outstandingMinor)
    }

    @Test fun sourceExclusionAndUnlinkDoNotEraseOriginalActivity() {
        val payment = transaction(4, 25_000L, "EXPENSE")
        val date = System.currentTimeMillis() - 60_000L
        receivables.record(null, "Borrower C", null,
            PersonalReceivableStore.LENT, 20_000L, date, payment)
        val entry = receivables.eventForTransaction(payment)!!
        assertTrue(entry.active)
        db.update("transactions", ContentValues().apply { put("review_state", "EXCLUDED") },
            "id = ?", arrayOf(payment.toString()))
        assertFalse(receivables.eventForTransaction(payment)!!.active)
        assertEquals(0L, receivables.people().single().outstandingMinor)
        assertTrue(receivables.linkedShares().isEmpty())
        receivables.removeEvent(entry.id)
        assertEquals(0L, receivables.people().single().outstandingMinor)
        assertEquals(25_000L, transactions.getById(payment)!!.amountMinor)
    }

    @Test fun schema34MigrationAndEncryptedRestorePreserveLedger() = migrateAndRestore(34)
    @Test fun schema35MigrationAndEncryptedRestorePreserveAllocations() = migrateAndRestore(35)

    private fun migrateAndRestore(oldVersion: Int) {
        val payment = transaction(20 + oldVersion, 50_000L, "EXPENSE")
        val legacy = db
        if (oldVersion == 35) legacy.execSQL(
            "INSERT INTO transaction_allocations(transaction_id, position, category, amount_minor) VALUES (?, 0, 'EATING_OUT', 50000)",
            arrayOf<Any>(payment))
        legacy.execSQL("DROP TABLE personal_receivable_events")
        legacy.execSQL("DROP TABLE personal_receivable_people")
        if (oldVersion == 34) {
            legacy.execSQL("DROP TABLE transaction_allocations")
            legacy.execSQL("DROP TABLE flexible_budget_options")
            legacy.execSQL("DROP TABLE statement_import_entries")
        }
        legacy.version = oldVersion
        val manager = com.kedevjimmy.pesatrack.security.PesaBackupManager(context)
        val encrypted = manager.exportEncryptedBytes("invented-migration-passphrase")
        PesaTrackDatabase.resetForTests()
        assertEquals(36, db.version)
        assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
        assertTrue(receivables.people().isEmpty())
        repeat(2) {
            manager.previewEncryptedBytes(encrypted, "invented-migration-passphrase")
            manager.restoreEncryptedBytes(encrypted, "invented-migration-passphrase")
            assertEquals(36, db.version)
            assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
            assertTrue(receivables.people().isEmpty())
            assertTrue(receivables.linkedShares().isEmpty())
            db.rawQuery("SELECT COUNT(*) FROM transaction_allocations WHERE transaction_id = ?",
                arrayOf(payment.toString())).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(if (oldVersion == 35) 1 else 0, cursor.getInt(0))
            }
        }
        PesaTrackDatabase.resetForTests()
        assertEquals(36, db.version)
        assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
    }
}
