package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Protects the exact default outcomes previously embedded in TransactionStore.classify. */
class DefaultCategoryFallbackPolicyTest {
    @Test
    fun everyTransactionTypeRetainsItsCurrentFallbackDecision() {
        val expected = mapOf(
            MpesaTransactionType.WITHDRAWAL to Triple(
                MoneyDirection.TRANSFER, TransactionCategory.CASH, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.AIRTIME to Triple(
                MoneyDirection.EXPENSE, TransactionCategory.PHONE_AND_DATA, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.REVERSAL to Triple(
                MoneyDirection.NEUTRAL, TransactionCategory.OTHER, ReviewState.NEEDS_REVIEW
            ),
            MpesaTransactionType.RECEIVED to Triple(
                MoneyDirection.INCOME, TransactionCategory.UNCATEGORIZED, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.DEPOSIT to Triple(
                MoneyDirection.INCOME, TransactionCategory.UNCATEGORIZED, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.SENT_MONEY to Triple(
                MoneyDirection.EXPENSE, TransactionCategory.UNCATEGORIZED, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.PAY_BILL to Triple(
                MoneyDirection.EXPENSE, TransactionCategory.UNCATEGORIZED, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.BUY_GOODS to Triple(
                MoneyDirection.EXPENSE, TransactionCategory.UNCATEGORIZED, ReviewState.AUTO_CLASSIFIED
            ),
            MpesaTransactionType.OTHER to Triple(
                MoneyDirection.NEUTRAL, TransactionCategory.UNCATEGORIZED, ReviewState.NEEDS_REVIEW
            )
        )
        assertEquals(MpesaTransactionType.entries.size, expected.size)
        expected.forEach { (type, want) ->
            val actual = DefaultCategoryFallbackPolicy.classify(sample(type))
            assertEquals(type.name, want.first, actual.direction)
            assertEquals(type.name, want.second, actual.category)
            assertEquals(type.name, want.third, actual.reviewState)
        }
    }

    @Test
    fun reversalReferenceRetainsAutomaticNeutralDecision() {
        val decision = DefaultCategoryFallbackPolicy.classify(
            sample(MpesaTransactionType.REVERSAL).copy(reversalOfCode = "EXAM_ORIGINAL_001")
        )
        assertEquals(MoneyDirection.NEUTRAL, decision.direction)
        assertEquals(TransactionCategory.OTHER, decision.category)
        assertEquals(ReviewState.AUTO_CLASSIFIED, decision.reviewState)
    }

    private fun sample(type: MpesaTransactionType) = ParsedMpesaTransaction(
        transactionCode = "EXAM_FALLBACK_001",
        type = type,
        amountMinor = 1250L,
        counterparty = "ExamCounterparty",
        feeMinor = 0L,
        balanceMinor = null,
        occurredAtText = null,
        rawMessage = "FICTIONAL TEST ONLY"
    )
}
