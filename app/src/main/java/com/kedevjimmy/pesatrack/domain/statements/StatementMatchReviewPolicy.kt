package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale

/** A presentation-only view of existing evidence; never changes or suppresses a record. */
object StatementMatchReviewPolicy {
    data class EvidenceWindow(val startInclusive: Long, val endExclusive: Long)

    /** Include a potential match and its own 48-hour competitors across period edges. */
    fun evidenceWindow(startInclusive: Long, endExclusive: Long): EvidenceWindow {
        require(endExclusive > startInclusive)
        val margin = 96L * 60L * 60L * 1000L
        val start = if (startInclusive <= margin + 1L) 1L else startInclusive - margin
        return EvidenceWindow(start, Math.addExact(endExclusive, margin))
    }
    data class Transfer(
        val outgoing: List<StatementEvidenceCorrelationPolicy.Observation>,
        val incoming: List<StatementEvidenceCorrelationPolicy.Observation>
    )
    data class Review(
        val transfers: List<Transfer>,
        val sameMovements: List<List<StatementEvidenceCorrelationPolicy.Observation>>,
        val ambiguousCount: Int
    )

    fun project(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        proof: StatementCrossSourceEventPolicy.Result,
        startInclusive: Long = Long.MIN_VALUE,
        endExclusive: Long = Long.MAX_VALUE
    ): Review {
        require(endExclusive > startInclusive)
        require(observations.map { it.id }.distinct().size == observations.size)
        val byId = observations.associateBy { it.id }
        fun resolve(ids: Set<String>): List<StatementEvidenceCorrelationPolicy.Observation>? {
            if (ids.isEmpty() || ids.any { it in proof.ambiguousIds }) return null
            val resolved = ids.mapNotNull(byId::get)
            return resolved.takeIf { it.size == ids.size && it.none { row ->
                row.roleProtected || !row.ownershipConfirmed || row.occurredAt <= 0 ||
                    row.movementMinor == 0L || row.movementMinor == Long.MIN_VALUE ||
                    (row.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY && !row.sourceTotalsVerified)
            } }
                ?.sortedWith(compareBy({ it.occurredAt }, { it.id }))
        }
        fun sharedReference(rows: List<StatementEvidenceCorrelationPolicy.Observation>): Boolean {
            val references = rows.map { it.reference?.trim()?.uppercase(Locale.ROOT) }
            val reference = references.firstOrNull() ?: return false
            return reference.matches(Regex("[A-Z0-9]{8,48}")) && reference.any(Char::isDigit) &&
                references.distinct().size == 1
        }
        val transfers = proof.transferCandidates.mapNotNull { candidate ->
            if (candidate.outgoingIds.intersect(candidate.incomingIds).isNotEmpty()) return@mapNotNull null
            val out = resolve(candidate.outgoingIds) ?: return@mapNotNull null
            val incoming = resolve(candidate.incomingIds) ?: return@mapNotNull null
            val magnitude = out.first().movementMinor
            if (magnitude >= 0 || magnitude == Long.MIN_VALUE ||
                out.any { it.accountId != out.first().accountId || it.movementMinor != magnitude } ||
                incoming.any { it.accountId != incoming.first().accountId || it.movementMinor != -magnitude } ||
                out.first().accountId == incoming.first().accountId ||
                !sharedReference(out + incoming) ||
                (out + incoming).map { it.currency.uppercase(Locale.ROOT) }.distinct().size != 1) return@mapNotNull null
            if ((out + incoming).none { it.occurredAt in startInclusive until endExclusive }) return@mapNotNull null
            Transfer(out, incoming)
        }.sortedByDescending { it.outgoing.maxOf { row -> row.occurredAt } }
        val transferIds = transfers.flatMap { it.outgoing + it.incoming }.mapTo(mutableSetOf()) { it.id }
        val sameMovements = proof.sameLegGroups.mapNotNull { ids ->
            if (ids.size < 2 || ids.any { it in transferIds }) return@mapNotNull null
            val rows = resolve(ids) ?: return@mapNotNull null
            if (rows.map { it.accountId }.distinct().size != 1 ||
                rows.map { it.movementMinor }.distinct().size != 1 ||
                !sharedReference(rows) ||
                rows.map { it.currency.uppercase(Locale.ROOT) }.distinct().size != 1) return@mapNotNull null
            rows.takeIf { group -> group.any { it.occurredAt in startInclusive until endExclusive } }
        }.sortedByDescending { it.maxOf { row -> row.occurredAt } }
        return Review(transfers, sameMovements, proof.ambiguousIds.count {
            byId[it]?.occurredAt?.let { at -> at in startInclusive until endExclusive } == true
        })
    }
}
