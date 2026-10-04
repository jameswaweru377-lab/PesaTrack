package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ingestion.FinancialDiagnosticMode
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeIngestionConcurrencyTest {
    private lateinit var context: Context
    private var completed = false

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        PesaTrackDatabase.get(context).writableDatabase
    }

    @After
    fun tearDown() {
        // A failed deadlock regression must fail promptly rather than block in close().
        if (completed) {
            PesaTrackDatabase.resetForTests()
            context.deleteDatabase("pesatrack.db")
        }
    }

    @Test
    fun homeReadsAndCycleRebuildsCompleteDuringAutomaticMessageIngestion() {
        val transactions = TransactionStore.get(context)
        val cycles = PayCycleStore.get(context)
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        val accounts = AccountStore.get(context)
        val understanding = FinancialUnderstandingStore.get(context)
        val debts = DebtStore.get(context)
        val identities = IdentityGraphStore.get(context)
        val relationships = RelationshipReviewStore.get(context)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(3) { task ->
            Thread(task, "home-ingestion-regression").apply { isDaemon = true }
        }
        try {
            val ingestion = executor.submit {
                assertTrue(start.await(5, TimeUnit.SECONDS))
                repeat(12) { index ->
                    val code = "TSTHOME" + index.toString().padStart(3, '0')
                    coordinator.ingest(
                        title = "MPESA",
                        body = "$code Confirmed. Ksh 1,000.00 withdrawn from M-PESA " +
                            "at SAMPLE AGENT on 28/9/26 at 8:20 AM. " +
                            "New M-PESA balance is Ksh 10,950.00.",
                        sourcePackage = "com.google.android.apps.messaging",
                        sourceKind = "TEST_SMS",
                        receivedAtEpochMs = 1_790_563_200_000L + index,
                        rebuildPayCycles = true,
                        diagnosticMode = FinancialDiagnosticMode.RECONCILE_ONLY
                    )
                }
            }
            val home = executor.submit {
                assertTrue(start.await(5, TimeUnit.SECONDS))
                repeat(12) {
                    cycles.snapshot()
                    transactions.snapshot()
                    transactions.activitySnapshot()
                    accounts.listAccounts()
                    understanding.confirmedAccountIds()
                    debts.accounts()
                    transactions.activityReviewOverview()
                    relationships.pendingReviews()
                    identities.pendingReviewPrompts(limit = 100, includeOptional = false)
                }
            }
            val rebuild = executor.submit {
                assertTrue(start.await(5, TimeUnit.SECONDS))
                repeat(12) { cycles.rebuild() }
            }
            start.countDown()
            ingestion.get(30, TimeUnit.SECONDS)
            home.get(30, TimeUnit.SECONDS)
            rebuild.get(30, TimeUnit.SECONDS)
            assertEquals(12, transactions.snapshot().transactionCount)
            assertEquals(1_095_000L, transactions.snapshot().latestBalanceMinor)
            completed = true
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun automaticSalaryReceiptStillCreatesCycle() {
        RuleStore.get(context).save(
            name = "Salary",
            counterpartyContains = "TEST EMPLOYER",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            preset = RulePreset.SALARY,
            startsPayCycle = true,
            accountId = AccountStore.DEFAULT_MPESA_ID
        )
        val result = FinancialMessageIngestionCoordinator.get(context).ingest(
            title = "MPESA",
            body = "TSTAUTOSAL Confirmed.You have received Ksh60,000.00 from " +
                "TEST EMPLOYER 0712345678 on 28/9/26 at 8:20 AM. " +
                "New M-PESA balance is Ksh60,000.00.",
            sourcePackage = "com.google.android.apps.messaging",
            sourceKind = "TEST_SMS",
            receivedAtEpochMs = 1_790_563_200_000L,
            diagnosticMode = FinancialDiagnosticMode.RECONCILE_ONLY
        )
        assertEquals(1, result.insertedTransactions)
        val snapshot = PayCycleStore.get(context).snapshot()
        assertNotNull(snapshot.cycle)
        assertEquals(6_000_000L, snapshot.cycle!!.openingIncomeMinor)
        completed = true
    }

    @Test
    fun manualSalaryStillCreatesCycleAndRemainsIdempotent() {
        val salary = RuleStore.get(context).save(
            name = "Salary",
            counterpartyContains = "TEST EMPLOYER",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            preset = RulePreset.SALARY,
            startsPayCycle = true,
            accountId = AccountStore.DEFAULT_MPESA_ID
        )
        val transactions = TransactionStore.get(context)
        val timestamp = 1_790_563_200_000L
        assertTrue(transactions.insertManualIncome(6_000_000L, timestamp, "TEST EMPLOYER", salary))
        transactions.insertManualIncome(6_000_000L, timestamp, "TEST EMPLOYER", salary)
        assertEquals(1, transactions.snapshot().transactionCount)
        val snapshot = PayCycleStore.get(context).snapshot()
        assertNotNull(snapshot.cycle)
        assertEquals(6_000_000L, snapshot.cycle!!.openingIncomeMinor)
        completed = true
    }
}
