package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.evidence.TransferPairDecision
import com.kedevjimmy.pesatrack.domain.evidence.TransferDecisionResult
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnedTransferReviewStoreTest {
    private lateinit var context: Context
    private lateinit var bankId: String
    private lateinit var store: OwnedTransferReviewStore
    private val epoch = 1_790_613_600_000L
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE).edit().clear()
            .putString("profile_name", "James Mwangi Waweru").commit()
        bankId = AccountStore.get(context).save("Synthetic Bank", AccountType.BANK).id
        store = OwnedTransferReviewStore(context)
        seed(101, bankId, "EXPENSE", 100)
        seed(102, AccountStore.DEFAULT_MPESA_ID, "INCOME", 0)
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun consent() {
        val consent = AccountOwnershipConsentStore(context)
        consent.recordConsent(bankId, true)
        consent.recordConsent(AccountStore.DEFAULT_MPESA_ID, true)
    }
    @Test fun legacyEnabledAccountsAreNotConsent() { assertTrue(store.pendingCandidates().isEmpty()) }
    @Test fun confirmedPairRejectsIndividualEditsAcrossStoresAndDeletion() {
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        val transactions = TransactionStore.get(context)
        for (id in listOf(101L, 102L)) {
            assertTrue(runCatching { transactions.classifyManually(id, TransactionCategory.GROCERIES, MoneyDirection.EXPENSE) }.isFailure)
            val exclusion = runCatching { transactions.excludeFromBudget(id) }
            if (id == 101L) assertTrue(exclusion.isFailure) else assertTrue(exclusion.isSuccess) // Already excluded: no changed financial field.
            assertTrue(runCatching { transactions.applyRuleToTransaction(id, SmartRule("synthetic-rule", "James", TransactionCategory.GROCERIES)) }.isFailure)
            val db = PesaTrackDatabase.get(context).writableDatabase
            assertTrue(runCatching { db.execSQL("UPDATE transactions SET amount_minor=20000 WHERE id=$id") }.isFailure)
            assertTrue(runCatching { db.delete("transactions", "id=?", arrayOf(id.toString())) }.isFailure)
        }
        assertTrue(runCatching { transactions.markDebtRepayment(101, 999, "Synthetic debt", true) }.isFailure)
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='TRANSFER'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE review_state='EXCLUDED'"))
        assertTrue(store.isConfirmedPair(101)); assertTrue(store.isConfirmedPair(102))
    }
    @Test fun undoFromEitherSideRestoresBothThenAllowsAuthoritativeCorrectionAndReplay() {
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        assertTrue(store.undoConfirmedPair(102)); assertFalse(store.undoConfirmedPair(101))
        assertFalse(store.isConfirmedPair(101)); assertFalse(store.isConfirmedPair(102))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='EXPENSE' AND review_state='NEEDS_REVIEW' AND linked_account_id IS NULL"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='INCOME' AND review_state='NEEDS_REVIEW'"))
        assertTrue(store.pendingCandidates().isEmpty())
        TransactionStore.get(context).classifyManually(101, TransactionCategory.GROCERIES, MoneyDirection.EXPENSE)
        replay(101, bankId, "EXPENSE", 100); replay(102, AccountStore.DEFAULT_MPESA_ID, "INCOME", 0)
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE category='GROCERIES' AND review_state='USER_CLASSIFIED'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='INCOME'"))
        assertEquals(2L, scalar("SELECT COUNT(*) FROM transactions"))
    }
    @Test fun laterBackdatedSalaryCanRebuildDerivedCyclesAroundAConfirmedPair() {
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        val salary = RuleStore.get(context).save(name = "Synthetic salary", counterpartyContains = "",
            counterpartyExact = "Synthetic employer", category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME, startsPayCycle = true, accountId = bankId)
        assertTrue(TransactionStore.get(context).insertManualIncome(500000, epoch - 60000, "Synthetic employer", salary))
        PayCycleStore.get(context).rebuild(); PayCycleStore.get(context).rebuild()
        assertTrue(store.isConfirmedPair(101)); assertTrue(store.isConfirmedPair(102))
        assertEquals(2L, scalar("SELECT COUNT(*) FROM transactions WHERE id IN (101,102) AND pay_cycle_id IS NOT NULL"))
        assertTrue(store.undoConfirmedPair(101))
        assertEquals(2L, scalar("SELECT COUNT(*) FROM transactions WHERE id IN (101,102) AND pay_cycle_id IS NOT NULL"))
    }
    @Test fun interruptedUndoRollsBackBothRowsDecisionAndBalanceAnchors() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET balance_minor=20000 WHERE id=102")
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        db.execSQL("CREATE TRIGGER synthetic_abort_undo BEFORE UPDATE ON transactions WHEN OLD.id=102 BEGIN SELECT RAISE(ABORT, 'synthetic undo failure'); END")
        assertTrue(runCatching { store.undoConfirmedPair(101) }.isFailure)
        assertTrue(store.isConfirmedPair(102))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='TRANSFER'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE review_state='EXCLUDED'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM account_balance_observations"))
    }
    @Test fun restoredConfirmationCanUndoWithoutRemovingUnrelatedBalanceEvidence() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET balance_minor=20000 WHERE id=102")
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        db.insertOrThrow("account_balance_observations", null, ContentValues().apply {
            put("observation_key", "synthetic-unrelated-check"); put("entity_key", "account:$bankId"); put("account_id", bankId)
            put("balance_minor", 90000); put("confidence", "HIGH"); put("observed_at_epoch_ms", epoch + 1)
            put("source_kind", "user_balance_check"); put("created_at_epoch_ms", epoch + 1)
        })
        val manager = com.kedevjimmy.pesatrack.security.PesaBackupManager(context)
        val password = "synthetic-paired-undo-password"
        manager.restoreEncryptedBytes(manager.exportEncryptedBytes(password), password)
        assertTrue(store.undoConfirmedPair(101))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM account_balance_observations WHERE observation_key='synthetic-unrelated-check'"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM account_balance_observations WHERE observation_key LIKE 'reviewed-transfer-balance:%'"))
        assertEquals(20000L, scalar("SELECT balance_minor FROM transactions WHERE id=102"))
    }
    @Test fun confirmationIsAtomicIdempotentAndHasOneTransferEffect() {
        consent(); val candidate = store.pendingCandidates().single()
        assertEquals(TransferDecisionResult.APPLIED, store.decide(candidate, TransferPairDecision.CONFIRMED))
        assertEquals(TransferDecisionResult.STALE, store.decide(candidate, TransferPairDecision.CONFIRMED))
        val db = PesaTrackDatabase.get(context).readableDatabase
        db.rawQuery("SELECT direction,review_state,linked_account_id,fee_minor FROM transactions WHERE id=101", null).use {
            assertTrue(it.moveToFirst()); assertEquals("TRANSFER", it.getString(0))
            assertEquals("USER_CLASSIFIED", it.getString(1)); assertEquals(AccountStore.DEFAULT_MPESA_ID, it.getString(2))
            assertEquals(100L, it.getLong(3))
        }
        db.rawQuery("SELECT review_state FROM transactions WHERE id=102", null).use {
            assertTrue(it.moveToFirst()); assertEquals("EXCLUDED", it.getString(0))
        }
        assertEquals(2L, scalar("SELECT COUNT(*) FROM transactions"))
        val summary = TransactionStore.get(context).activitySnapshot(epoch).summary
        assertEquals(0L, summary.incomeMinor); assertEquals(0L, summary.spentMinor)
        assertEquals(10000L, summary.transferMinor); assertEquals(100L, summary.feesMinor)
    }
    @Test fun confirmationPreservesNativeObservedBalancesAndHistoricalSnapshots() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET balance_minor=90000 WHERE id=101")
        db.execSQL("UPDATE transactions SET balance_minor=20000 WHERE id=102")
        val snapshots = PaydaySnapshotStore.get(context)
        val accounts = AccountStore.get(context).listAccounts().filter { it.id == bankId || it.id == AccountStore.DEFAULT_MPESA_ID }
        val times = listOf(epoch, epoch + 1)
        val before = times.map { snapshots.currentAccountSnapshots(accounts, it) }
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        assertEquals(before, times.map { snapshots.currentAccountSnapshots(accounts, it) })
        assertEquals(2L, scalar("SELECT COUNT(*) FROM account_balance_observations"))
        assertEquals(90000L, scalar("SELECT balance_minor FROM transactions WHERE id=101"))
        assertEquals(20000L, scalar("SELECT balance_minor FROM transactions WHERE id=102"))
        val prior = snapshots.currentAccountSnapshots(accounts, epoch - 1).associateBy { it.accountId }
        assertEquals(100100L, prior.getValue(bankId).balanceMinor)
        assertEquals(10000L, prior.getValue(AccountStore.DEFAULT_MPESA_ID).balanceMinor)
    }
    @Test fun staggeredSettlementCannotInventWalletFundsBeforeArrival() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET occurred_at_epoch_ms=${epoch + 60000},balance_minor=20000 WHERE id=102")
        val accounts = AccountStore.get(context)
        accounts.updateDetails(AccountStore.DEFAULT_MPESA_ID, null, 10000, epoch - 1)
        val wallet = accounts.listAccounts().single { it.id == AccountStore.DEFAULT_MPESA_ID }
        consent()
        assertTrue(store.pendingCandidates().isEmpty())
        assertEquals(10000L, PaydaySnapshotStore.get(context).currentAccountSnapshot(wallet, epoch + 1000).balanceMinor)
        assertEquals(20000L, PaydaySnapshotStore.get(context).currentAccountSnapshot(wallet, epoch + 60000).balanceMinor)
        assertEquals(0L, scalar("SELECT COUNT(*) FROM owned_transfer_review_decisions"))
    }
    @Test fun confirmationCannotDisplaceAnExistingHigherPriorityBalanceCheck() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET balance_minor=20000 WHERE id=102")
        db.insertOrThrow("account_balance_observations", null, ContentValues().apply {
            put("observation_key", "synthetic-authoritative-check"); put("entity_key", "account:${AccountStore.DEFAULT_MPESA_ID}")
            put("account_id", AccountStore.DEFAULT_MPESA_ID); put("balance_minor", 20500); put("confidence", "HIGH")
            put("observed_at_epoch_ms", epoch); put("source_kind", "user_balance_check"); put("created_at_epoch_ms", epoch)
        })
        val wallet = AccountStore.get(context).listAccounts().single { it.id == AccountStore.DEFAULT_MPESA_ID }
        val before = PaydaySnapshotStore.get(context).currentAccountSnapshot(wallet, epoch)
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        assertEquals(before, PaydaySnapshotStore.get(context).currentAccountSnapshot(wallet, epoch))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM account_balance_observations"))
        assertEquals(20500L, before.balanceMinor)
    }
    @Test fun ownershipRejectionMakesOpenCardStaleWithoutLedgerWrites() {
        consent(); val candidate = store.pendingCandidates().single()
        AccountOwnershipConsentStore(context).recordConsent(bankId, false)
        assertEquals(TransferDecisionResult.STALE, store.decide(candidate, TransferPairDecision.CONFIRMED))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM owned_transfer_review_decisions"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='INCOME'"))
    }
    @Test fun changedFactsUserCorrectionsAndNewTwinsInvalidateSnapshot() {
        consent(); val candidate = store.pendingCandidates().single()
        seed(103, AccountStore.DEFAULT_MPESA_ID, "INCOME", 0)
        assertEquals(TransferDecisionResult.STALE, store.decide(candidate, TransferPairDecision.CONFIRMED))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM owned_transfer_review_decisions"))
    }
    @Test fun rejectSurvivesReplayAndDoesNotChangeCash() {
        consent(); val candidate = store.pendingCandidates().single()
        assertEquals(TransferDecisionResult.APPLIED, store.decide(candidate, TransferPairDecision.REJECTED))
        replay(101, bankId, "EXPENSE", 100)
        replay(102, AccountStore.DEFAULT_MPESA_ID, "INCOME", 0)
        assertTrue(OwnedTransferReviewStore(context).pendingCandidates().isEmpty())
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='INCOME'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='EXPENSE'"))
    }
    @Test fun confirmedPairSurvivesCanonicalIngestionReplay() {
        consent(); store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        replay(101, bankId, "EXPENSE", 100)
        replay(102, AccountStore.DEFAULT_MPESA_ID, "INCOME", 0)
        assertEquals(2L, scalar("SELECT COUNT(*) FROM transactions"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='TRANSFER' AND review_state='USER_CLASSIFIED'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE review_state='EXCLUDED'"))
        assertTrue(store.pendingCandidates().isEmpty())
    }
    @Test fun deferReappearsOnlyWhenRelevantFactsChange() {
        consent(); val candidate = store.pendingCandidates().single()
        assertEquals(TransferDecisionResult.APPLIED, store.decide(candidate, TransferPairDecision.DEFERRED))
        assertTrue(store.pendingCandidates().isEmpty())
        PesaTrackDatabase.get(context).writableDatabase.execSQL("UPDATE transactions SET fee_minor=200 WHERE id=101")
        assertEquals(1, store.pendingCandidates().size)
    }
    @Test fun failedDecisionInsertRollsBackBothLedgerRows() {
        consent(); val candidate = store.pendingCandidates().single()
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("CREATE TRIGGER synthetic_fail_decision BEFORE INSERT ON owned_transfer_review_decisions BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        assertTrue(runCatching { store.decide(candidate, TransferPairDecision.CONFIRMED) }.isFailure)
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='INCOME'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='EXPENSE'"))
    }
    @Test fun changedUserClassificationAndSourceDeletionNeverApplyAnOldCard() {
        consent(); val candidate = store.pendingCandidates().single()
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET review_state='USER_CLASSIFIED',category='GROCERIES' WHERE id=101")
        assertEquals(TransferDecisionResult.STALE, store.decide(candidate, TransferPairDecision.CONFIRMED))
        db.delete("transactions", "id=102", null)
        assertEquals(TransferDecisionResult.STALE, store.decide(candidate, TransferPairDecision.CONFIRMED))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM owned_transfer_review_decisions"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE category='GROCERIES'"))
    }
    @Test fun encryptedRestorePreservesRejectionAndConfirmation() {
        consent(); val candidate = store.pendingCandidates().single()
        store.decide(candidate, TransferPairDecision.REJECTED)
        val manager = com.kedevjimmy.pesatrack.security.PesaBackupManager(context)
        val password = "synthetic-b2-roundtrip-password"
        val backup = manager.exportEncryptedBytes(password)
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.delete("owned_transfer_review_decisions", null, null)
        assertEquals(1, store.pendingCandidates().size)
        manager.restoreEncryptedBytes(backup, password)
        assertTrue(store.pendingCandidates().isEmpty())
        db.delete("owned_transfer_review_decisions", null, null)
        store.decide(store.pendingCandidates().single(), TransferPairDecision.CONFIRMED)
        val confirmed = manager.exportEncryptedBytes(password)
        manager.restoreEncryptedBytes(confirmed, password)
        assertTrue(store.pendingCandidates().isEmpty())
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE direction='TRANSFER'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM transactions WHERE review_state='EXCLUDED'"))
    }
    private fun scalar(sql: String) = PesaTrackDatabase.get(context).readableDatabase.rawQuery(sql, null).use { it.moveToFirst(); it.getLong(0) }
    private fun seed(id: Long, account: String, direction: String, fee: Long) {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.insertOrThrow("transactions", null, ContentValues().apply {
            put("id", id); put("fingerprint", TransactionFingerprint.of(parsed(id, direction, fee), epoch)); put("type", "OTHER")
            put("transaction_code", code(id, direction))
            put("amount_minor", 10000); put("fee_minor", fee); put("counterparty", "James Mwangi Waweru"); put("received_at_epoch_ms", epoch)
            put("created_at_epoch_ms", epoch)
            put("occurred_at_epoch_ms", epoch); put("source_kind", "statement_import")
            put("direction", direction); put("category", "UNCATEGORIZED"); put("review_state", "NEEDS_REVIEW")
            put("account_id", account)
        })
        db.insertOrThrow("transaction_external_references", null, ContentValues().apply {
            put("transaction_id", id); put("reference_value", "SYNTHETIC01"); put("reference_label", "transaction_code")
            put("source_kind", "statement_import"); put("observed_at_epoch_ms", epoch)
        })
        if (direction == "INCOME") db.insertOrThrow("transaction_external_references", null, ContentValues().apply {
            put("transaction_id", id); put("reference_value", "BANKCODE101")
            put("source_kind", "statement_import"); put("observed_at_epoch_ms", epoch)
        })
    }
    private fun code(id: Long, direction: String) = if (direction == "EXPENSE") "BANKCODE$id" else if (id == 102L) "SYNTHETIC01" else "WALLETCODE$id"
    private fun parsed(id: Long, direction: String, fee: Long) = ParsedMpesaTransaction(
        transactionCode = code(id, direction), type = MpesaTransactionType.OTHER, amountMinor = 10000,
        counterparty = "James Mwangi Waweru", feeMinor = fee, balanceMinor = null, occurredAtText = null,
        rawMessage = "Invented B2 observation $id", reversalOfCode = null)
    private fun replay(id: Long, account: String, direction: String, fee: Long) {
        assertFalse(TransactionStore.get(context).insertIfNew(parsed(id, direction, fee), null, "statement_import", epoch,
            occurredAtEpochMs = epoch, accountIdOverride = account,
            directionOverride = com.kedevjimmy.pesatrack.domain.model.MoneyDirection.valueOf(direction),
            categoryOverride = com.kedevjimmy.pesatrack.domain.model.TransactionCategory.UNCATEGORIZED,
            reviewStateOverride = com.kedevjimmy.pesatrack.domain.model.ReviewState.NEEDS_REVIEW,
            rebuildPayCycles = false, runRelationshipReconciliation = false, runMirrorCorrelation = false))
    }
}
