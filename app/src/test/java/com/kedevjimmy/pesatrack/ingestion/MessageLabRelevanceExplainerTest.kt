package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabRelevanceExplainerTest {
    @Test
    fun explainsPositiveFinancialSignals() {
        val explanation =
            MessageLabRelevanceExplainer
                .explain(
                    score = 5,
                    signals =
                        setOf(
                            FinancialRelevanceSignal
                                .CURRENCY_AMOUNT,
                            FinancialRelevanceSignal
                                .DEBT_CONTEXT,
                            FinancialRelevanceSignal
                                .FINANCIAL_CONFIRMATION
                        ),
                    userDecision = null
                )

        assertTrue(
            explanation.headline
                .contains("flagged")
        )
        assertTrue(
            explanation.reasons.any {
                it.contains(
                    "currency",
                    ignoreCase = true
                )
            }
        )
        assertTrue(
            explanation.reasons.any {
                it.contains(
                    "loan",
                    ignoreCase = true
                )
            }
        )
    }

    @Test
    fun oldShapeWithoutEvidenceAsksForReprocess() {
        val explanation =
            MessageLabRelevanceExplainer
                .explain(
                    score = null,
                    signals = emptySet(),
                    userDecision = null
                )

        assertTrue(
            explanation.reasons.single()
                .contains(
                    "Reprocess",
                    ignoreCase = true
                )
        )
    }

    @Test
    fun confirmedFinancialExplainsUserAuthority() {
        val explanation =
            MessageLabRelevanceExplainer
                .explain(
                    score = 1,
                    signals =
                        setOf(
                            FinancialRelevanceSignal
                                .ACCOUNT_CONTEXT
                        ),
                    userDecision =
                        MessageLabUserDecision
                            .CONFIRMED_FINANCIAL
                )

        assertTrue(
            explanation.headline
                .contains(
                    "confirmed",
                    ignoreCase = true
                )
        )
    }

    @Test
    fun ignoredShapeExplainsThatSuppressionIsDiagnosticOnly() {
        val explanation =
            MessageLabRelevanceExplainer
                .explain(
                    score = 3,
                    signals =
                        setOf(
                            FinancialRelevanceSignal
                                .FINANCIAL_ACTION
                        ),
                    userDecision =
                        MessageLabUserDecision
                            .NOT_FINANCIAL
                )

        assertTrue(
            explanation.caution
                .orEmpty()
                .contains(
                    "future",
                    ignoreCase = true
                )
        )
    }
}
