package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnedMirrorTransferIntegrationTest {
    private lateinit var context: Context
    private lateinit var bank: MoneyAccount
    private lateinit var store: TransactionStore
    private val epoch = 1_790_613_600_000L

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        bank = AccountStore.get(context).findOrCreate("Example Bank", AccountType.BANK)
        store = TransactionStore.get(context)
    }

    @Test fun bankThenWalletRemainsOneTransferAfterReplay() = verifyOrder(true)
    @Test fun walletThenBankRemainsOneTransferAfterReplay() = verifyOrder(false)

    private fun verifyOrder(bankFirst: Boolean) {
        assertTrue(insert(bankFirst))
        assertFalse(insert(!bankFirst))
        assertTransfer()
        assertFalse(insert(true))
        assertFalse(insert(false))
        assertTransfer()
    }

    private fun assertTransfer() {
        val transaction = store.recent().single()
        assertEquals(MoneyDirection.TRANSFER.name, transaction.direction)
        assertEquals(TransactionCategory.TRANSFER.name, transaction.category)
        assertEquals(bank.id, transaction.accountId)
        assertEquals(AccountStore.DEFAULT_MPESA_ID, transaction.linkedAccountId)
        assertEquals(1_000L, transaction.feeMinor)
        assertNull(transaction.balanceMinor)
        val summary = store.activitySnapshot(epoch).summary
        assertEquals(0L, summary.incomeMinor)
        assertEquals(0L, summary.spentMinor)
        assertEquals(20_000L, summary.transferMinor)
        assertEquals(1_000L, summary.feesMinor)
    }

    private fun insert(bankSide: Boolean) = store.insertInterpretedMoneyMovementIfNew(
        kind = if (bankSide) CanonicalFinancialRecordKind.PAYMENT else CanonicalFinancialRecordKind.RECEIPT,
        transactionCode = if (bankSide) "BANK000001" else "MPESA00001",
        amountMinor = 20_000L, counterparty = if (bankSide) "Sample Recipient" else "Sample Sender",
        counterpartyPhone = null, feeMinor = if (bankSide) 1_000L else 0L,
        balanceMinor = if (bankSide) null else 80_000L, occurredAtText = null,
        rawMessage = "Synthetic shared-reference transfer", sourcePackage = "test.sms",
        sourceKind = "interpreted_TEST_SMS", receivedAtEpochMs = epoch, occurredAtEpochMs = epoch,
        rebuildPayCycles = false, accountIdOverride = if (bankSide) bank.id else AccountStore.DEFAULT_MPESA_ID,
        directionOverride = if (bankSide) MoneyDirection.EXPENSE else MoneyDirection.INCOME,
        externalReferences = setOf("SHARED12345")
    )
}
