package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePackInvestmentSemanticBridgeTest {
    @Test
    fun sanlamFundCreditBecomesExecutedInvestmentObservation() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "SANLAM_UT",
                    body =
                        "Dear SAMPLE PERSON, Your account for Sanlam Money Market Fund has been credited with Kes 2,000.00 and 2,000.00 units, as of 06 Jun, 2024."
                )

        assertEquals(
            "INVESTMENT_CREDIT",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val semantic =
            interpretation.semanticAssessment
        assertEquals(
            FinancialSemanticIntent
                .INVESTMENT_EXECUTED,
            semantic?.primaryIntent
        )
        assertEquals(
            InterpretationConfidence.HIGH,
            semantic?.confidence
        )

        val investment =
            semantic?.investment
        assertEquals(
            SemanticInvestmentAction.BUY,
            investment?.action
        )
        assertEquals(
            SemanticInvestmentStage.EXECUTED,
            investment?.stage
        )
        assertEquals(
            2_000_000_000L,
            investment
                ?.quantityMicrounits
        )
        assertEquals(
            "Sanlam Money Market Fund",
            investment
                ?.instrumentLabel
        )
        assertEquals(
            "instrument:SANLAM_MONEY_MARKET_FUND",
            investment
                ?.instrumentKey
        )
        assertEquals(
            200_000L,
            investment?.valueMinor
        )
    }

    @Test
    fun sanlamAmountOnlyTopUpStaysInvestmentProvenanceOnly() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "SANLAM_UT",
                    body =
                        "Dear SAMPLE PERSON, you have successfully topped up your account with KES. 2000.00, your account will be updated soon, Thank You."
                )

        assertEquals(
            "INVESTMENT_TOP_UP",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertNull(
            interpretation
                .semanticAssessment
                ?.investment
        )
        assertEquals(
            FinancialSemanticIntent
                .CONFIRMATION_ONLY,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertTrue(
            interpretation
                .semanticAssessment
                ?.intents
                ?.none {
                    it in
                        setOf(
                            FinancialSemanticIntent
                                .PAYMENT,
                            FinancialSemanticIntent
                                .RECEIPT,
                            FinancialSemanticIntent
                                .TRANSFER
                        )
                } == true
        )
        assertTrue(
            interpretation
                .semanticallyUnderstood
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun eticaRedemptionCannotFallThroughAsOrdinaryPayment() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "ETICALTD",
                    body =
                        "You have sent KES 111,111.00 from your account 11111MC in EMMF - KES to Mr. SAMPLE PERSON (11111F)"
                )

        assertEquals(
            "INVESTMENT_WITHDRAWAL",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertNull(
            interpretation
                .semanticAssessment
                ?.investment
        )
        assertEquals(
            FinancialSemanticIntent
                .CONFIRMATION_ONLY,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun foreignCurrencyEticaRedemptionRemainsProvenanceOnly() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "ETICALTD",
                    body =
                        "You have sent USD 1,234.00 from your account 11111MU in EMMF - USD to Mr. SAMPLE PERSON (11111FU)"
                )

        assertEquals(
            "INVESTMENT_WITHDRAWAL",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertNull(
            interpretation
                .semanticAssessment
                ?.investment
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun provenanceOnlyDebtNoticeAlsoBlocksLegacyTransactionParsing() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "TALA",
                    body =
                        "Tala: Your loan payment of KES 3,300.00 is due on 31 Oct 2026. Pay via Paybill 851201, Acc: 0712345678."
                )

        assertEquals(
            "DEBT_DUE_NOTICE",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }
}
