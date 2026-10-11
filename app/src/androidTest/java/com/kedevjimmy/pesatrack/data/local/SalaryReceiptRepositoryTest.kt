package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SalaryReceiptRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: SalaryReceiptRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        repository = SalaryReceiptRepository(context)
    }
    @After fun clean() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    private fun at(date: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(date)!!.time
    private fun rule() = RuleStore.get(context).save(name = "Salary", counterpartyContains = "",
        counterpartyExact = "XYZ SCHOOL", displayLabel = "XYZ SCHOOL", category = TransactionCategory.INCOME,
        direction = MoneyDirection.INCOME, preset = RulePreset.SALARY, startsPayCycle = true,
        accountId = AccountStore.DEFAULT_MPESA_ID, payWindowStartDay = 24, payWindowEndDay = 28)
    private fun sms(code: String, date: String, salary: SmartRule) =
        TransactionStore.get(context).insertIfNew(ParsedMpesaTransaction(code, MpesaTransactionType.RECEIVED,
            6000000, "XYZ SCHOOL", feeMinor = 0, balanceMinor = null, occurredAtText = null, rawMessage = code),
            null, "TEST_SMS", at(date), at(date), salary, accountIdOverride = AccountStore.DEFAULT_MPESA_ID)
    @Test fun disabledReceivingAccountRejectsReceiptWithoutChangingHistory() {
        val salary = rule()
        AccountStore.get(context).setEnabled(AccountStore.DEFAULT_MPESA_ID, false)
        assertThrows(IllegalArgumentException::class.java) {
            repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary)
        }
        assertEquals(0, TransactionStore.get(context).snapshot().transactionCount)
        assertNull(PayCycleStore.get(context).currentCycle())
    }
    @Test fun removingManualEntryRebuildsBoundariesAndExplicitReentryRestoresOnlyOneRow() {
        val salary = rule(); val store = TransactionStore.get(context)
        repository.recordManualSalary(6000000, at("2025-08-28 00:00"), salary)
        repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary)
        val receipts = repository.receipts(salary.id)
        assertEquals(2, receipts.size)
        assertTrue(repository.removeSalary(receipts.first().transactionId, salary.id))
        assertEquals(ReviewState.EXCLUDED.name, store.getById(receipts.first().transactionId)!!.reviewState)
        assertEquals(receipts.last().transactionId, PayCycleStore.get(context).currentCycle()!!.startTransactionId)
        assertNull(PayCycleStore.get(context).currentCycle()!!.endEpochMs)
        assertEquals(6000000L, store.activitySnapshotBetween(at("2025-08-01 00:00"), at("2025-10-01 00:00")).summary.incomeMinor)
        assertFalse(store.insertManualIncome(6000000, at("2025-09-28 00:00"), "XYZ SCHOOL", salary))
        assertEquals(1, repository.receipts(salary.id).size)
        assertTrue(repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary))
        assertFalse(repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary))
        assertEquals(2, store.snapshot().transactionCount)
        assertEquals(receipts.first().transactionId, PayCycleStore.get(context).currentCycle()!!.startTransactionId)
        assertTrue(RuleStore.get(context).getById(salary.id)!!.enabled)
    }
    @Test fun removingCapturedSalaryKeepsIncomeAndReplayCannotRestoreItButFutureSalaryStillWorks() {
        val salary = rule(); val store = TransactionStore.get(context)
        assertTrue(sms("SALARYSEP", "2025-09-28 10:00", salary))
        val id = PayCycleStore.get(context).currentCycle()!!.startTransactionId
        assertFalse(repository.removeSalary(id, "wrong-rule"))
        assertTrue(repository.removeSalary(id, salary.id))
        val kept = store.getById(id)!!
        assertEquals(MoneyDirection.INCOME.name, kept.direction)
        assertEquals(ReviewState.USER_CLASSIFIED.name, kept.reviewState)
        assertEquals(AccountStore.DEFAULT_MPESA_ID, kept.accountId)
        assertNull(kept.ruleId); assertNull(PayCycleStore.get(context).currentCycle())
        assertNull(store.latestSalaryReceipt(salary.id))
        assertEquals(6000000L, store.activitySnapshotBetween(at("2025-09-01 00:00"), at("2025-10-01 00:00")).summary.incomeMinor)
        try {
            repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary)
            fail("A removed captured salary must not become duplicate manual income")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, store.snapshot().transactionCount)
        assertFalse(sms("SALARYSEP", "2025-09-28 10:00", salary))
        assertNull(store.getById(id)!!.ruleId)
        assertTrue(sms("SALARYOCT", "2025-10-28 10:00", salary))
        assertEquals(1, repository.receipts(salary.id).size)
        assertEquals(2, store.snapshot().transactionCount)
        assertNotEquals(id, PayCycleStore.get(context).currentCycle()!!.startTransactionId)
    }
    @Test fun manualEntryEnrichedByLateSmsIsKeptAsRealIncomeOnRemoval() {
        val salary = rule(); val store = TransactionStore.get(context)
        repository.recordManualSalary(6000000, at("2025-09-28 00:00"), salary)
        val id = repository.receipts(salary.id).single().transactionId
        assertFalse(sms("LATEMESSAGE", "2025-09-28 10:00", salary))
        assertFalse(repository.receipts(salary.id).single().manualOnly)
        assertTrue(repository.removeSalary(id, salary.id))
        assertEquals("LATEMESSAGE", store.getById(id)!!.transactionCode)
        assertEquals(ReviewState.USER_CLASSIFIED.name, store.getById(id)!!.reviewState)
        assertEquals(6000000L, store.activitySnapshotBetween(at("2025-09-01 00:00"), at("2025-10-01 00:00")).summary.incomeMinor)
        assertFalse(sms("LATEMESSAGE", "2025-09-28 10:00", salary))
        assertEquals(1, store.snapshot().transactionCount)
        assertNull(PayCycleStore.get(context).currentCycle())
    }
}
