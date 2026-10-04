package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

data class PossibleDuplicateObservation(
    val id: Long,
    val amountMinor: Long,
    val receivedAtEpochMs: Long,
    val transactionCode: String?,
    val sourcePackage: String?,
    val sourceKind: String,
    val direction: String,
    val reviewState: String,
    val category: String,
    val counterparty: String?,
    val evidenceLabel: String?,
    val type: String
)

object PossibleDuplicatePolicy {
    const val MAX_WINDOW_MS = 90_000L
    private const val CODED_WINDOW_MS = MAX_WINDOW_MS
    private const val CODELESS_WINDOW_MS = 20_000L
    private const val COMPLEMENTARY_WINDOW_MS = 60_000L

    fun isCandidate(
        first: PossibleDuplicateObservation,
        second: PossibleDuplicateObservation
    ): Boolean {
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

        val complementary =
            setOf(first.direction, second.direction) ==
                setOf("INCOME", "EXPENSE")

        val exactlyOneCoded =
            (firstCode == null) != (secondCode == null)

        return when {
            exactlyOneCoded -> delta <= CODED_WINDOW_MS
            complementary -> delta <= COMPLEMENTARY_WINDOW_MS
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

