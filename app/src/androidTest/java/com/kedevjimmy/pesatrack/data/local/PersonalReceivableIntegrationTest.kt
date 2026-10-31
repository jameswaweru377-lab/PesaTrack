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

    @Test fun explicitSharedBillPersistsPurposeCategoryAndAmountThroughEncryptedRestore() {
        val payment = transaction(170,20000,"EXPENSE")
        val source = transactions.getById(payment)!!
        receivables.record(null,"Norman","0712000000","LENT",12000,source.occurredAtEpochMs,
            payment,sharedBill = true,expectedTransaction = source)
        val person = receivables.people().single()
        assertTrue(receivables.events(person.id).single().sharedBill)
        assertTrue(receivables.eventForTransaction(payment)!!.sharedBill)
        assertEquals(12000L,receivables.linkedShares()[payment]!!.lentMinor)
        assertEquals(8000L,BudgetSpendingProjection.calculate(listOf(source),{ null },{ it },
            receivableLent = { receivables.linkedShares()[it]?.lentMinor ?: 0 })["EATING_OUT"])
        assertEquals("EATING_OUT",transactions.getById(payment)!!.category)
        assertEquals(20000L,transactions.getById(payment)!!.amountMinor)
        val manager = com.kedevjimmy.pesatrack.security.PesaBackupManager(context)
        val encrypted = manager.exportEncryptedBytes("invented-shared-passphrase")
        manager.restoreEncryptedBytes(encrypted,"invented-shared-passphrase")
        assertTrue(receivables.eventForTransaction(payment)!!.sharedBill)
        assertEquals(12000L,receivables.people().single().outstandingMinor)
    }

    @Test fun sharedBillCannotEqualFullPaymentOrSaveChangedSource() {
        val payment = transaction(171,20000,"EXPENSE")
        val source = transactions.getById(payment)!!
        try {
            receivables.record(null,"Norman",null,"LENT",20000,source.occurredAtEpochMs,
                payment,sharedBill = true,expectedTransaction = source)
            throw AssertionError("Full payment accepted as shared bill")
        } catch (_: IllegalArgumentException) {}
        db.update("transactions",ContentValues().apply { put("category","TRANSPORT") },"id = ?",arrayOf(payment.toString()))
        try {
            receivables.record(null,"Norman",null,"LENT",12000,source.occurredAtEpochMs,
                payment,sharedBill = true,expectedTransaction = source)
            throw AssertionError("Changed source accepted")
        } catch (_: IllegalArgumentException) {}
        assertTrue(receivables.people().isEmpty())
        assertEquals(null,receivables.eventForTransaction(payment))
    }

    @Test fun changedBillEqualToShareIsExcludedAndOldPartialLoansRemainLoans() {
        val payment = transaction(172,20000,"EXPENSE")
        val source = transactions.getById(payment)!!
        receivables.record(null,"Norman",null,"LENT",12000,source.occurredAtEpochMs,payment,sharedBill = true)
        db.update("transactions",ContentValues().apply { put("amount_minor",12000) },"id = ?",arrayOf(payment.toString()))
        assertFalse(receivables.eventForTransaction(payment)!!.active)
        assertTrue(receivables.linkedShares().isEmpty())
        val old = transaction(173,20000,"EXPENSE")
        receivables.record(null,"Legacy",null,"LENT",12000,source.occurredAtEpochMs,old)
        assertFalse(receivables.eventForTransaction(old)!!.sharedBill)
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
        dropB2Tables(legacy)
        dropPost38Tables(legacy)
        if (oldVersion == 35) legacy.execSQL(
            "INSERT INTO transaction_allocations(transaction_id, position, category, amount_minor) VALUES (?, 0, 'EATING_OUT', 50000)",
            arrayOf<Any>(payment))
        legacy.execSQL("DROP TABLE personal_receivable_review_exclusions")
        legacy.execSQL("DROP TRIGGER IF EXISTS trg_delete_receivable_review")
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
        assertEquals(40, db.version)
        assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
        assertTrue(receivables.people().isEmpty())
        repeat(2) {
            manager.previewEncryptedBytes(encrypted, "invented-migration-passphrase")
            manager.restoreEncryptedBytes(encrypted, "invented-migration-passphrase")
            assertEquals(40, db.version)
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
        assertEquals(40, db.version)
        assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
    }

    @Test fun invalidatedLoanAfterRepaymentKeepsHistoryAvailableForCorrection() {
        val payment = transaction(90, 50_000L, "EXPENSE")
        val date = System.currentTimeMillis() - 60000L
        receivables.record(null, "Recovery Borrower", null,
            PersonalReceivableStore.LENT, 50_000L, date, payment)
        val person = receivables.people().single()
        val repayment = receivables.record(person.id, person.name, null,
            PersonalReceivableStore.REPAID, 20_000L, date)
        val secondRepayment = receivables.record(person.id, person.name, null,
            PersonalReceivableStore.REPAID, 10_000L, date)
        db.update("transactions", ContentValues().apply { put("review_state", "EXCLUDED") },
            "id = ?", arrayOf(payment.toString()))
        assertEquals(person.id, receivables.people().single().id)
        assertEquals(null, receivables.people().single().outstandingMinor)
        assertEquals(3, receivables.events(person.id).size)
        receivables.removeEvent(repayment)
        assertEquals(null, receivables.people().single().outstandingMinor)
        receivables.removeEvent(secondRepayment)
        assertEquals(0L, receivables.people().single().outstandingMinor)
        assertEquals(50_000L, transactions.getById(payment)!!.amountMinor)
    }

    @Test fun historyBatchConfirmsPartialLoanAndReceiptButKeepsGiftOut() {
        val outgoing = transaction(100,160000,"EXPENSE")
        val incoming = transaction(101,30000,"INCOME")
        val gift = transaction(102,10000,"EXPENSE")
        val repo = ReceivableHistoryRepository(context)
        val rows = repo.search("Sample Borrower",null,"Sample Borrower","0712000000")
        assertEquals(3,rows.size)
        val person = receivables.confirmHistory(null,"Sample Borrower","0712000000",
            listOf(ReceivableHistorySelection(transactions.getById(outgoing)!!,"LENT",80000),
                ReceivableHistorySelection(transactions.getById(incoming)!!,"REPAID",30000)),
            listOf(transactions.getById(gift)!!))
        assertEquals(50000L,receivables.people().single().outstandingMinor)
        assertEquals(setOf(gift),repo.exclusions(person))
        assertEquals(null,receivables.eventForTransaction(gift))
        assertEquals(160000L,transactions.getById(outgoing)!!.amountMinor)
        assertEquals("EATING_OUT",transactions.getById(outgoing)!!.category)
        assertEquals(30000L,transactions.getById(incoming)!!.amountMinor)
    }

    @Test fun staleBatchRollsBackAllDecisions() {
        val outgoing = transaction(110,80000,"EXPENSE")
        val gift = transaction(111,10000,"EXPENSE")
        val selected = ReceivableHistorySelection(transactions.getById(outgoing)!!,"LENT",80000)
        val oldGift = transactions.getById(gift)!!
        db.update("transactions",ContentValues().apply { put("review_state","EXCLUDED") },
            "id = ?",arrayOf(gift.toString()))
        try {
            receivables.confirmHistory(null,"Sample Borrower",null,listOf(selected),listOf(oldGift))
            throw AssertionError("Stale history was accepted")
        } catch (_: IllegalArgumentException) {}
        assertTrue(receivables.people().isEmpty())
        assertEquals(null,receivables.eventForTransaction(outgoing))
        assertEquals(80000L,transactions.getById(outgoing)!!.amountMinor)
    }

    @Test fun repeatedConfirmationCannotDuplicateDebt() {
        val outgoing = transaction(120,80000,"EXPENSE")
        val selected = ReceivableHistorySelection(transactions.getById(outgoing)!!,"LENT",80000)
        val person = receivables.confirmHistory(null,"Sample Borrower",null,listOf(selected),emptyList())
        try {
            receivables.confirmHistory(person,"Sample Borrower",null,listOf(selected),emptyList())
            throw AssertionError("Duplicate debt was accepted")
        } catch (_: IllegalArgumentException) {}
        assertEquals(1,receivables.events(person).size)
        assertEquals(80000L,receivables.people().single().outstandingMinor)
    }

    @Test fun historySearchIsBoundedAcrossAccountsAndExcludesUnsafeLinks() {
        val outgoing = transaction(130,80000,"EXPENSE")
        val incoming = transaction(131,30000,"INCOME")
        db.update("transactions",ContentValues().apply {
            put("account_id","synthetic-bank"); put("counterparty_phone","+254712000000")
        },"id = ?",arrayOf(incoming.toString()))
        val transfer = transaction(132,10000,"TRANSFER")
        val repo = ReceivableHistoryRepository(context)
        val matches = repo.search("0712000000",null,"Sample Borrower","0712000000")
        assertEquals(setOf(outgoing,incoming,transfer),matches.map { it.row.id }.toSet())
        assertTrue(matches.first { it.row.id == incoming }.match.bulkEligible)
        assertTrue(matches.first { it.row.id == transfer }.problem != null)
        assertEquals(2,repo.search("Sample Borrower",null,"Sample Borrower",null,2).size)
        assertEquals(1,repo.search("Sample Borrower",null,"Sample Borrower",null,2,2).size)
    }

    @Test fun phoneInsideImportedStatementDescriptionIsSearchableButRequiresIndividualReview() {
        val id = transaction(970, 25000L, "EXPENSE")
        db.update("transactions",ContentValues().apply {
            put("source_kind","statement_import")
            put("counterparty","Send Money to Sample Borrower 0712 000000")
            putNull("counterparty_phone")
        },"id = ?",arrayOf(id.toString()))
        val result = ReceivableHistoryRepository(context).search(
            "0712000000",null,"Sample Borrower","0712000000",
            sourceFilter = ReceivableHistorySourceFilter.STATEMENTS)
        val row = result.single { it.row.id == id }
        assertEquals(com.kedevjimmy.pesatrack.domain.receivables.HistoryMatchKind.PHONE,row.match.kind)
        assertFalse(row.match.bulkEligible)
        assertEquals("Statements",row.sourceLabel)
    }

    @Test fun statementAndMessageHistoryCanBeFilteredWithoutCreatingDuplicateRows() {
        val statementId = transaction(133, 80000, "EXPENSE")
        val messageId = transaction(134, 30000, "EXPENSE")
        val interpretedMessageId = transaction(135,20000,"EXPENSE")
        db.update("transactions",ContentValues().apply {
            put("source_kind","statement_import")
            put("occurred_at_epoch_ms",1_735_689_600_000L) // 2025-01-01 synthetic historical statement row
        },"id = ?",arrayOf(statementId.toString()))
        db.update("transactions",ContentValues().apply {
            put("source_kind","historical_sms")
        },"id = ?",arrayOf(messageId.toString()))
        db.update("transactions",ContentValues().apply {
            put("source_kind","interpreted_historical_financial_message")
        },"id = ?",arrayOf(interpretedMessageId.toString()))
        val message = transactions.getById(messageId)!!
        synchronized(CanonicalStoreLock.monitor) {
            db.beginTransaction()
            try {
                EvidenceJournalStore.observe(db,"STATEMENT","synthetic-statement","row-134",
                    AccountStore.DEFAULT_MPESA_ID,-message.amountMinor,message.occurredAtEpochMs,
                    message.transactionCode,messageId,"REFERENCE")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }

        val repository = ReceivableHistoryRepository(context)
        val all = repository.search("Sample Borrower",null,"Sample Borrower",null)
        assertEquals(setOf(statementId,messageId,interpretedMessageId),all.map { it.row.id }.toSet())
        assertEquals("Statements",all.single { it.row.id == statementId }.sourceLabel)
        assertEquals("Messages · Statements",all.single { it.row.id == messageId }.sourceLabel)
        assertEquals("Messages",all.single { it.row.id == interpretedMessageId }.sourceLabel)
        assertEquals(setOf(statementId,messageId),repository.search("Sample Borrower",null,"Sample Borrower",null,
            sourceFilter = ReceivableHistorySourceFilter.STATEMENTS).map { it.row.id }.toSet())
        assertEquals(setOf(messageId,interpretedMessageId),repository.search("Sample Borrower",null,"Sample Borrower",null,
            sourceFilter = ReceivableHistorySourceFilter.MESSAGES).map { it.row.id }.toSet())
        assertEquals(listOf(statementId),repository.search("Sample Borrower",null,"Sample Borrower",null,
            sourceFilter = ReceivableHistorySourceFilter.STATEMENTS,
            fromEpochMs = 1_735_689_600_000L,throughEpochMs = 1_735_776_000_000L).map { it.row.id })
        db.update("transactions",ContentValues().apply { put("amount_minor",31000L) },
            "id = ?",arrayOf(messageId.toString()))
        assertEquals("Messages",repository.search("Sample Borrower",null,"Sample Borrower",null)
            .single { it.row.id == messageId }.sourceLabel)
        assertEquals(listOf(statementId),repository.search("Sample Borrower",null,"Sample Borrower",null,
            sourceFilter = ReceivableHistorySourceFilter.STATEMENTS).map { it.row.id })
    }

    @Test fun staleStatementEvidenceCannotHideLaterValidHistoryFromTheNextPage() {
        val staleIds = (0 until 50).map { transaction(500 + it,10000,"EXPENSE") }
        synchronized(CanonicalStoreLock.monitor) {
            db.beginTransaction()
            try {
                staleIds.forEachIndexed { index,id ->
                    val time = now + 100000L + index
                    db.update("transactions",ContentValues().apply {
                        put("source_kind","historical_sms"); put("occurred_at_epoch_ms",time)
                    },"id = ?",arrayOf(id.toString()))
                    val row = transactions.getById(id)!!
                    EvidenceJournalStore.observe(db,"STATEMENT","stale-synthetic-statement","stale-row-$index",
                        AccountStore.DEFAULT_MPESA_ID,-row.amountMinor,row.occurredAtEpochMs,
                        row.transactionCode,id,"REFERENCE")
                    db.update("transactions",ContentValues().apply { put("amount_minor",row.amountMinor + 1L) },
                        "id = ?",arrayOf(id.toString()))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        val validStatement = transaction(700,15000,"EXPENSE")
        db.update("transactions",ContentValues().apply { put("source_kind","statement_import") },
            "id = ?",arrayOf(validStatement.toString()))

        val page = ReceivableHistoryRepository(context).searchPage("Sample Borrower",null,
            "Sample Borrower",null,50,0,ReceivableHistorySourceFilter.STATEMENTS)
        assertEquals(listOf(validStatement),page.candidates.map { it.row.id })
        assertEquals(null,page.nextOffset)
    }

    @Test fun schema36AndEncryptedRestorePreserveLoansThenGiftDecisions() {
        val outgoing = transaction(140,80000,"EXPENSE")
        val gift = transaction(141,10000,"EXPENSE")
        val person = receivables.confirmHistory(null,"Sample Borrower","0712000000",
            listOf(ReceivableHistorySelection(transactions.getById(outgoing)!!,"LENT",80000)),emptyList())
        dropB2Tables(db)
        dropPost38Tables(db)
        db.execSQL("DROP TABLE personal_receivable_review_exclusions")
        db.execSQL("DROP TRIGGER IF EXISTS trg_delete_receivable_review")
        db.version = 36
        val manager = com.kedevjimmy.pesatrack.security.PesaBackupManager(context)
        val oldBackup = manager.exportEncryptedBytes("invented-history-passphrase")
        PesaTrackDatabase.resetForTests()
        assertEquals(40, db.version)
        repeat(2) {
            manager.previewEncryptedBytes(oldBackup,"invented-history-passphrase")
            manager.restoreEncryptedBytes(oldBackup,"invented-history-passphrase")
            assertEquals(80000L,receivables.people().single().outstandingMinor)
            assertEquals(1,receivables.events(person).size)
        }
        receivables.confirmHistory(person,"Sample Borrower","0712000000",emptyList(),listOf(transactions.getById(gift)!!))
        val newBackup = manager.exportEncryptedBytes("invented-history-passphrase")
        repeat(2) {
            manager.restoreEncryptedBytes(newBackup,"invented-history-passphrase")
            assertEquals(setOf(gift),ReceivableHistoryRepository(context).exclusions(person))
            assertEquals(80000L,receivables.people().single().outstandingMinor)
        }
    }

    /** Keep fabricated legacy backups consistent with their claimed schema version. */
    private fun dropPost38Tables(database: android.database.sqlite.SQLiteDatabase) {
        listOf("evidence_link_audit", "evidence_links", "evidence_observations",
            "payroll_lines", "payroll_records").forEach { table ->
            database.execSQL("DROP TABLE IF EXISTS $table")
        }
    }

    private fun dropB2Tables(database: android.database.sqlite.SQLiteDatabase) {
        database.execSQL("DROP TRIGGER IF EXISTS trg_b2_delete_account_consent")
        database.execSQL("DROP TRIGGER IF EXISTS trg_b2_delete_transfer_review")
        database.execSQL("DROP TRIGGER IF EXISTS trg_b2_guard_pair_update")
        database.execSQL("DROP TRIGGER IF EXISTS trg_b2_guard_pair_delete")
        database.execSQL("DROP TABLE account_ownership_consents")
        database.execSQL("DROP TABLE owned_transfer_review_decisions")
    }

    @Test fun categoryLinkedReceiptCannotRepayALaterLoan() {
        val earlierReceipt = transaction(150,30000,"INCOME")
        val laterLoan = transaction(151,80000,"EXPENSE")
        val loanRow = transactions.getById(laterLoan)!!
        receivables.record(null,"Sample Borrower",null,"LENT",80000,loanRow.occurredAtEpochMs,laterLoan)
        val person = receivables.people().single()
        try {
            receivables.record(person.id,person.name,null,"REPAID",30000,
                transactions.getById(earlierReceipt)!!.occurredAtEpochMs,earlierReceipt)
            throw AssertionError("Earlier receipt paid a later loan")
        } catch (_: IllegalArgumentException) {}
        assertEquals(80000L,receivables.people().single().outstandingMinor)
        assertEquals(null,receivables.eventForTransaction(earlierReceipt))
    }

    @Test fun legacyPhoneStillAllowsExplicitIndividualConfirmation() {
        receivables.record(null,"Sample Borrower","1234567","LENT",1000,System.currentTimeMillis() - 60000)
        val person = receivables.people().single()
        val outgoing = transaction(160,80000,"EXPENSE")
        val repo = ReceivableHistoryRepository(context)
        val match = repo.search("Sample Borrower",person.id,person.name,person.phone).single()
        assertFalse(match.match.bulkEligible)
        receivables.confirmHistory(person.id,person.name,person.phone,
            listOf(ReceivableHistorySelection(transactions.getById(outgoing)!!,"LENT",80000)),emptyList())
        assertEquals(81000L,receivables.people().single().outstandingMinor)
        assertEquals("1234567",receivables.people().single().phone)
    }
}
