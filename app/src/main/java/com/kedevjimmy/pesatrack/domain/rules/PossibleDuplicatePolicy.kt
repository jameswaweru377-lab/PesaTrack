package com.kedevjimmy.pesatrack.domain.rules

import java.util.Locale
import kotlin.math.abs

data class PossibleDuplicateObservation(
    val id: Long,
    val accountId: String?,
    val amountMinor: Long,
    val receivedAtEpochMs: Long,
    val transactionCode: String?,
    val sourcePackage: String?,
    val sourceKind: String,
    val direction: String,
    val reviewState: String,
    val category: String,
    val hasProtectedCashRole: Boolean,
    val counterparty: String?,
    val evidenceLabel: String?,
    val type: String
)

object PossibleDuplicatePolicy {
    const val MAX_CANDIDATE_WINDOW_MS = 90_000L
    private const val CODED_WINDOW_MS =
        MAX_CANDIDATE_WINDOW_MS
    private const val CODELESS_WINDOW_MS = 20_000L
    private val protectedCashCategories = setOf(
        "DEBT_LOAN", "FEES", "FEE", "REFUND", "SAVINGS", "INVESTMENT", "TRANSFER"
    )

    /**
     * Preserve both ID orderings for existing Keep Both callers, but never
     * silently discard malformed tokens or accept self-pairs.
     */
    fun parsePairIds(pairKey: String): Pair<Long, Long>? {
        val parts = pairKey.split(':')
        if (parts.size != 2) return null
        val first = parts[0].toLongOrNull() ?: return null
        val second = parts[1].toLongOrNull() ?: return null
        if (first <= 0L || second <= 0L || first == second) return null
        if (pairKey != "$first:$second") return null
        return first to second
    }

    /** Validate what the user saw against the currently persisted observations. */
    fun canConfirm(
        pairKey: String,
        shownFirst: PossibleDuplicateObservation,
        shownSecond: PossibleDuplicateObservation,
        currentFirst: PossibleDuplicateObservation,
        currentSecond: PossibleDuplicateObservation
    ): Boolean {
        val ids = parsePairIds(pairKey) ?: return false
        val snapshotsMatch =
            (shownFirst == currentFirst && shownSecond == currentSecond) ||
                (shownFirst == currentSecond && shownSecond == currentFirst)
        return ids.first == currentFirst.id &&
            ids.second == currentSecond.id &&
            snapshotsMatch &&
            isCandidate(currentFirst, currentSecond)
    }

    fun isCandidate(
        first: PossibleDuplicateObservation,
        second: PossibleDuplicateObservation
    ): Boolean {
        // A reversal changes the financial meaning of the movement; it is not
        // a weak-evidence duplicate of an ordinary payment.
        if (first.type.equals("REVERSAL", ignoreCase = true) ||
            second.type.equals("REVERSAL", ignoreCase = true)) return false
        // A duplicate is one cash leg observed twice, not opposite sides of
        // an owned transfer, loan repayment or refund. Missing account identity
        // is not permission to remove either source's movement from totals.
        if (first.accountId.isNullOrBlank() || second.accountId.isNullOrBlank() ||
            first.accountId != second.accountId ||
            first.hasProtectedCashRole || second.hasProtectedCashRole ||
            first.direction == "TRANSFER" || second.direction == "TRANSFER" ||
            (first.direction != "NEUTRAL" && second.direction != "NEUTRAL" &&
                first.direction != second.direction) ||
            first.category.uppercase(Locale.ROOT) in protectedCashCategories ||
            second.category.uppercase(Locale.ROOT) in protectedCashCategories) return false
        if (first.id == second.id) return false
        if (first.amountMinor <= 0L || first.amountMinor != second.amountMinor) {
            return false
        }

        val delta = abs(
            first.receivedAtEpochMs - second.receivedAtEpochMs
        )
        if (delta > CODED_WINDOW_MS) return false

        if (
            isProtected(first.reviewState) ||
            isProtected(second.reviewState)
        ) {
            return false
        }
        if (
            first.reviewState != "NEEDS_REVIEW" ||
            second.reviewState != "NEEDS_REVIEW"
        ) {
            return false
        }

        val firstCode = normalized(first.transactionCode)
        val secondCode = normalized(second.transactionCode)
        if (firstCode != null && secondCode != null) {
            return false
        }

        val distinctSources = hasDistinctSources(first, second)
        if (!distinctSources) return false

        val firstIdentity = normalizedIdentity(first)
        val secondIdentity = normalizedIdentity(second)
        if (
            firstIdentity != null &&
            secondIdentity != null &&
            firstIdentity != secondIdentity
        ) {
            return false
        }

        val exactlyOneCoded =
            (firstCode == null) != (secondCode == null)

        return when {
            exactlyOneCoded -> delta <= CODED_WINDOW_MS
            firstIdentity != null && firstIdentity == secondIdentity ->
                delta <= CODED_WINDOW_MS
            else -> delta <= CODELESS_WINDOW_MS
        }
    }

    private fun isProtected(reviewState: String): Boolean =
        reviewState == "USER_CLASSIFIED" ||
            reviewState == "EXCLUDED"

    private fun hasDistinctSources(
        first: PossibleDuplicateObservation,
        second: PossibleDuplicateObservation
    ): Boolean {
        val firstPackage = normalized(first.sourcePackage)
        val secondPackage = normalized(second.sourcePackage)

        if (
            firstPackage != null &&
            secondPackage != null &&
            firstPackage != secondPackage
        ) {
            return true
        }

        return first.sourceKind != second.sourceKind
    }

    private fun normalizedIdentity(
        item: PossibleDuplicateObservation
    ): String? =
        normalized(item.counterparty)
            ?: normalized(item.evidenceLabel)

    private fun normalized(value: String?): String? =
        value
            ?.trim()
            ?.lowercase()
            ?.replace(Regex("""[^a-z0-9]+"""), " ")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.takeIf(String::isNotBlank)
}
