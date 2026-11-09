package com.kedevjimmy.pesatrack.data.local

import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.rules.ReversalEvidence
import com.kedevjimmy.pesatrack.domain.rules.ReversalMatchingPolicy
import com.kedevjimmy.pesatrack.domain.rules.ReversalOriginal
import java.util.Locale

/** References alone are not proof: never exclude an unrelated cash leg. */
internal object ReversalAccountingReader {
    fun reversedTransactionIds(db: SQLiteDatabase): Set<Long> {
        // Two linear scans avoid an expression join on the receipt-code index.
        val originals = mutableMapOf<String, MutableList<ReversalOriginal>>()
        db.rawQuery(
            """
            SELECT id, transaction_code, amount_minor, account_id, occurred_at_epoch_ms
            FROM transactions
            WHERE transaction_code IS NOT NULL AND type != 'REVERSAL'
              AND review_state != 'EXCLUDED'
            """.trimIndent(), null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val code = cursor.getString(1)
                originals.getOrPut(code.trim().uppercase(Locale.ROOT)) { mutableListOf() }
                    .add(ReversalOriginal(cursor.getLong(0), code, cursor.getLong(2),
                        if (cursor.isNull(3)) null else cursor.getString(3), cursor.getLong(4)))
            }
        }
        val reversedIds = mutableSetOf<Long>()
        db.rawQuery(
            """
            SELECT reversal_of_code, amount_minor, account_id, occurred_at_epoch_ms
            FROM transactions
            WHERE type = 'REVERSAL' AND reversal_of_code IS NOT NULL
              AND review_state != 'EXCLUDED'
            """.trimIndent(), null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val code = cursor.getString(0)
                val evidence = ReversalEvidence(code, cursor.getLong(1),
                    if (cursor.isNull(2)) null else cursor.getString(2), cursor.getLong(3))
                ReversalMatchingPolicy.match(evidence,
                    originals[code.trim().uppercase(Locale.ROOT)].orEmpty())
                    ?.let(reversedIds::add)
            }
        }
        return reversedIds
    }
}
