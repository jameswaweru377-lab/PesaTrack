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
        createFulizaEventsTable(db)
        createDuplicateResolutionsTable(db)
        createCategoryBudgetsTable(db)
        createCustomCategoriesTable(db)
        createDebtTables(db)
        seedDefaultAccounts(db)
        createIndexes(db)
        createTriggers(db)
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

        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN fuliza_financed_minor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN fuliza_access_fee_minor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN fuliza_outstanding_minor INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN fuliza_event_code TEXT")
            createFulizaEventsTable(db)
        }

        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE money_accounts ADD COLUMN identifier TEXT")
            db.execSQL("ALTER TABLE money_accounts ADD COLUMN opening_balance_minor INTEGER")
            db.execSQL("ALTER TABLE money_accounts ADD COLUMN opening_balance_as_of_epoch_ms INTEGER")
        }

        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN evidence_label TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN evidence_source TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN evidence_confidence TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN evidence_time_delta_ms INTEGER")
        }

        if (oldVersion < 9) {
            createDuplicateResolutionsTable(db)
        }

        if (oldVersion < 10) {
            createCategoryBudgetsTable(db)
        }

        if (oldVersion < 11) {
            createCustomCategoriesTable(db)
            db.execSQL(
                "UPDATE transactions SET category = 'EATING_OUT' WHERE category = 'LUNCH'"
            )
            db.execSQL(
                "UPDATE smart_rules SET category = 'EATING_OUT' WHERE category = 'LUNCH'"
            )
            db.execSQL(
                "UPDATE category_budgets SET category = 'EATING_OUT' WHERE category = 'LUNCH'"
            )
        }

        if (oldVersion < 12) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN relation_type TEXT"
            )
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN related_transaction_id INTEGER"
            )
            createDebtTables(db)
            migrateFulizaEventsToDebtLedger(db)
            db.execSQL(
                """
                UPDATE transactions
                SET direction = 'TRANSFER'
                WHERE category = 'DEBT_LOAN'
                  AND direction = 'EXPENSE'
                """.trimIndent()
            )
        }

        createIndexes(db)
        createTriggers(db)
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
                cycle_override TEXT,
                fuliza_financed_minor INTEGER NOT NULL DEFAULT 0,
                fuliza_access_fee_minor INTEGER NOT NULL DEFAULT 0,
                fuliza_outstanding_minor INTEGER,
                fuliza_event_code TEXT,
                evidence_label TEXT,
                evidence_source TEXT,
                evidence_confidence TEXT,
                evidence_time_delta_ms INTEGER,
                relation_type TEXT,
                related_transaction_id INTEGER
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
                identifier TEXT,
                opening_balance_minor INTEGER,
                opening_balance_as_of_epoch_ms INTEGER,
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

    private fun createFulizaEventsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS fuliza_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_key TEXT NOT NULL UNIQUE,
                transaction_code TEXT,
                financed_amount_minor INTEGER NOT NULL,
                access_fee_minor INTEGER NOT NULL,
                outstanding_amount_minor INTEGER,
                available_limit_minor INTEGER,
                occurred_at_epoch_ms INTEGER NOT NULL,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                linked_transaction_id INTEGER,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createDuplicateResolutionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS duplicate_resolutions (
                pair_key TEXT PRIMARY KEY,
                first_transaction_id INTEGER NOT NULL,
                second_transaction_id INTEGER NOT NULL,
                decision TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createCategoryBudgetsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_budgets (
                category TEXT PRIMARY KEY,
                limit_minor INTEGER NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createCustomCategoriesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_categories (
                id TEXT PRIMARY KEY,
                label TEXT NOT NULL UNIQUE COLLATE NOCASE,
                kind TEXT NOT NULL DEFAULT 'EXPENSE',
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createDebtTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_accounts (
                id TEXT PRIMARY KEY,
                provider_key TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                outstanding_minor INTEGER NOT NULL DEFAULT 0,
                balance_quality TEXT NOT NULL DEFAULT 'ESTIMATED',
                last_observed_at_epoch_ms INTEGER,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_key TEXT NOT NULL UNIQUE,
                debt_account_id TEXT NOT NULL,
                event_type TEXT NOT NULL,
                amount_minor INTEGER NOT NULL,
                fee_minor INTEGER NOT NULL DEFAULT 0,
                outstanding_after_minor INTEGER,
                linked_transaction_id INTEGER,
                wallet_balance_after_minor INTEGER,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                occurred_at_epoch_ms INTEGER NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT OR IGNORE INTO debt_accounts(
                id, provider_key, name, outstanding_minor,
                balance_quality, created_at_epoch_ms,
                updated_at_epoch_ms
            ) VALUES(
                'debt-fuliza-mpesa',
                'SAFARICOM_FULIZA',
                'Fuliza M-PESA',
                0,
                'ESTIMATED',
                $now,
                $now
            )
            """.trimIndent()
        )
    }

    private fun migrateFulizaEventsToDebtLedger(
        db: SQLiteDatabase
    ) {
        val now = System.currentTimeMillis()

        db.execSQL(
            """
            INSERT OR IGNORE INTO debt_events(
                event_key,
                debt_account_id,
                event_type,
                amount_minor,
                fee_minor,
                outstanding_after_minor,
                linked_transaction_id,
                wallet_balance_after_minor,
                source_package,
                source_kind,
                occurred_at_epoch_ms,
                created_at_epoch_ms
            )
            SELECT
                'fuliza-drawdown:' || event_key,
                'debt-fuliza-mpesa',
                'DRAWDOWN',
                financed_amount_minor,
                access_fee_minor,
                outstanding_amount_minor,
                linked_transaction_id,
                NULL,
                source_package,
                source_kind,
                occurred_at_epoch_ms,
                $now
            FROM fuliza_events
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE debt_accounts
            SET outstanding_minor = COALESCE(
                    (
                        SELECT outstanding_amount_minor
                        FROM fuliza_events
                        WHERE outstanding_amount_minor IS NOT NULL
                        ORDER BY occurred_at_epoch_ms DESC, id DESC
                        LIMIT 1
                    ),
                    outstanding_minor
                ),
                balance_quality = CASE
                    WHEN EXISTS(
                        SELECT 1
                        FROM fuliza_events
                        WHERE outstanding_amount_minor IS NOT NULL
                    ) THEN 'OBSERVED'
                    ELSE balance_quality
                END,
                last_observed_at_epoch_ms = (
                    SELECT occurred_at_epoch_ms
                    FROM fuliza_events
                    WHERE outstanding_amount_minor IS NOT NULL
                    ORDER BY occurred_at_epoch_ms DESC, id DESC
                    LIMIT 1
                ),
                updated_at_epoch_ms = $now
            WHERE id = 'debt-fuliza-mpesa'
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
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_money_accounts_identifier ON money_accounts(identifier) WHERE identifier IS NOT NULL AND identifier != ''"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_pay_cycles_start ON pay_cycles(start_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_fuliza_occurred_at ON fuliza_events(occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_fuliza_linked_transaction ON fuliza_events(linked_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_duplicate_resolution_first ON duplicate_resolutions(first_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_duplicate_resolution_second ON duplicate_resolutions(second_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transactions_related ON transactions(related_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_debt_events_account_time ON debt_events(debt_account_id, occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_debt_events_linked_transaction ON debt_events(linked_transaction_id)"
        )
    }

    private fun createTriggers(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_sync_refund_category
            AFTER UPDATE OF category ON transactions
            WHEN NEW.category != OLD.category
            BEGIN
                UPDATE transactions
                SET category = NEW.category
                WHERE relation_type = 'REFUND'
                  AND related_transaction_id = NEW.id;
            END
            """.trimIndent()
        )
    }

    companion object {
        private const val DATABASE_NAME = "pesatrack.db"
        private const val DATABASE_VERSION = 12
    }
}
