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
            AccountOwnershipHint.USER_OWNED,
            receipt.destination.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            receipt.destination.nature
        )
        assertEquals(
            InterpretationConfidence.HIGH,
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
    fun understandsExplicitBankDebitWithoutInventingCounterparty() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX1234 has been debited with KES 1,250.00 on 03/10/26. Available balance KES 8,750.00. Ref FT26276ABC."
            )

        assertTrue(
            interpretation.recognized
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            125_000L,
            debit.amountMinor
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            debit.observedAccount
                ?.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            debit.observedAccount
                ?.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            debit.confidence
        )
        assertEquals(
            FinancialMovementDirectionHint
                .OUTGOING,
            debit.directionHint
        )
        assertTrue(
            debit.evidence.any {
                it.field ==
                    FinancialField
                        .ACCOUNT_REFERENCE
            }
        )
        assertEquals(
            875_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun brandedAccountDebitWithBeneficiaryStillUsesPaymentPath() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX5678 has been debited with KES 2,400.00. Transfer to SAMPLE BENEFICIARY completed. Available balance KES 6,350.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            240_000L,
            payment.amountMinor
        )
        assertEquals(
            "SAMPLE BENEFICIARY",
            payment.counterparty
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            payment.source.providerKey
        )
    }

    @Test
    fun genericBankDebitWithoutCounterpartyUsesOwnedMessageSource() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by Ksh 825.50 on 03/10/26. Available balance KES 4,100.00."
            )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            82_550L,
            debit.amountMinor
        )
        assertEquals(
            "My Bank",
            debit.observedAccount
                ?.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            debit.observedAccount
                ?.ownershipHint
        )
    }

    @Test
    fun genericUnresolvedRelationshipDoesNotInventDirection() {
        val interpretation =
            FinancialEventClassifier.classify(
                ResolvedFinancialMessage(
                    transactionCode = null,
                    occurredAt = null,
                    balances = emptyList(),
                    relationship =
                        ResolvedUnresolvedMovementRelationship(
                            amountMinor = 92_500L,
                            observedAccount = null,
                            feeMinor = 0L,
                            confidence =
                                InterpretationConfidence.LOW,
                            evidence = emptyList()
                        )
                )
            )

        val unresolved =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            null,
            unresolved.directionHint
        )
    }

    @Test
    fun understandsExplicitBankCreditWithoutInventingCounterparty() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX1234 has been credited with KES 5,000.00 on 03/10/26. Available balance KES 13,750.00. Ref FT26276XYZ."
            )

        assertTrue(
            interpretation.recognized
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            500_000L,
            credit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            credit.observedAccount
                ?.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            credit.observedAccount
                ?.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            credit.confidence
        )
        assertTrue(
            credit.evidence.any {
                it.field ==
                    FinancialField
                        .ACCOUNT_REFERENCE
            }
        )
        assertEquals(
            1_375_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun amountFirstBankCreditWithoutCounterpartyIsIncoming() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 825.50 has been credited to your account XX9087 on 03/10/26. Available balance KES 4,925.50."
            )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            82_550L,
            credit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            "My Bank",
            credit.observedAccount
                ?.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            credit.observedAccount
                ?.ownershipHint
        )
    }

    @Test
    fun creditedFromCounterpartyStillUsesReceiptPath() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account has been credited with KES 4,500.00 from SAMPLE EMPLOYER on 03/10/26. Available balance KES 9,600.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(
            450_000L,
            receipt.amountMinor
        )
        assertEquals(
            "SAMPLE EMPLOYER",
            receipt.counterparty
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
