package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic balances only. Runs on JVM, never reads an actual phone. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class MpesaReportedBalanceTest {
    private lateinit var app: Context

    @Before fun setUp() {
        app = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
    }

    @Test fun mpesaKeepsLastReportedBalanceDespiteLaterLedgerMovements() {
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.execSQL("""
            INSERT INTO account_balance_observations
              (observation_key, entity_key, account_id, balance_minor, confidence,
               observed_at_epoch_ms, source_kind, created_at_epoch_ms)
            VALUES ('synthetic-mpesa', 'mpesa', 'account-mpesa', 184635, 'HIGH',
                    1000, 'sms', 1000)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO transactions
              (fingerprint, type, amount_minor, fee_minor, occurred_at_epoch_ms,
               received_at_epoch_ms, source_kind, created_at_epoch_ms, direction,
               review_state, account_id)
            VALUES ('synthetic-later-money', 'OTHER', 310000, 0, 2000,
                    2000, 'sms', 2000, 'INCOME', 'USER_CLASSIFIED', 'account-mpesa')
        """.trimIndent())
        val account = MoneyAccount(AccountStore.DEFAULT_MPESA_ID, "M-Pesa", AccountType.MOBILE_WALLET)
        val store = PaydaySnapshotStore.get(app)

        val afterLaterTransaction = store.currentAccountSnapshot(account, 3000L)
        assertEquals(184_635L, afterLaterTransaction.balanceMinor)
        assertEquals(PaydayBalanceQuality.OBSERVED, afterLaterTransaction.quality)
        assertEquals(1000L, afterLaterTransaction.referenceEpochMs)

        // A future observation is never backcast into a month without evidence.
        val beforeFirstObservation = store.currentAccountSnapshot(account, 500L)
        assertNull(beforeFirstObservation.balanceMinor)
        assertEquals(PaydayBalanceQuality.UNKNOWN, beforeFirstObservation.quality)

        db.execSQL("""
            INSERT INTO account_balance_observations
              (observation_key, entity_key, account_id, balance_minor, confidence,
               observed_at_epoch_ms, source_kind, created_at_epoch_ms)
            VALUES ('synthetic-user-confirmed', 'mpesa', 'account-mpesa', 461220, 'HIGH',
                    4000, 'user_balance_check', 4000)
        """.trimIndent())
        val afterManualCorrection = store.currentAccountSnapshot(account, 5000L)
        assertEquals(461_220L, afterManualCorrection.balanceMinor)
        assertEquals(PaydayBalanceQuality.USER_ENTERED, afterManualCorrection.quality)

        // A saved bank balance remains the latest recorded fact even if no new
        // bank transaction arrives for several hours. Its as-of time is retained.
        db.execSQL("""
            INSERT INTO account_balance_observations
              (observation_key, entity_key, account_id, balance_minor, confidence,
               observed_at_epoch_ms, source_kind, created_at_epoch_ms)
            VALUES ('synthetic-bank-check', 'sample-bank', 'sample-bank-account',
                    528615, 'HIGH', 1000, 'user_balance_check', 1000)
        """.trimIndent())
        val bankAccount = MoneyAccount("sample-bank-account", "Sample bank", AccountType.BANK)
        val bankHoursLater = store.currentAccountSnapshot(bankAccount, 20L * 60L * 60L * 1000L)
        assertEquals(528_615L, bankHoursLater.balanceMinor)
        assertEquals(PaydayBalanceQuality.USER_ENTERED, bankHoursLater.quality)
        assertEquals(1000L, bankHoursLater.referenceEpochMs)

        val openingOnlyBank = MoneyAccount("opening-bank-account", "Opening bank", AccountType.BANK,
            openingBalanceMinor = 64_175L, openingBalanceAsOfEpochMs = 1000L)
        val openingHoursLater = store.currentAccountSnapshot(openingOnlyBank, 10L * 60L * 60L * 1000L)
        assertEquals(64_175L, openingHoursLater.balanceMinor)
        assertEquals(PaydayBalanceQuality.USER_ENTERED, openingHoursLater.quality)
    }
}
