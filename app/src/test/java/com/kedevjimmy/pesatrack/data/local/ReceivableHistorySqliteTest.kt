package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Executes the production history-search SQL against synthetic on-device SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class ReceivableHistorySqliteTest {
    private fun syntheticDb(): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        execSQL("""CREATE TABLE transactions (
            id INTEGER PRIMARY KEY, counterparty TEXT, counterparty_phone TEXT,
            occurred_at_epoch_ms INTEGER, source_kind TEXT
        )""")
        listOf(
            listOf(1, "Sample Borrower", "+254 712 555 010", 1001L, "sms"),
            listOf(2, "Another Person", "0712-555-011", 1002L, "sms"),
            listOf(3, "Payment to Sample Borrower (+254 712 555 010)", null, 1003L, "statement_import")
        ).forEach { row ->
            execSQL("""INSERT INTO transactions
                (id,counterparty,counterparty_phone,occurred_at_epoch_ms,source_kind)
                VALUES (?,?,?,?,?)""", row.toTypedArray())
        }
    }

    private fun ids(db: SQLiteDatabase, sql: String, vararg args: String): List<Long> =
        db.rawQuery(sql, args.toList().toTypedArray()).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
        }

    @Test fun searchingNameFindsCaseInsensitiveMatchingPersonAcrossSources() {
        syntheticDb().use { db ->
            assertEquals(listOf(3L, 1L), ids(db, receivableHistoryIdsSql(false),
                "%sample%borrower%", "%__no_phone_match__%", "50", "0"))
        }
    }

    @Test fun searchingPhoneMatchesNormalizedPhoneAndStatementDescriptions() {
        syntheticDb().use { db ->
            assertEquals(listOf(3L, 1L), ids(db, receivableHistoryIdsSql(true),
                "%0712555010%", "%712555010%", "%712555010%", "50", "0"))
        }
    }

    @Test fun sourceAndDateFiltersKeepUnrelatedRecordsOut() {
        syntheticDb().use { db ->
            val filtered = receivableHistoryIdsSql(false,
                sourceClause = "AND t.source_kind='sms'",
                dateClause = "AND t.occurred_at_epoch_ms <= ?")
            assertEquals(listOf(1L), ids(db, filtered,
                "%Sample%Borrower%", "%__no_phone_match__%", "1002", "50", "0"))
            assertTrue(ids(db, receivableHistoryIdsSql(false),
                "%\\%\\_%", "%__no_phone_match__%", "50", "0").isEmpty())
        }
    }
}
