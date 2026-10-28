package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** One lossless schema for legacy M-Pesa evidence and independent bank evidence. */
internal class StatementArchiveDb(context: Context) :
    SQLiteOpenHelper(context, "pesatrack_statement_activity.db", null, VERSION) {
    companion object { const val VERSION = 5 }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE statement_batches (
            id TEXT PRIMARY KEY NOT NULL,
            account_id TEXT NOT NULL,
            recorded_at INTEGER NOT NULL,
            row_count INTEGER NOT NULL,
            movement_count INTEGER NOT NULL,
            informational_count INTEGER NOT NULL,
            paid_in_minor INTEGER NOT NULL,
            paid_out_minor INTEGER NOT NULL,
            totals_verified INTEGER NOT NULL
        )""".trimIndent())
        db.execSQL("""CREATE TABLE statement_activity_lines (
            batch_id TEXT NOT NULL REFERENCES statement_batches(id) ON DELETE CASCADE,
            line_number INTEGER NOT NULL,
            page_number INTEGER NOT NULL,
            row_number INTEGER NOT NULL,
            reference TEXT NOT NULL,
            occurred_at INTEGER NOT NULL,
            description TEXT NOT NULL,
            paid_in_minor INTEGER NOT NULL,
            withdrawn_minor INTEGER NOT NULL,
            balance_minor INTEGER NOT NULL,
            source_cells_json TEXT NOT NULL DEFAULT '[]',
            PRIMARY KEY(batch_id, line_number)
        )""".trimIndent())
        db.execSQL("CREATE INDEX statement_lines_time ON statement_activity_lines(batch_id, occurred_at DESC)")
        createGlobalHistoryIndex(db)
        createLegacyCategories(db)
        createBankEvidence(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1 until VERSION && newVersion == VERSION) {
            "Statement Activity requires a lossless migration."
        }
        if (oldVersion < 2)
            db.execSQL("ALTER TABLE statement_activity_lines ADD COLUMN source_cells_json TEXT NOT NULL DEFAULT '[]'")
        if (oldVersion < 3) createLegacyCategories(db)
        if (oldVersion < 4) createGlobalHistoryIndex(db)
        if (oldVersion < 5) createBankEvidence(db)
    }

    private fun createLegacyCategories(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS statement_category_labels (
            batch_id TEXT NOT NULL REFERENCES statement_batches(id) ON DELETE CASCADE,
            page_number INTEGER NOT NULL,
            row_number INTEGER NOT NULL,
            category_id TEXT NOT NULL,
            PRIMARY KEY (batch_id, page_number, row_number)
        )""".trimIndent())
        db.execSQL("""CREATE TABLE IF NOT EXISTS statement_category_rules (
            exact_merchant_key TEXT PRIMARY KEY NOT NULL,
            category_id TEXT NOT NULL
        )""".trimIndent())
    }

    private fun createGlobalHistoryIndex(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS statement_lines_recent_global " +
            "ON statement_activity_lines(occurred_at DESC, batch_id, line_number)")
    }

    /** Additive archive. Older M-Pesa rows and user categories stay untouched. */
    private fun createBankEvidence(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS statement_sources_v2 (
            id TEXT PRIMARY KEY NOT NULL,
            account_id TEXT NOT NULL,
            provider_key TEXT NOT NULL,
            account_identity_hash TEXT NOT NULL,
            currency TEXT NOT NULL,
            document_format TEXT NOT NULL,
            imported_at INTEGER NOT NULL,
            row_count INTEGER NOT NULL CHECK(row_count > 0),
            first_at INTEGER NOT NULL,
            last_at INTEGER NOT NULL,
            money_in_minor INTEGER NOT NULL CHECK(money_in_minor >= 0),
            money_out_minor INTEGER NOT NULL CHECK(money_out_minor >= 0),
            totals_verified INTEGER NOT NULL CHECK(totals_verified = 1),
            CHECK(first_at <= last_at)
        )""".trimIndent())
        db.execSQL("""CREATE TABLE IF NOT EXISTS statement_legs_v2 (
            source_id TEXT NOT NULL REFERENCES statement_sources_v2(id) ON DELETE CASCADE,
            leg_number INTEGER NOT NULL,
            page_number INTEGER NOT NULL,
            row_number INTEGER NOT NULL,
            occurred_at INTEGER NOT NULL,
            description TEXT NOT NULL,
            reference TEXT,
            movement_minor INTEGER NOT NULL,
            running_balance_minor INTEGER NOT NULL,
            source_cells_json TEXT NOT NULL,
            PRIMARY KEY (source_id, leg_number),
            UNIQUE(source_id, page_number, row_number)
        )""".trimIndent())
        db.execSQL("""CREATE TABLE IF NOT EXISTS statement_category_labels_v2 (
            source_id TEXT NOT NULL REFERENCES statement_sources_v2(id) ON DELETE CASCADE,
            leg_number INTEGER NOT NULL,
            category_id TEXT NOT NULL,
            PRIMARY KEY (source_id, leg_number),
            FOREIGN KEY(source_id, leg_number) REFERENCES statement_legs_v2(source_id, leg_number) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS statement_sources_v2_account ON statement_sources_v2(account_id, last_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS statement_legs_v2_time ON statement_legs_v2(source_id, occurred_at DESC)")
    }
}
