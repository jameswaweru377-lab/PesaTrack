package com.kedevjimmy.pesatrack.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonObject
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRecoveryIntegrationTest {
    private lateinit var context: Context
    private lateinit var manager: PesaBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteSharedPreferences(PREFS)
        context.deleteSharedPreferences(SECOND_PREFS)
        manager = PesaBackupManager(context)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteSharedPreferences(PREFS)
        context.deleteSharedPreferences(SECOND_PREFS)
    }

    @Test
    fun olderSchemaRestoreIsRefusedBeforeDeletingCurrentRows() {
        val older = olderSchemaBackup()
        val saved = saveCurrentRule()

        val failure = runCatching {
            manager.restoreEncryptedBytes(older, PASSPHRASE)
        }.exceptionOrNull()

        assertEquals(listOf(saved.id), RuleStore.get(context).listRules().map { it.id })
        assertTrue("Unsupported old schema must be refused", failure is IllegalArgumentException)
    }

    @Test
    fun olderSchemaPreviewDoesNotOfferAnUnsupportedRestore() {
        val older = olderSchemaBackup()
        val failure = runCatching {
            manager.previewEncryptedBytes(older, PASSPHRASE)
        }.exceptionOrNull()
        assertTrue("Preview must reject schemas without a restore migration", failure is IllegalArgumentException)
    }

    @Test
    fun authenticatedMalformedPreferencesLeaveCurrentLedgerAndSettingsIntact() {
        // The empty ledger is a valid exported snapshot. Only its preferences are malformed.
        val malformed = EncryptedBackupFixture.rewrite(
            manager.exportEncryptedBytes(PASSPHRASE), PASSPHRASE
        ) { payload ->
            payload.getAsJsonObject("preferences").add(PREFS, JsonObject().apply {
                add("marker", JsonObject().apply {
                    addProperty("type", "int")
                    addProperty("value", "not-an-integer")
                })
            })
        }
        val saved = saveCurrentRule()
        assertTrue(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("marker", "current settings").commit())

        val failure = runCatching {
            manager.restoreEncryptedBytes(malformed, PASSPHRASE)
        }.exceptionOrNull()

        assertTrue("Authenticated malformed preference must be refused", failure != null)
        assertEquals(listOf(saved.id), RuleStore.get(context).listRules().map { it.id })
        assertEquals("current settings", context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("marker", null))
    }

    private fun olderSchemaBackup(): ByteArray = EncryptedBackupFixture.rewrite(
        manager.exportEncryptedBytes(PASSPHRASE), PASSPHRASE
    ) { payload ->
        val current = payload.get("schemaVersion").asInt
        assertTrue("Fixture requires a schema with a previous version", current > 1)
        payload.addProperty("schemaVersion", current - 1)
    }

    @Test
    fun restartAfterStagingBeforeLedgerCommitKeepsOldLedgerAndSettings() {
        val saved = saveCurrentRule()
        setPreference(PREFS, "old settings")
        val database = PesaTrackDatabase.get(context).writableDatabase
        BackupRestoreJournal(context).stage(database, stagedSettings())
        assertTrue(BackupRestoreJournal.isPending(context))

        // Persisted interruption boundary: journal exists, SQLite has no committed marker.
        PesaTrackDatabase.resetForTests()
        PesaTrackDatabase.get(context).writableDatabase

        assertEquals(listOf(saved.id), RuleStore.get(context).listRules().map { it.id })
        assertEquals("old settings", preference(PREFS))
        assertFalse(BackupRestoreJournal.isPending(context))
    }

    @Test
    fun restartAfterLedgerCommitFinishesPartiallyWrittenSettingsAndIsIdempotent() {
        saveCurrentRule()
        setPreference(PREFS, "old settings")
        setPreference(SECOND_PREFS, "old settings")
        val database = PesaTrackDatabase.get(context).writableDatabase
        val journal = BackupRestoreJournal(context)
        val operationId = journal.stage(database, stagedSettings())
        database.beginTransaction()
        try {
            database.delete("smart_rules", null, null)
            journal.markCommitted(database, operationId)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        // Simulate termination between the durable writes of two real preference files.
        setPreference(PREFS, "new settings")
        assertEquals("old settings", preference(SECOND_PREFS))

        PesaTrackDatabase.resetForTests()
        PesaTrackDatabase.get(context).writableDatabase

        assertTrue(RuleStore.get(context).listRules().isEmpty())
        assertEquals("new settings", preference(PREFS))
        assertEquals("new settings", preference(SECOND_PREFS))
        assertFalse(BackupRestoreJournal.isPending(context))
        PesaTrackDatabase.resetForTests()
        PesaTrackDatabase.get(context).writableDatabase
        assertEquals("new settings", preference(SECOND_PREFS))
    }

    private fun stagedSettings() = JsonObject().apply {
        listOf(PREFS, SECOND_PREFS).forEach { name ->
            add(name, JsonObject().apply {
                add("marker", JsonObject().apply {
                    addProperty("type", "string")
                    addProperty("value", "new settings")
                })
            })
        }
    }

    private fun setPreference(name: String, value: String) {
        assertTrue(context.getSharedPreferences(name, Context.MODE_PRIVATE)
            .edit().putString("marker", value).commit())
    }

    private fun preference(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .getString("marker", null)

    private fun saveCurrentRule() = RuleStore.get(context).save(
        name = "Current ledger marker",
        counterpartyContains = "CURRENT PAYEE",
        category = TransactionCategory.SHOPPING,
        direction = MoneyDirection.EXPENSE
    )

    companion object {
        private const val PREFS = "backup_recovery_test"
        private const val SECOND_PREFS = "backup_recovery_test_second"
        private const val PASSPHRASE = "strong-passphrase"
    }
}

