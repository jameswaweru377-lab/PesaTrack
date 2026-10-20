package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePackDebtBridgeTest {
    @Test
    fun branchDrawdownBecomesCanonicalDebtEvent() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "Branch",
                    body =
                        "Branch: KES 2,000.00 has been transferred to your M-Pesa account. Repay KES 2,300.00 by 31 Oct 2026 to grow your limit."
                )

        val event =
            interpretation.events
                .single() as
                DebtDrawdownEventFact

        assertEquals(
            200_000L,
            event.amountMinor
        )
        assertEquals(
            230_000L,
            event.outstandingAfterMinor
        )
        assertEquals(
            "BRANCH_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.proceedsAccount
                ?.providerKey
        )
        assertEquals(
            0L,
            event.feeMinor
        )
    }

    @Test
    fun timizaTemplateAddsFeeTotalDueAndTimizaProceeds() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "TIMIZA",
                    body =
                        "Timiza: KES 4,000.00 disbursed to your Timiza account. KES 200.00 fee applied. Total due KES 4,200.00 on 31 Oct 2026. Paybill 300067."
                )

        val event =
            interpretation.events
                .single() as
                DebtDrawdownEventFact

        assertEquals(
            400_000L,
            event.amountMinor
        )
        assertEquals(
            20_000L,
            event.feeMinor
        )
        assertEquals(
            420_000L,
            event.outstandingAfterMinor
        )
        assertEquals(
            "ABSA_TIMIZA_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            "ABSA_TIMIZA_ACCOUNT",
            event.proceedsAccount
                ?.providerKey
        )
    }

    @Test
    fun mshwariTemplateCanFillFeeAndObservedOutstanding() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "M-Shwari",
                    body =
                        "M-Shwari: You have received a loan of KES 5,000.00. KES 150.00 facility fee charged. Amount sent to M-Pesa. Repay KES 5,150.00 by 31 Oct 2026."
                )

        val event =
            interpretation.events
                .single() as
                DebtDrawdownEventFact

        assertEquals(
            500_000L,
            event.amountMinor
        )
        assertEquals(
            15_000L,
            event.feeMinor
        )
        assertEquals(
            515_000L,
            event.outstandingAfterMinor
        )
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.proceedsAccount
                ?.providerKey
        )
    }

    @Test
    fun realMshwariRepaymentShapeUsesObservedBalanceAndMessageDate() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "MPESA",
                    body =
                        "TSTV14M001 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00"
                )

        val event =
            interpretation.events
                .single() as
                DebtRepaymentEventFact

        assertEquals(
            57_500L,
            event.amountMinor
        )
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.fundingAccount
                ?.providerKey
        )
        assertEquals(
            DebtCompletion.FULL,
            event.completion
        )
        assertEquals(
            0L,
            event.outstandingAfterMinor
        )
        assertEquals(
            "TSTV14M001",
            interpretation.transactionCode
        )
        assertEquals(
            "2/10/26 at 7:33 AM",
            interpretation.occurredAtText
        )
    }

    @Test
    fun mpesaFullMshwariRepaymentWithSavingsOverpaymentClosesDebtWithoutInventedCredit() {
        val body = "QAZ1234567 Confirmed. Loan of Ksh123.45 repaid from your M-PESA " +
            "account on 16/10/26 at 7:30 AM. M-PESA balance is Ksh9,876.54." +
            "Your loan is repaid in full, and the overpayment has been moved to your " +
            "Savings Account. New M-Shwari balance is Ksh42.37. " +
            "Transaction cost Kshs 0.00"
        val interpretation = FinancialMessageInterpreter.interpret("MPESA", body)
        val event = interpretation.events.single() as DebtRepaymentEventFact
        assertEquals(12_345L, event.amountMinor)
        assertEquals("NCBA_MSHWARI_LOAN", event.liability.providerKey)
        assertEquals("SAFARICOM_MPESA", event.fundingAccount?.providerKey)
        assertEquals(DebtCompletion.FULL, event.completion)
        assertEquals(0L, event.outstandingAfterMinor)
        assertNull(event.availableCreditAfterMinor)
        assertEquals("QAZ1234567", interpretation.transactionCode)
        assertEquals("16/10/26 at 7:30 AM", interpretation.occurredAtText)
        assertTrue(interpretation.parseProvenance.templateId ==
            "ke.mshwari.loan.full-repayment-overpayment")
        assertTrue(interpretation.balances.any {
            it.account.providerKey == "NCBA_MSHWARI_SAVINGS" &&
                it.balanceMinor == 4_237L
        })
        assertTrue(interpretation.balances.any {
            it.account.providerKey == "SAFARICOM_MPESA" &&
                it.balanceMinor == 987_654L
        })
        // The KES 25.02 is a post-event savings balance, NOT a
        // proven overpayment amount or a second credit/transfer.
        val draft = interpretation.toCanonicalDraft()
        assertEquals(1, draft.records.count {
            it is DebtMovementDraft
        })
        assertEquals(0, draft.records.count {
            it is MoneyMovementDraft
        })
    }

    @Test
    fun similarTextWithoutExplicitFullLoanSettlementCannotCloseLiability() {
        val body = "QAZ1234568 Confirmed. Loan of Ksh123.45 repaid from your M-PESA " +
            "account on 16/10/26 at 7:30 AM. M-PESA balance is Ksh9,876.54. " +
            "Your loan has been partially repaid, and overpayment has been moved " +
            "to your Savings Account. New M-Shwari balance is Ksh42.37."
        val interpretation = FinancialMessageInterpreter.interpret("MPESA", body)
        assertTrue(interpretation.events.none {
            it is DebtRepaymentEventFact &&
                it.completion == DebtCompletion.FULL
        })
    }

    @Test
    fun okashDrawdownKeepsDestinationUnknownWhenMessageDoesNotSay() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "OKash",
                    body =
                        "OKash: Your loan of KES 2,500.00 is successfully disbursed. Please pay KES 2,900.00 on or before 31 Oct 2026 to avoid late fees."
                )

        val event =
            interpretation.events
                .single() as
                DebtDrawdownEventFact

        assertEquals(
            "OKASH_LOAN",
            event.liability.providerKey
        )
        assertEquals(
            250_000L,
            event.amountMinor
        )
        assertEquals(
            290_000L,
            event.outstandingAfterMinor
        )
        assertNull(
            event.proceedsAccount
        )
    }

    @Test
    fun dueNoticeStillDoesNotBecomeDebtMovement() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "TALA",
                    body =
                        "Tala: Your loan payment of KES 3,300.00 is due on 31 Oct 2026. Pay via Paybill 851201, Acc: 0712345678."
                )

        assertTrue(
            interpretation.events.none {
                it is DebtDrawdownEventFact ||
                    it is DebtRepaymentEventFact
            }
        )
    }

    @Test
    fun hustlerSplitDrawdownRemainsProvenanceOnlyUntilSplitModelExists() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "HUSTLERFUND",
                    body =
                        "Hustler Fund: Your loan of Ksh 5,000.00 has been approved. Ksh 4,750.00 sent to M-Pesa, Ksh 250.00 to your savings account. Repay Ksh 5,000.00 by 31 Oct 2026."
                )

        assertEquals(
            "DEBT_DRAWDOWN_WITH_SAVINGS",
            interpretation
                .parseProvenance
                .eventKind
        )
        assertTrue(
            interpretation.events.none {
                it is DebtDrawdownEventFact
            }
        )
    }
}
