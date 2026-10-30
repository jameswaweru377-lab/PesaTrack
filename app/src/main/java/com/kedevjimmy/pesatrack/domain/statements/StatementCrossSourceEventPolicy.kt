package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale
import kotlin.math.abs

/**
 * Read-only cash-event proof for future cross-product reporting. A cash leg can
 * have several independently sourced observations, but a transfer has exactly
 * two opposite cash legs of different confirmed owned accounts.
 *
 * Neither this model nor its result is permission to write to Activity, apply
 * categories, suppress a statement row, or publish combined economic totals.
 */
object StatementCrossSourceEventPolicy {
    data class OwnedTransfer(
        val outgoingIds: Set<String>,
        val incomingIds: Set<String>
    )

    data class Result(
        val sameLegGroups: List<Set<String>>,
        val ownedTransfers: List<OwnedTransfer>,
        val ambiguousIds: Set<String>,
        val unlinkedIds: Set<String>
    )

    private data class BucketKey(val reference: String, val currency: String, val magnitude: Long)
    private data class LegKey(val accountId: String, val movement: Long)
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

        val eligible = observations.filter {
            it.ownershipConfirmed && it.accountId in ownedAccountIds &&
                !it.roleProtected && stableReference(it.reference) != null
        }
        val byReference = eligible.groupBy {
            BucketKey(stableReference(it.reference)!!,
                it.currency.uppercase(Locale.ROOT), abs(it.movementMinor))
        }
        val sameLegs = mutableListOf<Set<String>>()
        val transfers = mutableListOf<OwnedTransfer>()
        val ambiguous = mutableSetOf<String>()
        val proven = mutableSetOf<String>()

        for ((_, bucket) in byReference.toSortedMap(compareBy<BucketKey> { it.reference }
                .thenBy { it.currency }.thenBy { it.magnitude })) {
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
            transfers.add(OwnedTransfer(outgoing.ids, incoming.ids))
            proven.addAll(outgoing.ids)
            proven.addAll(incoming.ids)
        }
        return Result(
            sameLegGroups = sameLegs.sortedBy { it.minOrNull().orEmpty() },
            ownedTransfers = transfers.sortedBy { it.outgoingIds.minOrNull().orEmpty() },
            ambiguousIds = ambiguous,
            unlinkedIds = observations.map { it.id }.toSet() - proven
        )
    }
}
