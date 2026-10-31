package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

/** Existing TransactionStore default classification, isolated for deterministic JVM tests. */
data class DefaultCategoryFallbackDecision(
    val direction: MoneyDirection,
    val category: TransactionCategory,
    val reviewState: ReviewState
)

object DefaultCategoryFallbackPolicy {
    fun classify(parsed: ParsedMpesaTransaction): DefaultCategoryFallbackDecision {
        val decision = when (parsed.type) {
            MpesaTransactionType.WITHDRAWAL -> Triple(
                MoneyDirection.TRANSFER,
                TransactionCategory.CASH,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.AIRTIME -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.PHONE_AND_DATA,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.REVERSAL -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.OTHER,
                if (parsed.reversalOfCode != null) ReviewState.AUTO_CLASSIFIED
                else ReviewState.NEEDS_REVIEW
            )
            MpesaTransactionType.RECEIVED,
            MpesaTransactionType.DEPOSIT -> Triple(
                MoneyDirection.INCOME,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.SENT_MONEY,
            MpesaTransactionType.PAY_BILL,
            MpesaTransactionType.BUY_GOODS -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.OTHER -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
            )
        }
        return DefaultCategoryFallbackDecision(
            direction = decision.first,
            category = decision.second,
            reviewState = decision.third
        )
    }
}
