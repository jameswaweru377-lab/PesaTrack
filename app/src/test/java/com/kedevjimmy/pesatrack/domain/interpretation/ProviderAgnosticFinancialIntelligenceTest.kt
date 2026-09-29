package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAgnosticFinancialIntelligenceTest {
    @Test
    fun unknownBankDebitUsesMessageSourceWithoutProviderParser() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY",
                body =
                    "Your account has been debited with KES 2,500.00. Transfer to SAMPLE PERSON successful. Available balance KES 12,100.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(250_000L, payment.amountMinor)
        assertEquals("SAMPLE PERSON", payment.counterparty)
        assertEquals(
            "Equity Bank",
            payment.source.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            payment.source.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            payment.source.nature
        )
        assertNull(payment.source.providerKey)
    }

    @Test
    fun unknownBankCanTransferToKnownWalletWithoutBankSpecificCode() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "CO-OP BANK",
                body =
                    "KES 5,000.00 transferred to M-PESA account on 29/9/26 at 10:30 AM. Available balance KES 20,000.00."
            )

        val transfer =
            interpretation.events.single() as
                TransferEventFact

        assertEquals(500_000L, transfer.amountMinor)
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            transfer.source.ownershipHint
        )
        assertEquals(
            "SAFARICOM_MPESA",
            transfer.destination.providerKey
        )
        assertNull(transfer.source.providerKey)
    }


    @Test
    fun unknownBankNamedTransferToPersonBecomesPayment() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "FAMILY BANK",
                body =
                    "KES 3,200.00 transferred to SAMPLE PERSON on 29/9/26 at 11:15 AM. Available balance KES 9,000.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(320_000L, payment.amountMinor)
        assertEquals("SAMPLE PERSON", payment.counterparty)
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            payment.source.ownershipHint
        )
        assertNull(payment.source.providerKey)
    }

    @Test
    fun unknownLenderRepaymentUsesGenericLoanLanguage() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "TALA",
                body =
                    "Loan repayment of KES 1,000.00 received. Tala loan balance is KES 4,000.00."
            )

        val repayment =
            interpretation.events.single() as
                DebtRepaymentEventFact

        assertEquals(100_000L, repayment.amountMinor)
        assertEquals(
            "Tala loan",
            repayment.liability.displayName
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            repayment.liability.nature
        )
        assertEquals(
            400_000L,
            repayment.outstandingAfterMinor
        )
        assertNull(repayment.fundingAccount)
        assertNull(repayment.liability.providerKey)
    }

    @Test
    fun existingMshwariLoanBalanceIsRecognizedAsLiability() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "M-Shwari Loan balance is Ksh 1,381.00."
            )

        assertTrue(interpretation.recognized)
        assertTrue(interpretation.events.isEmpty())

        val balance = interpretation.balances.single()
        assertEquals(138_100L, balance.balanceMinor)
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            balance.account.providerKey
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            balance.account.nature
        )
    }

    @Test
    fun explicitUnknownLoanLabelBecomesLiabilityButCreditNameDoesNot() {
        val loan =
            FinancialProviderRegistry.resolveAccount(
                "Future Finance Loan"
            )
        val ordinaryCreditName =
            FinancialProviderRegistry.resolveAccount(
                "Nova Credit account"
            )

        assertEquals(
            FinancialAccountNature.LIABILITY,
            loan.nature
        )
        assertEquals(
            FinancialAccountNature.UNKNOWN,
            ordinaryCreditName.nature
        )
    }
}
