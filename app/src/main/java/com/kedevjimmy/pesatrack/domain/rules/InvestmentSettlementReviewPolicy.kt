package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentAction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import kotlin.math.abs

data class InvestmentSettlementReviewAssessment(
    val eligible: Boolean,
    val reason: String? = null
)

object InvestmentSettlementReviewPolicy {
    const val MAX_TIME_DELTA_MS =
        10L * 60L * 1000L

    fun assess(
        action: SemanticInvestmentAction,
        investmentValueMinor: Long,
        investmentOccurredAtEpochMs: Long,
        transactionAmountMinor: Long,
        transactionDirection: String,
        transactionOccurredAtEpochMs: Long,
        sharesExactReference: Boolean,
        protected: Boolean
    ): InvestmentSettlementReviewAssessment {
        if (
            protected ||
            investmentValueMinor <= 0L ||
            transactionAmountMinor !=
                investmentValueMinor
        ) {
            return InvestmentSettlementReviewAssessment(
                eligible = false
            )
        }

        val expectedDirection =
            when (action) {
                SemanticInvestmentAction.BUY ->
                    MoneyDirection.EXPENSE.name
                SemanticInvestmentAction.SELL ->
                    MoneyDirection.INCOME.name
            }
        if (
            transactionDirection !=
                expectedDirection
        ) {
            return InvestmentSettlementReviewAssessment(
                eligible = false
            )
        }

        if (sharesExactReference) {
            return InvestmentSettlementReviewAssessment(
                eligible = true,
                reason =
                    "Shares an exact external reference and the same investment value, but automatic settlement remained ambiguous."
            )
        }

        val delta =
            abs(
                transactionOccurredAtEpochMs -
                    investmentOccurredAtEpochMs
            )
        if (delta > MAX_TIME_DELTA_MS) {
            return InvestmentSettlementReviewAssessment(
                eligible = false
            )
        }

        val minutes =
            maxOf(1L, delta / 60_000L)
        return InvestmentSettlementReviewAssessment(
            eligible = true,
            reason =
                "Same investment value and correct cash direction within " +
                    if (minutes == 1L) {
                        "about one minute"
                    } else {
                        "about $minutes minutes"
                    } +
                    " of the execution."
        )
    }
}
