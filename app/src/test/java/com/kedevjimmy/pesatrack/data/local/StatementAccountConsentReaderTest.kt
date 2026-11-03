package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Disposable synthetic local database; JVM only, no emulator or instrumentation. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class StatementAccountConsentReaderTest {
    private lateinit var app: Context

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        resetStores()
        app.deleteDatabase("pesatrack.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        resetStores()
        app.deleteDatabase("pesatrack.db")
    }

    private fun resetStores() {
        for (type in listOf(AccountStore::class.java, FinancialUnderstandingStore::class.java)) {
            type.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        }
    }

    @Test fun rejectedB2DecisionOverridesLegacyOwnedEntityWithoutChangingHistoricalEntity() {
        val account = AccountStore.get(app).save("Synthetic Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(account.id, "001234567890", null)
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.insertOrThrow("financial_entities", null, ContentValues().apply {
            put("entity_key", "synthetic-old-bank")
            put("display_name", "Synthetic Equity")
            put("provider_key", "SYNTHETIC")
            put("account_type_hint", "BANK")
            put("ownership_hint", "USER_OWNED")
            put("nature_hint", "ASSET")
            put("confirmed_account_id", account.id)
            put("first_seen_epoch_ms", 1000L)
            put("last_seen_epoch_ms", 1000L)
        })
        val reader = StatementAccountConsentReader(app)
        val consent = AccountOwnershipConsentStore(app)
        assertTrue(account.id in reader.confirmedAccountIds())

        consent.recordConsent(account.id, false)
        assertTrue(account.id in consent.rejectedAccountIds())
        assertFalse(account.id in reader.confirmedAccountIds())
        assertTrue(account.id in FinancialUnderstandingStore.get(app).confirmedAccountIds())

        consent.recordConsent(account.id, true)
        assertFalse(account.id in consent.rejectedAccountIds())
        assertTrue(account.id in reader.confirmedAccountIds())
    }
}
