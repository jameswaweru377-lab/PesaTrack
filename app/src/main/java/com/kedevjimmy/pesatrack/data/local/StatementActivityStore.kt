package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxVerificationReport
import com.kedevjimmy.pesatrack.domain.model.AccountType
import java.nio.ByteBuffer
import java.security.MessageDigest

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
        SQLiteOpenHelper(context, "pesatrack_statement_activity.db", null, 1) {
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
                    PRIMARY KEY(batch_id, line_number)
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX statement_lines_time ON statement_activity_lines(batch_id, occurred_at DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Statement Activity schema upgrade requires an explicit lossless migration.")
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
        proof: PdfBoxVerificationReport
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
                    "paid_in_minor, withdrawn_minor, balance_minor " +
                    "FROM statement_activity_lines WHERE batch_id=? ORDER BY line_number",
                arrayOf(batchId)
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(StatementActivityLine(
                        c.getInt(0), c.getInt(1), c.getString(2), c.getLong(3),
                        c.getString(4), c.getLong(5), c.getLong(6), c.getLong(7)
                    ))
                }
            }
        } finally { helper.close() }
    }
}
