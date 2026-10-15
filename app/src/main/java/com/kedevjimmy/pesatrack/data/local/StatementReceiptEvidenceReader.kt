package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementReceiptEvidence
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import java.util.Locale

/**
 * Read-only, receipt-scoped lookup against canonical SMS evidence.
 * It does not write a transaction, link a ledger, update a balance, or persist
 * a cross-source association. Only necessary receipts are queried, in chunks.
 */
class StatementReceiptEvidenceReader(context: Context) {
    private val app = context.applicationContext

    fun read(lines: List<StatementActivityLine>): Map<String, List<StatementReceiptEvidence.ActivityReceipt>> {
        val codes = lines.map { it.reference.uppercase(Locale.ROOT) }
            .filter { it.matches(Regex("[A-Z0-9]{10}")) }.distinct()
        if (codes.isEmpty()) return emptyMap()
        val db = PesaTrackDatabase.get(app).readableDatabase
        val result = mutableMapOf<String, MutableList<StatementReceiptEvidence.ActivityReceipt>>()
        codes.chunked(400).forEach { chunk ->
            val marks = chunk.joinToString(",") { "?" }
            db.rawQuery(
                """SELECT transaction_code, account_id, amount_minor,
                   occurred_at_epoch_ms, direction, counterparty,
                   counterparty_phone, review_state, relation_type, reversal_of_code
                   FROM transactions WHERE transaction_code IN ($marks)""",
                chunk.toTypedArray()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val code = cursor.getString(0)?.uppercase(Locale.ROOT) ?: continue
                    val candidate = StatementReceiptEvidence.ActivityReceipt(
                        code = code,
                        accountId = cursor.getString(1),
                        amountMinor = cursor.getLong(2),
                        occurredAtEpochMs = cursor.getLong(3),
                        direction = cursor.getString(4),
                        counterparty = cursor.getString(5),
                        phone = cursor.getString(6),
                        excluded = cursor.getString(7) == "EXCLUDED",
                        structurallyLinked = !cursor.isNull(8) || !cursor.isNull(9)
                    )
                    result.getOrPut(code) { mutableListOf() }.add(candidate)
                }
            }
        }
        return result
    }
}
