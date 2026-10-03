package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal

enum class MessageLabFinancialKind(
    val label: String
) {
    PAYMENT_EXPENSE("Payment / expense"),
    MONEY_RECEIVED("Money received"),
    TRANSFER("Transfer"),
    DEBT("Loan / repayment"),
    BALANCE_UPDATE("Balance / account update"),
    INVESTMENT("Investment"),
    REVERSAL_REFUND("Reversal / refund"),
    FEE_CHARGE("Fee / charge"),
    OTHER_FINANCIAL("Other financial")
}

object MessageLabFinancialKindSuggester {
    fun suggest(
        shape:
            UnsupportedMessageShape
    ): MessageLabFinancialKind {
        val body =
            shape.redactedBody
                .lowercase()
        val signals =
            shape.relevanceSignals

        return when {
            FinancialRelevanceSignal
                .DEBT_CONTEXT in signals ||
                containsAny(
                    body,
                    "loan",
                    "repay",
                    "outstanding",
                    "credit facility",
                    "borrow"
                ) ->
                MessageLabFinancialKind
                    .DEBT

            FinancialRelevanceSignal
                .INVESTMENT_CONTEXT in
                signals ||
                containsAny(
                    body,
                    "shares",
                    "units",
                    "dividend",
                    "investment",
                    "fund"
                ) ->
                MessageLabFinancialKind
                    .INVESTMENT

            containsAny(
                body,
                "reversal",
                "reversed",
                "refund",
                "refunded"
            ) ->
                MessageLabFinancialKind
                    .REVERSAL_REFUND

            containsAny(
                body,
                "transferred",
                "transfer",
                "sent to",
                "moved to"
            ) ->
                MessageLabFinancialKind
                    .TRANSFER

            containsAny(
                body,
                "received",
                "credited",
                "credit to",
                "deposit"
            ) ->
                MessageLabFinancialKind
                    .MONEY_RECEIVED

            containsAny(
                body,
                "paid",
                "payment",
                "purchase",
                "buy goods",
                "merchant"
            ) ->
                MessageLabFinancialKind
                    .PAYMENT_EXPENSE

            containsAny(
                body,
                "balance",
                "available balance",
                "account balance"
            ) ->
                MessageLabFinancialKind
                    .BALANCE_UPDATE

            containsAny(
                body,
                "fee",
                "charge",
                "transaction cost"
            ) ->
                MessageLabFinancialKind
                    .FEE_CHARGE

            else ->
                MessageLabFinancialKind
                    .OTHER_FINANCIAL
        }
    }

    private fun containsAny(
        body: String,
        vararg needles: String
    ): Boolean =
        needles.any {
            body.contains(it)
        }
}
