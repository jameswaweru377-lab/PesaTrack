package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityProof
import com.kedevjimmy.pesatrack.domain.model.AccountType
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.json.JSONArray

/**
 * A separate, app-private PDF Statement Activity journal.
 *
 * There is deliberately NO reference to TransactionStore, transactions,
 * TransactionFingerprint, SMS ingestion, account balances or ledger update
 * events. Persisting a statement here cannot create an income/expense entry.
 * Original PDFs, passwords and raw text outside verified cells are not stored.
 */
class StatementActivityStore(context: Context) {
    private val app = context.applicationContext

    data class Batch(
        val id: String,
        val accountId: String,
        val recordedAt: Long,
        val rows: Int,
        val movements: Int,
        val informational: Int,
        val paidInMinor: Long,
        val paidOutMinor: Long,
        val totalsIndependentlyVerified: Boolean
    )

    data class SaveResult(val added: Boolean, val batchId: String)

    private class Db(context: Context) :
        SQLiteOpenHelper(context, "pesatrack_statement_activity.db", null, 3) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE statement_batches (
                    id TEXT PRIMARY KEY NOT NULL,
                    account_id TEXT NOT NULL,
                    recorded_at INTEGER NOT NULL,
                    row_count INTEGER NOT NULL,
                    movement_count INTEGER NOT NULL,
                    informational_count INTEGER NOT NULL,
                    paid_in_minor INTEGER NOT NULL,
                    paid_out_minor INTEGER NOT NULL,
                    totals_verified INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE statement_activity_lines (
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
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX statement_lines_time ON statement_activity_lines(batch_id, occurred_at DESC)")
            createCategoryTables(db)
        }

        private fun createCategoryTables(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS statement_category_labels (
                    batch_id TEXT NOT NULL REFERENCES statement_batches(id) ON DELETE CASCADE,
                    page_number INTEGER NOT NULL,
                    row_number INTEGER NOT NULL,
                    category_id TEXT NOT NULL,
                    PRIMARY KEY (batch_id, page_number, row_number)
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS statement_category_rules (
                    exact_merchant_key TEXT PRIMARY KEY NOT NULL,
                    category_id TEXT NOT NULL
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            require(oldVersion in 1..2 && newVersion == 3) {
                "Statement Activity requires a lossless migration."
            }
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE statement_activity_lines ADD COLUMN source_cells_json TEXT NOT NULL DEFAULT '[]'")
            }
            if (oldVersion < 3) createCategoryTables(db)
        }
    }

    private fun fingerprint(accountId: String, lines: List<StatementActivityLine>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun feed(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        feed(accountId)
        lines.forEach { line ->
            feed(line.page.toString()); feed(line.row.toString())
            feed(line.reference); feed(line.occurredAt.toString())
            feed(line.description)
            feed(line.paidInMinor.toString())
            feed(line.withdrawnMinor.toString())
            feed(line.balanceMinor.toString())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun requireOwnedWallet(accountId: String) {
        require(EverydayMoneyStore(app).ownedAccounts().any {
            it.id == accountId && it.type == AccountType.MOBILE_WALLET
        }) { "Select your confirmed M-Pesa wallet to save its statement." }
    }

    /**
     * One document is saved atomically, after explicit user's screen review.
     * Duplicate document+wallet saves are idempotent; individual receipts may
     * repeat because they are not unique cash-leg identities.
     */
    fun save(
        accountId: String,
        lines: List<StatementActivityLine>,
        proof: StatementActivityProof
    ): SaveResult {
        requireOwnedWallet(accountId)
        require(lines.size in 1..5_000 && lines.size == proof.receiptRows)
        require(lines.count { !it.informational } == proof.cashRows)
        require(lines.zipWithNext().all { (a,b) ->
            a.occurredAt >= b.occurredAt &&
                (a.page < b.page || (a.page == b.page && a.row < b.row))
        }) { "Source row order changed after verification." }
        val paid = lines.fold(0L) { sum, row -> Math.addExact(sum, row.paidInMinor) }
        val withdrawn = lines.fold(0L) { sum, row -> Math.addExact(sum, row.withdrawnMinor) }
        val batchId = fingerprint(accountId, lines)
        val helper = Db(app)
        try {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val exists = db.rawQuery(
                    "SELECT 1 FROM statement_batches WHERE id=? LIMIT 1",
                    arrayOf(batchId)
                ).use { it.moveToFirst() }
                if (exists) {
                    db.setTransactionSuccessful()
                    return SaveResult(false, batchId)
                }
                db.insertOrThrow("statement_batches", null, ContentValues().apply {
                    put("id", batchId)
                    put("account_id", accountId)
                    put("recorded_at", System.currentTimeMillis())
                    put("row_count", lines.size)
                    put("movement_count", proof.cashRows)
                    put("informational_count", lines.size - proof.cashRows)
                    put("paid_in_minor", paid)
                    put("paid_out_minor", withdrawn)
                    put("totals_verified", if (proof.totalsVerified) 1 else 0)
                })
                lines.forEachIndexed { i, line ->
                    db.insertOrThrow("statement_activity_lines", null, ContentValues().apply {
                        put("batch_id", batchId)
                        put("line_number", i)
                        put("page_number", line.page)
                        put("row_number", line.row)
                        put("reference", line.reference)
                        put("occurred_at", line.occurredAt)
                        put("description", line.description)
                        put("paid_in_minor", line.paidInMinor)
                        put("withdrawn_minor", line.withdrawnMinor)
                        put("balance_minor", line.balanceMinor)
                        put("source_cells_json", JSONArray(line.sourceCells).toString())
                    })
                }
                db.setTransactionSuccessful()
                return SaveResult(true, batchId)
            } finally {
                db.endTransaction()
            }
        } finally {
            helper.close()
        }
    }

    fun batches(): List<Batch> {
        val helper = Db(app)
        try {
            return helper.readableDatabase.rawQuery(
                "SELECT id, account_id, recorded_at, row_count, movement_count, " +
                    "informational_count, paid_in_minor, paid_out_minor, totals_verified " +
                    "FROM statement_batches ORDER BY recorded_at DESC", null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(Batch(
                        c.getString(0), c.getString(1), c.getLong(2), c.getInt(3),
                        c.getInt(4), c.getInt(5), c.getLong(6), c.getLong(7),
                        c.getInt(8) == 1
                    ))
                }
            }
        } finally { helper.close() }
    }

    fun lines(batchId: String): List<StatementActivityLine> {
        require(batchId.matches(Regex("[a-f0-9]{64}")))
        val helper = Db(app)
        try {
            return helper.readableDatabase.rawQuery(
                "SELECT page_number, row_number, reference, occurred_at, description, " +
                    "paid_in_minor, withdrawn_minor, balance_minor, source_cells_json " +
                    "FROM statement_activity_lines WHERE batch_id=? ORDER BY line_number",
                arrayOf(batchId)
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(StatementActivityLine(
                        c.getInt(0), c.getInt(1), c.getString(2), c.getLong(3),
                        c.getString(4), c.getLong(5), c.getLong(6), c.getLong(7),
                        JSONArray(c.getString(8)).let { cells ->
                            (0 until cells.length()).map { i -> cells.getString(i) }
                        }
                    ))
                }
            }
        } finally { helper.close() }
    }

    /**
     * Small read-only monthly summaries across all saved PDFs, calculated in
     * SQLite so a 112-page document does not have to be loaded for each chart.
     * Nairobi has UTC+03:00 for modern M-Pesa statement periods.
     * Individual source batches remain separate for overlap-safe selection.
     */
    fun sourceMonths(): List<com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport.SourceMonth> {
        val helper = Db(app)
        try {
            val sql = """
                SELECT b.id, b.account_id, b.recorded_at, b.totals_verified,
                    s.month_key, COUNT(*),
                    SUM(CASE WHEN s.paid_in_minor = 0 AND s.withdrawn_minor = 0 THEN 1 ELSE 0 END),
                    SUM(s.paid_in_minor), SUM(s.withdrawn_minor),
                    MIN(s.occurred_at), MAX(s.occurred_at)
                FROM statement_batches b
                JOIN (
                    SELECT batch_id, occurred_at, paid_in_minor, withdrawn_minor,
                        strftime('%Y-%m', occurred_at / 1000, 'unixepoch', '+3 hours') AS month_key
                    FROM statement_activity_lines
                ) s ON s.batch_id = b.id
                GROUP BY b.id, s.month_key
                ORDER BY s.month_key DESC, b.recorded_at DESC
            """.trimIndent()
            return helper.readableDatabase.rawQuery(sql, null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val key = cursor.getString(4)
                        val count = cursor.getInt(5)
                        val informational = cursor.getInt(6)
                        val usage = com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage.Month(
                            year = key.substring(0, 4).toInt(),
                            month = key.substring(5, 7).toInt(),
                            sourceRows = count,
                            financialMovements = count - informational,
                            informationalRows = informational,
                            paidInMinor = cursor.getLong(7),
                            paidOutMinor = cursor.getLong(8)
                        )
                        add(com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport.SourceMonth(
                            accountId = cursor.getString(1),
                            batchId = cursor.getString(0),
                            recordedAt = cursor.getLong(2),
                            independentlyVerified = cursor.getInt(3) == 1,
                            usage = usage,
                            firstTransactionAt = cursor.getLong(9),
                            lastTransactionAt = cursor.getLong(10)
                        ))
                    }
                }
            }
        } finally { helper.close() }
    }


    /** Saved per-source user corrections (never touches Activity, budgets or balances). */
    fun categoriesFor(batchId: String): Map<Pair<Int, Int>, String> {
        require(batchId.matches(Regex("[a-f0-9]{64}")))
        val helper = Db(app)
        try {
            return helper.readableDatabase.rawQuery(
                "SELECT page_number, row_number, category_id FROM statement_category_labels WHERE batch_id=?",
                arrayOf(batchId)
            ).use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) put(
                        cursor.getInt(0) to cursor.getInt(1), cursor.getString(2)
                    )
                }
            }
        } finally { helper.close() }
    }

    /** Explicitly remembered merchant categories for all future statement PDFs. */
    fun categoryRules(): Map<String, String> {
        val helper = Db(app)
        try {
            return helper.readableDatabase.rawQuery(
                "SELECT exact_merchant_key, category_id FROM statement_category_rules", null
            ).use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
                }
            }
        } finally { helper.close() }
    }

    /**
     * Verify the exact source row exists before saving its correction. Remember
     * across statements is opt-in and never mutates canonical SMS transactions.
     */
    fun categorize(
        batchId: String, page: Int, row: Int, categoryId: String,
        rememberMerchant: Boolean
    ) {
        require(batchId.matches(Regex("[a-f0-9]{64}")))
        require(page > 0 && row > 0)
        val category = requireNotNull(CategoryStore.get(app).find(categoryId)) {
            "Choose a valid PesaTrack category."
        }
        require(category.kind != CategoryKind.SYSTEM)
        val helper = Db(app)
        try {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val sourceLine = db.rawQuery(
                    """SELECT reference, occurred_at, description, paid_in_minor,
                              withdrawn_minor, balance_minor
                       FROM statement_activity_lines
                       WHERE batch_id=? AND page_number=? AND row_number=? LIMIT 1""",
                    arrayOf(batchId, page.toString(), row.toString())
                ).use { cursor ->
                    require(cursor.moveToFirst()) { "Statement row no longer exists." }
                    StatementActivityLine(
                        page = page, row = row,
                        reference = cursor.getString(0),
                        occurredAt = cursor.getLong(1),
                        description = cursor.getString(2),
                        paidInMinor = cursor.getLong(3),
                        withdrawnMinor = cursor.getLong(4),
                        balanceMinor = cursor.getLong(5)
                    )
                }
                require(
                    com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
                        .canAssignCategory(
                            sourceLine, category.id,
                            CategoryStore.get(app).all().associate { it.id to it.kind.name }
                        )
                ) {
                    "This transaction has protected financial evidence, or the chosen category conflicts with its direction."
                }
                val key = com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
                    .merchantKey(sourceLine.description)
                require(!rememberMerchant || (key != null && category.kind == CategoryKind.EXPENSE)) {
                    "Only an identifiable merchant expense may be remembered."
                }
                db.insertWithOnConflict(
                    "statement_category_labels", null, ContentValues().apply {
                        put("batch_id", batchId)
                        put("page_number", page)
                        put("row_number", row)
                        put("category_id", category.id)
                    }, SQLiteDatabase.CONFLICT_REPLACE
                )
                if (rememberMerchant) {
                    db.insertWithOnConflict(
                        "statement_category_rules", null, ContentValues().apply {
                            put("exact_merchant_key", requireNotNull(key))
                            put("category_id", category.id)
                        }, SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        } finally { helper.close() }
    }

}
