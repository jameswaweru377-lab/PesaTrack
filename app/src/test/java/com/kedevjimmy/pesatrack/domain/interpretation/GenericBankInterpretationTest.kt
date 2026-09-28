package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericBankInterpretationTest {
    @Test
    fun understandsBankCreditAndSeparatesAvailableBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account has been credited with KES 60,000.00 from XYZ COMPANY on 26/09/2026. Available balance KES 72,100.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(6_000_000L, receipt.amountMinor)
        assertEquals("XYZ COMPANY", receipt.counterparty)
        assertEquals(
            "My Bank",
            receipt.destination.displayName
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            receipt.destination.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            receipt.destination.nature
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            receipt.confidence
        )

        val balance =
            interpretation.balances.single()
        assertEquals(
            7_210_000L,
            balance.balanceMinor
        )
        assertEquals(
            receipt.destination.entityKey,
            balance.account.entityKey
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun understandsAmountFirstReceivedFromBankWording() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 60,000.00 received from My Employer. Available balance KES 72,100.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(6_000_000L, receipt.amountMinor)
        assertEquals("My Employer", receipt.counterparty)
        assertEquals(
            7_210_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }
}
