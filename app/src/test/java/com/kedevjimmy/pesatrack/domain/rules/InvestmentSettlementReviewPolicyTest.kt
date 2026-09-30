package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentAction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentSettlementReviewPolicyTest {
    @Test
    fun exactReferenceCanRemainReviewableOutsideTimeWindow() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.BUY,
                investmentValueMinor = 302_100L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 302_100L,
                transactionDirection =
                    MoneyDirection.EXPENSE.name,
                transactionOccurredAtEpochMs =
                    1_000_000L +
                        60L * 60L * 1000L,
                sharesExactReference = true,
                protected = false
            )

        assertTrue(result.eligible)
    }

    @Test
    fun sameValueCorrectDirectionInsideWindowIsReviewable() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.SELL,
                investmentValueMinor = 125_000L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 125_000L,
                transactionDirection =
                    MoneyDirection.INCOME.name,
                transactionOccurredAtEpochMs =
                    1_240_000L,
                sharesExactReference = false,
                protected = false
            )

        assertTrue(result.eligible)
    }

    @Test
    fun sameValueOutsideWindowWithoutReferenceIsNotReviewable() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.BUY,
                investmentValueMinor = 302_100L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 302_100L,
                transactionDirection =
                    MoneyDirection.EXPENSE.name,
                transactionOccurredAtEpochMs =
                    1_000_000L +
                        InvestmentSettlementReviewPolicy
                            .MAX_TIME_DELTA_MS +
                        1L,
                sharesExactReference = false,
                protected = false
            )

        assertFalse(result.eligible)
    }

    @Test
    fun wrongCashDirectionIsNotReviewable() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.BUY,
                investmentValueMinor = 302_100L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 302_100L,
                transactionDirection =
                    MoneyDirection.INCOME.name,
                transactionOccurredAtEpochMs = 1_001_000L,
                sharesExactReference = true,
                protected = false
            )

        assertFalse(result.eligible)
    }

    @Test
    fun protectedTransactionIsNeverReviewable() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.SELL,
                investmentValueMinor = 125_000L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 125_000L,
                transactionDirection =
                    MoneyDirection.INCOME.name,
                transactionOccurredAtEpochMs = 1_001_000L,
                sharesExactReference = true,
                protected = true
            )

        assertFalse(result.eligible)
    }

    @Test
    fun differentAmountIsNeverReviewable() {
        val result =
            InvestmentSettlementReviewPolicy.assess(
                action = SemanticInvestmentAction.BUY,
                investmentValueMinor = 302_100L,
                investmentOccurredAtEpochMs = 1_000_000L,
                transactionAmountMinor = 300_000L,
                transactionDirection =
                    MoneyDirection.EXPENSE.name,
                transactionOccurredAtEpochMs = 1_001_000L,
                sharesExactReference = true,
                protected = false
            )

        assertFalse(result.eligible)
    }
}
