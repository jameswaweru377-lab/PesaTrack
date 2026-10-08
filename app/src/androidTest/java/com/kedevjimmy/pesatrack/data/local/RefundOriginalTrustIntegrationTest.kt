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
    fun trustedTaxiPaymentThenEqualReceiptStillLinksAsRefund() {
        val originalId = insertOutgoing(ReviewState.AUTO_CLASSIFIED)
        val receipt = incomingReceipt()
        assertEquals(TransactionStore.RELATION_REFUND, receipt.relationType)
        assertEquals(originalId, receipt.relatedTransactionId)
        assertEquals(MoneyDirection.TRANSFER.name, receipt.direction)
        assertEquals(TransactionCategory.TRANSPORT.name, receipt.category)
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

    private fun incomingReceipt(): StoredTransaction {
        assertTrue(
            store.insertInterpretedMoneyMovementIfNew(
                kind = CanonicalFinancialRecordKind.RECEIPT,
                transactionCode = "REFTEST220",
                amountMinor = 22_000L,
                counterparty = "SAMPLE TAXI",
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = "Synthetic merchant payment returned",
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
