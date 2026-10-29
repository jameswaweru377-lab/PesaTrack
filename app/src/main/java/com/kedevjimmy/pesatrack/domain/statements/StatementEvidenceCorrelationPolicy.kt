package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale

/**
 * Phase 3 read-only evidence correlation. The output is a proposed relationship,
 * not permission to mutate Activity, deduplicate a report, or classify income.
 * In particular, an Equity narration without a structured reference is not proof.
 */
object StatementEvidenceCorrelationPolicy {
    enum class Source { ACTIVITY, MPESA_STATEMENT, BANK_STATEMENT }
    enum class LinkKind { SAME_LEG, OWNED_TRANSFER }

    data class Observation(
        val id: String,
        val source: Source,
        val sourceId: String,
        val accountId: String,
        val currency: String,
        val occurredAt: Long,
        val movementMinor: Long,
        val reference: String?,
        val ownershipConfirmed: Boolean,
        val roleProtected: Boolean = false
    )

    data class Link(val firstId: String, val secondId: String, val kind: LinkKind) {
        val ids: Set<String> get() = setOf(firstId, secondId)
    }

    data class Result(
        val uniqueLinks: List<Link>,
        val ambiguousIds: Set<String>,
        val unlinkedIds: Set<String>,
        val missingReferenceIds: Set<String>
    )

    private const val MAX_OBSERVATIONS = 20_000
    private const val MAX_REFERENCE_GROUP = 16
    private const val MAX_DELTA_MS = 48L * 60 * 60 * 1000
    private val safeReference = Regex("[A-Z0-9]{8,48}")

    private fun normalizedReference(reference: String?): String? =
        reference?.trim()?.uppercase(Locale.ROOT)?.takeIf(safeReference::matches)

    /**
     * Never outputs combined money values. Each edge requires independently
     * owned accounts, a stable reference and a unique one-to-one cash leg.
     * Ambiguous relationships are left open for later explicit review.
     */
    fun reconcile(observations: List<Observation>, ownedAccountIds: Set<String>): Result {
        require(observations.size <= MAX_OBSERVATIONS)
        require(observations.map { it.id }.distinct().size == observations.size)
        require(observations.all {
            it.id.isNotBlank() && it.sourceId.isNotBlank() && it.accountId.isNotBlank() &&
                it.currency.isNotBlank() && it.occurredAt > 0L &&
                it.movementMinor != 0L && it.movementMinor != Long.MIN_VALUE
        })
        val missingReference = observations.filter { normalizedReference(it.reference) == null }
            .map { it.id }.toSet()
        val eligible = observations.filter {
            !it.roleProtected && it.ownershipConfirmed && it.accountId in ownedAccountIds &&
                normalizedReference(it.reference) != null
        }
        // Keyed bucketing bounds comparison work: never scan all records pairwise.
        val groups = eligible.groupBy { observation ->
            Triple(normalizedReference(observation.reference)!!,
                observation.currency.uppercase(Locale.ROOT),
                kotlin.math.abs(observation.movementMinor))
        }
        val neighbors = mutableMapOf<String, MutableSet<String>>()
        val edgeKinds = mutableMapOf<Pair<String, String>, LinkKind>()
        val oversizedGroups = mutableSetOf<String>()
        for (group in groups.values) {
            if (group.size > MAX_REFERENCE_GROUP) {
                oversizedGroups.addAll(group.map { it.id })
                continue
            }
            for (i in group.indices) for (j in (i + 1) until group.size) {
                val a = group[i]
                val b = group[j]
                if (a.source == b.source && a.sourceId == b.sourceId) continue
                val delta = if (a.occurredAt >= b.occurredAt)
                    a.occurredAt - b.occurredAt else b.occurredAt - a.occurredAt
                if (delta > MAX_DELTA_MS) continue
                val kind = when {
                    a.accountId == b.accountId &&
                        (a.movementMinor > 0L) == (b.movementMinor > 0L) -> LinkKind.SAME_LEG
                    a.accountId != b.accountId &&
                        (a.movementMinor > 0L) != (b.movementMinor > 0L) -> LinkKind.OWNED_TRANSFER
                    else -> null
                } ?: continue
                val pair = if (a.id < b.id) a.id to b.id else b.id to a.id
                neighbors.getOrPut(a.id) { mutableSetOf() }.add(b.id)
                neighbors.getOrPut(b.id) { mutableSetOf() }.add(a.id)
                edgeKinds[pair] = kind
            }
        }
        val links = edgeKinds.entries.mapNotNull { (ids, kind) ->
            if (neighbors[ids.first]?.size == 1 && neighbors[ids.second]?.size == 1)
                Link(ids.first, ids.second, kind) else null
        }.sortedWith(compareBy<Link> { it.firstId }.thenBy { it.secondId })
        val linked = links.flatMap { it.ids }.toSet()
        val ambiguous = oversizedGroups + neighbors.keys.filter { it !in linked }
        return Result(links, ambiguous, observations.map { it.id }.toSet() - linked,
            missingReference)
    }
}
