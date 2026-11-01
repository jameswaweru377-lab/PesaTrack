package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale
import kotlin.math.abs

/** Weak, read-only leads. Amount and date alone never prove a transfer or authorize reporting changes. */
object StatementPossibleTransferPolicy {
    data class Suggestion(
        val outgoing: StatementEvidenceCorrelationPolicy.Observation,
        val incoming: StatementEvidenceCorrelationPolicy.Observation
    )

    private data class Bucket(val currency: String, val magnitude: Long)
    private const val MAX_ROWS = 20_000
    private const val MAX_BUCKET_ROWS = 256
    private const val MAX_DIFFERENCE = 48L * 60L * 60L * 1_000L

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
            // Busy amounts are too ambiguous for a reliable review lead; retain every original record.
            if (bucket.size > MAX_BUCKET_ROWS) continue
            val outgoing = bucket.filter { it.movementMinor < 0 }
            val incoming = bucket.filter { it.movementMinor > 0 }
            fun nearby(a: StatementEvidenceCorrelationPolicy.Observation, b: StatementEvidenceCorrelationPolicy.Observation): Boolean =
                if (a.occurredAt >= b.occurredAt) a.occurredAt - b.occurredAt <= MAX_DIFFERENCE
                else b.occurredAt - a.occurredAt <= MAX_DIFFERENCE
            for (out in outgoing) {
                if (!eligible(out)) continue
                // Count all plausible rivals, including records that cannot themselves be suggested.
                val matches = incoming.filter { nearby(out, it) }
                val candidate = matches.singleOrNull() ?: continue
                if (!eligible(candidate) || candidate.accountId == out.accountId ||
                    outgoing.count { nearby(it, candidate) } != 1 ||
                    (out.occurredAt !in startInclusive until endExclusive &&
                        candidate.occurredAt !in startInclusive until endExclusive)) continue
                suggestions += Suggestion(out, candidate)
            }
        }
        return suggestions.sortedWith(compareByDescending<Suggestion> { maxOf(it.outgoing.occurredAt, it.incoming.occurredAt) }
            .thenBy { it.outgoing.id }.thenBy { it.incoming.id })
    }
}
