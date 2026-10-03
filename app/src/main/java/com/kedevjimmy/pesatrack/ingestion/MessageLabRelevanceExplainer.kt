package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal

data class MessageLabRelevanceExplanation(
    val headline: String,
    val reasons: List<String>,
    val caution: String? = null
)

object MessageLabRelevanceExplainer {
    fun explain(
        score: Int?,
        signals: Set<FinancialRelevanceSignal>,
        userDecision: MessageLabUserDecision?
    ): MessageLabRelevanceExplanation {
        if (
            userDecision ==
            MessageLabUserDecision
                .CONFIRMED_FINANCIAL
        ) {
            return MessageLabRelevanceExplanation(
                headline =
                    "You confirmed this is financial",
                reasons =
                    signalReasons(signals)
                        .ifEmpty {
                            listOf(
                                "Kept as real financial evidence for future parser coverage."
                            )
                        },
                caution =
                    score?.let {
                        "Local relevance score: $it"
                    }
            )
        }

        if (
            userDecision ==
            MessageLabUserDecision
                .NOT_FINANCIAL
        ) {
            return MessageLabRelevanceExplanation(
                headline =
                    "You marked this as non-financial",
                reasons =
                    signalReasons(signals)
                        .ifEmpty {
                            listOf(
                                "The earlier relevance decision is suppressed for this message shape."
                            )
                        },
                caution =
                    "This only suppresses unsupported-message noise; a future genuine parser match can still be recognized."
            )
        }

        val reasons =
            signalReasons(signals)

        return MessageLabRelevanceExplanation(
            headline =
                if (reasons.isEmpty()) {
                    "Why this was flagged is not stored yet"
                } else {
                    "Why PesaTrack flagged this"
                },
            reasons =
                reasons.ifEmpty {
                    listOf(
                        "Reprocess recent history to refresh the relevance evidence for this older Message Lab shape."
                    )
                },
            caution =
                score?.let {
                    "Local relevance score: $it"
                }
        )
    }

    private fun signalReasons(
        signals:
            Set<FinancialRelevanceSignal>
    ): List<String> =
        buildList {
            if (
                FinancialRelevanceSignal
                    .CURRENCY_AMOUNT in signals
            ) {
                add(
                    "Contains a currency amount."
                )
            }
            if (
                FinancialRelevanceSignal
                    .FINANCIAL_ACTION in signals
            ) {
                add(
                    "Uses money-movement language such as paid, credited, debited, sent, received or transferred."
                )
            }
            if (
                FinancialRelevanceSignal
                    .ACCOUNT_CONTEXT in signals
            ) {
                add(
                    "Mentions an account, wallet, balance, transaction reference or banking context."
                )
            }
            if (
                FinancialRelevanceSignal
                    .DEBT_CONTEXT in signals
            ) {
                add(
                    "Contains loan, repayment, outstanding-balance or credit-facility language."
                )
            }
            if (
                FinancialRelevanceSignal
                    .INVESTMENT_CONTEXT in signals
            ) {
                add(
                    "Contains investment, shares, units, order or dividend language."
                )
            }
            if (
                FinancialRelevanceSignal
                    .FINANCIAL_CONFIRMATION in signals
            ) {
                add(
                    "Looks like a confirmation of a transaction, transfer, payment, loan or funds movement."
                )
            }
            if (
                FinancialRelevanceSignal
                    .PURCHASE_CONTEXT in signals
            ) {
                add(
                    "Looks like a paid purchase of airtime, data, utilities or a subscription."
                )
            }
            if (
                FinancialRelevanceSignal
                    .SECURITY_NOISE in signals
            ) {
                add(
                    "Also contains OTP, PIN or security-code language."
                )
            }
            if (
                FinancialRelevanceSignal
                    .PROMOTIONAL_NOISE in signals
            ) {
                add(
                    "Also contains promotional or reward language."
                )
            }
            if (
                FinancialRelevanceSignal
                    .FINANCIAL_SOLICITATION in signals
            ) {
                add(
                    "Also looks like a loan or credit solicitation rather than a completed money event."
                )
            }
            if (
                FinancialRelevanceSignal
                    .TELECOM_INFORMATION_ONLY in signals
            ) {
                add(
                    "Also looks like telecom balance or expiry information."
                )
            }
        }
}
