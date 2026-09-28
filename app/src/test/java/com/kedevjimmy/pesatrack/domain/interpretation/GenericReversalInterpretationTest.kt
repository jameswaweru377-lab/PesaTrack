package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericReversalInterpretationTest {
    @Test
    fun understandsReversalOfTransactionWithoutLegacyParser() {
        val message =
            "TST12ABC10 Confirmed. The reversal of transaction TST12ABC04 of Ksh 450.00 has been completed on 28/9/26 at 7:40 PM. New M-PESA balance is Ksh 8,100.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val event =
            interpretation.events.single() as
                ReversalEventFact

        assertEquals(45_000L, event.amountMinor)
        assertEquals(
            "TST12ABC04",
            event.referencedTransactionCode
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.observedAccount?.providerKey
        )
        assertTrue(
            event.evidence.any {
                it.field ==
                    FinancialField
                        .RELATED_TRANSACTION_CODE
            }
        )

        val canonical =
            interpretation.toCanonicalDraft()

        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.REVERSAL
            ),
            canonical.records.map { it.kind }
        )
        assertFalse(canonical.requiresLegacyFallback)
    }

    @Test
    fun understandsTransactionWasReversedVariant() {
        val message =
            "TST12ABC11 Confirmed. Transaction TST12ABC04 of Ksh 450.00 has been reversed. New M-PESA balance is Ksh 8,100.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        val event =
            interpretation.events.single() as
                ReversalEventFact

        assertEquals(45_000L, event.amountMinor)
        assertEquals(
            "TST12ABC04",
            event.referencedTransactionCode
        )
    }

    @Test
    fun ordinaryReceiptContainingNoReversalStaysReceipt() {
        val message =
            "TST12ABC12 Confirmed. You have received Ksh 450.00 from SAMPLE PERSON 0712000000 on 28/9/26 at 7:45 PM. New M-PESA balance is Ksh 8,550.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        assertTrue(
            interpretation.events.single() is
                ReceiptEventFact
        )
    }
}
