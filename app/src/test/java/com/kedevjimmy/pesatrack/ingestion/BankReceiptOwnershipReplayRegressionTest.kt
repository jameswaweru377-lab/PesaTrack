package com.kedevjimmy.pesatrack.ingestion

import android.app.Application
import android.content.Context
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic bank SMS and an isolated local ledger; no instrumentation or real messages. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class BankReceiptOwnershipReplayRegressionTest {
    private lateinit var context: Context
    private val sender = "My Bank"
    private val message = "Your account has been credited with KES 60,000.00 from XYZ COMPANY on 26/09/2026. Available balance KES 72,100.00."
    private val receivedAt = 1_794_000_000_000L

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun repeatedPreOwnershipCreditStillTriggersReplayAndAttachesExistingTransaction() {
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        fun scan() = coordinator.ingest(
            title = sender, body = message, sourcePackage = null,
            sourceKind = HistoricalSmsImporter.SOURCE_HISTORICAL_FINANCIAL_MESSAGE,
            receivedAtEpochMs = receivedAt, rebuildPayCycles = false
        )

        val first = scan()
        assertEquals(1, first.insertedTransactions)
        assertTrue(first.pendingReviewEvents > 0)
        val duplicate = scan()
        assertEquals(0, duplicate.insertedTransactions)
        assertTrue("Repeated unassigned receipt must remain pending", duplicate.pendingReviewEvents > 0)

        val transactions = TransactionStore.get(context)
        val before = transactions.transactionsBetween(0L, Long.MAX_VALUE)
        assertEquals(1, before.size)
        assertNull(before.single().accountId)

        val accountReference = FinancialMessageInterpreter.interpret(sender, message)
            .events.filterIsInstance<ReceiptEventFact>().single().destination
        val ownedAccountId = FinancialUnderstandingStore.get(context)
            .confirmEntityOwned(accountReference.entityKey)
        assertNotNull(ownedAccountId)
        scan() // Idempotent import enriches the existing row, not a second credit.

        val after = transactions.transactionsBetween(0L, Long.MAX_VALUE)
        assertEquals(1, after.size)
        assertEquals(ownedAccountId, after.single().accountId)
        assertEquals(6_000_000L, after.single().amountMinor)
    }
}
