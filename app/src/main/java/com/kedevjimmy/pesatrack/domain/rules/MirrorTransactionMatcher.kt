package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

data class MirrorTransactionObservation(
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val receivedAtEpochMs: Long,
    val sourceKind: String,
    val sourcePackage: String?,
    val transactionCode: String?,
    val type: String,
    val counterparty: String?,
    val counterpartyPhone: String?,
    val direction: String? = null
)

object MirrorTransactionMatcher {
    const val MAX_TIME_DELTA_MS = 120_000L
    private const val VERY_CLOSE_MS = 20_000L
    private const val COMPLEMENTARY_DIRECTION_MS = 60_000L
    private const val CODELESS_ULTRA_CLOSE_MS = 5_000L

    private val ignoredIdentityTokens = setOf(
        "account",
        "bank",
        "payment",
        "paid",
        "sent",
        "received",
        "transfer",
        "transferred",
        "mpesa",
        "m",
        "pesa",
        "money",
        "kes",
        "ksh"
    )

    fun isMirror(
        first: MirrorTransactionObservation,
        second: MirrorTransactionObservation
    ): Boolean {
        if (first.amountMinor <= 0L || first.amountMinor != second.amountMinor) {
            return false
        }
        if (
            first.type.equals("REVERSAL", ignoreCase = true) ||
            second.type.equals("REVERSAL", ignoreCase = true)
        ) {
            return false
        }

        val receivedDelta = abs(
            first.receivedAtEpochMs - second.receivedAtEpochMs
        )
        val occurredDelta = abs(
            first.occurredAtEpochMs - second.occurredAtEpochMs
        )
        val bestDelta = minOf(receivedDelta, occurredDelta)
        if (bestDelta > MAX_TIME_DELTA_MS) return false

        val firstCode = first.transactionCode
            ?.trim()
            ?.uppercase()
            ?.takeIf(String::isNotBlank)
        val secondCode = second.transactionCode
            ?.trim()
            ?.uppercase()
            ?.takeIf(String::isNotBlank)

        if (firstCode != null && secondCode != null) {
            return firstCode == secondCode
        }

        val samePhone = normalizePhone(first.counterpartyPhone) != null &&
            normalizePhone(first.counterpartyPhone) ==
                normalizePhone(second.counterpartyPhone)

        val sharedIdentityToken =
            identityTokens(first.counterparty)
                .intersect(identityTokens(second.counterparty))
                .isNotEmpty()

        val distinctSources = hasDistinctSources(first, second)
        val complementaryDirections =
            areComplementaryDirections(first.direction, second.direction)

        val exactlyOneHasCode = (firstCode == null) != (secondCode == null)
        if (exactlyOneHasCode) {
            return samePhone ||
                sharedIdentityToken ||
                bestDelta <= VERY_CLOSE_MS ||
                (
                    complementaryDirections &&
                        bestDelta <= COMPLEMENTARY_DIRECTION_MS
                    )
        }

        return distinctSources &&
            (
                samePhone ||
                    sharedIdentityToken ||
                    bestDelta <= CODELESS_ULTRA_CLOSE_MS
                )
    }

    private fun hasDistinctSources(
        first: MirrorTransactionObservation,
        second: MirrorTransactionObservation
    ): Boolean {
        val firstPackage = first.sourcePackage
            ?.trim()
            ?.lowercase()
            ?.takeIf(String::isNotBlank)
        val secondPackage = second.sourcePackage
            ?.trim()
            ?.lowercase()
            ?.takeIf(String::isNotBlank)

        if (
            firstPackage != null &&
            secondPackage != null &&
            firstPackage != secondPackage
        ) {
            return true
        }

        return first.sourceKind != second.sourceKind
    }

    private fun areComplementaryDirections(
        first: String?,
        second: String?
    ): Boolean =
        setOf(first, second) == setOf("INCOME", "EXPENSE")

    private fun identityTokens(value: String?): Set<String> =
        value.orEmpty()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .split(Regex("""\s+"""))
            .asSequence()
            .map(String::trim)
            .filter { it.length >= 3 }
            .filterNot(ignoredIdentityTokens::contains)
            .toSet()

    private fun normalizePhone(value: String?): String? {
        val digits = value.orEmpty().filter(Char::isDigit)
        return when {
            digits.length == 12 && digits.startsWith("254") -> digits
            digits.length == 10 && digits.startsWith("0") ->
                "254" + digits.drop(1)
            else -> null
        }
    }
}
