package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import com.kedevjimmy.pesatrack.ui.screens.HomeStateReader
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SalaryAndHomeRegressionTest {
    private lateinit var context: Context

    @Before fun freshDatabase() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun cleanDatabase() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    private fun salaryRule() = RuleStore.get(context).save(
        name = "Salary", counterpartyContains = "", counterpartyExact = "SAMPLE EMPLOYER",
        category = TransactionCategory.INCOME, direction = MoneyDirection.INCOME,
        preset = RulePreset.SALARY, startsPayCycle = true,
        accountId = AccountStore.DEFAULT_MPESA_ID, payWindowStartDay = 24, payWindowEndDay = 28
    )

    @Test fun manualSalaryStartsFirstCycleAndExplicitEarlyPaydayWithoutAnotherReview() {
        val rule = salaryRule()
        val transactions = TransactionStore.get(context)
        assertTrue(transactions.insertManualIncome(6_000_000L, EPOCH - 31 * DAY, "Salary", rule))
        assertFalse(transactions.insertManualIncome(6_000_000L, EPOCH - 31 * DAY, "Salary", rule))
        // The person explicitly records an early salary, outside the usual window.
        assertTrue(transactions.insertManualIncome(6_100_000L, EPOCH - 10 * DAY, "Salary", rule))
        val snapshot = PayCycleStore.get(context).readSnapshot(EPOCH)
        assertEquals(EPOCH - 10 * DAY, snapshot.cycle?.startEpochMs)
        assertEquals(6_100_000L, snapshot.cycle?.openingIncomeMinor)
        assertNull(snapshot.pendingSalary)
        assertEquals(2, transactions.readSnapshot().transactionCount)
    }

    @Test fun knownEmployerSalaryIsAutomaticAndReplayDoesNotAddAnotherCycle() {
        val rule = salaryRule()
        TransactionStore.get(context).insertManualIncome(6_000_000L, EPOCH - 31 * DAY, "Salary", rule)
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        val message = "SAL28SEP26 Confirmed.You have received Ksh60,000.00 from SAMPLE EMPLOYER 0712345678 on 28/9/26 at 10:00 AM New M-PESA balance is Ksh60,500.00."
        assertEquals(1, coordinator.ingest("MPESA", message, "test.sms", "TEST_SMS", EPOCH).insertedTransactions)
        val firstCycle = PayCycleStore.get(context).readSnapshot(EPOCH).cycle!!
        assertEquals(6_000_000L, firstCycle.openingIncomeMinor)
        assertNull(PayCycleStore.get(context).readSnapshot(EPOCH).pendingSalary)
        assertEquals(0, coordinator.ingest("MPESA", message, "test.sms", "TEST_SMS", EPOCH).insertedTransactions)
        assertEquals(firstCycle.startTransactionId, PayCycleStore.get(context).readSnapshot(EPOCH).cycle?.startTransactionId)
        assertEquals(2, TransactionStore.get(context).readSnapshot().transactionCount)
    }

    @Test fun homeReadsPersistedCycleWithoutWaitingForTransactionAndDebtWriterMonitors() {
        TransactionStore.get(context).insertManualIncome(6_000_000L, EPOCH, "Salary", salaryRule())
        val cycleBefore = PayCycleStore.get(context).currentCycle()!!
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val writer = executor.submit {
                synchronized(TransactionStore.get(context)) {
                    synchronized(DebtStore.get(context)) {
                        locked.countDown()
                        release.await(10, TimeUnit.SECONDS)
                    }
                }
            }
            assertTrue(locked.await(3, TimeUnit.SECONDS))
            val home = executor.submit<HomeLoadedStateAdapter> {
                val state = HomeStateReader.read(context)
                HomeLoadedStateAdapter(state.cycleSnapshot.cycle!!.id, state.ledgerSnapshot.transactionCount)
            }.get(3, TimeUnit.SECONDS)
            assertEquals(cycleBefore.id, home.cycleId)
            assertEquals(1, home.transactionCount)
            assertEquals(cycleBefore.id, PayCycleStore.get(context).currentCycle()?.id)
            release.countDown()
            writer.get(3, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private data class HomeLoadedStateAdapter(val cycleId: String, val transactionCount: Int)
    companion object {
        private const val DAY = 86_400_000L
        private const val EPOCH = 1_790_613_600_000L
    }
}
