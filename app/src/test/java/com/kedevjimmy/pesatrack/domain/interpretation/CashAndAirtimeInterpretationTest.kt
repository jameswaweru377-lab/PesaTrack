package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashAndAirtimeInterpretationTest {
    @Test
    fun understandsCashWithdrawalAsAccountToCashTransfer() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12CASH1 Confirmed. Ksh 1,000.00 withdrawn from M-PESA at SAMPLE AGENT on 28/9/26 at 8:20 AM. New M-PESA balance is Ksh 10,950.00."
            )

        val event =
            interpretation.events.single() as
                CashWithdrawalEventFact

        assertEquals(100_000L, event.amountMinor)
        assertEquals(
            "SAFARICOM_MPESA",
            event.source.providerKey
        )
        assertEquals("Cash", event.cashDestination.displayName)
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            event.cashDestination.ownershipHint
        )
        assertEquals("SAMPLE AGENT", event.counterparty)

        val canonical =
            interpretation.toCanonicalDraft()

        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_WITHDRAWAL
            ),
            canonical.records.map { it.kind }
        )
        assertTrue(
            interpretation.consumesLegacyTransactionParsing
        )
    }

    @Test
    fun understandsCashDepositAsCashToAccountTransfer() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12CASH2 Confirmed. Ksh 2,000.00 deposited into your M-PESA on 28/9/26 at 8:30 AM. New M-PESA balance is Ksh 12,950.00."
            )

        val event =
            interpretation.events.single() as
                CashDepositEventFact

        assertEquals(200_000L, event.amountMinor)
        assertEquals("Cash", event.cashSource.displayName)
        assertEquals(
            "SAFARICOM_MPESA",
            event.destination.providerKey
        )
        assertNull(event.counterparty)

        val canonical =
            interpretation.toCanonicalDraft()

        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_DEPOSIT
            ),
            canonical.records.map { it.kind }
        )
    }

    @Test
    fun understandsAirtimeAsServicePaymentNotCompanyRule() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12CASH3 Confirmed. Ksh 100.00 airtime purchased on 28/9/26 at 8:40 AM. New M-PESA balance is Ksh 12,850.00."
            )

        val event =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(10_000L, event.amountMinor)
        assertEquals(
            PaymentRouteHint.AIRTIME,
            event.routeHint
        )
        assertEquals("Airtime", event.counterparty)
        assertEquals(
            "SAFARICOM_MPESA",
            event.source.providerKey
        )
    }

    @Test
    fun unknownWalletWithdrawalStillHasGenericCashMeaning() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Finance",
                body =
                    "TST12CASH4 Confirmed. Ksh 300.00 withdrawn from PesaNova Wallet at SAMPLE AGENT on 28/9/26 at 8:50 AM. PesaNova Wallet balance is Ksh 2,000.00."
            )

        val event =
            interpretation.events.single() as
                CashWithdrawalEventFact

        assertEquals("PesaNova", event.source.displayName)
        assertNull(event.source.providerKey)
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            event.source.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            event.confidence
        )
        assertEquals("Cash", event.cashDestination.displayName)
    }
}
