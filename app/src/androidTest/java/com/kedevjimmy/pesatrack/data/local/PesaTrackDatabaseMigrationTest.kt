package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
        context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun freshDatabaseCreatesCurrentCriticalSchema() {
        val helper =
            PesaTrackDatabase(context)

        try {
            val db =
                helper.writableDatabase

            assertEquals(
                CURRENT_DATABASE_VERSION,
                db.version
            )

            listOf(
                "transactions",
                "money_accounts",
                "debt_accounts",
                "financial_entities",
                "identity_entities",
                "personal_learning_rules",
                "recurring_pattern_decisions",
                "entity_enrichment_cache",
                "payday_snapshots"
            ).forEach { table ->
                assertTrue(
                    "Expected table $table",
                    tableExists(
                        db,
                        table
                    )
                )
            }

            assertTrue(
                "Expected refund category trigger",
                triggerExists(
                    db,
                    "trg_sync_refund_category"
                )
            )

            assertEquals(
                1L,
                rowCount(
                    db,
                    "money_accounts",
                    "id = ?",
                    arrayOf("account-mpesa")
                )
            )
            assertEquals(
                1L,
                rowCount(
                    db,
                    "money_accounts",
                    "id = ?",
                    arrayOf("account-cash")
                )
            )
        } finally {
            helper.close()
        }
    }

    @Test
    fun upgradeFrom30To31CreatesRecurringTableAndPreservesLedgerRow() {
        val originalHelper =
            PesaTrackDatabase(context)

        try {
            val db =
                originalHelper.writableDatabase

            seedLedgerTransaction(
                db
            )

            db.execSQL(
                "DROP TABLE IF EXISTS recurring_pattern_decisions"
            )
            db.version =
                PREVIOUS_DATABASE_VERSION
        } finally {
            originalHelper.close()
        }

        val upgradedHelper =
            PesaTrackDatabase(context)

        try {
            val upgraded =
                upgradedHelper.writableDatabase

            assertEquals(
                CURRENT_DATABASE_VERSION,
                upgraded.version
            )
            assertTrue(
                tableExists(
                    upgraded,
                    "recurring_pattern_decisions"
                )
            )
            assertEquals(
                1L,
                rowCount(
                    upgraded,
                    "transactions",
                    "fingerprint = ?",
                    arrayOf(
                        MIGRATION_FIXTURE_FINGERPRINT
                    )
                )
            )
        } finally {
            upgradedHelper.close()
        }
    }

    private fun seedLedgerTransaction(
        db: SQLiteDatabase
    ) {
        val now =
            1_760_000_000_000L
        val values =
            ContentValues().apply {
                put(
                    "fingerprint",
                    MIGRATION_FIXTURE_FINGERPRINT
                )
                put(
                    "type",
                    "OTHER"
                )
                put(
                    "amount_minor",
                    12_345L
                )
                put(
                    "fee_minor",
                    0L
                )
                put(
                    "occurred_at_epoch_ms",
                    now
                )
                put(
                    "received_at_epoch_ms",
                    now
                )
                put(
                    "source_kind",
                    "migration_test"
                )
                put(
                    "created_at_epoch_ms",
                    now
                )
            }

        db.insertOrThrow(
            "transactions",
            null,
            values
        )
    }

    private fun tableExists(
        db: SQLiteDatabase,
        tableName: String
    ): Boolean =
        sqliteObjectExists(
            db = db,
            type = "table",
            name = tableName
        )

    private fun triggerExists(
        db: SQLiteDatabase,
        triggerName: String
    ): Boolean =
        sqliteObjectExists(
            db = db,
            type = "trigger",
            name = triggerName
        )

    private fun sqliteObjectExists(
        db: SQLiteDatabase,
        type: String,
        name: String
    ): Boolean =
        db.rawQuery(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = ?
              AND name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                type,
                name
            )
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun rowCount(
        db: SQLiteDatabase,
        tableName: String,
        selection: String,
        selectionArgs: Array<String>
    ): Long =
        db.rawQuery(
            """
            SELECT COUNT(*)
            FROM $tableName
            WHERE $selection
            """.trimIndent(),
            selectionArgs
        ).use { cursor ->
            check(
                cursor.moveToFirst()
            )
            cursor.getLong(0)
        }

    companion object {
        private const val DATABASE_NAME =
            "pesatrack.db"
        private const val CURRENT_DATABASE_VERSION =
            31
        private const val PREVIOUS_DATABASE_VERSION =
            30
        private const val MIGRATION_FIXTURE_FINGERPRINT =
            "migration-v30-fixture"
    }
}
