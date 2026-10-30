package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale
import kotlin.math.abs

/**
 * Read-only cash-event proof for future cross-product reporting. A cash leg can
 * have several independently sourced observations, but a transfer has exactly
 * two opposite cash legs of different confirmed owned accounts. The latter is
 * only a **review candidate** until cash-leg roles are independently verified.
 *
 * Neither this model nor its result is permission to write to Activity, apply
 * categories, suppress a statement row, or publish combined economic totals.
 */
object StatementCrossSourceEventPolicy {
    data class TransferCandidate(
        val outgoingIds: Set<String>,
        val incomingIds: Set<String>
    )

    data class Result(
        val sameLegGroups: List<Set<String>>,
        val transferCandidates: List<TransferCandidate>,
        val ambiguousIds: Set<String>,
        val unlinkedIds: Set<String>
    )

    private data class BucketKey(val reference: String, val currency: String, val magnitude: Long)
    private data class LegKey(val accountId: String, val movement: Long)
    private data class AccountReferenceKey(
        val reference: String, val accountId: String,
        val currency: String, val incoming: Boolean
    )
    private data class Leg(val rows: List<StatementEvidenceCorrelationPolicy.Observation>) {
        val ids: Set<String> = rows.map { it.id }.toSet()
        val sample = rows.first()
    }

    private const val MAX_OBSERVATIONS = 20_000
    private const val MAX_BUCKET_ROWS = 16
    private const val MAX_TIME_DIFFERENCE_MS = 48L * 60 * 60 * 1_000
    private val stableReference = Regex("[A-Z0-9]{8,48}")

    private fun stableReference(value: String?): String? = value?.trim()
        ?.uppercase(Locale.ROOT)?.takeIf { stableReference.matches(it) && it.any(Char::isDigit) }

    private fun timesWithin(rows: List<StatementEvidenceCorrelationPolicy.Observation>): Boolean =
        rows.maxOf { it.occurredAt } - rows.minOf { it.occurredAt } <= MAX_TIME_DIFFERENCE_MS

    fun reconcile(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        ownedAccountIds: Set<String>
    ): Result {
        require(observations.size <= MAX_OBSERVATIONS)
        require(observations.map { it.id }.toSet().size == observations.size)
        require(observations.all {
            it.id.isNotBlank() && it.sourceId.isNotBlank() && it.accountId.isNotBlank() &&
                it.currency.isNotBlank() && it.occurredAt > 0L &&
                it.movementMinor != 0L && it.movementMinor != Long.MIN_VALUE
        })

        val referenced = observations.filter {
            it.ownershipConfirmed && it.accountId in ownedAccountIds &&
                stableReference(it.reference) != null
        }
        val suppressedBuckets = referenced.filter { it.roleProtected }.map {
            BucketKey(stableReference(it.reference)!!,
                it.currency.uppercase(Locale.ROOT), abs(it.movementMinor))
        }.toSet()
        val eligible = referenced.filterNot { it.roleProtected }

        // Compare competing PDF *documents* by reference before amount bucketing.
        // An original principal and fee in one document are distinct legitimate
        // legs; different PDFs that disagree about one reference are not proof.
        val amountConflicts = mutableSetOf<String>()
        val byAccountReference = referenced.filter {
            it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY
        }.groupBy {
            AccountReferenceKey(stableReference(it.reference)!!, it.accountId,
                it.currency.uppercase(Locale.ROOT), it.movementMinor > 0L)
        }
        for (rows in byAccountReference.values) {
            if (rows.size > 64) {
                amountConflicts.addAll(rows.map { it.id })
                continue
            }
            val distinctLegs = rows.groupBy { it.movementMinor }.values.toList()
            for (i in distinctLegs.indices) for (j in i + 1 until distinctLegs.size) {
                val first = distinctLegs[i]
                val second = distinctLegs[j]
                if (first.map { it.sourceId }.toSet()
                        .intersect(second.map { it.sourceId }.toSet()).isEmpty() &&
                    first.any { a -> second.any { b ->
                        (if (a.occurredAt >= b.occurredAt)
                            a.occurredAt - b.occurredAt
                         else b.occurredAt - a.occurredAt) <= MAX_TIME_DIFFERENCE_MS
                    } }) {
                    amountConflicts.addAll(first.map { it.id })
                    amountConflicts.addAll(second.map { it.id })
                }
            }
        }
        val allByBucket = referenced.groupBy {
            BucketKey(stableReference(it.reference)!!,
                it.currency.uppercase(Locale.ROOT), abs(it.movementMinor))
        }
        val byReference = eligible.groupBy {
            BucketKey(stableReference(it.reference)!!,
                it.currency.uppercase(Locale.ROOT), abs(it.movementMinor))
        }
        val sameLegs = mutableListOf<Set<String>>()
        val transfers = mutableListOf<TransferCandidate>()
        val ambiguous = mutableSetOf<String>()
        val proven = mutableSetOf<String>()

        for ((key, bucket) in byReference.toSortedMap(compareBy<BucketKey> { it.reference }
                .thenBy { it.currency }.thenBy { it.magnitude })) {
            if (key in suppressedBuckets || bucket.any { it.id in amountConflicts }) {
                ambiguous.addAll(allByBucket.getValue(key).map { it.id })
                continue
            }
            if (bucket.size > MAX_BUCKET_ROWS) {
                ambiguous.addAll(bucket.map { it.id })
                continue
            }
            val legs = mutableListOf<Leg>()
            var badCluster = false
            for (rows in bucket.groupBy { LegKey(it.accountId, it.movementMinor) }.values) {
                val activityRows = rows.filter {
                    it.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY
                }
                val documents = rows.filter {
                    it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY
                }
                val safeCopies = activityRows.size <= 1 &&
                    documents.map { it.sourceId }.distinct().size == documents.size &&
                    documents.map { it.occurredAt }.distinct().size <= 1 &&
                    (rows.size == 1 || documents.all { it.sourceTotalsVerified }) &&
                    timesWithin(rows)
                if (!safeCopies) {
                    badCluster = true
                    ambiguous.addAll(rows.map { it.id })
                    continue
                }
                val leg = Leg(rows)
                legs.add(leg)
            }
            if (badCluster) {
                // No transfer can be selected when one account has conflicting
                // copies, even if the other account has only a single row.
                ambiguous.addAll(bucket.map { it.id })
                continue
            }
            if (legs.size > 2) {
                ambiguous.addAll(bucket.map { it.id })
                continue
            }
            for (leg in legs.filter { it.ids.size > 1 }) {
                sameLegs.add(leg.ids)
                proven.addAll(leg.ids)
            }
            if (legs.size != 2 ||
                legs[0].sample.accountId == legs[1].sample.accountId ||
                (legs[0].sample.movementMinor > 0L) ==
                    (legs[1].sample.movementMinor > 0L) ||
                !timesWithin(bucket) ||
                bucket.any {
                    it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY &&
                        !it.sourceTotalsVerified
                }) continue

            val outgoing = legs.first { it.sample.movementMinor < 0L }
            val incoming = legs.first { it.sample.movementMinor > 0L }
            transfers.add(TransferCandidate(outgoing.ids, incoming.ids))
            proven.addAll(outgoing.ids)
            proven.addAll(incoming.ids)
        }
        return Result(
            sameLegGroups = sameLegs.sortedBy { it.minOrNull().orEmpty() },
            transferCandidates = transfers.sortedBy { it.outgoingIds.minOrNull().orEmpty() },
            ambiguousIds = ambiguous,
            unlinkedIds = observations.map { it.id }.toSet() - proven
        )
    }
}
