package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealDeviceFinancialMessageRegressionTest {
    @Test
    fun destinationCreditBankWordingBecomesOutgoingPayment() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY",
                body =
                    "Your transaction of Kenya Shillings 20,000 " +
                        "has been credited to SAMPLE PERSON. " +
                        "Reference number EQA12ABC34. " +
                        "M-Pesa reference UFT12ABC34."
            )

        val event =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(2_000_000L, event.amountMinor)
        assertEquals("Equity Bank Kenya", event.source.displayName)
        assertEquals("SAMPLE PERSON", event.counterparty)
        assertEquals(
            FinancialAccountNature.ASSET,
            event.source.nature
        )
    }

    @Test
    fun explicitLoanBalanceWithoutCurrencyMarkerStillCreatesLiabilityBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "M-Shwari",
                body =
                    "Your total loan balance is 1,075.00."
            )

        assertTrue(interpretation.recognized)

        val balance = interpretation.balances.single()
        assertEquals(107_500L, balance.balanceMinor)
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
    fun mpesaThreadMshwariApprovalBecomesMshwariDebtDrawdown() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "M-PESA",
                body =
                    "UINFT7V1JD Confirmed. Your M-Shwari loan has been approved on 23/9/26 5:27 PM and Ksh985.00 less excise duty has been deposited to your M-PESA account. New M-PESA balance is Ksh985.00. Did you know you can top up your M-Shwari loan once within the first 20 days?"
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertEquals(98_500L, event.amountMinor)
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.proceedsAccount?.providerKey
        )
        assertTrue(event.amountIsNetProceeds)
        assertEquals(null, event.outstandingAfterMinor)
        assertEquals(
            "23/9/26 5:27 PM",
            interpretation.occurredAtText
        )
    }

    @Test
    fun mshwariOutstandingLoanBalanceUsingOfWordingIsObserved() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "M-Shwari",
                body =
                    "Dear customer. Kindly note you have an outstanding loan balance of KSH 2,150.00 due 26/2/2013."
            )

        val balance =
            interpretation.balances.single()

        assertEquals(215_000L, balance.balanceMinor)
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
    fun mpesaThreadExplicitMshwariLoanBalanceUsesMshwariLiability() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "M-PESA",
                body =
                    "Your M-Shwari loan balance is Ksh 1,075.00."
            )

        val balance =
            interpretation.balances.single()

        assertEquals(107_500L, balance.balanceMinor)
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            balance.account.providerKey
        )
    }

    @Test
    fun senderScopedLoanDisbursementDoesNotInventPrincipalFromCashProceeds() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Future Finance",
                body =
                    "Your loan of KES 464 has been disbursed " +
                        "to your Mobile Wallet account."
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertEquals(46_400L, event.amountMinor)
        assertTrue(event.amountIsNetProceeds)
        assertEquals(null, event.outstandingAfterMinor)
    }
}
