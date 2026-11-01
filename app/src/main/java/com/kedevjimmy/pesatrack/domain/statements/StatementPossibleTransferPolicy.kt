package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale
import kotlin.math.abs

/** Weak, read-only leads. Amount and date alone never prove a transfer or authorize reporting changes. */
object StatementPossibleTransferPolicy {
    data class Suggestion(
        val outgoing: StatementEvidenceCorrelationPolicy.Observation,
        val incoming: StatementEvidenceCorrelationPolicy.Observation
    ) {
        override fun toString(): String = "[private possible transfer]"
    }

    private data class Bucket(val currency: String, val magnitude: Long)
    private const val MAX_ROWS = 20_000
    private const val MAX_DIFFERENCE = 48L * 60L * 60L * 1_000L

    /** Indices of every observation within the inclusive time window, in a sorted list. */
    private fun nearbyRange(rows: List<StatementEvidenceCorrelationPolicy.Observation>, time: Long): IntRange {
        val min = if (time <= MAX_DIFFERENCE) 1L else time - MAX_DIFFERENCE
        val max = if (time > Long.MAX_VALUE - MAX_DIFFERENCE) Long.MAX_VALUE else time + MAX_DIFFERENCE
        fun boundary(value: Long, afterEqual: Boolean): Int {
            var low = 0
            var high = rows.size
            while (low < high) {
                val middle = low + (high - low) / 2
                val current = rows[middle].occurredAt
                if (current < value || (afterEqual && current == value)) low = middle + 1 else high = middle
            }
            return low
        }
        return boundary(min, false) until boundary(max, true)
    }

    fun suggest(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        proof: StatementCrossSourceEventPolicy.Result,
        startInclusive: Long,
        endExclusive: Long
    ): List<Suggestion> {
        require(endExclusive > startInclusive)
        require(observations.size <= MAX_ROWS)
        require(observations.map { it.id }.distinct().size == observations.size)
        val alreadyReviewed = proof.ambiguousIds + proof.sameLegGroups.flatten() +
            proof.transferCandidates.flatMap { it.outgoingIds + it.incomingIds }
        fun valid(row: StatementEvidenceCorrelationPolicy.Observation): Boolean =
            row.id.isNotBlank() && row.sourceId.isNotBlank() && row.accountId.isNotBlank() &&
                row.currency.isNotBlank() && row.occurredAt > 0 && row.movementMinor != 0L &&
                row.movementMinor != Long.MIN_VALUE
        fun eligible(row: StatementEvidenceCorrelationPolicy.Observation): Boolean =
            row.id !in alreadyReviewed && row.ownershipConfirmed && !row.roleProtected &&
                (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY || row.sourceTotalsVerified)
        val buckets = observations.filter(::valid).groupBy {
            Bucket(it.currency.uppercase(Locale.ROOT), abs(it.movementMinor))
        }
        val suggestions = mutableListOf<Suggestion>()
        for (bucket in buckets.values) {
            val outgoing = bucket.filter { it.movementMinor < 0 }.sortedBy { it.occurredAt }
            val incoming = bucket.filter { it.movementMinor > 0 }.sortedBy { it.occurredAt }
            for (out in outgoing) {
                if (!eligible(out)) continue
                // Count all plausible rivals, including records that cannot themselves be suggested.
                // Binary search keeps common amounts bounded without ignoring the rest of the year.
                val matches = nearbyRange(incoming, out.occurredAt)
                if (matches.isEmpty() || matches.first != matches.last) continue
                val candidate = incoming[matches.first]
                val rivals = nearbyRange(outgoing, candidate.occurredAt)
                if (!eligible(candidate) || candidate.accountId == out.accountId ||
                    rivals.isEmpty() || rivals.first != rivals.last ||
                    (out.occurredAt !in startInclusive until endExclusive &&
                        candidate.occurredAt !in startInclusive until endExclusive)) continue
                suggestions += Suggestion(out, candidate)
            }
        }
        return suggestions.sortedWith(compareByDescending<Suggestion> { maxOf(it.outgoing.occurredAt, it.incoming.occurredAt) }
            .thenBy { it.outgoing.id }.thenBy { it.incoming.id })
    }
}
