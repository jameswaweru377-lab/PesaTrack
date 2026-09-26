package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class PesaTrackDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createTransactionsTable(db)
        createRulesTable(db)
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN direction TEXT NOT NULL DEFAULT 'NEUTRAL'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'UNCATEGORIZED'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN review_state TEXT NOT NULL DEFAULT 'NEEDS_REVIEW'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN rule_id TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN planning_month TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN reversal_of_code TEXT")
            createRulesTable(db)
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_transactions_planning_month ON transactions(planning_month)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_transactions_review_state ON transactions(review_state)"
            )
        }
    }

    private fun createTransactionsTable(db: SQLiteDatabase) {
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
                created_at_epoch_ms INTEGER NOT NULL,
                direction TEXT NOT NULL DEFAULT 'NEUTRAL',
                category TEXT NOT NULL DEFAULT 'UNCATEGORIZED',
                review_state TEXT NOT NULL DEFAULT 'NEEDS_REVIEW',
                rule_id TEXT,
                planning_month TEXT,
                reversal_of_code TEXT
            )
            """.trimIndent()
        )
    }

    private fun createRulesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS smart_rules (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                counterparty_contains TEXT NOT NULL DEFAULT '',
                notification_title_contains TEXT NOT NULL DEFAULT '',
                message_contains TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL,
                direction TEXT NOT NULL,
                maximum_amount_minor INTEGER,
                planning_month_offset INTEGER NOT NULL DEFAULT 0,
                display_label TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE UNIQUE INDEX idx_transactions_code ON transactions(transaction_code) WHERE transaction_code IS NOT NULL"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_received_at ON transactions(received_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_balance ON transactions(balance_minor) WHERE balance_minor IS NOT NULL"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_planning_month ON transactions(planning_month)"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_review_state ON transactions(review_state)"
        )
    }

    companion object {
        private const val DATABASE_NAME = "pesatrack.db"
        private const val DATABASE_VERSION = 2
    }
}
