package com.kedevjimmy.pesatrack.data.local

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
    fun freshInstallCreatesCurrentSchemaAndDefaultAccounts() {
        val helper = PesaTrackDatabase(context)
        val db = helper.writableDatabase

        assertEquals(CURRENT_VERSION, db.version)
        assertTrue(tableExists(db, "transactions"))
        assertTrue(tableExists(db, "recurring_pattern_decisions"))
        assertTrue(accountExists(db, "account-mpesa"))
        assertTrue(accountExists(db, "account-cash"))

        helper.close()
    }

    @Test
    fun upgradeFrom30To31PreservesExistingAccountAndCreatesRecurringPatternTable() {
        val setupHelper = PesaTrackDatabase(context)
        val setupDb = setupHelper.writableDatabase
        val now = 1_800_000_000_000L

        setupDb.execSQL(
            """
            INSERT INTO money_accounts(
                id,
                name,
                type,
                enabled,
                created_at_epoch_ms,
                updated_at_epoch_ms
            ) VALUES(?, ?, ?, 1, ?, ?)
            """.trimIndent(),
            arrayOf(
                SENTINEL_ACCOUNT_ID,
                "Migration Sentinel",
                "BANK",
                now,
                now
            )
        )
        setupDb.execSQL(
            "DROP TABLE recurring_pattern_decisions"
        )
        setupDb.version = 30
        setupHelper.close()

        val upgradedHelper = PesaTrackDatabase(context)
        val upgradedDb = upgradedHelper.writableDatabase

        assertEquals(CURRENT_VERSION, upgradedDb.version)
        assertTrue(
            tableExists(
                upgradedDb,
                "recurring_pattern_decisions"
            )
        )
        assertTrue(
            accountExists(
                upgradedDb,
                SENTINEL_ACCOUNT_ID
            )
        )

        upgradedHelper.close()
    }

    private fun tableExists(
        db: SQLiteDatabase,
        table: String
    ): Boolean =
        db.rawQuery(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'table'
              AND name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(table)
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun accountExists(
        db: SQLiteDatabase,
        accountId: String
    ): Boolean =
        db.rawQuery(
            """
            SELECT 1
            FROM money_accounts
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val CURRENT_VERSION = 31
        const val SENTINEL_ACCOUNT_ID =
            "account-migration-sentinel"
    }
}
