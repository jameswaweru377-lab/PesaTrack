package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.data.local.StatementArchiveDb

/**
 * The separate, private statement journal is evidence, not canonical cash.
 * Keep its schema and backup contract separate from the SMS ledger.
 *
 * All attached reads/writes must be performed on the calling ledger transaction
 * connection. Attachment alone does not reserve a pooled SQLite connection.
 */
internal object StatementBackupArchive {
    const val SCHEMA_VERSION = StatementArchiveDb.VERSION
    private const val FILE_NAME = "pesatrack_statement_activity.db"
    private const val ALIAS = "statement_backup"

    // Restore parents before their foreign-key children.
    val legacyTablesInRestoreOrder = listOf(
        "statement_batches",
        "statement_activity_lines",
        "statement_category_labels",
        "statement_category_rules"
    )
    val verifiedTablesInRestoreOrder = legacyTablesInRestoreOrder + listOf(
        "statement_sources_v2",
        "statement_legs_v2",
        "statement_category_labels_v2"
    )
    val tablesInRestoreOrder = verifiedTablesInRestoreOrder + listOf(
        "statement_limited_sources_v1", "statement_limited_legs_v1"
    )

    // Keep this explicit: a statement schema upgrade requires a reviewed
    // archive-format migration, never best-effort column dropping.
    val legacyColumns = mapOf(
        "statement_batches" to setOf(
            "id", "account_id", "recorded_at", "row_count",
            "movement_count", "informational_count", "paid_in_minor",
            "paid_out_minor", "totals_verified"
        ),
        "statement_activity_lines" to setOf(
            "batch_id", "line_number", "page_number", "row_number",
            "reference", "occurred_at", "description", "paid_in_minor",
            "withdrawn_minor", "balance_minor", "source_cells_json"
        ),
        "statement_category_labels" to setOf(
            "batch_id", "page_number", "row_number", "category_id"
        ),
        "statement_category_rules" to setOf(
            "exact_merchant_key", "category_id"
        )
    )
    val verifiedColumns = legacyColumns + mapOf(
        "statement_sources_v2" to setOf(
            "id", "account_id", "provider_key", "account_identity_hash",
            "currency", "document_format", "imported_at", "row_count",
            "first_at", "last_at", "money_in_minor", "money_out_minor", "totals_verified"
        ),
        "statement_legs_v2" to setOf(
            "source_id", "leg_number", "page_number", "row_number",
            "occurred_at", "description", "reference", "movement_minor",
            "running_balance_minor", "source_cells_json"
        ),
        "statement_category_labels_v2" to setOf(
            "source_id", "leg_number", "category_id"
        )
    )
    val columns = verifiedColumns + mapOf(
        "statement_limited_sources_v1" to setOf(
            "id", "account_id", "provider_key", "account_identity_hash",
            "currency", "document_format", "imported_at", "row_count",
            "first_at", "last_at", "money_in_minor", "money_out_minor", "assurance"
        ),
        "statement_limited_legs_v1" to setOf(
            "source_id", "leg_number", "page_number", "row_number",
            "occurred_at", "description", "reference", "movement_minor",
            "running_balance_minor", "source_cells_json"
        )
    )

    /** Legacy payloads contain precisely the old four tables, never inferred missing rows. */
    fun backupColumnsFor(version: Int): Map<String, Set<String>> = when (version) {
        3, 4 -> legacyColumns
        5 -> verifiedColumns
        SCHEMA_VERSION -> columns
        else -> throw IllegalArgumentException("Unsupported saved statement history schema.")
    }

    fun prepare(context: Context) {
        // Open through the authoritative statement helper; this runs its
        // Lossless legacy migration before an ATTACH or restore.
        StatementActivityStore(context).batches()
    }

    fun hasExistingData(context: Context): Boolean {
        val archive = StatementActivityStore(context)
        if (archive.batches().isNotEmpty() || archive.categoryRules().isNotEmpty()) return true
        val db = StatementArchiveDb(context)
        return try {
            db.readableDatabase.rawQuery(
                "SELECT 1 FROM statement_sources_v2 UNION ALL " +
                    "SELECT 1 FROM statement_limited_sources_v1 LIMIT 1", null
            ).use { it.moveToFirst() }
        } finally { db.close() }
    }

    fun attach(context: Context, db: SQLiteDatabase) {
        val path = context.getDatabasePath(FILE_NAME).absolutePath
        db.execSQL("ATTACH DATABASE ? AS $ALIAS", arrayOf(path))
    }

    fun detach(db: SQLiteDatabase) {
        db.execSQL("DETACH DATABASE $ALIAS")
    }

    fun qualified(table: String): String {
        require(table in columns) { "Unexpected statement backup table." }
        return "$ALIAS.$table"
    }

    fun assertSchema(db: SQLiteDatabase) {
        val version = db.rawQuery("PRAGMA $ALIAS.user_version", null).use {
            require(it.moveToFirst())
            it.getInt(0)
        }
        require(version == SCHEMA_VERSION) {
            "Statement history schema requires a reviewed migration before backup or restore."
        }
        columns.forEach { (table, expected) ->
            val actual = db.rawQuery("PRAGMA $ALIAS.table_info($table)", null).use { rows ->
                buildSet {
                    val name = rows.getColumnIndexOrThrow("name")
                    while (rows.moveToNext()) add(rows.getString(name))
                }
            }
            require(actual == expected) { "Statement history schema does not match the backup contract." }
        }
    }

    /** Cross-file SQLite commits are crash-atomic only with rollback journals. */
    fun requireAtomicRestoreJournals(db: SQLiteDatabase) {
        for (schema in listOf("main", ALIAS)) {
            val mode = db.rawQuery("PRAGMA $schema.journal_mode", null).use {
                require(it.moveToFirst())
                it.getString(0).lowercase()
            }
            require(mode in setOf("delete", "truncate", "persist")) {
                "A safe statement restore requires SQLite rollback journaling ($schema is $mode). No financial records were changed."
            }
        }
    }

    fun containsAnyRows(db: SQLiteDatabase): Boolean =
        tablesInRestoreOrder.any { table ->
            db.rawQuery("SELECT 1 FROM ${qualified(table)} LIMIT 1", null)
                .use { it.moveToFirst() }
        }

    fun requireNoOrphans(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA $ALIAS.foreign_key_check", null).use {
            require(!it.moveToFirst()) { "Statement archive contains broken batch references." }
        }
    }
}
