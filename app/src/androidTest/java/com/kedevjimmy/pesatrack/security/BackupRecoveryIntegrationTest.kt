package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonObject
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.io.File
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
        BackupRecoveryStatus.failure = null
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteSharedPreferences(PREFS)
        context.deleteSharedPreferences(SECOND_PREFS)
        BackupRecoveryStatus.failure = null
        File(context.cacheDir, "blocked-journal-parent").delete()
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
        payload.addProperty("schemaVersion", 33) // Unsupported: 34..40 have explicit migrations.
    }

    @Test
    fun previewRejectsAuthenticatedMalformedPreferencesBeforeOfferingRestore() {
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
        assertTrue(runCatching { manager.previewEncryptedBytes(malformed, PASSPHRASE) }
            .exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun previewRejectsMissingLedgerTableBeforeOfferingRestore() {
        val malformed = EncryptedBackupFixture.rewrite(
            manager.exportEncryptedBytes(PASSPHRASE), PASSPHRASE
        ) { it.getAsJsonObject("tables").remove("smart_rules") }
        assertTrue(runCatching { manager.previewEncryptedBytes(malformed, PASSPHRASE) }
            .exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun failedPreferenceCommitBlocksAccessUntilRealRestoreJournalRetries() {
        setPreference(PREFS, "snapshot settings")
        val backup = manager.exportEncryptedBytes(PASSPHRASE)
        saveCurrentRule()
        setPreference(PREFS, "current settings")
        val failingContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val real = context.getSharedPreferences(name, mode)
                if (name != PREFS) return real
                return object : SharedPreferences by real {
                    override fun edit(): SharedPreferences.Editor {
                        val realEditor = real.edit()
                        return object : SharedPreferences.Editor by realEditor {
                            override fun clear(): SharedPreferences.Editor { realEditor.clear(); return this }
                            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                                realEditor.putString(key, value)
                                return this
                            }
                            override fun commit(): Boolean = false
                        }
                    }
                }
            }
        }

        val failure = runCatching {
            PesaBackupManager(failingContext).restoreEncryptedBytes(backup, PASSPHRASE)
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message?.contains("settings recovery is pending") == true)
        assertTrue(BackupRecoveryStatus.failure != null)
        assertEquals(BackupRecoveryStatus.failure, BackupRecoveryStatus.failures.value)
        assertTrue(BackupRestoreJournal.isPending(context))
        assertEquals("current settings", preference(PREFS))

        // The actual helper reopens using its real application context and completes durable writes.
        BackupRecoveryStatus.retry(context)
        assertEquals(null, BackupRecoveryStatus.failure)
        assertEquals(null, BackupRecoveryStatus.failures.value)
        assertFalse(BackupRestoreJournal.isPending(context))
        assertEquals("snapshot settings", preference(PREFS))
        assertTrue(RuleStore.get(context).listRules().isEmpty())
    }

    @Test
    fun journalStagingFailureLeavesCurrentLedgerAndPreferencesUntouched() {
        val backup = manager.exportEncryptedBytes(PASSPHRASE)
        val saved = saveCurrentRule()
        setPreference(PREFS, "current settings")
        val blockedParent = File(context.cacheDir, "blocked-journal-parent").apply { writeText("file") }
        val blockedContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = blockedParent
        }
        val failure = runCatching {
            PesaBackupManager(blockedContext).restoreEncryptedBytes(backup, PASSPHRASE)
        }.exceptionOrNull()
        assertTrue(failure != null)
        assertEquals(listOf(saved.id), RuleStore.get(context).listRules().map { it.id })
        assertEquals("current settings", preference(PREFS))
        assertEquals(null, BackupRecoveryStatus.failure)
    }

    @Test
    fun failedLedgerInsertRollsBackReplacementWithoutWritingSettings() {
        val saved = saveCurrentRule()
        setPreference(PREFS, "snapshot settings")
        val duplicateId = EncryptedBackupFixture.rewrite(
            manager.exportEncryptedBytes(PASSPHRASE), PASSPHRASE
        ) { payload ->
            val rows = payload.getAsJsonObject("tables").getAsJsonArray("smart_rules")
            rows.add(rows[0].deepCopy())
        }
        assertTrue(RuleStore.get(context).setEnabled(saved.id, false))
        setPreference(PREFS, "current settings")

        assertTrue(runCatching { manager.restoreEncryptedBytes(duplicateId, PASSPHRASE) }
            .exceptionOrNull() != null)
        assertFalse(RuleStore.get(context).listRules().single().enabled)
        assertEquals("current settings", preference(PREFS))
        PesaTrackDatabase.resetForTests()
        PesaTrackDatabase.get(context).writableDatabase
        assertFalse(BackupRestoreJournal.isPending(context))
        assertFalse(RuleStore.get(context).listRules().single().enabled)
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

