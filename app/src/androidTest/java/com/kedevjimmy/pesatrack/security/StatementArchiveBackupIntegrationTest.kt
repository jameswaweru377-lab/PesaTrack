package com.kedevjimmy.pesatrack.security

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic statement rows only; never put a real receipt or statement in tests. */
@RunWith(AndroidJUnit4::class)
class StatementArchiveBackupIntegrationTest {
    private lateinit var app: Context
    private lateinit var manager: PesaBackupManager
    private val password = "invented-statement-backup-secret"
    private val batchId = "a".repeat(64)

    @Before fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
        manager = PesaBackupManager(app)
        BackupRecoveryStatus.failure = null
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
        BackupRecoveryStatus.failure = null
    }

    private fun <T> archive(block: (SQLiteDatabase) -> T): T {
        StatementActivityStore(app).batches() // Initialize or migrate the statement archive.
        val path = app.getDatabasePath("pesatrack_statement_activity.db")
        SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            .use { db ->
                db.setForeignKeyConstraintsEnabled(true)
                return block(db)
            }
    }

    private fun seedArchive() {
        val wallet = AccountStore.get(app).save("Invented M-Pesa wallet", AccountType.MOBILE_WALLET)
        archive { db ->
            db.beginTransaction()
            try {
                db.insertOrThrow("statement_batches", null, ContentValues().apply {
                    put("id", batchId)
                    put("account_id", wallet.id)
                    put("recorded_at", 1_780_000_000_000L)
                    put("row_count", 2)
                    put("movement_count", 1)
                    put("informational_count", 1)
                    put("paid_in_minor", 0L)
                    put("paid_out_minor", 25_000L)
                    put("totals_verified", 1)
                })
                listOf(
                    Triple("AAA1234567", 25_000L, "Invented cafe expense"),
                    Triple("AAA1234568", 0L, "Invented informational notice")
                ).forEachIndexed { index, (receipt, withdrawn, desc) ->
                    db.insertOrThrow("statement_activity_lines", null, ContentValues().apply {
                        put("batch_id", batchId)
                        put("line_number", index)
                        put("page_number", 1)
                        put("row_number", index + 1)
                        put("reference", receipt)
                        put("occurred_at", 1_780_000_000_000L - index * 60_000L)
                        put("description", desc)
                        put("paid_in_minor", 0L)
                        put("withdrawn_minor", withdrawn)
                        put("balance_minor", 50_000L)
                        put("source_cells_json", JSONArray(listOf(
                            receipt, "Synthetic date", desc, "Completed",
                            "", withdrawn.toString(), "500.00"
                        )).toString())
                    })
                }
                db.insertOrThrow("statement_category_labels", null, ContentValues().apply {
                    put("batch_id", batchId)
                    put("page_number", 1)
                    put("row_number", 1)
                    put("category_id", "synthetic_food_dining")
                })
                db.insertOrThrow("statement_category_rules", null, ContentValues().apply {
                    put("exact_merchant_key", "invented cafe expense")
                    put("category_id", "synthetic_food_dining")
                })
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun clearStatementArchive() = archive { db ->
        listOf(
            "statement_category_labels_v2", "statement_legs_v2", "statement_sources_v2",
            "statement_category_labels", "statement_activity_lines", "statement_category_rules", "statement_batches"
        ).forEach { db.delete(it, null, null) }
    }

    private fun seedBankEvidence() = archive { db ->
        db.insertOrThrow("statement_sources_v2", null, ContentValues().apply {
            put("id", "b".repeat(64)); put("account_id", "invented-owned-bank")
            put("provider_key", "KENYAN_BANK_EQUITY"); put("account_identity_hash", "c".repeat(64))
            put("currency", "KES"); put("document_format", "PDF")
            put("imported_at", 1_780_000_000_000L); put("row_count", 1)
            put("first_at", 1_780_000_000_000L); put("last_at", 1_780_000_000_000L)
            put("money_in_minor", 1000L); put("money_out_minor", 0L); put("totals_verified", 1)
        })
        db.insertOrThrow("statement_legs_v2", null, ContentValues().apply {
            put("source_id", "b".repeat(64)); put("leg_number", 0)
            put("page_number", 1); put("row_number", 1)
            put("occurred_at", 1_780_000_000_000L)
            put("description", "Invented bank movement")
            put("movement_minor", 1000L); put("running_balance_minor", 1000L)
            put("source_cells_json", "[\"01-01-2026\",\"\",\"Invented bank movement\",\"\",\"10.00\",\"10.00 Cr\"]")
        })
    }

    @Test fun encryptedBackupRestoresAllStatementRowsAndCategoriesWithoutCashPosting() {
        seedArchive()
        val backup = manager.exportEncryptedBytes(password)
        val preview = manager.previewEncryptedBytes(backup, password)
        assertTrue(preview.statementHistoryIncluded)
        assertEquals(2, preview.statementRows)

        clearStatementArchive()
        assertTrue(StatementActivityStore(app).batches().isEmpty())

        val restored = manager.restoreEncryptedBytes(backup, password)
        assertEquals(2, restored.statementRows)
        val store = StatementActivityStore(app)
        assertEquals(batchId, store.batches().single().id)
        assertEquals(2, store.lines(batchId).size)
        assertEquals(7, store.lines(batchId).first().sourceCells.size)
        assertEquals(25_000L, store.batches().single().paidOutMinor)
        assertEquals("synthetic_food_dining", store.categoriesFor(batchId)[1 to 1])
        assertEquals("synthetic_food_dining", store.categoryRules()["invented cafe expense"])

        // Same encrypted backup is idempotent, not a second statement batch.
        manager.restoreEncryptedBytes(backup, password)
        assertEquals(1, store.batches().size)
        assertEquals(2, store.lines(batchId).size)
        val ledger = PesaTrackDatabase.get(app).readableDatabase
        assertEquals(0L, ledger.compileStatement("SELECT count(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun v4LiveArchiveMigratesToV5WithoutRewritingLegacyEvidence() {
        seedArchive()
        archive { db ->
            // Recreate the existing v4 schema in this isolated test database.
            db.execSQL("DROP TABLE statement_category_labels_v2")
            db.execSQL("DROP TABLE statement_legs_v2")
            db.execSQL("DROP TABLE statement_sources_v2")
            db.version = 4
        }
        assertEquals(2, StatementActivityStore(app).lines(batchId).size)
        archive { db ->
            assertEquals(5, db.version)
            assertEquals(1L, db.compileStatement("SELECT COUNT(*) FROM statement_batches").simpleQueryForLong())
            assertEquals(2L, db.compileStatement("SELECT COUNT(*) FROM statement_activity_lines").simpleQueryForLong())
            assertEquals(0L, db.compileStatement("SELECT COUNT(*) FROM statement_sources_v2").simpleQueryForLong())
        }
    }

    @Test fun authenticatedIncompleteStatementHistoryIsRejectedBeforeEitherDatabaseChanges() {
        seedArchive()
        val valid = manager.exportEncryptedBytes(password)
        val altered = EncryptedBackupFixture.rewrite(valid, password) { root ->
            root.getAsJsonObject("statementArchive")
                .getAsJsonObject("tables").remove("statement_category_rules")
        }
        val saved = RuleStore.get(app).save(
            "Keep existing rule", "INVENTED", TransactionCategory.SHOPPING, MoneyDirection.EXPENSE
        )
        assertNotNull(runCatching { manager.restoreEncryptedBytes(altered, password) }.exceptionOrNull())
        assertEquals(listOf(saved.id), RuleStore.get(app).listRules().map { it.id })
        assertEquals(2, StatementActivityStore(app).lines(batchId).size)
    }

    @Test fun statementInsertFailureRollsBackLedgerAndStatementArchive() {
        seedArchive()
        val valid = manager.exportEncryptedBytes(password)
        val duplicate = EncryptedBackupFixture.rewrite(valid, password) { root ->
            val batches = root.getAsJsonObject("statementArchive")
                .getAsJsonObject("tables").getAsJsonArray("statement_batches")
            batches.add(batches[0].deepCopy())
        }
        val saved = RuleStore.get(app).save(
            "Keep after rollback", "TEST", TransactionCategory.SHOPPING, MoneyDirection.EXPENSE
        )
        assertNotNull(runCatching { manager.restoreEncryptedBytes(duplicate, password) }.exceptionOrNull())
        assertEquals(listOf(saved.id), RuleStore.get(app).listRules().map { it.id })
        assertEquals(1, StatementActivityStore(app).batches().size)
        assertEquals(2, StatementActivityStore(app).lines(batchId).size)
    }

    @Test fun oldBackupWithoutArchiveCannotSilentlyReplaceExistingStatementHistory() {
        seedArchive()
        val legacy = EncryptedBackupFixture.rewrite(manager.exportEncryptedBytes(password), password) {
            it.addProperty("formatVersion", 1)
            it.remove("statementArchive")
        }
        val preview = manager.previewEncryptedBytes(legacy, password)
        assertFalse(preview.statementHistoryIncluded)
        assertEquals(0, preview.statementRows)
        val saved = RuleStore.get(app).save(
            "Kept on legacy rejection", "TEST", TransactionCategory.SHOPPING, MoneyDirection.EXPENSE
        )
        assertTrue(runCatching { manager.restoreEncryptedBytes(legacy, password) }
            .exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf(saved.id), RuleStore.get(app).listRules().map { it.id })
        assertEquals(2, StatementActivityStore(app).lines(batchId).size)
    }

    @Test fun legacyBackupStillRestoresOnFreshEmptyStatementArchive() {
        val legacy = EncryptedBackupFixture.rewrite(manager.exportEncryptedBytes(password), password) {
            it.addProperty("formatVersion", 1)
            it.remove("statementArchive")
        }
        RuleStore.get(app).save(
            "Replace on fresh device", "TEST", TransactionCategory.SHOPPING, MoneyDirection.EXPENSE
        )
        manager.restoreEncryptedBytes(legacy, password)
        assertTrue(RuleStore.get(app).listRules().isEmpty())
        assertTrue(StatementActivityStore(app).batches().isEmpty())
    }

    @Test fun currentBackupRestoresBothKindsOfEvidenceWithoutLedgerTransactions() {
        seedArchive()
        seedBankEvidence()
        val encrypted = manager.exportEncryptedBytes(password)
        assertEquals(3, manager.previewEncryptedBytes(encrypted, password).statementRows)
        clearStatementArchive()
        assertEquals(3, manager.restoreEncryptedBytes(encrypted, password).statementRows)
        archive { db ->
            assertEquals(1L, db.compileStatement("SELECT COUNT(*) FROM statement_sources_v2").simpleQueryForLong())
            assertEquals(1L, db.compileStatement("SELECT COUNT(*) FROM statement_legs_v2").simpleQueryForLong())
            assertEquals(1L, db.compileStatement("SELECT COUNT(*) FROM statement_batches").simpleQueryForLong())
        }
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase
            .compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun legacyV3AndV4PayloadsRestoreOldEvidenceAndClearNewEvidence() {
        seedArchive()
        for (oldVersion in listOf(3, 4)) {
            val legacy = EncryptedBackupFixture.rewrite(manager.exportEncryptedBytes(password), password) { root ->
                val archive = root.getAsJsonObject("statementArchive")
                archive.addProperty("schemaVersion", oldVersion)
                val tables = archive.getAsJsonObject("tables")
                listOf("statement_sources_v2", "statement_legs_v2", "statement_category_labels_v2")
                    .forEach(tables::remove)
            }
            seedBankEvidence()
            manager.restoreEncryptedBytes(legacy, password)
            archive { db ->
                assertEquals(0L, db.compileStatement("SELECT COUNT(*) FROM statement_sources_v2").simpleQueryForLong())
                assertEquals(1L, db.compileStatement("SELECT COUNT(*) FROM statement_batches").simpleQueryForLong())
            }
        }
    }
}
