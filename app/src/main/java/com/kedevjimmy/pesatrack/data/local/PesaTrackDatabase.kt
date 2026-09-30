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
        createDuplicateResolutionsTable(db)
        createCategoryBudgetsTable(db)
        createCustomCategoriesTable(db)
        createDebtTables(db)
        createFinancialUnderstandingTables(db)
        createTransactionExternalReferencesTable(db)
        createLiabilityPaymentDecisionsTable(db)
        createInvestmentTables(db)
        createEntityEnrichmentCacheTable(db)
        createPaydaySnapshotTables(db)
        createCycleProtectionTable(db)
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

        if (oldVersion < 13) {
            createFinancialUnderstandingTables(db)
        }

        if (oldVersion < 14) {
            addColumnIfMissing(
                db,
                "debt_accounts",
                "available_credit_minor",
                "INTEGER"
            )
            addColumnIfMissing(
                db,
                "debt_accounts",
                "available_credit_observed_at_epoch_ms",
                "INTEGER"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "available_credit_after_minor",
                "INTEGER"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "repayment_completion",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "funding_link_confidence",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "funding_link_reason",
                "TEXT"
            )
        }

        if (oldVersion < 15) {
            addColumnIfMissing(
                db,
                "debt_events",
                "funding_account_key",
                "TEXT"
            )
        }

        if (oldVersion < 16) {
            // Legacy Fuliza rows were migrated into the generic
            // debt ledger in schema v12. The provider-specific
            // staging table is no longer used at runtime.
            db.execSQL(
                "DROP TABLE IF EXISTS fuliza_events"
            )
        }

        if (oldVersion < 17) {
            addColumnIfMissing(
                db,
                "debt_events",
                "related_debt_event_id",
                "INTEGER"
            )
        }

        if (oldVersion < 18) {
            createEntityEnrichmentCacheTable(db)
        }

        if (oldVersion < 19) {
            createTransactionExternalReferencesTable(db)
            db.execSQL(
                """
                INSERT OR IGNORE INTO
                    transaction_external_references(
                        transaction_id,
                        reference_value,
                        reference_label,
                        source_kind,
                        observed_at_epoch_ms
                    )
                SELECT
                    id,
                    UPPER(transaction_code),
                    'transaction_code',
                    source_kind,
                    received_at_epoch_ms
                FROM transactions
                WHERE transaction_code IS NOT NULL
                  AND TRIM(transaction_code) != ''
                """.trimIndent()
            )
        }

        if (oldVersion < 20) {
            addColumnIfMissing(
                db,
                "debt_events",
                "funding_account_id",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "resulting_balance_observation_id",
                "INTEGER"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "balance_link_confidence",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "balance_link_reason",
                "TEXT"
            )
            db.execSQL(
                """
                UPDATE debt_events
                SET funding_account_id = (
                    SELECT confirmed_account_id
                    FROM financial_entities
                    WHERE entity_key =
                        debt_events.funding_account_key
                    LIMIT 1
                )
                WHERE funding_account_id IS NULL
                  AND funding_account_key IS NOT NULL
                """.trimIndent()
            )
        }

        if (oldVersion < 21) {
            addColumnIfMissing(
                db,
                "debt_events",
                "funding_link_user_decision",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "debt_events",
                "balance_link_user_decision",
                "TEXT"
            )
        }

        if (oldVersion < 22) {
            createLiabilityPaymentDecisionsTable(db)
        }

        if (oldVersion < 23) {
            createInvestmentTables(db)
        }

        if (oldVersion < 24) {
            addColumnIfMissing(
                db,
                "investment_events",
                "settlement_user_decision",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "investment_events",
                "settlement_confidence",
                "TEXT"
            )
            addColumnIfMissing(
                db,
                "investment_events",
                "settlement_reason",
                "TEXT"
            )
        }

        if (oldVersion < 25) {
            createPaydaySnapshotTables(db)
            db.execSQL(
                """
                UPDATE financial_entities
                SET ownership_hint = 'UNKNOWN'
                WHERE confirmed_account_id IS NULL
                  AND nature_hint != 'LIABILITY'
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE financial_entities
                SET ownership_hint = 'UNKNOWN',
                    confirmed_account_id = NULL
                WHERE account_type_hint = 'BANK'
                  AND nature_hint != 'LIABILITY'
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE financial_entities
                SET ownership_hint = 'UNKNOWN',
                    confirmed_account_id = NULL
                WHERE provider_key IN (
                        'SAFARICOM_MPESA',
                        'NCBA_MSHWARI_SAVINGS',
                        'NCBA_MSHWARI_LOCK_SAVINGS'
                      )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM money_accounts
                        WHERE id = 'account-mpesa'
                          AND identifier IS NOT NULL
                          AND TRIM(identifier) != ''
                      )
                """.trimIndent()
            )
        }

        if (oldVersion < 26) {
            createCycleProtectionTable(db)
        }

        if (oldVersion < 27) {
            mergeLegacyMshwariLockSavings(
                db
            )
        }

        createIndexes(db)
        createTriggers(db)
    }

    private fun mergeLegacyMshwariLockSavings(
        db: SQLiteDatabase
    ) {
        val legacyIds =
            db.rawQuery(
                """
                SELECT id
                FROM money_accounts
                WHERE type = 'SAVINGS'
                  AND LOWER(TRIM(name)) IN (
                        'lock savings',
                        'lock saving'
                      )
                """.trimIndent(),
                null
            ).use { cursor ->
                buildList {
                    while (
                        cursor.moveToNext()
                    ) {
                        add(
                            cursor.getString(0)
                        )
                    }
                }
            }

        if (legacyIds.isEmpty()) {
            return
        }

        var targetId =
            db.rawQuery(
                """
                SELECT id
                FROM money_accounts
                WHERE type = 'SAVINGS'
                  AND LOWER(TRIM(name)) =
                        'm-shwari lock savings'
                LIMIT 1
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }

        if (targetId == null) {
            targetId =
                legacyIds.first()
            db.execSQL(
                """
                UPDATE money_accounts
                SET name = 'M-Shwari Lock Savings',
                    updated_at_epoch_ms = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf<Any?>(
                    System.currentTimeMillis(),
                    targetId
                )
            )
        }

        legacyIds
            .filter {
                it != targetId
            }
            .forEach { sourceId ->
                db.execSQL(
                    """
                    UPDATE transactions
                    SET account_id = ?
                    WHERE account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE transactions
                    SET linked_account_id = ?
                    WHERE linked_account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE smart_rules
                    SET account_id = ?
                    WHERE account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE smart_rules
                    SET linked_account_id = ?
                    WHERE linked_account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE financial_entities
                    SET confirmed_account_id = ?
                    WHERE confirmed_account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE account_balance_observations
                    SET account_id = ?
                    WHERE account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE debt_events
                    SET funding_account_id = ?
                    WHERE funding_account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    UPDATE payday_snapshots
                    SET salary_account_id = ?
                    WHERE salary_account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.execSQL(
                    """
                    DELETE FROM payday_snapshot_accounts
                    WHERE account_id = ?
                      AND snapshot_id IN (
                            SELECT snapshot_id
                            FROM payday_snapshot_accounts
                            WHERE account_id = ?
                      )
                    """.trimIndent(),
                    arrayOf<Any?>(
                        sourceId,
                        targetId
                    )
                )
                db.execSQL(
                    """
                    UPDATE payday_snapshot_accounts
                    SET account_id = ?,
                        account_name =
                            'M-Shwari Lock Savings'
                    WHERE account_id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        targetId,
                        sourceId
                    )
                )
                db.delete(
                    "money_accounts",
                    "id = ?",
                    arrayOf(sourceId)
                )
            }

        db.execSQL(
            """
            UPDATE financial_entities
            SET display_name =
                    'M-Shwari Lock Savings',
                provider_key =
                    'NCBA_MSHWARI_LOCK_SAVINGS',
                account_type_hint =
                    'SAVINGS',
                confirmed_account_id =
                    COALESCE(
                        confirmed_account_id,
                        ?
                    )
            WHERE LOWER(TRIM(display_name))
                    IN (
                        'lock savings',
                        'lock saving'
                    )
            """.trimIndent(),
            arrayOf<Any?>(
                targetId
            )
        )
    }

    private fun addColumnIfMissing(
        db: SQLiteDatabase,
        table: String,
        column: String,
        definition: String
    ) {
        val exists = db.rawQuery(
            "PRAGMA table_info($table)",
            null
        ).use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true
                    break
                }
            }
            found
        }

        if (!exists) {
            db.execSQL(
                "ALTER TABLE $table ADD COLUMN " +
                    "$column $definition"
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
                available_credit_minor INTEGER,
                available_credit_observed_at_epoch_ms INTEGER,
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
                related_debt_event_id INTEGER,
                wallet_balance_after_minor INTEGER,
                funding_account_key TEXT,
                funding_account_id TEXT,
                resulting_balance_observation_id INTEGER,
                balance_link_confidence TEXT,
                balance_link_reason TEXT,
                available_credit_after_minor INTEGER,
                repayment_completion TEXT,
                funding_link_confidence TEXT,
                funding_link_reason TEXT,
                funding_link_user_decision TEXT,
                balance_link_user_decision TEXT,
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

    private fun createFinancialUnderstandingTables(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS financial_entities (
                entity_key TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                provider_key TEXT,
                account_type_hint TEXT NOT NULL,
                ownership_hint TEXT NOT NULL,
                nature_hint TEXT NOT NULL,
                confirmed_account_id TEXT,
                first_seen_epoch_ms INTEGER NOT NULL,
                last_seen_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS account_balance_observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                observation_key TEXT NOT NULL UNIQUE,
                entity_key TEXT NOT NULL,
                account_id TEXT,
                balance_minor INTEGER NOT NULL,
                confidence TEXT NOT NULL,
                observed_at_epoch_ms INTEGER NOT NULL,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS financial_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_key TEXT NOT NULL UNIQUE,
                event_type TEXT NOT NULL,
                amount_minor INTEGER NOT NULL,
                fee_minor INTEGER NOT NULL DEFAULT 0,
                source_entity_key TEXT,
                destination_entity_key TEXT,
                confidence TEXT NOT NULL,
                status TEXT NOT NULL,
                linked_transaction_id INTEGER,
                occurred_at_epoch_ms INTEGER NOT NULL,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createTransactionExternalReferencesTable(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transaction_external_references (
                transaction_id INTEGER NOT NULL,
                reference_value TEXT NOT NULL,
                reference_label TEXT,
                source_kind TEXT,
                observed_at_epoch_ms INTEGER NOT NULL,
                PRIMARY KEY(transaction_id, reference_value)
            )
            """.trimIndent()
        )
    }

    private fun createLiabilityPaymentDecisionsTable(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS
                liability_payment_decisions (
                    transaction_id INTEGER PRIMARY KEY,
                    decision TEXT NOT NULL,
                    debt_account_id TEXT,
                    created_at_epoch_ms INTEGER NOT NULL
                )
            """.trimIndent()
        )
    }

    private fun createInvestmentTables(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS investment_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_key TEXT NOT NULL UNIQUE,
                event_type TEXT NOT NULL,
                instrument_key TEXT NOT NULL,
                instrument_label TEXT NOT NULL,
                quantity_microunits INTEGER NOT NULL,
                value_minor INTEGER,
                linked_transaction_id INTEGER,
                settlement_user_decision TEXT,
                settlement_confidence TEXT,
                settlement_reason TEXT,
                primary_reference TEXT,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                occurred_at_epoch_ms INTEGER NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS investment_event_references (
                investment_event_id INTEGER NOT NULL,
                reference_value TEXT NOT NULL,
                PRIMARY KEY(
                    investment_event_id,
                    reference_value
                )
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS investment_positions (
                instrument_key TEXT PRIMARY KEY,
                instrument_label TEXT NOT NULL,
                net_observed_quantity_microunits INTEGER NOT NULL DEFAULT 0,
                quality TEXT NOT NULL DEFAULT 'OBSERVED_EVENTS_ONLY',
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createCycleProtectionTable(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cycle_protections (
                cycle_start_transaction_id INTEGER PRIMARY KEY,
                amount_minor INTEGER NOT NULL DEFAULT 0,
                label TEXT NOT NULL DEFAULT 'Protected money',
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createPaydaySnapshotTables(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payday_snapshots (
                id TEXT PRIMARY KEY,
                salary_transaction_id INTEGER NOT NULL UNIQUE,
                salary_epoch_ms INTEGER NOT NULL,
                salary_account_id TEXT,
                salary_amount_minor INTEGER NOT NULL,
                liquid_carryover_minor INTEGER NOT NULL DEFAULT 0,
                protected_savings_minor INTEGER NOT NULL DEFAULT 0,
                debt_outstanding_minor INTEGER NOT NULL DEFAULT 0,
                unknown_liquid_account_count INTEGER NOT NULL DEFAULT 0,
                known_liquid_account_count INTEGER NOT NULL DEFAULT 0,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payday_snapshot_accounts (
                snapshot_id TEXT NOT NULL,
                account_id TEXT NOT NULL,
                account_name TEXT NOT NULL,
                account_type TEXT NOT NULL,
                balance_minor INTEGER,
                balance_quality TEXT NOT NULL,
                reference_epoch_ms INTEGER,
                spendability TEXT NOT NULL,
                PRIMARY KEY(snapshot_id, account_id)
            )
            """.trimIndent()
        )
    }

    private fun createEntityEnrichmentCacheTable(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entity_enrichment_cache (
                descriptor_key TEXT PRIMARY KEY,
                canonical_name TEXT NOT NULL,
                domain TEXT,
                logo_url TEXT,
                category_hint TEXT,
                confidence TEXT NOT NULL,
                source TEXT NOT NULL,
                fetched_at_epoch_ms INTEGER NOT NULL,
                expires_at_epoch_ms INTEGER NOT NULL
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
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_money_accounts_identifier ON money_accounts(identifier) WHERE identifier IS NOT NULL AND identifier != ''"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_pay_cycles_start ON pay_cycles(start_epoch_ms DESC)"
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
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_debt_events_funding_account ON debt_events(funding_account_id, occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_debt_events_result_balance ON debt_events(resulting_balance_observation_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_financial_entities_provider ON financial_entities(provider_key)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_balance_observations_account_time ON account_balance_observations(account_id, observed_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_balance_observations_entity_time ON account_balance_observations(entity_key, observed_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_financial_events_status_time ON financial_events(status, occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_financial_events_linked_transaction ON financial_events(linked_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_entity_enrichment_expiry ON entity_enrichment_cache(expires_at_epoch_ms)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transaction_external_reference_value ON transaction_external_references(reference_value)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transaction_external_reference_transaction ON transaction_external_references(transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_liability_payment_decision_debt ON liability_payment_decisions(debt_account_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_investment_events_instrument_time ON investment_events(instrument_key, occurred_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_investment_events_linked_transaction ON investment_events(linked_transaction_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_investment_event_reference_value ON investment_event_references(reference_value)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_payday_snapshots_salary_time ON payday_snapshots(salary_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_payday_snapshot_accounts_snapshot ON payday_snapshot_accounts(snapshot_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_cycle_protections_updated ON cycle_protections(updated_at_epoch_ms DESC)"
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
        private const val DATABASE_VERSION = 27
    }
}
