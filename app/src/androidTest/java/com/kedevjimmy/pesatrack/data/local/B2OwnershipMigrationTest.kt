package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.security.PesaBackupManager
import com.kedevjimmy.pesatrack.security.EncryptedBackupFixture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class B2OwnershipMigrationTest {
    private lateinit var context: Context
    private val password = "synthetic-b2-backup-passphrase"
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    @Test fun populatedV37UpgradeDoesNotInventConsentOrRewriteHistory() {
        val legacy = PesaTrackDatabase(context)
        val db = legacy.writableDatabase
        val account = AccountStore.get(context).save("Legacy Synthetic Bank", AccountType.BANK)
        db.insertOrThrow("transactions", null, ContentValues().apply {
            put("fingerprint", "b2-migration-salary"); put("type", "OTHER"); put("amount_minor", 5200000)
            put("fee_minor", 0);
            put("occurred_at_epoch_ms", 1000); put("received_at_epoch_ms", 2000); put("created_at_epoch_ms", 3000)
            put("source_kind", "manual_entry"); put("direction", "INCOME"); put("category", "INCOME")
            put("review_state", "USER_CLASSIFIED"); put("account_id", account.id)
        })
        db.execSQL("DROP TABLE account_ownership_consents")
        db.execSQL("DROP TABLE owned_transfer_review_decisions")
        db.version = 37
        legacy.close(); PesaTrackDatabase.resetForTests()
        val upgraded = PesaTrackDatabase.get(context).writableDatabase
        assertEquals(39, upgraded.version)
        assertTrue(AccountOwnershipConsentStore(context).confirmedRevisions().isEmpty())
        upgraded.rawQuery("SELECT amount_minor,review_state,account_id FROM transactions WHERE fingerprint='b2-migration-salary'", null).use {
            assertTrue(it.moveToFirst()); assertEquals(5200000L, it.getLong(0))
            assertEquals("USER_CLASSIFIED", it.getString(1)); assertEquals(account.id, it.getString(2))
        }
    }
    @Test fun consentRevisionsRejectDisableAndReconfirmWithoutChangingBalances() {
        val account = AccountStore.get(context).save("Synthetic Bank", AccountType.BANK)
        AccountStore.get(context).updateDetails(account.id, "SYNTHETIC-ACCOUNT", 50000, 1000)
        val consent = AccountOwnershipConsentStore(context)
        assertEquals(1L, consent.recordConsent(account.id, true))
        assertEquals(1L, consent.confirmedRevisions()[account.id])
        AccountStore.get(context).setEnabled(account.id, false)
        assertFalse(account.id in consent.confirmedRevisions())
        AccountStore.get(context).setEnabled(account.id, true)
        assertEquals(2L, consent.recordConsent(account.id, false))
        assertFalse(account.id in consent.confirmedRevisions())
        assertEquals(3L, consent.recordConsent(account.id, true))
        assertEquals(50000L, AccountStore.get(context).listAccounts().single { it.id == account.id }.openingBalanceMinor)
    }
    @Test fun v34V36AndV37EncryptedBackupsRestoreWithEmptyNewConsent() {
        val manager = PesaBackupManager(context)
        val account = AccountStore.get(context).save("Synthetic Bank", AccountType.BANK)
        val exported = manager.exportEncryptedBytes(password)
        for (version in listOf(34, 36, 37)) {
            val old = EncryptedBackupFixture.rewrite(exported, password) { payload ->
                payload.addProperty("schemaVersion", version)
                val tables = payload.getAsJsonObject("tables")
                tables.remove("payroll_records"); tables.remove("payroll_lines")
                tables.remove("account_ownership_consents"); tables.remove("owned_transfer_review_decisions")
                if (version < 37) tables.remove("personal_receivable_review_exclusions")
                if (version < 36) { tables.remove("personal_receivable_people"); tables.remove("personal_receivable_events") }
                if (version < 35) { tables.remove("transaction_allocations"); tables.remove("flexible_budget_options"); tables.remove("statement_import_entries") }
            }
            AccountOwnershipConsentStore(context).recordConsent(account.id, true)
            assertEquals(39, manager.previewEncryptedBytes(old, password).schemaVersion)
            manager.restoreEncryptedBytes(old, password)
            assertTrue("Legacy restore cannot fabricate B2 consent", AccountOwnershipConsentStore(context).confirmedRevisions().isEmpty())
            assertTrue(AccountStore.get(context).listAccounts().any { it.id == account.id })
        }
    }
    @Test fun currentEncryptedRestorePreservesExplicitConsentAndRevisions() {
        val account = AccountStore.get(context).save("Synthetic Bank", AccountType.BANK)
        val consent = AccountOwnershipConsentStore(context)
        consent.recordConsent(account.id, true)
        val manager = PesaBackupManager(context)
        val backup = manager.exportEncryptedBytes(password)
        consent.recordConsent(account.id, false)
        manager.restoreEncryptedBytes(backup, password)
        assertEquals(1L, consent.confirmedRevisions()[account.id])
    }
    @Test fun creatingAnOwnedAccountRollsBackIfConsentCannotBeSaved() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("CREATE TRIGGER synthetic_consent_failure BEFORE INSERT ON account_ownership_consents BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        assertTrue(runCatching { AccountOwnershipConsentStore(context).createOwnedAccount("Synthetic Rollback Bank", AccountType.BANK) }.isFailure)
        assertFalse(AccountStore.get(context).listAccounts().any { it.name == "Synthetic Rollback Bank" })
        assertTrue(AccountOwnershipConsentStore(context).confirmedRevisions().isEmpty())
    }
    @Test fun conflictingEntityChoiceWithdrawsMatchingConsentUntilExplicitReconfirmation() {
        val account = AccountOwnershipConsentStore(context).createOwnedAccount("Synthetic Sibling Bank", AccountType.BANK)
        val db = PesaTrackDatabase.get(context).writableDatabase
        for (key in listOf("synthetic-entity-a", "synthetic-entity-b")) db.insertOrThrow("financial_entities", null, ContentValues().apply {
            put("entity_key", key); put("display_name", "Synthetic Sibling Bank"); put("provider_key", "SYNTHETIC")
            put("account_type_hint", "BANK"); put("ownership_hint", "USER_OWNED"); put("nature_hint", "ASSET")
            put("confirmed_account_id", account.id); put("first_seen_epoch_ms", 1000); put("last_seen_epoch_ms", 1000)
        })
        assertTrue(FinancialUnderstandingStore.get(context).markEntityExternal("synthetic-entity-a"))
        assertTrue(AccountStore.get(context).listAccounts().any { it.id == account.id })
        assertFalse(account.id in AccountOwnershipConsentStore(context).confirmedRevisions())
        AccountOwnershipConsentStore(context).recordConsent(account.id, true)
        assertTrue(account.id in AccountOwnershipConsentStore(context).confirmedRevisions())
    }
}
