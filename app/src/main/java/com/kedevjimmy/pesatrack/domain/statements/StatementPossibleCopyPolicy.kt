package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale

/** Weak, read-only clues for records that may describe the same account movement. */
object StatementPossibleCopyPolicy {
    data class Suggestion(val rows: List<StatementEvidenceCorrelationPolicy.Observation>) {
        override fun toString(): String = "[private possible copy]"
    }
    private data class Bucket(val account: String, val currency: String, val movement: Long)

    fun suggest(observations: List<StatementEvidenceCorrelationPolicy.Observation>, excludedIds: Set<String>,
                startInclusive: Long, endExclusive: Long): List<Suggestion> {
        require(endExclusive > startInclusive && observations.size <= 20_000)
        require(observations.map { it.id }.distinct().size == observations.size)
        fun valid(row: StatementEvidenceCorrelationPolicy.Observation): Boolean =
            row.id.isNotBlank() && row.sourceId.isNotBlank() && row.accountId.isNotBlank() &&
                row.currency.isNotBlank() && row.occurredAt > 0 && row.movementMinor != 0L && row.movementMinor != Long.MIN_VALUE
        fun eligible(row: StatementEvidenceCorrelationPolicy.Observation): Boolean =
            row.id !in excludedIds && row.ownershipConfirmed && !row.roleProtected &&
                (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY || row.sourceTotalsVerified)
        val buckets = observations.filter(::valid).groupBy {
            Bucket(it.accountId, it.currency.uppercase(Locale.ROOT), it.movementMinor)
        }
        val results = mutableListOf<Suggestion>()
        for (bucket in buckets.values) {
            val rows = bucket.sortedWith(compareBy({ it.occurredAt }, { it.id }))
            for (row in rows) {
                if (!eligible(row)) continue
                // Count every rival, even one that cannot itself be suggested.
                val nearby = StatementPossibleTransferPolicy.nearbyRange(rows, row.occurredAt)
                if (nearby.isEmpty() || nearby.last - nearby.first != 1) continue
                val first = rows[nearby.first]
                val second = rows[nearby.last]
                val separateActivityRows = first.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY &&
                    second.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY
                if (first.id != row.id || !eligible(second) ||
                    (first.sourceId == second.sourceId && !separateActivityRows)) continue
                val reverse = StatementPossibleTransferPolicy.nearbyRange(rows, second.occurredAt)
                if (reverse.isEmpty() || reverse.first != nearby.first || reverse.last != nearby.last ||
                    (first.occurredAt !in startInclusive until endExclusive &&
                        second.occurredAt !in startInclusive until endExclusive)) continue
                results += Suggestion(listOf(first, second))
            }
        }
        return results.sortedWith(compareByDescending<Suggestion> { it.rows.maxOf { row -> row.occurredAt } }
            .thenBy { it.rows.first().id })
    }
}
