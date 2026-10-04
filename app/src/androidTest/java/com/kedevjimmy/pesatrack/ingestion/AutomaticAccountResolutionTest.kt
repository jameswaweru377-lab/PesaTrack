package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomaticAccountResolutionTest {
    private lateinit var context: Context

    @Before fun freshInstall() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        assertNull(AccountStore.get(context).listAccounts().first {
            it.id == AccountStore.DEFAULT_MPESA_ID
        }.identifier)
    }

    @Test fun mshwariTransferAndBalancesWorkWithoutPhoneSetupAndReplayOnlyOnce() {
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        val first = coordinator.ingest("MPESA", TRANSFER, "test.sms", "TEST_SMS", EPOCH, false)
        assertEquals(0, first.pendingReviewEvents)
        assertEquals(1, first.insertedTransactions)
        val store = FinancialUnderstandingStore.get(context)
        assertTrue(store.isUserOwned("provider:SAFARICOM_MPESA"))
        assertTrue(store.isUserOwned("provider:NCBA_MSHWARI_SAVINGS"))
        assertEquals(2, store.latestBalances().size)
        assertEquals(MoneyDirection.TRANSFER.name,
            TransactionStore.get(context).findByTransactionCode("RQK12ABC34")?.direction)
        assertEquals(0, coordinator.ingest("MPESA", TRANSFER, "test.sms", "TEST_SMS", EPOCH, false).insertedTransactions)
    }

    @Test fun resolvedBankPaymentGetsNamedBankAccountWithoutOwnershipPrompt() {
        val result = FinancialMessageIngestionCoordinator.get(context).ingest(
            "EQUITY", "Your transaction of Kenya Shillings 20,000 has been credited to SAMPLE PERSON. Reference number EQA12ABC34. M-Pesa reference UFT12ABC34.",
            "test.sms", "TEST_SMS", EPOCH, false
        )
        assertEquals(1, result.insertedTransactions)
        val accountId = FinancialUnderstandingStore.get(context).entityByKey("provider:KENYAN_BANK_EQUITY")?.confirmedAccountId
        assertNotNull(accountId)
        assertEquals("Equity Bank Kenya", AccountStore.get(context).listAccounts().first { it.id == accountId }.name)
        assertEquals(accountId, TransactionStore.get(context).recent().single().accountId)
    }

    @Test fun replayAttachesPreviouslyUnassignedBalanceAndPreservesExternalDecision() {
        val store = FinancialUnderstandingStore.get(context)
        val known = FinancialProviderRegistry.resolveOwnedMessageSourceAccount("EQUITY", true)!!
        val unknown = known.copy(ownershipHint = AccountOwnershipHint.UNKNOWN)
        val fact = BalanceObservationFact(unknown, 50_000L, InterpretationConfidence.HIGH)
        store.recordBalance(fact, "same-message", EPOCH, "test.sms", "TEST_SMS")
        assertNull(store.latestBalances().single().accountId)
        assertTrue(store.recordBalance(fact.copy(account = known), "same-message", EPOCH, "test.sms", "TEST_SMS"))
        assertNotNull(store.latestBalances().single().accountId)
        assertEquals(1, store.latestBalances().size)
        store.markEntityExternal(known.entityKey)
        assertNull(store.observeEntity(known, EPOCH + 1))
        assertFalse(store.isUserOwned(known.entityKey))
    }

    companion object {
        private const val EPOCH = 1_790_613_600_000L
        private const val TRANSFER = "RQK12ABC34 Confirmed. Ksh 7,000.00 transferred from M-Shwari account on 28/9/26 at 6:00 PM. M-Shwari balance is Ksh 543.00. M-PESA balance is Ksh 7,789.00."
    }
}
