package com.kedevjimmy.pesatrack.security

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FingerprintConflictBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val password = "invented-conflict-backup-secret"

    @Before fun setup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteDatabase("pesatrack_statement_activity.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.deleteDatabase("pesatrack_statement_activity.db")
    }

    private fun count(table: String): Int = PesaTrackDatabase.get(context).readableDatabase
        .rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }

    @Test fun encryptedRestorePreservesNonPostingConflictAndCanonicalCash() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        val id = db.insertOrThrow("transactions", null, ContentValues().apply {
            put("fingerprint", "code:SYNTHETIC123")
            put("transaction_code", "SYNTHETIC123")
            put("type", "SENT_MONEY")
            put("amount_minor", 45_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", 1_790_000_000_000L)
            put("received_at_epoch_ms", 1_790_000_000_000L)
            put("source_kind", "sms")
            put("created_at_epoch_ms", 1_790_000_000_000L)
        })
        db.insertOrThrow("fingerprint_conflicts", null, ContentValues().apply {
            put("existing_transaction_id", id)
            put("evidence_digest", "synthetic-digest")
            put("source_kind", "notification")
            put("transaction_code", "SYNTHETIC123")
            put("type", "SENT_MONEY")
            put("amount_minor", 46_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", 1_790_000_001_000L)
        })
        val manager = PesaBackupManager(context)
        val backup = manager.exportEncryptedBytes(password)
        db.delete("fingerprint_conflicts", null, null)
        db.delete("transactions", null, null)
        manager.restoreEncryptedBytes(backup, password)
        assertEquals(1, count("transactions"))
        assertEquals(1, count("fingerprint_conflicts"))
        assertEquals(45_000L, PesaTrackDatabase.get(context).readableDatabase
            .rawQuery("SELECT amount_minor FROM transactions", null).use { it.moveToFirst(); it.getLong(0) })
    }

    @Test fun schema40BackupCanRestoreWithoutInventingConflicts() {
        val manager = PesaBackupManager(context)
        val older = EncryptedBackupFixture.rewrite(manager.exportEncryptedBytes(password), password) { payload ->
            payload.addProperty("schemaVersion", 40)
            payload.getAsJsonObject("tables").remove("fingerprint_conflicts")
        }
        manager.restoreEncryptedBytes(older, password)
        assertEquals(0, count("fingerprint_conflicts"))
    }
}
