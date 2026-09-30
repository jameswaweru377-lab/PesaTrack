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
