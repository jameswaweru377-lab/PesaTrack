package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore

/**
 * The separate, private statement journal is evidence, not canonical cash.
 * Keep its schema and backup contract separate from the SMS ledger.
 *
 * All attached reads/writes must be performed on the calling ledger transaction
 * connection. Attachment alone does not reserve a pooled SQLite connection.
 */
internal object StatementBackupArchive {
    const val SCHEMA_VERSION = 3
    private const val FILE_NAME = "pesatrack_statement_activity.db"
    private const val ALIAS = "statement_backup"

    // Restore parents before their foreign-key children.
    val tablesInRestoreOrder = listOf(
        "statement_batches",
        "statement_activity_lines",
        "statement_category_labels",
        "statement_category_rules"
    )

    // Keep this explicit: a statement schema upgrade requires a reviewed
    // archive-format migration, never best-effort column dropping.
    val columns = mapOf(
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

    fun prepare(context: Context) {
        // Open through the authoritative statement helper; this runs its
        // lossless v1/v2 -> v3 migration before an ATTACH or restore.
        StatementActivityStore(context).batches()
    }

    fun hasExistingData(context: Context): Boolean {
        val archive = StatementActivityStore(context)
        return archive.batches().isNotEmpty() || archive.categoryRules().isNotEmpty()
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
