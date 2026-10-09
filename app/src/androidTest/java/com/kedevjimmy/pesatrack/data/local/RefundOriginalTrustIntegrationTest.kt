package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0/A2 R03: only proven, eligible outgoing expenses may be
 * automatically linked to later matching receipts as refunds.
 *
 * Synthetic taxi examples exercise the real persisted TransactionStore
 * auto-link path, not only the pure RefundMatchingPolicy matcher.
 */
@RunWith(AndroidJUnit4::class)
class RefundOriginalTrustIntegrationTest {
    private lateinit var context: Context
    private lateinit var store: TransactionStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        store = TransactionStore.get(context)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun legacyAutomaticRefundWithoutMessageEvidenceReturnsToReview() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt()
        seedLegacyRefund(receipt.id, ReviewState.AUTO_CLASSIFIED)
        store.activitySnapshotBetween(EPOCH - 1L, EPOCH + 10_000_000L)
        val restored = requireNotNull(store.findByTransactionCode("REFTEST220"))
        assertNull(restored.relationType)
        assertEquals(ReviewState.NEEDS_REVIEW.name, restored.reviewState)
    }

    @Test
    fun legacyRefundReconciliationPreservesUserClassification() {
        val originalId = insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt()
        seedLegacyRefund(receipt.id, ReviewState.USER_CLASSIFIED)
        store.activitySnapshotBetween(EPOCH - 1L, EPOCH + 10_000_000L)
        val restored = requireNotNull(store.findByTransactionCode("REFTEST220"))
        assertEquals(TransactionStore.RELATION_REFUND, restored.relationType)
        assertEquals(originalId, restored.relatedTransactionId)
        assertEquals(ReviewState.USER_CLASSIFIED.name, restored.reviewState)
    }

    private fun seedLegacyRefund(id: Long, reviewState: ReviewState) {
        val values = ContentValues().apply {
            put("evidence_source", "refund_match")
            put("review_state", reviewState.name)
        }
        PesaTrackDatabase.get(context).writableDatabase.update("transactions", values,
            "id = ?", arrayOf(id.toString()))
        context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
            .edit().remove("refund_reconciliation_v2").commit()
    }

    @Test
    fun trustedTaxiPaymentThenEqualReceiptStillLinksAsRefund() {
        val originalId = insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt()
        assertEquals(TransactionStore.RELATION_REFUND, receipt.relationType)
        assertEquals(originalId, receipt.relatedTransactionId)
        assertEquals(MoneyDirection.TRANSFER.name, receipt.direction)
        assertEquals(TransactionCategory.TRANSPORT.name, receipt.category)
    }

    @Test
    fun futureRefundPromiseIsNotCompletedRefundEvidence() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        assertNull(incomingReceipt("Refund of Ksh 220 will be credited tomorrow").relationType)
    }

    @Test
    fun deniedRefundIsNotCompletedRefundEvidence() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        assertNull(incomingReceipt("Refund for this payment has not been completed").relationType)
    }

    @Test
    fun precedingNegationDoesNotEstablishCompletedRefund() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt("No money was refunded")
        assertNull(receipt.relationType)
        assertNull(receipt.relatedTransactionId)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    @Test
    fun conditionalRefundDoesNotEstablishCompletedRefund() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt("If payment is refunded, you will receive a separate notification")
        assertNull(receipt.relationType)
        assertNull(receipt.relatedTransactionId)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    @Test
    fun unrelatedReceiptFromSameMerchantIsNotRefundEvidence() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt("Synthetic consultancy payment received from SAMPLE TAXI")
        assertNull(receipt.relationType)
        assertNull(receipt.relatedTransactionId)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    @Test
    fun prefixRelatedBusinessDoesNotEstablishRefundIdentity() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt("Synthetic payment refunded", "SAMPLE TAXI EXPRESS")
        assertNull(receipt.relationType)
    }

    @Test
    fun reversedOriginalCannotContributeTrustedRefund() {
        insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        incomingReceipt()
        val values = ContentValues().apply {
            put("transaction_code", "REVTEST220")
            put("fingerprint", "synthetic-reversed-original")
            put("type", "REVERSAL")
            put("amount_minor", 22_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", EPOCH + 7_200_000L)
            put("received_at_epoch_ms", EPOCH + 7_200_000L)
            put("created_at_epoch_ms", EPOCH + 7_200_000L)
            put("source_kind", "TEST_SYNTHETIC")
            put("direction", MoneyDirection.NEUTRAL.name)
            put("category", TransactionCategory.OTHER.name)
            put("review_state", ReviewState.AUTO_CLASSIFIED.name)
            put("reversal_of_code", "PAYTEST220")
        }
        PesaTrackDatabase.get(context).writableDatabase.insertOrThrow("transactions", null, values)
        val snapshot = store.activitySnapshotBetween(EPOCH - 1L, EPOCH + 10_000_000L, performMaintenance = false)
        assertEquals(0L, snapshot.summary.refundMinor)
        assertEquals(0L, snapshot.summary.netMinor)
    }

    @Test
    fun originalBecomingCycleCandidateStopsTrustedRefund() {
        val originalId = insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        incomingReceipt()
        val values = ContentValues().apply { put("cycle_candidate_reason", "Synthetic conflicting cycle") }
        PesaTrackDatabase.get(context).writableDatabase.update("transactions", values, "id = ?", arrayOf(originalId.toString()))
        assertEquals(0L, store.activitySnapshotBetween(EPOCH - 1L, EPOCH + 10_000_000L,
            performMaintenance = false).summary.refundMinor)
    }

    @Test
    fun unreviewedPaymentCannotBeAutoPromotedToHighConfidenceRefund() {
        insertOutgoing(ReviewState.NEEDS_REVIEW)
        val receipt = incomingReceipt()
        assertNull(receipt.relationType)
        assertNull(receipt.relatedTransactionId)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    @Test
    fun liabilityRepaymentCannotBeMisidentifiedAsMerchantRefund() {
        insertOutgoing(
            reviewState = ReviewState.AUTO_CLASSIFIED,
            relationType = TransactionStore.RELATION_DEBT_REPAYMENT
        )
        val receipt = incomingReceipt()
        assertNull(receipt.relationType)
        assertNull(receipt.relatedTransactionId)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    @Test
    fun unresolvedPayCycleCandidateCannotBeAutoLinkedAsRefund() {
        insertOutgoing(
            reviewState = ReviewState.AUTO_CLASSIFIED,
            cycleCandidateReason = "Synthetic unresolved cycle evidence"
        )
        val receipt = incomingReceipt()
        assertNull(receipt.relationType)
        assertEquals(MoneyDirection.INCOME.name, receipt.direction)
    }

    private fun incomingReceipt(
        message: String = "Synthetic merchant payment returned",
        merchant: String = "SAMPLE TAXI"
    ): StoredTransaction {
        assertTrue(
            store.insertInterpretedMoneyMovementIfNew(
                kind = CanonicalFinancialRecordKind.RECEIPT,
                transactionCode = "REFTEST220",
                amountMinor = 22_000L,
                counterparty = merchant,
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = message,
                sourcePackage = "test.sms",
                sourceKind = "interpreted_TEST_SMS",
                receivedAtEpochMs = EPOCH + 3_600_000L,
                occurredAtEpochMs = EPOCH + 3_600_000L,
                rebuildPayCycles = false,
                accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
                directionOverride = MoneyDirection.INCOME
            )
        )
        return requireNotNull(store.findByTransactionCode("REFTEST220"))
    }

    private fun insertOutgoing(
        reviewState: ReviewState,
        relationType: String? = null,
        cycleCandidateReason: String? = null
    ): Long {
        val values = ContentValues().apply {
            put("transaction_code", "PAYTEST220")
            put("fingerprint", "synthetic-original-taxi")
            put("type", "BUY_GOODS")
            put("amount_minor", 22_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", EPOCH)
            put("received_at_epoch_ms", EPOCH)
            put("created_at_epoch_ms", EPOCH)
            put("source_kind", "TEST_SYNTHETIC")
            put("direction", MoneyDirection.EXPENSE.name)
            put("category", TransactionCategory.TRANSPORT.name)
            put("review_state", reviewState.name)
            put("counterparty", "SAMPLE TAXI")
            if (relationType != null) put("relation_type", relationType)
            if (cycleCandidateReason != null) {
                put("cycle_candidate_reason", cycleCandidateReason)
            }
        }
        return PesaTrackDatabase.get(context).writableDatabase.insertOrThrow(
            "transactions", null, values
        )
    }

    private companion object {
        const val EPOCH = 1_790_611_800_000L
    }
}
