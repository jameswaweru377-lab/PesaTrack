package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLabFinancialKindSuggesterTest {
    @Test
    fun debtLanguageWinsForRepaymentShape() {
        val shape =
            shape(
                body =
                    "Loan of <AMOUNT> repaid. Loan balance is <AMOUNT>.",
                signals =
                    setOf(
                        FinancialRelevanceSignal
                            .CURRENCY_AMOUNT,
                        FinancialRelevanceSignal
                            .DEBT_CONTEXT
                    )
            )

        assertEquals(
            MessageLabFinancialKind.DEBT,
            MessageLabFinancialKindSuggester
                .suggest(shape)
        )
    }

    @Test
    fun receivedLanguageSuggestsMoneyReceived() {
        val shape =
            shape(
                body =
                    "<AMOUNT> received from <COUNTERPARTY>."
            )

        assertEquals(
            MessageLabFinancialKind
                .MONEY_RECEIVED,
            MessageLabFinancialKindSuggester
                .suggest(shape)
        )
    }

    @Test
    fun paidLanguageSuggestsExpense() {
        val shape =
            shape(
                body =
                    "You paid <AMOUNT> to <COUNTERPARTY>."
            )

        assertEquals(
            MessageLabFinancialKind
                .PAYMENT_EXPENSE,
            MessageLabFinancialKindSuggester
                .suggest(shape)
        )
    }

    @Test
    fun investmentSignalWinsWithoutProviderKnowledge() {
        val shape =
            shape(
                body =
                    "Your order for <AMOUNT> has been processed.",
                signals =
                    setOf(
                        FinancialRelevanceSignal
                            .INVESTMENT_CONTEXT
                    )
            )

        assertEquals(
            MessageLabFinancialKind
                .INVESTMENT,
            MessageLabFinancialKindSuggester
                .suggest(shape)
        )
    }

    @Test
    fun unknownFinancialShapeFallsBackToOther() {
        val shape =
            shape(
                body =
                    "Financial notification <REF>."
            )

        assertEquals(
            MessageLabFinancialKind
                .OTHER_FINANCIAL,
            MessageLabFinancialKindSuggester
                .suggest(shape)
        )
    }

    private fun shape(
        body: String,
        signals:
            Set<FinancialRelevanceSignal> =
            emptySet()
    ) =
        UnsupportedMessageShape(
            shapeId = "shape",
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody = body,
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount = 1,
            userDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            relevanceSignals =
                signals
        )
}
