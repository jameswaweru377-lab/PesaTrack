package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale
import kotlin.math.abs

/** Review-only relationships. Neither a candidate nor a decision authorizes ledger writes. */
object TransferReconciliationPolicy {
    enum class Basis { SHARED_REFERENCE, ACCOUNT_ROUTE, REFERENCE_AND_ROUTE }
    class Pair(
        val outgoing: List<StatementEvidenceCorrelationPolicy.Observation>,
        val incoming: List<StatementEvidenceCorrelationPolicy.Observation>,
        val basis: Basis, val fingerprint: String
    ) {
        val rows get() = outgoing + incoming
        val principalMinor get() = -outgoing.first().movementMinor
        val currency get() = outgoing.first().currency.uppercase(Locale.ROOT)
        override fun toString(): String = "[private transfer candidate]"
    }
    class UnmatchedRoute(val row: StatementEvidenceCorrelationPolicy.Observation, val peerAccountId: String) {
        override fun toString(): String = "[private unmatched route]"
    }
    data class Result(val pairs: List<Pair>, val unmatchedRoutes: List<UnmatchedRoute>,
                      val ambiguousObservationCount: Int, val withheldObservationCount: Int)
    private data class Key(val currency: String, val amount: Long)
    private data class Edge(val out: String, val incoming: String)
    private class Leg(val rows: List<StatementEvidenceCorrelationPolicy.Observation>) {
        val id = rows.minOf { it.id }
        val sample = rows.first()
    }
    private const val WINDOW = 48L * 60 * 60 * 1000
    private val protectedNarration = Regex(
        "\\b(loans?|repay(?:ment|ments|paid)?|repaid|written[ -]+off|interest|fees?|charges?|charged|commissions?|excise[ -]+duty|chargebacks?|levy|levies|overdrafts?|disburs(?:e(?:d|ments?)?|ing)|write[ -]?offs?|reversal|reversed|refund(?:ed)?|fuliza|m[ -]?shwari|savings|locked|pending|failed|declined|cancelled|clearances?|cleared|settlements?|settled)\\b",
        RegexOption.IGNORE_CASE)

    /** Conservative role guard shared by all statement transfer-review paths. */
    fun isProtectedNarration(text: String?): Boolean =
        text != null && (text.length > 4096 || protectedNarration.containsMatchIn(text))

    fun reconcile(observations: List<StatementEvidenceCorrelationPolicy.Observation>,
                  ownership: Map<String, StatementMatchDecisionPolicy.Ownership>,
                  descriptions: Map<String, String>, startInclusive: Long, endExclusive: Long): Result {
        require(endExclusive > startInclusive && observations.size <= 20_000 && ownership.size <= 200)
        require(observations.map { it.id }.distinct().size == observations.size)
        val assessed = observations.map { row ->
            val text = descriptions[row.id]
            if (isProtectedNarration(text))
                row.copy(roleProtected = true) else row
        }
        val valid = assessed.filter(::structurallyValid)
        val owned = StatementOwnedAccountUniquenessPolicy.unambiguous(ownership.filterValues {
            it.revision > 0 && it.type in setOf("BANK", "MOBILE_WALLET") && it.name.isNotBlank()
        })
        val proof = StatementCrossSourceEventPolicy.reconcile(valid, owned.keys)
        val routes = valid.mapNotNull { row -> descriptions[row.id]?.let { TransferRoutePolicy.find(row, it, owned) } }
            .associateBy { it.observationId }
        val legs = legs(valid, proof)
        val rowToLeg = legs.values.flatMap { leg -> leg.rows.map { it.id to leg.id } }.toMap()
        val ambiguous = proof.ambiguousIds.toMutableSet()
        val edges = linkedMapOf<Edge, Basis>()
        for (candidate in proof.transferCandidates) {
            val out = candidate.outgoingIds.mapNotNull(rowToLeg::get).distinct().singleOrNull() ?: continue
            val incoming = candidate.incomingIds.mapNotNull(rowToLeg::get).distinct().singleOrNull() ?: continue
            edges[Edge(out, incoming)] = Basis.SHARED_REFERENCE
        }
        addRoutes(legs, routes, edges, ambiguous)
        val degrees = edges.keys.flatMap { listOf(it.out, it.incoming) }.groupingBy { it }.eachCount()
        degrees.filterValues { it > 1 }.keys.forEach { id -> ambiguous.addAll(legs.getValue(id).rows.map { it.id }) }
        val pairs = edges.mapNotNull { (edge, basis) ->
            val out = legs.getValue(edge.out)
            val incoming = legs.getValue(edge.incoming)
            val rows = out.rows + incoming.rows
            if (rows.size > 32 || rows.any { it.id in ambiguous || !eligible(it, owned.keys) } ||
                degrees[edge.out] != 1 || degrees[edge.incoming] != 1 ||
                !compatibleRoutes(out, incoming, routes) ||
                rows.none { it.occurredAt in startInclusive until endExclusive }) return@mapNotNull null
            val hash = fingerprint(rows, owned, descriptions, basis)
            Pair(out.rows, incoming.rows, basis, hash)
        }.sortedWith(compareByDescending<Pair> { it.rows.maxOf { row -> row.occurredAt } }.thenBy { it.fingerprint })
        val matchedIds = pairs.flatMap { it.rows }.mapTo(mutableSetOf()) { it.id }
        val unmatched = legs.values.mapNotNull { leg ->
            if (leg.rows.any { it.id in matchedIds || it.id in ambiguous || !eligible(it, owned.keys) }) return@mapNotNull null
            val routeRows = leg.rows.filter { it.id in routes && it.occurredAt in startInclusive until endExclusive }
            val peer = routeRows.map { routes.getValue(it.id).peerAccountId }.distinct().singleOrNull() ?: return@mapNotNull null
            UnmatchedRoute(routeRows.minBy { it.id }, peer)
        }.sortedByDescending { it.row.occurredAt }
        return Result(pairs, unmatched, valid.count { it.id in ambiguous && it.occurredAt in startInclusive until endExclusive },
            assessed.count { it.occurredAt in startInclusive until endExclusive &&
                (!structurallyValid(it) || !eligible(it, owned.keys)) })
    }

    private fun legs(rows: List<StatementEvidenceCorrelationPolicy.Observation>,
                     proof: StatementCrossSourceEventPolicy.Result): Map<String, Leg> {
        val byId = rows.associateBy { it.id }
        // The proof also carries same-leg groups within a transfer candidate.
        val groups = proof.sameLegGroups + proof.transferCandidates.flatMap { listOf(it.outgoingIds, it.incomingIds) }
        val membership = groups.flatMap { group -> group.map { it to group.min() } }.toMap()
        return rows.groupBy { membership[it.id] ?: it.id }.values.map { group ->
            Leg(group.map { byId.getValue(it.id) }.sortedBy { it.id })
        }.associateBy { it.id }
    }

    private fun addRoutes(legs: Map<String, Leg>, routes: Map<String, TransferRoutePolicy.Route>,
                          edges: MutableMap<Edge, Basis>, ambiguous: MutableSet<String>) {
        val buckets = legs.values.groupBy { Key(it.sample.currency.uppercase(Locale.ROOT), abs(it.sample.movementMinor)) }
        for (bucket in buckets.values) {
            if (bucket.none { leg -> leg.rows.any { it.id in routes } }) continue
            // Fail closed instead of unbounded quadratic matching in repetitive amounts.
            if (bucket.size > 64) { ambiguous.addAll(bucket.flatMap { it.rows }.map { it.id }); continue }
            for (a in bucket) for (b in bucket) {
                if (a.sample.movementMinor >= 0 || b.sample.movementMinor <= 0 || a.sample.accountId == b.sample.accountId) continue
                val rows = a.rows + b.rows
                if (rows.maxOf { it.occurredAt } - rows.minOf { it.occurredAt } > WINDOW) continue
                val linked = a.rows.any { routes[it.id]?.peerAccountId == b.sample.accountId } ||
                    b.rows.any { routes[it.id]?.peerAccountId == a.sample.accountId }
                if (!linked) continue
                val edge = Edge(a.id, b.id)
                edges[edge] = if (edges[edge] == Basis.SHARED_REFERENCE) Basis.REFERENCE_AND_ROUTE else Basis.ACCOUNT_ROUTE
            }
        }
    }

    private fun compatibleRoutes(a: Leg, b: Leg, routes: Map<String, TransferRoutePolicy.Route>): Boolean =
        a.rows.all { routes[it.id]?.peerAccountId?.let { peer -> peer == b.sample.accountId } != false } &&
            b.rows.all { routes[it.id]?.peerAccountId?.let { peer -> peer == a.sample.accountId } != false }

    private fun structurallyValid(row: StatementEvidenceCorrelationPolicy.Observation): Boolean =
        row.id.isNotBlank() && row.sourceId.isNotBlank() && row.accountId.isNotBlank() &&
            row.currency.matches(Regex("[A-Za-z]{3}")) && row.occurredAt > 0 &&
            row.movementMinor != 0L && row.movementMinor != Long.MIN_VALUE

    private fun eligible(row: StatementEvidenceCorrelationPolicy.Observation, owned: Set<String>): Boolean =
        row.accountId in owned && row.ownershipConfirmed && !row.roleProtected &&
            (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY || row.sourceTotalsVerified)

    private fun fingerprint(rows: List<StatementEvidenceCorrelationPolicy.Observation>,
                            ownership: Map<String, StatementMatchDecisionPolicy.Ownership>,
                            descriptions: Map<String, String>, basis: Basis): String {
        val base = StatementMatchDecisionPolicy.fingerprint(rows, ownership)
        // Bind all available narration, including text which no longer yields a route.
        val narration = rows.sortedBy { it.id }.joinToString("") {
            descriptions[it.id]?.let { text -> "1" + TransferRoutePolicy.digest(text) } ?: "0"
        }
        return TransferRoutePolicy.digest("transfer-reconciliation-v1|$basis|$base|$narration")
    }
}
