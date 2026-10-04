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
        PesaTrackDatabase(context).use { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("transactions"))
            assertTrue(db.hasTable("recurring_pattern_decisions"))
            assertTrue(db.hasIndex("idx_transactions_code"))
            assertTrue(db.hasTrigger("trg_sync_refund_category"))
        }
    }

    @Test
    fun version30UpgradePreservesLedgerAndCreatesVersion31Schema() {
        seedVersion30Database()

        PesaTrackDatabase(context).use { helper ->
            val db = helper.writableDatabase

            assertEquals(CURRENT_VERSION, db.version)
            assertTrue(db.hasTable("recurring_pattern_decisions"))

            val storedAmount = db.rawQuery(
                """
                SELECT amount_minor
                FROM transactions
                WHERE fingerprint = ?
                """.trimIndent(),
                arrayOf(TEST_FINGERPRINT)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                cursor.getLong(0)
            }

            assertEquals(TEST_AMOUNT_MINOR, storedAmount)
        }
    }

    private fun seedVersion30Database() {
        PesaTrackDatabase(context).use { helper ->
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
                arrayOf(
                    TEST_FINGERPRINT,
                    "OTHER",
                    TEST_AMOUNT_MINOR,
                    0L,
                    TEST_EPOCH_MS,
                    TEST_EPOCH_MS,
                    "TEST_FIXTURE",
                    TEST_EPOCH_MS
                )
            )

            // Schema v31 adds recurring-pattern decisions. Build a faithful
            // v30 fixture from the current schema by removing only that
            // v31 table and downgrading SQLite's user_version.
            db.execSQL("DROP TABLE IF EXISTS recurring_pattern_decisions")
            db.version = 30
        }
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
        const val CURRENT_VERSION = 31
        const val TEST_FINGERPRINT = "migration-fixture-v30"
        const val TEST_AMOUNT_MINOR = 12_345L
        const val TEST_EPOCH_MS = 1_790_000_000_000L
    }
}
