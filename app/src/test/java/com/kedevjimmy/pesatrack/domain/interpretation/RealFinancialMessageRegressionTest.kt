package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealFinancialMessageRegressionTest {
    @Test
    fun lockSavingsTransferDoesNotRequireAccountKeyword() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12SAVE1 Confirmed. Ksh10,000.00 transferred to M-Shwari Lock Savings on 2/8/26 at 10:41 AM. M-PESA balance is Ksh1,880.98. Your Lock Savings balance is Ksh16,000.00. Transaction cost Ksh0.00"
            )

        val event =
            interpretation.events.single() as
                TransferEventFact

        assertEquals(1_000_000L, event.amountMinor)
        assertEquals(
            "SAFARICOM_MPESA",
            event.source.providerKey
        )
        assertEquals(
            "NCBA_MSHWARI_LOCK_SAVINGS",
            event.destination.providerKey
        )
        assertEquals(
            AccountType.SAVINGS,
            event.destination.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            event.destination.ownershipHint
        )

        val balances =
            interpretation.balances.associateBy {
                it.account.entityKey
            }

        assertEquals(
            188_098L,
            balances["provider:SAFARICOM_MPESA"]
                ?.balanceMinor
        )
        assertEquals(
            1_600_000L,
            balances[
                "provider:NCBA_MSHWARI_LOCK_SAVINGS"
            ]?.balanceMinor
        )
    }

    @Test
    fun unknownLockSavingsUsesSameGenericTransferAndContextLogic() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12SAVE2 Confirmed. Ksh2,000.00 transferred to Nova Lock Savings on 3/8/26 at 9:15 AM. M-PESA balance is Ksh800.00. Your Lock Savings balance is Ksh7,500.00."
            )

        val event =
            interpretation.events.single() as
                TransferEventFact

        assertNull(event.destination.providerKey)
        assertEquals(
            "Nova Lock Savings",
            event.destination.displayName
        )
        assertEquals(
            AccountType.SAVINGS,
            event.destination.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            event.destination.ownershipHint
        )

        val savingsBalance =
            interpretation.balances.single {
                it.account.entityKey ==
                    event.destination.entityKey
            }
        assertEquals(
            750_000L,
            savingsBalance.balanceMinor
        )
    }

    @Test
    fun reversalCanTakeCreditedAmountAfterReversalClause() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12REV01 Confirmed. Reversal of transaction TST12PAY01 has been successfully reversed on 25/8/26 at 4:19 PM and Ksh276.00 is credited to your M-PESA account. New M-PESA account balance is Ksh939.09."
            )

        val event =
            interpretation.events.single() as
                ReversalEventFact

        assertEquals(27_600L, event.amountMinor)
        assertEquals(
            "TST12PAY01",
            event.referencedTransactionCode
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.observedAccount?.providerKey
        )
        assertEquals(
            93_909L,
            interpretation.balances.single()
                .balanceMinor
        )
    }

    @Test
    fun approvedLoanDisbursementUnderstandsNetProceedsDestination() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12LOAN1 Confirmed. Your M-Shwari loan has been approved on 23/9/26 5:27 PM and Ksh985.00 less excise duty has been deposited to your M-PESA account. New M-PESA balance is Ksh985.00."
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertEquals(98_500L, event.amountMinor)
        assertTrue(event.amountIsNetProceeds)
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            event.liability.nature
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.proceedsAccount?.providerKey
        )
        assertNull(event.outstandingAfterMinor)
        assertEquals(
            98_500L,
            interpretation.balances.single()
                .balanceMinor
        )

        val canonical =
            interpretation.toCanonicalDraft()
                .records
                .filterIsInstance<DebtMovementDraft>()
                .single()

        assertTrue(canonical.amountIsNetProceeds)
        assertEquals(
            "SAFARICOM_MPESA",
            canonical.proceedsAccount?.providerKey
        )
    }

    @Test
    fun unknownLenderUsesSameLoanDisbursementGrammar() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Finance",
                body =
                    "TST12LOAN2 Confirmed. Your Nova Flex Loan has been approved on 28/9/26 at 9:00 AM and Ksh5,000.00 has been deposited to your PesaNova Wallet account. PesaNova Wallet balance is Ksh5,000.00."
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertNull(event.liability.providerKey)
        assertEquals(
            "Nova Flex Loan",
            event.liability.displayName
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            event.liability.nature
        )
        assertNull(event.proceedsAccount?.providerKey)
        assertEquals(
            "PesaNova",
            event.proceedsAccount?.displayName
        )
        assertTrue(event.amountIsNetProceeds)
    }

    @Test
    fun fulizaRepaymentStillUnderstandsFundingAndResultingWalletBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12DEBT9 Confirmed. Ksh 985.00 from your M-PESA has been used to partially pay your outstanding Fuliza M-PESA. Your available Fuliza M-PESA limit is Ksh 1062.29. Your M-PESA balance is 0.00."
            )

        val event =
            interpretation.events.single() as
                DebtRepaymentEventFact

        assertEquals(98_500L, event.amountMinor)
        assertEquals(
            DebtCompletion.PARTIAL,
            event.completion
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.fundingAccount?.providerKey
        )
        assertEquals(
            "SAFARICOM_FULIZA",
            event.liability.providerKey
        )
        assertEquals(0L, event.walletBalanceAfterMinor)
    }
}
