package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.SQLException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.InterpretationConfidence
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic fixtures: no personal SMS, providers or ledger data. */
@RunWith(AndroidJUnit4::class)
class DebtAccountingRegressionTest {
    private lateinit var context: Context
    private lateinit var store: DebtStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        store = DebtStore.get(context)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun unknownRepaymentWithoutPrincipalRemainsUnknown() {
        val liability = liability("UNKNOWN")
        repayment(liability, 50_000L, T2, "unknown", DebtCompletion.UNKNOWN)
        val account = account(liability)
        // An amount repaid cannot establish the amount still owed.
        assertEquals("UNKNOWN", account.balanceQuality.name)
        assertNull(account.observedAtEpochMs)
        assertEquals(50_000L, account.lastRepaymentMinor)
    }

    @Test
    fun partialRepaymentWithoutPrincipalRemainsUnknownOnReplay() {
        val liability = liability("PARTIAL")
        repayment(liability, 50_000L, T2, "partial", DebtCompletion.PARTIAL)
        assertEquals("UNKNOWN", account(liability).balanceQuality.name)
        val replay = repayment(liability, 50_000L, T2, "partial", DebtCompletion.PARTIAL)
        assertFalse(replay.inserted)
        assertEquals("UNKNOWN", account(liability).balanceQuality.name)
        assertNull(account(liability).observedAtEpochMs)
    }

    @Test
    fun fullRepaymentWithoutPrincipalProvidesObservedZero() {
        val liability = liability("FULL")
        repayment(liability, 50_000L, T2, "full", DebtCompletion.FULL)
        assertEquals(0L, account(liability).outstandingMinor)
        assertEquals(DebtBalanceQuality.OBSERVED, account(liability).balanceQuality)
        assertEquals(T2, account(liability).observedAtEpochMs)
    }

    @Test
    fun reverseOrderDrawdownsConvergeWithChronologicalHistoryAndReplay() {
        val chronological = liability("CHRONO")
        val reversed = liability("REVERSED")
        drawdown(chronological, 10_000L, T1, "first")
        drawdown(chronological, 20_000L, T2, "second")
        drawdown(reversed, 20_000L, T2, "second")
        drawdown(reversed, 10_000L, T1, "first")
        assertEquals(30_000L, account(chronological).outstandingMinor)
        assertEquals(30_000L, account(reversed).outstandingMinor)
        assertEquals(account(chronological).balanceQuality, account(reversed).balanceQuality)
        assertFalse(drawdown(reversed, 10_000L, T1, "first").inserted)
        assertFalse(drawdown(reversed, 20_000L, T2, "second").inserted)
        assertEquals(30_000L, account(reversed).outstandingMinor)
    }

    @Test
    fun observedPrincipalLessRepaymentRetainsTrueObservationTimestamp() {
        val liability = liability("PRINCIPAL")
        store.recordObservedOutstanding(liability, 250_000L, T1)
        repayment(liability, 50_000L, T2, "known", DebtCompletion.PARTIAL)
        assertEquals(200_000L, account(liability).outstandingMinor)
        assertEquals(DebtBalanceQuality.ESTIMATED, account(liability).balanceQuality)
        // Timestamp must describe the genuine anchor, not the latest inferred delta.
        assertEquals(T1, account(liability).observedAtEpochMs)
    }

    @Test
    fun lateObservedPrincipalIncludesAlreadyRecordedLaterRepayment() {
        val liability = liability("LATE_ANCHOR")
        repayment(liability, 50_000L, T2, "later", DebtCompletion.PARTIAL)
        store.recordObservedOutstanding(liability, 250_000L, T1)
        assertEquals(200_000L, account(liability).outstandingMinor)
        assertEquals(DebtBalanceQuality.ESTIMATED, account(liability).balanceQuality)
        assertEquals(T1, account(liability).observedAtEpochMs)
    }

    @Test
    fun observedAnchorLaterDeltaBackfillAndReplayPreserveLatestEstimate() {
        val liability = liability("ANCHOR_REPLAY")
        store.recordObservedOutstanding(liability, 250_000L, T1)
        drawdown(liability, 20_000L, T3, "latest")
        // Backfill before the anchor is already represented by the observation.
        drawdown(liability, 900_000L, T1 - 60_000L, "pre-anchor")
        // Backfill after the anchor must be included despite arriving after T3.
        repayment(liability, 50_000L, T2, "middle", DebtCompletion.PARTIAL)
        assertEquals(220_000L, account(liability).outstandingMinor)
        assertEquals(T1, account(liability).observedAtEpochMs)
        // Replaying the anchor must not erase the already-recorded later movements.
        store.recordObservedOutstanding(liability, 250_000L, T1)
        assertFalse(drawdown(liability, 20_000L, T3, "latest").inserted)
        assertFalse(repayment(liability, 50_000L, T2, "middle", DebtCompletion.PARTIAL).inserted)
        assertEquals(220_000L, account(liability).outstandingMinor)
        assertEquals(DebtBalanceQuality.ESTIMATED, account(liability).balanceQuality)
        assertEquals(T1, account(liability).observedAtEpochMs)
    }

    @Test
    fun eventObservedAnchorReplayDoesNotEraseLaterDrawdown() {
        val liability = liability("EVENT_ANCHOR")
        drawdown(liability, 10_000L, T1, "observed", outstanding = 250_000L)
        drawdown(liability, 20_000L, T3, "latest")
        repayment(liability, 50_000L, T2, "middle", DebtCompletion.PARTIAL)
        assertEquals(220_000L, account(liability).outstandingMinor)
        assertFalse(drawdown(liability, 10_000L, T1, "observed", outstanding = 250_000L).inserted)
        assertEquals(220_000L, account(liability).outstandingMinor)
    }

    @Test
    fun manualRepaymentClassificationFailureRollsBackDebtAndRetryFinishesBoth() {
        val liability = liability("MANUAL")
        store.recordObservedOutstanding(liability, 250_000L, T1)
        val transactions = TransactionStore.get(context)
        assertTrue(transactions.insertInterpretedMoneyMovementIfNew(
            kind = CanonicalFinancialRecordKind.PAYMENT,
            paymentRouteHint = PaymentRouteHint.MERCHANT,
            transactionCode = "DEBTTEST01", amountMinor = 50_000L,
            // Initially unrelated, so ingestion cannot auto-link the payment.
            counterparty = "Synthetic merchant", counterpartyPhone = null,
            feeMinor = 0L, balanceMinor = null, occurredAtText = null,
            rawMessage = "Synthetic debt correction fixture", sourcePackage = "test.package",
            sourceKind = "interpreted_TEST", receivedAtEpochMs = T2, occurredAtEpochMs = T2,
            matchedRule = null, rebuildPayCycles = false,
            accountIdOverride = AccountStore.DEFAULT_MPESA_ID
        ))
        val original = transactions.findByTransactionCode("DEBTTEST01")!!
        val debt = account(liability)
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("UPDATE transactions SET counterparty = ? WHERE id = ?", arrayOf<Any>(liability.displayName, original.id))
        db.execSQL("""
            CREATE TRIGGER abort_debt_classification_for_test
            BEFORE UPDATE OF relation_type ON transactions
            WHEN NEW.id = ${original.id} AND NEW.relation_type = 'DEBT_REPAYMENT'
            BEGIN
                SELECT RAISE(ABORT, 'forced debt classification failure');
            END
        """.trimIndent())
        assertThrows(SQLException::class.java) {
            store.recordPaymentAsRepayment(original.id, debt.id, userConfirmed = true)
        }
        assertEquals(250_000L, account(liability).outstandingMinor)
        assertEquals(0, db.rawQuery("SELECT COUNT(*) FROM debt_events WHERE linked_transaction_id = ?", arrayOf(original.id.toString())).use {
            it.moveToFirst(); it.getInt(0)
        })
        assertEquals(original.direction, transactions.findByTransactionCode("DEBTTEST01")!!.direction)
        db.execSQL("DROP TRIGGER abort_debt_classification_for_test")
        assertTrue(store.recordPaymentAsRepayment(original.id, debt.id, userConfirmed = true))
        assertEquals(200_000L, account(liability).outstandingMinor)
        assertEquals("DEBT_REPAYMENT", transactions.findByTransactionCode("DEBTTEST01")!!.relationType)
        assertTrue(store.recordPaymentAsRepayment(original.id, debt.id, userConfirmed = true))
        assertEquals(200_000L, account(liability).outstandingMinor)
    }

    private fun liability(key: String) = FinancialAccountReference(
        entityKey = "discovered:synthetic-${key.lowercase().replace('_', '-')}-lender", displayName = "Synthetic $key lender",
        providerKey = "discovered:synthetic-${key.lowercase().replace('_', '-')}-lender", accountTypeHint = AccountType.OTHER,
        ownershipHint = AccountOwnershipHint.USER_OWNED, nature = FinancialAccountNature.LIABILITY
    )

    private fun account(liability: FinancialAccountReference) =
        store.accounts().single { it.providerKey == liability.providerKey }

    private fun drawdown(liability: FinancialAccountReference, amount: Long, at: Long, key: String, outstanding: Long? = null) =
        store.recordDrawdown(
            event = DebtDrawdownEventFact(amountMinor = amount, liability = liability,
                feeMinor = 0L, outstandingAfterMinor = outstanding, availableCreditAfterMinor = null,
                confidence = InterpretationConfidence.HIGH),
            transactionCode = null, messageFingerprint = "${liability.entityKey}:$key",
            sourcePackage = "test.package", sourceKind = "TEST", occurredAtEpochMs = at
        )

    private fun repayment(liability: FinancialAccountReference, amount: Long, at: Long, key: String, completion: DebtCompletion) =
        store.recordRepayment(
            event = DebtRepaymentEventFact(amountMinor = amount, liability = liability,
                fundingAccount = null, completion = completion, outstandingAfterMinor = null,
                availableCreditAfterMinor = null, walletBalanceAfterMinor = null,
                confidence = InterpretationConfidence.HIGH),
            transactionCode = null, messageFingerprint = "${liability.entityKey}:$key",
            sourcePackage = "test.package", sourceKind = "TEST", occurredAtEpochMs = at
        )

    private companion object {
        const val T1 = 1_790_000_000_000L
        const val T2 = T1 + 60_000L
        const val T3 = T1 + 120_000L
    }
}
