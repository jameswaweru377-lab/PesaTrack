package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P0/A1: pin concrete minor-unit financial meanings.
 * These are synthetic, existing message fixtures, never user SMS.
 *
 * The assertions are intentionally on interpreter drafts, not on aggregated
 * balance arithmetic: the observed balance is a snapshot, not a cash inflow.
 */
class FinancialAmountContractTest {
    private fun records(id: String): List<CanonicalFinancialRecord> {
        val fixture = FinancialMessageFixtureCorpus.all.single { it.id == id }
        return FinancialMessageInterpreter.interpret(
            title = fixture.title,
            body = fixture.body
        ).toCanonicalDraft().records
    }

    private fun movement(id: String, kind: CanonicalFinancialRecordKind): MoneyMovementDraft =
        records(id).filterIsInstance<MoneyMovementDraft>().single { it.kind == kind }

    @Test
    fun regularPaymentAndFeeRemainSeparate() {
        assertEquals(
            45_000L,
            movement("ordinary_mpesa_payment", CanonicalFinancialRecordKind.PAYMENT).amountMinor
        )
        val fee = records("ordinary_mpesa_payment").filterIsInstance<FeeDraft>().single()
        assertEquals(700L, fee.amountMinor)
        assertEquals(
            1,
            records("ordinary_mpesa_payment").count {
                it.kind == CanonicalFinancialRecordKind.PAYMENT
            }
        )
    }

    @Test
    fun incomingReceiptAndInternalSavingsTransferHaveDifferentMeanings() {
        assertEquals(
            200_000L,
            movement("ordinary_mpesa_receipt", CanonicalFinancialRecordKind.RECEIPT).amountMinor
        )
        for ((id, amount) in listOf(
            "mshwari_to_mpesa" to 700_000L,
            "mpesa_to_mshwari" to 250_000L
        )) {
            val events = records(id)
            assertEquals(
                amount,
                events.filterIsInstance<MoneyMovementDraft>()
                    .single { it.kind == CanonicalFinancialRecordKind.TRANSFER }
                    .amountMinor
            )
            assertFalse("$id must not create earnings", events.any {
                it.kind == CanonicalFinancialRecordKind.RECEIPT
            })
            assertFalse("$id must not create consumption", events.any {
                it.kind == CanonicalFinancialRecordKind.PAYMENT
            })
            assertEquals(
                "$id has two independent observed account balances",
                2,
                events.filterIsInstance<BalanceObservationDraft>().size
            )
        }
    }

    @Test
    fun loanDrawdownAndRepaymentAreNotSalaryOrShopping() {
        val drawdown = records("fuliza_drawdown")
        assertEquals(
            16_700L,
            drawdown.filterIsInstance<DebtMovementDraft>()
                .single { it.kind == CanonicalFinancialRecordKind.DEBT_DRAWDOWN }
                .amountMinor
        )
        assertEquals(167L, drawdown.filterIsInstance<FeeDraft>().single().amountMinor)
        val repayment = records("fuliza_partial_repayment")
        assertEquals(
            22_000L,
            repayment.filterIsInstance<DebtMovementDraft>()
                .single { it.kind == CanonicalFinancialRecordKind.DEBT_REPAYMENT }
                .amountMinor
        )
        (drawdown + repayment).forEach {
            assertFalse("Debt events are not cash receipts", it.kind == CanonicalFinancialRecordKind.RECEIPT)
            assertFalse("Debt events are not ordinary purchases", it.kind == CanonicalFinancialRecordKind.PAYMENT)
        }
    }

    @Test
    fun reversalReferencesOriginalTransactionWithoutCreatingNewIncome() {
        val fixture = FinancialMessageFixtureCorpus.all.single { it.id == "mpesa_reversal" }
        val draft = FinancialMessageInterpreter.interpret(fixture.title, fixture.body)
            .toCanonicalDraft()
        val reversal = draft.records.filterIsInstance<ReversalDraft>().single()
        assertEquals(45_000L, reversal.amountMinor)
        assertEquals("TST12ABC04", reversal.referencedTransactionCode)
        assertFalse(draft.records.any { it.kind == CanonicalFinancialRecordKind.RECEIPT })
    }

    @Test
    fun unknownPaymentDirectionIsNotInventedAndBalancesAreSnapshots() {
        val events = records("unknown_wording_single_amount")
        val unresolved = events.filterIsInstance<UnresolvedMovementDraft>().single()
        assertEquals(64_000L, unresolved.amountMinor)
        assertTrue(events.any { it is BalanceObservationDraft })
        assertFalse(events.any {
            it.kind == CanonicalFinancialRecordKind.PAYMENT ||
                it.kind == CanonicalFinancialRecordKind.RECEIPT
        })
    }
}
