package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage
import java.util.Locale

/**
 * Read-only coverage of imported statement cash legs, never an Activity ledger or
 * income/spending classifier. Only a corroborated *same account cash leg* can be
 * folded across PDFs. Suspected or conflicting overlaps withhold combined totals.
 */
object StatementSourceCoveragePolicy {
    enum class Status { SINGLE_SOURCE, VERIFIED_REPEAT, COMPLEMENTARY, NEEDS_REVIEW }

    data class MonthCoverage(
        val accountId: String,
        val currency: String,
        val monthKey: String,
        val sourceCount: Int,
        val observedLegCount: Int,
        /** Remaining observations after strictly proven repeat copies are removed. */
        val remainingObservedLegCount: Int,
        val confirmedRepeatCopies: Int,
        /** Rows involved in unresolved candidates, not a count of confirmed duplicates. */
        val unresolvedCandidateCount: Int,
        val status: Status,
        /** Observed source cash flow only; null while potential overlap is unresolved. */
        val safeMoneyInMinor: Long?,
        val safeMoneyOutMinor: Long?
    )

    private data class PeriodKey(val account: String, val currency: String, val month: String)
    private data class ExactKey(val reference: String, val timestamp: Long, val movement: Long)
    private data class Cluster(val rows: List<StatementEvidenceCorrelationPolicy.Observation>) {
        val example: StatementEvidenceCorrelationPolicy.Observation get() = rows.first()
        val sourceIds: Set<String> get() = rows.map { it.sourceId }.toSet()
        val identifiers: Set<String> get() = rows.map { it.id }.toSet()
    }

    private const val MAX_OBSERVATIONS = 20_000
    private const val MAX_COMPARISON_GROUP = 64
    private const val NEAR_TIME_MS = 5L * 60 * 1_000
    private val stableReference = Regex("[A-Z0-9]{8,48}")

    private fun referenceOf(value: String?): String? = value?.trim()
        ?.uppercase(Locale.ROOT)?.takeIf { stableReference.matches(it) }

    /**
     * The caller must supply only statement archive observations, never Activity.
     * Works per confirmed account/currency/Nairobi month and never persists data.
     * No month without imported evidence is manufactured as a zero balance.
     */
    fun analyze(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>
    ): List<MonthCoverage> {
        require(observations.size <= MAX_OBSERVATIONS)
        require(observations.map { it.id }.distinct().size == observations.size)
        require(observations.all {
            it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY &&
                it.id.isNotBlank() && it.sourceId.isNotBlank() && it.accountId.isNotBlank() &&
                it.currency.isNotBlank() && it.occurredAt > 0L &&
                it.movementMinor != 0L && it.movementMinor != Long.MIN_VALUE
        })
        return observations.groupBy {
            PeriodKey(it.accountId, it.currency.uppercase(Locale.ROOT),
                StatementMonthlyUsage.monthKey(it.occurredAt))
        }.map { (key, entries) -> cover(key, entries.sortedBy { it.id }) }
            .sortedWith(compareBy<MonthCoverage> { it.accountId }
                .thenBy { it.currency }.thenByDescending { it.monthKey })
    }

    private fun cover(
        period: PeriodKey,
        rows: List<StatementEvidenceCorrelationPolicy.Observation>
    ): MonthCoverage {
        val problematic = mutableSetOf<String>()
        val accountedFor = mutableSetOf<String>()
        val clusters = mutableListOf<Cluster>()
        var repeatCopies = 0

        val exact = rows.filter {
            it.ownershipConfirmed && !it.roleProtected && referenceOf(it.reference) != null
        }.groupBy { ExactKey(referenceOf(it.reference)!!, it.occurredAt, it.movementMinor) }

        for (row in rows) {
            if (!accountedFor.add(row.id)) continue
            if (!row.ownershipConfirmed || row.roleProtected || !row.sourceTotalsVerified)
                problematic.add(row.id)
            val ref = referenceOf(row.reference)
            val matches = if (ref == null || !row.ownershipConfirmed || row.roleProtected) {
                listOf(row)
            } else exact.getValue(ExactKey(ref, row.occurredAt, row.movementMinor))

            // A repeated row within one PDF is NOT permission to discard a cash
            // leg, even if another PDF also contains that same receipt.
            if (matches.size > 1 && matches.map { it.sourceId }.distinct().size != matches.size) {
                problematic.addAll(matches.map { it.id })
                matches.forEach { matching ->
                    accountedFor.add(matching.id)
                    clusters.add(Cluster(listOf(matching)))
                }
            } else {
                accountedFor.addAll(matches.map { it.id })
                clusters.add(Cluster(matches))
                repeatCopies += matches.size - 1
            }
        }

        fun review(first: Cluster, second: Cluster) {
            problematic.addAll(first.identifiers)
            problematic.addAll(second.identifiers)
        }
        fun independentSources(a: Cluster, b: Cluster): Boolean =
            a.sourceIds.intersect(b.sourceIds).isEmpty()
        fun within(a: Long, b: Long, window: Long): Boolean =
            if (a >= b) a - b <= window else b - a <= window

        // One receipt may legitimately identify both a principal and a fee.
        // Different amounts are only suspect when *different* PDFs contain no
        // common observation of those two legs (not when both PDFs show both).
        clusters.filter { referenceOf(it.example.reference) != null }
            .groupBy { referenceOf(it.example.reference)!! }
            .values.forEach { group ->
                if (group.size > MAX_COMPARISON_GROUP) {
                    problematic.addAll(group.flatMap { it.identifiers })
                } else for (i in group.indices) for (j in i + 1 until group.size) {
                    val a = group[i]; val b = group[j]
                    if (independentSources(a, b)) review(a, b)
                }
            }

        // A narration with no usable reference is *not* a duplicate key.
        // Flag equal-money/near-time observations from different PDFs when
        // either side lacks a reliable reference, rather than guessing.
        clusters.groupBy { it.example.movementMinor }.values.forEach { group ->
            if (group.size > MAX_COMPARISON_GROUP) {
                if (group.flatMap { it.sourceIds }.distinct().size > 1)
                    problematic.addAll(group.flatMap { it.identifiers })
            } else for (i in group.indices) for (j in i + 1 until group.size) {
                val a = group[i]; val b = group[j]
                if (independentSources(a, b) && within(a.example.occurredAt,
                        b.example.occurredAt, NEAR_TIME_MS) &&
                    (referenceOf(a.example.reference) == null ||
                        referenceOf(b.example.reference) == null)) review(a, b)
            }
        }

        val sourceCount = rows.map { it.sourceId }.distinct().size
        // A bank's narration-only row has no stable identity. Even when two
        // documents show it on different dates, observed row dates are not
        // proof that their declared statement coverage does not overlap.
        // Never publish an authoritative multi-PDF cash sum for those rows.
        if (sourceCount > 1) problematic.addAll(rows.filter {
            referenceOf(it.reference) == null
        }.map { it.id })
        val status = when {
            problematic.isNotEmpty() -> Status.NEEDS_REVIEW
            sourceCount == 1 -> Status.SINGLE_SOURCE
            repeatCopies > 0 -> Status.VERIFIED_REPEAT
            else -> Status.COMPLEMENTARY
        }
        val safe = problematic.isEmpty()
        val incoming = if (safe) clusters.filter { it.example.movementMinor > 0L }
            .fold(0L) { sum, it -> Math.addExact(sum, it.example.movementMinor) } else null
        val outgoing = if (safe) clusters.filter { it.example.movementMinor < 0L }
            .fold(0L) { sum, it -> Math.addExact(sum, Math.negateExact(it.example.movementMinor)) }
            else null
        return MonthCoverage(period.account, period.currency, period.month,
            sourceCount, rows.size, clusters.size, repeatCopies, problematic.size,
            status, incoming, outgoing)
    }
}
