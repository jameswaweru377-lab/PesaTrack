package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericDebtInterpretationTest {
    @Test
    fun understandsUnknownDebtProviderDrawdownWithoutCompanyParser() {
        val message =
            "TST12DEBT1 Confirmed. Nova Credit amount is Ksh 500.00. Access Fee charged Ksh 10.00. Total Nova Credit outstanding amount is Ksh 510.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Finance",
                body = message
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertEquals(50_000L, event.amountMinor)
        assertEquals(1_000L, event.feeMinor)
        assertEquals(
            51_000L,
            event.outstandingAfterMinor
        )
        assertEquals(
            "Nova Credit",
            event.liability.displayName
        )
        assertNull(event.liability.providerKey)
        assertEquals(
            FinancialAccountNature.LIABILITY,
            event.liability.nature
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            event.liability.ownershipHint
        )
        assertTrue(
            interpretation
                .toCanonicalDraft()
                .requiresLegacyFallback
        )
    }

    @Test
    fun understandsUnknownDebtProviderFullRepaymentAndAvailableCredit() {
        val message =
            "TST12DEBT2 Confirmed. Ksh 200.00 from your M-PESA has been used to fully pay your outstanding Nova Credit. Available Nova Credit limit is Ksh 1,000.00. Your M-PESA balance is Ksh 300.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        val event =
            interpretation.events.single() as
                DebtRepaymentEventFact

        assertEquals(20_000L, event.amountMinor)
        assertEquals(
            DebtCompletion.FULL,
            event.completion
        )
        assertEquals(0L, event.outstandingAfterMinor)
        assertEquals(
            100_000L,
            event.availableCreditAfterMinor
        )
        assertEquals(
            30_000L,
            event.walletBalanceAfterMinor
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.fundingAccount?.providerKey
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            event.liability.nature
        )

        val canonical =
            interpretation.toCanonicalDraft()

        assertEquals(
            listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION
            ),
            canonical.records.map { it.kind }
        )
    }

    @Test
    fun debtGrammarDoesNotTurnOrdinaryAccountTransferIntoDebt() {
        val message =
            "TST12DEBT3 Confirmed. Ksh 5,000.00 transferred from Nova Credit account on 28/9/26 at 7:00 PM. Nova Credit balance is Ksh 12,400.00. M-PESA balance is Ksh 8,100.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        assertTrue(
            interpretation.events.single() is
                TransferEventFact
        )
    }
}
