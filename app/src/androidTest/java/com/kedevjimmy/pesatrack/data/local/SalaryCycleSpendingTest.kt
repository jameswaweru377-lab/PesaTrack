package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.text.SimpleDateFormat

@RunWith(AndroidJUnit4::class)
class SalaryCycleSpendingTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun clean() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    private fun at(date: String) = SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date)!!.time
    private fun rule() = RuleStore.get(context).save(name = "Salary", counterpartyContains = "",
        counterpartyExact = "XYZ SCHOOL", displayLabel = "XYZ SCHOOL", category = TransactionCategory.INCOME,
        direction = MoneyDirection.INCOME, preset = RulePreset.SALARY, startsPayCycle = true,
        accountId = AccountStore.DEFAULT_MPESA_ID, payWindowStartDay = 24, payWindowEndDay = 28)
    private fun income(code: String, date: String, salary: SmartRule, account: String = AccountStore.DEFAULT_MPESA_ID) =
        TransactionStore.get(context).insertIfNew(ParsedMpesaTransaction(code, MpesaTransactionType.RECEIVED,
            6000000, "XYZ SCHOOL", feeMinor = 0, balanceMinor = null, occurredAtText = null, rawMessage = code),
            null, "TEST_SMS", at(date), at(date), salary, accountIdOverride = account)
    private fun spending(code: String, date: String, amount: Long, account: String) =
        TransactionStore.get(context).insertIfNew(ParsedMpesaTransaction(code, MpesaTransactionType.OTHER,
            amount, "Family", feeMinor = 100, balanceMinor = null, occurredAtText = null, rawMessage = code),
            null, "TEST", at(date), at(date), accountIdOverride = account,
            directionOverride = MoneyDirection.EXPENSE, categoryOverride = TransactionCategory.FAMILY,
            reviewStateOverride = ReviewState.USER_CLASSIFIED)
    @Test fun manualSalaryAndLateSmsBecomeOneReceiptAndReplayStaysOne() {
        val salary = rule(); val store = TransactionStore.get(context)
        assertTrue(store.insertManualIncome(6000000, at("2026-09-28 00:00"), "XYZ SCHOOL", salary))
        val id = PayCycleStore.get(context).currentCycle()!!.startTransactionId
        assertFalse(income("SALARY28", "2026-09-28 10:00", salary))
        assertFalse(income("SALARY28", "2026-09-28 10:00", salary))
        assertEquals(1, store.snapshot().transactionCount)
        assertEquals("SALARY28", store.getById(id)!!.transactionCode)
        assertEquals(id, PayCycleStore.get(context).currentCycle()!!.startTransactionId)
        assertFalse(store.insertManualIncome(6000000, at("2026-09-28 00:00"), "XYZ SCHOOL", salary))
    }
    @Test fun salaryOnAnotherDateOrAccountIsNotSilentlyCombined() {
        val salary = rule(); val store = TransactionStore.get(context)
        store.insertManualIncome(6000000, at("2026-09-28 00:00"), "XYZ SCHOOL", salary)
        assertTrue(income("OTHERDAY", "2026-09-29 10:00", salary))
        assertTrue(income("OTHERACCOUNT", "2026-09-28 10:00", salary, AccountStore.DEFAULT_CASH_ID))
        assertEquals(3, store.snapshot().transactionCount)
    }
    @Test fun backfillingThreeMonthsKeepsCurrentSalaryAndAcceptsExplicitEarlyPayday() {
        val salary = rule(); val store = TransactionStore.get(context)
        store.insertManualIncome(6000000, at("2026-09-20 00:00"), "XYZ SCHOOL", salary)
        val currentId = PayCycleStore.get(context).currentCycle()!!.startTransactionId
        store.insertManualIncome(5900000, at("2026-07-28 00:00"), "XYZ SCHOOL", salary)
        store.insertManualIncome(5900000, at("2026-08-28 00:00"), "XYZ SCHOOL", salary)
        val cycles = PayCycleStore.get(context).listCycles()
        assertEquals(3, cycles.size)
        assertEquals(currentId, cycles.first().startTransactionId)
        assertEquals(at("2026-09-20 00:00"), cycles[1].endEpochMs)
        assertEquals(at("2026-08-28 00:00"), cycles[2].endEpochMs)
    }
    @Test fun confirmingCapturedIncomeDoesNotInsertAnotherReceipt() {
        val salary = rule(); val store = TransactionStore.get(context)
        store.insertIfNew(ParsedMpesaTransaction("EXISTING", MpesaTransactionType.RECEIVED,
            6000000, "XYZ SCHOOL", feeMinor = 0, balanceMinor = null,
            occurredAtText = null, rawMessage = "EXISTING"), null, "TEST_SMS",
            at("2026-09-28 10:00"), at("2026-09-28 10:00"),
            accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
            directionOverride = MoneyDirection.INCOME, categoryOverride = TransactionCategory.INCOME,
            reviewStateOverride = ReviewState.USER_CLASSIFIED)
        val receipt = store.recent().single()
        assertNull(PayCycleStore.get(context).currentCycle())
        assertTrue(store.applyRuleToTransaction(receipt.id, salary))
        PayCycleStore.get(context).confirmNewCycle(receipt.id)
        assertEquals(1, store.snapshot().transactionCount)
        assertEquals(receipt.id, PayCycleStore.get(context).currentCycle()!!.startTransactionId)
    }
    @Test fun cycleIncludesLateMonthAndSavingsSpendingAndExcludesNextSalary() {
        val salary = rule(); val store = TransactionStore.get(context)
        store.insertManualIncome(6000000, at("2026-08-28 00:00"), "XYZ SCHOOL", salary)
        spending("AUGFAMILY", "2026-08-29 10:00", 1000000, AccountStore.DEFAULT_MPESA_ID)
        spending("SEPSAVINGS", "2026-09-03 10:00", 500000, AccountStore.DEFAULT_CASH_ID)
        store.insertManualIncome(6100000, at("2026-09-28 00:00"), "XYZ SCHOOL", salary)
        spending("NEXTCYCLE", "2026-09-28 12:00", 200000, AccountStore.DEFAULT_MPESA_ID)
        val cycles = PayCycleStore.get(context).listCycles()
        assertEquals(2, cycles.size)
        val closed = cycles[1]
        val summary = store.activitySnapshotBetween(closed.startEpochMs, closed.endEpochMs!!).summary
        assertEquals(1500000L, summary.spentMinor)
        assertEquals(6000000L, summary.incomeMinor)
        assertEquals(200L, summary.feesMinor)
        assertEquals(700000L, store.activitySnapshot(at("2026-09-15 00:00")).summary.spentMinor)
    }
}
