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
        createAccountsTable(db)
        createPayCyclesTable(db)
        seedDefaultAccounts(db)
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
        }

        if (oldVersion < 3) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN occurred_at_epoch_ms INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "UPDATE transactions SET occurred_at_epoch_ms = received_at_epoch_ms WHERE occurred_at_epoch_ms = 0"
            )
        }

        if (oldVersion < 4) {
            createAccountsTable(db)
            createPayCyclesTable(db)
            seedDefaultAccounts(db)

            db.execSQL("ALTER TABLE transactions ADD COLUMN account_id TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN linked_account_id TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN pay_cycle_id TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN cycle_candidate_reason TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN cycle_override TEXT")

            db.execSQL("ALTER TABLE smart_rules ADD COLUMN preset TEXT NOT NULL DEFAULT 'CUSTOM'")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN planned_amount_minor INTEGER")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN starts_pay_cycle INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN account_id TEXT")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN linked_account_id TEXT")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN pay_window_start_day INTEGER")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN pay_window_end_day INTEGER")
            db.execSQL(
                """
                UPDATE transactions
                SET account_id = 'account-mpesa'
                WHERE account_id IS NULL
                  AND type != 'OTHER'
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE transactions
                SET linked_account_id = 'account-cash'
                WHERE type = 'WITHDRAWAL'
                  AND linked_account_id IS NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE smart_rules
                SET preset = 'SALARY',
                    starts_pay_cycle = 1,
                    planning_month_offset = 0
                WHERE category = 'INCOME'
                  AND direction = 'INCOME'
                """.trimIndent()
            )
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty_phone TEXT")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN counterparty_exact TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE smart_rules ADD COLUMN counterparty_phone_equals TEXT NOT NULL DEFAULT ''")
        }

        createIndexes(db)
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
                counterparty_phone TEXT,
                occurred_at_text TEXT,
                occurred_at_epoch_ms INTEGER NOT NULL,
                received_at_epoch_ms INTEGER NOT NULL,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                direction TEXT NOT NULL DEFAULT 'NEUTRAL',
                category TEXT NOT NULL DEFAULT 'UNCATEGORIZED',
                review_state TEXT NOT NULL DEFAULT 'NEEDS_REVIEW',
                rule_id TEXT,
                planning_month TEXT,
                reversal_of_code TEXT,
                account_id TEXT,
                linked_account_id TEXT,
                pay_cycle_id TEXT,
                cycle_candidate_reason TEXT,
                cycle_override TEXT
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
                preset TEXT NOT NULL DEFAULT 'CUSTOM',
                planned_amount_minor INTEGER,
                starts_pay_cycle INTEGER NOT NULL DEFAULT 0,
                account_id TEXT,
                linked_account_id TEXT,
                pay_window_start_day INTEGER,
                pay_window_end_day INTEGER,
                counterparty_exact TEXT NOT NULL DEFAULT '',
                counterparty_phone_equals TEXT NOT NULL DEFAULT '',
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createAccountsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS money_accounts (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 1,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createPayCyclesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pay_cycles (
                id TEXT PRIMARY KEY,
                start_transaction_id INTEGER NOT NULL UNIQUE,
                start_epoch_ms INTEGER NOT NULL,
                end_epoch_ms INTEGER,
                opening_income_minor INTEGER NOT NULL,
                carryover_minor INTEGER NOT NULL DEFAULT 0,
                ending_tracked_minor INTEGER,
                state TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun seedDefaultAccounts(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT OR IGNORE INTO money_accounts(
                id, name, type, enabled, created_at_epoch_ms, updated_at_epoch_ms
            ) VALUES('account-mpesa', 'M-Pesa', 'MOBILE_WALLET', 1, $now, $now)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO money_accounts(
                id, name, type, enabled, created_at_epoch_ms, updated_at_epoch_ms
            ) VALUES('account-cash', 'Cash', 'CASH', 1, $now, $now)
            """.trimIndent()
        )
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_code ON transactions(transaction_code) WHERE transaction_code IS NOT NULL"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_received_at ON transactions(received_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_occurred_at ON transactions(occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_balance ON transactions(balance_minor) WHERE balance_minor IS NOT NULL"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_planning_month ON transactions(planning_month)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_review_state ON transactions(review_state)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_pay_cycle ON transactions(pay_cycle_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_pay_cycles_start ON pay_cycles(start_epoch_ms DESC)"
        )
    }

    companion object {
        private const val DATABASE_NAME = "pesatrack.db"
        private const val DATABASE_VERSION = 5
    }
}
