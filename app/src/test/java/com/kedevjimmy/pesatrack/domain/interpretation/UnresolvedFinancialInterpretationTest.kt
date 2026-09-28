package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnresolvedFinancialInterpretationTest {
    @Test
    fun preservesUniqueUnknownAmountForReviewWithoutGuessingDirection() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12UNK01 Confirmed. Ksh 640.00 processed through M-PESA on 28/9/26 at 9:10 AM. New M-PESA balance is Ksh 12,210.00."
            )

        val event =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(64_000L, event.amountMinor)
        assertEquals(
            InterpretationConfidence.LOW,
            event.confidence
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.observedAccount?.providerKey
        )
        assertTrue(
            event.evidence.any {
                it.field == FinancialField.AMOUNT &&
                    it.span != null
            }
        )
        assertTrue(
            interpretation.consumesLegacyTransactionParsing
        )

        val canonical =
            interpretation.toCanonicalDraft()
        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT
            ),
            canonical.records.map { it.kind }
        )
    }

    @Test
    fun excludesFeeAndBalanceAmountsBeforeChoosingUnknownMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12UNK02 Confirmed. Ksh 640.00 processed through M-PESA on 28/9/26 at 9:10 AM. Transaction cost, Ksh 10.00. New M-PESA balance is Ksh 12,200.00."
            )

        val event =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(64_000L, event.amountMinor)
        assertEquals(1_000L, event.feeMinor)

        val canonical =
            interpretation.toCanonicalDraft()
        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT,
                CanonicalFinancialRecordKind.FEE
            ),
            canonical.records.map { it.kind }
        )
    }

    @Test
    fun refusesToChooseWhenTwoTransactionAmountsRemainPlausible() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12UNK03 Confirmed. Ksh 640.00 and Ksh 120.00 were processed through M-PESA on 28/9/26 at 9:10 AM. New M-PESA balance is Ksh 12,090.00."
            )

        assertTrue(interpretation.events.isEmpty())
        assertEquals(1, interpretation.balances.size)
        assertFalse(
            interpretation.consumesLegacyTransactionParsing
        )
    }

    @Test
    fun requiresTransactionReferenceBeforeCreatingUnknownReviewMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "Ksh 640.00 processed through M-PESA. New M-PESA balance is Ksh 12,210.00."
            )

        assertTrue(interpretation.events.isEmpty())
        assertEquals(1, interpretation.balances.size)
    }
}
