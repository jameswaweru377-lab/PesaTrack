package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy

/** Bounded local narration/category projection. Never emits source text into diagnostics. */
internal class TransferStatementEvidenceReader(context: Context) {
    private val app = context.applicationContext
    class Details(val descriptions: Map<String, String>, val protectedIds: Set<String>) {
        override fun toString(): String = "[private transfer statement evidence]"
    }

    fun read(rows: List<StatementEvidenceCorrelationPolicy.Observation>): Details {
        require(rows.size <= 20_000)
        if (rows.isEmpty()) return Details(emptyMap(), emptySet())
        val wanted = rows.mapTo(mutableSetOf()) { it.id }
        val descriptions = mutableMapOf<String, String>()
        val protected = mutableSetOf<String>()
        val helper = StatementArchiveDb(app)
        try {
            for ((source, selected) in rows.groupBy { it.source }) {
                if (source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY) continue
                for (chunk in selected.map { it.sourceId }.distinct().chunked(400)) {
                    val marks = chunk.joinToString(",") { "?" }
                    val bank = source == StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT
                    val query = if (bank) """SELECT 'bank:' || l.source_id || ':' || l.leg_number,
                        l.description,c.category_id FROM statement_legs_v2 l
                        LEFT JOIN statement_category_labels_v2 c ON c.source_id=l.source_id AND c.leg_number=l.leg_number
                        WHERE l.source_id IN ($marks) AND l.occurred_at BETWEEN ? AND ? LIMIT 20001""" else
                        """SELECT 'mpesa:' || l.batch_id || ':' || l.page_number || ':' || l.row_number,
                        l.description,c.category_id FROM statement_activity_lines l
                        LEFT JOIN statement_category_labels c ON c.batch_id=l.batch_id AND c.page_number=l.page_number AND c.row_number=l.row_number
                        WHERE l.batch_id IN ($marks) AND l.occurred_at BETWEEN ? AND ? LIMIT 20001"""
                    val args = chunk.toTypedArray() + arrayOf(selected.minOf { it.occurredAt }.toString(), selected.maxOf { it.occurredAt }.toString())
                    helper.readableDatabase.rawQuery(query, args).use { cursor ->
                        var count = 0
                        while (cursor.moveToNext()) {
                            require(++count <= 20_000) { "Evidence range exceeds safe review limit." }
                            val id = cursor.getString(0)
                            if (id !in wanted) continue
                            descriptions[id] = cursor.getString(1).orEmpty()
                            // A persisted user category is authority, irrespective of its label.
                            if (!cursor.isNull(2)) protected.add(id)
                        }
                    }
                }
            }
        } finally { helper.close() }
        // Missing source evidence is not permission to bypass role checks.
        protected.addAll(wanted - descriptions.keys)
        return Details(descriptions, protected)
    }
}
