package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementFinancialPolicyTest {
    private val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        isLenient = false
    }
    private val kinds = mapOf(
        "GROCERIES" to "EXPENSE", "EATING_OUT" to "EXPENSE",
        "FEES" to "EXPENSE", "INCOME" to "INCOME", "TRANSFER" to "TRANSFER",
        "DEBT_LOAN" to "ALLOCATION", "SAVINGS" to "ALLOCATION",
        "INVESTMENT" to "ALLOCATION"
    )
    private fun line(desc: String, inbound: Long = 0L, outbound: Long = 0L,
                     at: String = "2026-09-17 10:00:00"): StatementActivityLine =
        StatementActivityLine(
            page = 1, row = 1, reference = "QAB1234569",
            occurredAt = requireNotNull(date.parse(at)).time,
            description = desc, paidInMinor = inbound, withdrawnMinor = outbound,
            balanceMinor = 120_000L
        )
    private fun classify(source: StatementActivityLine,
                         assigned: String? = null, learned: String? = null) =
        StatementFinancialPolicy.classify("fake-source", source, assigned, learned, kinds)

    @Test fun merchantIsExpenseAndFoodGroupRemainsPrecise() {
        val supermarket = classify(line("Naivas Supermarket", outbound = 245_000L))
        assertEquals(StatementFlow.EXPENSE, supermarket.flow)
        assertEquals("GROCERIES", supermarket.categoryId)
        assertEquals(245_000L, StatementFinancialPolicy.totals(listOf(supermarket)).expenseMinor)
    }

    @Test fun cashWithdrawalAndArbitraryOutgoingCannotBecomeSpending() {
        val money = classify(line("Cash withdrawal agent 1102", outbound = 250_000L))
        assertEquals(StatementFlow.UNKNOWN, money.flow)
        val totals = StatementFinancialPolicy.totals(listOf(money))
        assertEquals(0L, totals.expenseMinor)
        assertEquals(250_000L, totals.unknownOutMinor)
    }

    @Test fun exactRememberedRecipientCanBeClassifiedWithoutOverridingOwnTransfer() {
        val out = classify(line("Customer Transfer to Elizabeth and Mira", outbound=120_000L),
            learned="EATING_OUT")
        assertEquals(StatementFlow.EXPENSE, out.flow)
        assertEquals("EATING_OUT", out.categoryId)
        val structural = classify(line("Transfer to own Equity account", outbound=120_000L),
            learned="EATING_OUT")
        assertEquals(StatementFlow.TRANSFER, structural.flow)
        assertEquals("TRANSFER", structural.categoryId)
    }

    @Test fun explicitDebtsSeparateBorrowAndRepaymentFromIncomeAndSpending() {
        val borrow = classify(line("M-Shwari loan disbursement", inbound=500_000L))
        val repayment = classify(line("M-Shwari loan repayment", outbound=380_000L))
        assertEquals(StatementFlow.DEBT, borrow.flow)
        assertEquals(StatementDebtAction.BORROWED, borrow.debtAction)
        assertEquals(StatementFlow.DEBT, repayment.flow)
        assertEquals(StatementDebtAction.REPAID, repayment.debtAction)
        val totals = StatementFinancialPolicy.totals(listOf(borrow, repayment))
        assertEquals(0L, totals.incomeMinor)
        assertEquals(0L, totals.expenseMinor)
        assertEquals(880_000L, totals.debtMinor)
    }

    @Test fun ambiguousFulizaPaybillCannotFabricateLoanRepayment() {
        val result = classify(line("Fuliza Paybill payment", outbound = 10_000L),
            learned="EATING_OUT")
        assertEquals(StatementFlow.UNKNOWN, result.flow)
        assertNull(result.debtAction)
        assertEquals("FULIZA", result.debtProvider)
    }

    @Test fun savingsAndFeesNeverBecomeGenericExpenses() {
        val save = classify(line("M-Shwari Lock Savings Deposit", outbound=300_000L))
        val fee = classify(line("Transaction cost", outbound=2_500L))
        val totals = StatementFinancialPolicy.totals(listOf(save,fee))
        assertEquals(StatementFlow.SAVINGS, save.flow)
        assertEquals(StatementFlow.FEE, fee.flow)
        assertEquals(300_000L, totals.savingMinor)
        assertEquals(2_500L, totals.feesMinor)
        assertEquals(0L, totals.expenseMinor)
    }

    @Test fun salaryIsIncomeButUnknownReceiptIsNot() {
        val salary = classify(line("Salary Payment ABC Limited", inbound=5_000_000L))
        val gift = classify(line("Funds received from unknown source", inbound=100_000L))
        val totals = StatementFinancialPolicy.totals(listOf(salary,gift))
        assertEquals(5_000_000L, totals.incomeMinor)
        assertEquals(100_000L, totals.unknownInMinor)
    }

    @Test fun zeroMovementRecordsRemainInformational() {
        assertEquals(StatementFlow.INFORMATIONAL,
            classify(line("M-Shwari Loan Request")).flow)
    }

    @Test fun explicitRefundIsSeparateNotSalaryIncome() {
        val refund = classify(line("Reversed Buy Goods Payment", inbound=25_000L))
        assertEquals(StatementFlow.REFUND, refund.flow)
        assertEquals(25_000L, StatementFinancialPolicy.totals(listOf(refund)).refundsMinor)
    }

    @Test fun weekUsesNairobiMondayBoundaryAcrossMonth() {
        val anchor = requireNotNull(date.parse("2026-09-01 10:00:00")).time // Tuesday
        val aug31 = requireNotNull(date.parse("2026-08-31 00:00:00")).time
        val sep06 = requireNotNull(date.parse("2026-09-06 23:59:59")).time
        val sep07 = requireNotNull(date.parse("2026-09-07 00:00:00")).time
        assertTrue(StatementFinancialPolicy.within(aug31,anchor,StatementFinancialPolicy.Period.WEEK))
        assertTrue(StatementFinancialPolicy.within(sep06,anchor,StatementFinancialPolicy.Period.WEEK))
        assertFalse(StatementFinancialPolicy.within(sep07,anchor,StatementFinancialPolicy.Period.WEEK))
    }

    @Test fun fullExactMerchantDoesNotMatchArbitrarySubstring() {
        assertEquals("ELIZABETH AND MIRA",
            StatementFinancialPolicy.merchantKey("Customer Transfer to Elizabeth and Mira"))
        assertNull(StatementFinancialPolicy.merchantKey("Customer Transfer to 0722000000"))
        assertNull(StatementFinancialPolicy.merchantKey("Fuliza Repayment"))
    }
}
