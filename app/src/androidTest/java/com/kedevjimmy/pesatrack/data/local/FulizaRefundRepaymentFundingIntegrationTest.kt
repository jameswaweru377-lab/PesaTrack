package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.InterpretationConfidence
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.RepaymentChainConfidence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 / A2 / R03: a trusted merchant refund is a *wallet credit* even though
 * the legacy Activity ledger stores it as a linked TRANSFER. It must be
 * eligible to fund an automatic Fuliza repayment without counting as salary.
 *
 * All amounts/messages are synthetic. SMS-provider ingestion and full
 * device-specific acceptance are separate roadmap requirements.
 */
@RunWith(AndroidJUnit4::class)
class FulizaRefundRepaymentFundingIntegrationTest {
    private lateinit var context: Context
    private lateinit var transactions: TransactionStore
    private lateinit var debts: DebtStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        transactions = TransactionStore.get(context)
        debts = DebtStore.get(context)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun refundedTaxiPaymentCanFundImmediateFulizaRepayment() {
        assertTrue(
            transactions.insertInterpretedMoneyMovementIfNew(
                kind = CanonicalFinancialRecordKind.PAYMENT,
                paymentRouteHint = PaymentRouteHint.MERCHANT,
                transactionCode = "TRIPTEST001",
                amountMinor = 22_000L,
                counterparty = "SAMPLE TAXI",
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = "Synthetic Fuliza-funded taxi payment",
                sourcePackage = "test.sms",
                sourceKind = "interpreted_TEST_SMS",
                receivedAtEpochMs = BASE_TIME,
                occurredAtEpochMs = BASE_TIME,
                rebuildPayCycles = false,
                accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
                categoryOverride = TransactionCategory.TRANSPORT
            )
        )
        val original = requireNotNull(transactions.findByTransactionCode("TRIPTEST001"))
        assertEquals(MoneyDirection.EXPENSE.name, original.direction)

        assertTrue(
            transactions.insertInterpretedMoneyMovementIfNew(
                kind = CanonicalFinancialRecordKind.RECEIPT,
                transactionCode = "REFTEST001",
                amountMinor = 22_000L,
                counterparty = "SAMPLE TAXI",
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = "Synthetic cancelled ride payment returned",
                sourcePackage = "test.sms",
                sourceKind = "interpreted_TEST_SMS",
                receivedAtEpochMs = REFUND_TIME,
                occurredAtEpochMs = REFUND_TIME,
                rebuildPayCycles = false,
                accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
                directionOverride = MoneyDirection.INCOME
            )
        )
        val refund = requireNotNull(transactions.findByTransactionCode("REFTEST001"))
        assertEquals(TransactionStore.RELATION_REFUND, refund.relationType)
        assertEquals(original.id, refund.relatedTransactionId)
        assertEquals(MoneyDirection.TRANSFER.name, refund.direction)
        assertEquals(AccountStore.DEFAULT_MPESA_ID, refund.accountId)
        assertNull(refund.linkedAccountId)

        val repayment = recordSyntheticRepayment("REPAYTEST001")
        assertEquals(refund.id, repayment.linkedTransactionId)
        assertEquals(RepaymentChainConfidence.MEDIUM, fundingConfidence(repayment.eventId))

        // Same evidence replay must not create a second repayment.
        val replay = recordSyntheticRepayment("REPAYTEST001")
        assertFalse(replay.inserted)
        assertEquals(refund.id, replay.linkedTransactionId)
    }

    @Test
    fun unresolvedIncomingMoneyCannotBeUsedAsConfirmedRepaymentFunding() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        ContentValues().apply {
            put("transaction_code", "UNCERTAIN001")
            put("fingerprint", "synthetic-unresolved-funding")
            put("type", "RECEIVED")
            put("amount_minor", 22_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", REFUND_TIME)
            put("received_at_epoch_ms", REFUND_TIME)
            put("created_at_epoch_ms", REFUND_TIME)
            put("source_kind", "TEST_SYNTHETIC")
            put("direction", MoneyDirection.INCOME.name)
            put("category", TransactionCategory.UNCATEGORIZED.name)
            put("review_state", ReviewState.NEEDS_REVIEW.name)
            put("account_id", AccountStore.DEFAULT_MPESA_ID)
        }.also { db.insertOrThrow("transactions", null, it) }

        val repayment = recordSyntheticRepayment("REPAYTEST002")
        assertNull(repayment.linkedTransactionId)
    }

    private fun recordSyntheticRepayment(code: String): DebtWriteResult =
        debts.recordRepayment(
            event = DebtRepaymentEventFact(
                amountMinor = 22_000L,
                liability = FinancialAccountReference(
                    entityKey = "provider:SAFARICOM_FULIZA",
                    displayName = "Fuliza",
                    providerKey = "SAFARICOM_FULIZA",
                    accountTypeHint = AccountType.OTHER,
                    ownershipHint = AccountOwnershipHint.UNKNOWN,
                    nature = FinancialAccountNature.LIABILITY
                ),
                fundingAccount = FinancialAccountReference(
                    entityKey = "provider:SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    providerKey = "SAFARICOM_MPESA",
                    accountTypeHint = AccountType.MOBILE_WALLET,
                    ownershipHint = AccountOwnershipHint.USER_OWNED,
                    nature = FinancialAccountNature.ASSET
                ),
                completion = DebtCompletion.UNKNOWN,
                outstandingAfterMinor = null,
                availableCreditAfterMinor = null,
                walletBalanceAfterMinor = null,
                confidence = InterpretationConfidence.HIGH
            ),
            transactionCode = code,
            messageFingerprint = "synthetic-repayment-$code",
            sourcePackage = "test.sms",
            sourceKind = "interpreted_TEST_SMS",
            occurredAtEpochMs = REPAYMENT_TIME
        )

    private fun fundingConfidence(eventId: Long): RepaymentChainConfidence? =
        PesaTrackDatabase.get(context).readableDatabase.rawQuery(
            "SELECT funding_link_confidence FROM debt_events WHERE id = ?",
            arrayOf(eventId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0)) null
            else RepaymentChainConfidence.valueOf(cursor.getString(0))
        }

    private companion object {
        const val BASE_TIME = 1_790_611_800_000L
        const val REFUND_TIME = BASE_TIME + 3_600_000L
        const val REPAYMENT_TIME = REFUND_TIME + 60_000L
    }
}
