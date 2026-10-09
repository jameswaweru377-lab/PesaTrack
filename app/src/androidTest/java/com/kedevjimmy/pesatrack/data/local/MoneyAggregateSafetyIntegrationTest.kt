package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Invented boundary amounts in an isolated database, never personal ledger data. */
@RunWith(AndroidJUnit4::class)
class MoneyAggregateSafetyIntegrationTest {
    private lateinit var context: Context
    private val start = 1_790_000_000_000L

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun incomeOverflowRejectsSnapshotRatherThanPublishingNegativeMoney() {
        insert("first", Long.MAX_VALUE, MoneyDirection.INCOME, start)
        insert("second", 1L, MoneyDirection.INCOME, start + 60_000L)
        assertThrows(ArithmeticException::class.java) { snapshot() }
    }

    @Test fun netOverflowRejectsSnapshotEvenWhenEachSubtotalFits() {
        insert("expense", Long.MAX_VALUE, MoneyDirection.EXPENSE, start)
        insert("fee", 0L, MoneyDirection.NEUTRAL, start + 60_000L, fee = 2L)
        assertThrows(ArithmeticException::class.java) { snapshot() }
    }

    @Test fun exactlyRepresentableBoundaryRemainsAvailable() {
        insert("first", Long.MAX_VALUE - 1L, MoneyDirection.INCOME, start)
        insert("second", 1L, MoneyDirection.INCOME, start + 60_000L)
        assertEquals(Long.MAX_VALUE, snapshot().summary.incomeMinor)
        assertEquals(Long.MAX_VALUE, snapshot().summary.netMinor)
    }

    @Test fun savingsWithdrawalRejectsUnrepresentableSignedAmount() {
        insert("saving", 1L, MoneyDirection.TRANSFER, start)
        val row = snapshot().transactions.single().copy(
            amountMinor = Long.MIN_VALUE, accountId = "saving", linkedAccountId = "wallet")
        assertThrows(ArithmeticException::class.java) {
            ConceptLedgerPolicy.savingsDelta(row, mapOf("saving" to AccountType.SAVINGS))
        }
    }

    @Test fun debtOverflowRemainsUnknownAcrossReplayAndCannotAppearSettled() {
        val liability = FinancialAccountReference(entityKey = "synthetic:boundary-lender",
            displayName = "Synthetic boundary lender", providerKey = "synthetic:boundary-lender",
            accountTypeHint = AccountType.OTHER, ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.LIABILITY)
        val store = DebtStore.get(context)
        fun draw(amount: Long, at: Long, key: String) = store.recordDrawdown(
            DebtDrawdownEventFact(amountMinor = amount, liability = liability, feeMinor = 0L,
                outstandingAfterMinor = null, availableCreditAfterMinor = null,
                confidence = InterpretationConfidence.HIGH), null, key, null, "TEST", at)
        draw(Long.MAX_VALUE, start, "first")
        draw(1L, start + 60_000L, "second")
        val account = store.accounts().single()
        assertEquals(DebtBalanceQuality.UNKNOWN, account.balanceQuality)
        assertFalse(account.isSettled)
        assertFalse(draw(1L, start + 60_000L, "second").inserted)
        assertEquals(DebtBalanceQuality.UNKNOWN, store.accounts().single().balanceQuality)
    }

    @Test fun reconstructedAccountOverflowShowsUnknownBalanceRatherThanWrappedNegative() {
        val account = MoneyAccount("synthetic-wallet", "Synthetic wallet", AccountType.BANK,
            openingBalanceMinor = Long.MAX_VALUE, openingBalanceAsOfEpochMs = start - 60_000L)
        insert("incoming", 1L, MoneyDirection.INCOME, start, accountId = account.id)
        val result = PaydaySnapshotStore.get(context).currentAccountSnapshot(account, start + 60_000L)
        assertNull(result.balanceMinor)
        assertEquals(PaydayBalanceQuality.UNKNOWN, result.quality)
    }

    @Test fun historicalAccountDeltaOverflowDoesNotPublishAReconstruction() {
        val account = MoneyAccount("synthetic-wallet", "Synthetic wallet", AccountType.BANK,
            openingBalanceMinor = 0L, openingBalanceAsOfEpochMs = start - 60_000L)
        insert("first", Long.MAX_VALUE, MoneyDirection.INCOME, start, accountId = account.id)
        insert("second", 1L, MoneyDirection.INCOME, start + 60_000L, accountId = account.id)
        val result = PaydaySnapshotStore.get(context).currentAccountSnapshot(account, start + 120_000L)
        assertNull(result.balanceMinor)
        assertEquals(PaydayBalanceQuality.UNKNOWN, result.quality)
    }

    private fun snapshot() = TransactionStore.get(context).activitySnapshotBetween(
        start, start + 180_000L, start + 180_000L, performMaintenance = false)

    private fun insert(code: String, amount: Long, direction: MoneyDirection, at: Long,
        fee: Long = 0L, accountId: String? = null) {
        PesaTrackDatabase.get(context).writableDatabase.insertOrThrow("transactions", null,
            ContentValues().apply {
                put("transaction_code", code); put("fingerprint", "invented-$code")
                put("type", "OTHER"); put("amount_minor", amount); put("fee_minor", fee)
                put("direction", direction.name); put("category", "OTHER")
                put("review_state", ReviewState.USER_CLASSIFIED.name)
                put("occurred_at_epoch_ms", at); put("received_at_epoch_ms", at)
                put("created_at_epoch_ms", at); put("source_kind", "TEST")
                if (accountId != null) put("account_id", accountId)
            })
    }
}
