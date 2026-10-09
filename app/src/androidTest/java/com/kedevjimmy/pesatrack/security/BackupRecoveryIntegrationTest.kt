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
        manager = PesaBackupManager(context)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteSharedPreferences(PREFS)
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

    private fun saveCurrentRule() = RuleStore.get(context).save(
        name = "Current ledger marker",
        counterpartyContains = "CURRENT PAYEE",
        category = TransactionCategory.SHOPPING,
        direction = MoneyDirection.EXPENSE
    )

    companion object {
        private const val PREFS = "backup_recovery_test"
        private const val PASSPHRASE = "strong-passphrase"
    }
}

