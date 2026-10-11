package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PesaTrackDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun freshDatabaseCreatesCurrentSchema() {
        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("transactions"))
            assertTrue(db.hasTable("evidence_observations"))
            assertTrue(db.hasTable("evidence_links"))
            assertTrue(db.hasTable("evidence_link_audit"))
            assertTrue(db.hasTable("recurring_pattern_decisions"))
            assertTrue(db.hasTable("saving_goal_metadata"))
            assertTrue(db.hasColumn("debt_events", "amount_is_net_proceeds"))
            assertTrue(db.hasIndex("idx_transactions_code"))
            assertTrue(db.hasIndex("idx_transactions_review_occurred"))
            assertTrue(db.hasIndex("idx_transactions_account_occurred"))
            assertTrue(db.hasIndex("idx_transactions_fingerprint_occurred"))
            assertTrue(db.hasTrigger("trg_sync_refund_category"))
        }
    }

    @Test
    fun version33UpgradePreservesDebtEventsWithoutInventingPrincipalProvenance() {
        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase
            removeV34DebtProvenanceColumn(db)
            db.execSQL("""
                INSERT INTO debt_events
                    (event_key, debt_account_id, event_type, amount_minor, fee_minor,
                     source_kind, occurred_at_epoch_ms, created_at_epoch_ms)
                VALUES ('legacy-debt-fixture', 'synthetic-liability', 'DRAWDOWN',
                        10000, 0, 'TEST', 1000, 1000)
            """.trimIndent())
            db.version = 33
        }
        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase
            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasColumn("debt_events", "amount_is_net_proceeds"))
            db.rawQuery("SELECT amount_minor, amount_is_net_proceeds FROM debt_events WHERE event_key = 'legacy-debt-fixture'", null).use {
                assertTrue(it.moveToFirst())
                assertEquals(10000L, it.getLong(0))
                assertTrue(it.isNull(1))
            }
        }
    }

    @Test
    fun version1UpgradePreservesLedgerAndCreatesCurrentSchema() {
        seedVersion1Database()

        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("recurring_pattern_decisions"))
            assertTrue(db.hasTable("saving_goal_metadata"))
            assertTrue(db.hasColumn("smart_rules", "preset"))
            assertTrue(db.hasColumn("smart_rules", "counterparty_exact"))
            assertTrue(db.hasColumn("money_accounts", "identifier"))
            assertTrue(db.hasIndex("idx_transactions_code"))
            assertTrue(db.hasTrigger("trg_sync_refund_category"))

            val storedAmount = db.storedAmount(V1_FINGERPRINT)
            assertEquals(TEST_AMOUNT_MINOR, storedAmount)
        }
    }

    @Test
    fun version30UpgradePreservesLedgerAndCreatesCurrentSchema() {
        seedVersion30Database()

        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("recurring_pattern_decisions"))
            assertTrue(db.hasTable("saving_goal_metadata"))

            val storedAmount = db.storedAmount(V30_FINGERPRINT)

            assertEquals(TEST_AMOUNT_MINOR, storedAmount)
        }
    }

    @Test
    fun version31UpgradeCreatesSavingGoalMetadata() {
        seedVersion31Database()

        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("saving_goal_metadata"))
            assertTrue(db.hasTable("recurring_pattern_decisions"))
        }
    }

    @Test
    fun version32UpgradeAddsLedgerPerformanceIndexes() {
        seedVersion32Database()

        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            assertEquals(
                CURRENT_VERSION,
                db.version
            )
            assertTrue(
                db.hasIndex(
                    "idx_transactions_review_occurred"
                )
            )
            assertTrue(
                db.hasIndex(
                    "idx_transactions_account_occurred"
                )
            )
            assertTrue(
                db.hasIndex(
                    "idx_transactions_linked_account_occurred"
                )
            )
            assertTrue(
                db.hasIndex(
                    "idx_transactions_fingerprint_occurred"
                )
            )
        }
    }

    private fun seedVersion1Database() {
        context.openOrCreateDatabase(
            DATABASE_NAME,
            Context.MODE_PRIVATE,
            null
        ).use { db ->
            db.execSQL(
                """
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_code TEXT,
                    fingerprint TEXT NOT NULL UNIQUE,
                    type TEXT NOT NULL,
                    amount_minor INTEGER NOT NULL,
                    fee_minor INTEGER NOT NULL,
                    balance_minor INTEGER,
                    counterparty TEXT,
                    occurred_at_text TEXT,
                    received_at_epoch_ms INTEGER NOT NULL,
                    source_package TEXT,
                    source_kind TEXT NOT NULL,
                    created_at_epoch_ms INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX idx_transactions_code
                ON transactions(transaction_code)
                WHERE transaction_code IS NOT NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX idx_transactions_received_at
                ON transactions(received_at_epoch_ms DESC)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX idx_transactions_balance
                ON transactions(balance_minor)
                WHERE balance_minor IS NOT NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions(
                    transaction_code,
                    fingerprint,
                    type,
                    amount_minor,
                    fee_minor,
                    balance_minor,
                    counterparty,
                    occurred_at_text,
                    received_at_epoch_ms,
                    source_package,
                    source_kind,
                    created_at_epoch_ms
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "V1TEST0001",
                    V1_FINGERPRINT,
                    "OTHER",
                    TEST_AMOUNT_MINOR,
                    0L,
                    null,
                    "Historical fixture",
                    "1/1/2026 at 8:00 AM",
                    TEST_EPOCH_MS,
                    "test.package",
                    "TEST_FIXTURE_V1",
                    TEST_EPOCH_MS
                )
            )
            db.version = 1
        }
    }

    private fun seedVersion30Database() {
        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            db.execSQL(
                """
                INSERT INTO transactions(
                    fingerprint,
                    type,
                    amount_minor,
                    fee_minor,
                    occurred_at_epoch_ms,
                    received_at_epoch_ms,
                    source_kind,
                    created_at_epoch_ms
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    V30_FINGERPRINT,
                    "OTHER",
                    TEST_AMOUNT_MINOR,
                    0L,
                    TEST_EPOCH_MS,
                    TEST_EPOCH_MS,
                    "TEST_FIXTURE",
                    TEST_EPOCH_MS
                )
            )

            // Build a faithful v30 fixture from the current schema by
            // removing the v31 and v32 additions before downgrading.
            db.execSQL("DROP TABLE IF EXISTS recurring_pattern_decisions")
            db.execSQL("DROP TABLE IF EXISTS saving_goal_metadata")
            db.version = 30
        }
    }

    private fun seedVersion32Database() {
        PesaTrackDatabase(context).useDatabase {
                helper ->
            val db = helper.writableDatabase

            db.execSQL(
                "DROP INDEX IF EXISTS idx_transactions_review_occurred"
            )
            db.execSQL(
                "DROP INDEX IF EXISTS idx_transactions_account_occurred"
            )
            db.execSQL(
                "DROP INDEX IF EXISTS idx_transactions_linked_account_occurred"
            )
            db.execSQL(
                "DROP INDEX IF EXISTS idx_transactions_fingerprint_occurred"
            )
            db.version = 32
        }
    }

    private fun seedVersion31Database() {
        PesaTrackDatabase(context).useDatabase { helper ->
            val db = helper.writableDatabase

            db.execSQL(
                "DROP TABLE IF EXISTS saving_goal_metadata"
            )
            db.version = 31
        }
    }

    private fun SQLiteDatabase.storedAmount(
        fingerprint: String
    ): Long =
        rawQuery(
            """
            SELECT amount_minor
            FROM transactions
            WHERE fingerprint = ?
            """.trimIndent(),
            arrayOf(fingerprint)
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun SQLiteDatabase.hasColumn(
        table: String,
        column: String
    ): Boolean =
        rawQuery(
            "PRAGMA table_info($table)",
            null
        ).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            generateSequence {
                if (cursor.moveToNext()) cursor.getString(nameIndex) else null
            }.any { it == column }
        }

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'table'
              AND name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(name)
        ).use { it.moveToFirst() }

    private fun SQLiteDatabase.hasIndex(name: String): Boolean =
        rawQuery(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'index'
              AND name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(name)
        ).use { it.moveToFirst() }

    private fun SQLiteDatabase.hasTrigger(name: String): Boolean =
        rawQuery(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'trigger'
              AND name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(name)
        ).use { it.moveToFirst() }

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val CURRENT_VERSION = 40
        const val V1_FINGERPRINT = "migration-fixture-v1"
        const val V30_FINGERPRINT = "migration-fixture-v30"
        const val TEST_AMOUNT_MINOR = 12_345L
        const val TEST_EPOCH_MS = 1_790_000_000_000L
    }
}

// SQLiteOpenHelper implements AutoCloseable only from API 26. Keep API 24 fixture cleanup explicit.
private inline fun <T> PesaTrackDatabase.useDatabase(block: (PesaTrackDatabase) -> T): T =
    try { block(this) } finally { close() }
