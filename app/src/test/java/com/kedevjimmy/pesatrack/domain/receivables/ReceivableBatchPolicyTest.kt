package com.kedevjimmy.pesatrack.domain.receivables
import org.junit.Assert.*
import org.junit.Test

class ReceivableBatchPolicyTest {
    private fun loan(id: Long, amount: Long, time: Long = id) = DebtMovement(id, "LENT", amount, time)
    private fun repayment(id: Long, amount: Long, time: Long = id) = DebtMovement(id, "REPAID", amount, time)
    @Test fun confirmedLoansAndRepaymentsYieldOutstanding() {
        val total = ReceivableBatchPolicy.validate(emptyList(), listOf(loan(1,500000),repayment(2,200000)))
        assertEquals(500000L,total.lentMinor); assertEquals(200000L,total.repaidMinor); assertEquals(300000L,total.outstandingMinor)
    }
    @Test fun sharedBillPrincipalDoesNotIncludeFee() {
        assertEquals(80000L, ReceivableBatchPolicy.validate(emptyList(),listOf(loan(1,80000))).outstandingMinor)
    }
    @Test fun rejectsOverpayment() = rejected { ReceivableBatchPolicy.validate(listOf(loan(1,80000)),listOf(repayment(2,80001))) }
    @Test fun rejectsRepaymentBeforeLoan() = rejected { ReceivableBatchPolicy.validate(emptyList(),listOf(loan(2,80000),repayment(1,50000))) }
    @Test fun rejectsRepeatedSource() = rejected { ReceivableBatchPolicy.validate(listOf(loan(1,80000)),listOf(loan(1,80000))) }
    @Test fun rejectsNonPositiveAmounts() = rejected { ReceivableBatchPolicy.validate(emptyList(),listOf(loan(1,0))) }
    @Test fun checksOverflow() = rejected { ReceivableBatchPolicy.validate(emptyList(),listOf(loan(1,Long.MAX_VALUE),loan(2,1))) }
    @Test fun sameTimeLoansPrecedeReceiptsDeterministically() {
        assertEquals(30000L,ReceivableBatchPolicy.validate(emptyList(),listOf(repayment(2,50000,100),loan(1,80000,100))).outstandingMinor)
    }
    private fun rejected(block: () -> Unit) { try { block(); fail("Must reject unsafe debt") } catch (expected: IllegalArgumentException) {} catch (expected: ArithmeticException) {} }
}
