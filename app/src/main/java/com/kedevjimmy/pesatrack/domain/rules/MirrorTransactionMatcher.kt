package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

enum class MirrorMatchConfidence {
    NONE,
    MEDIUM,
    HIGH
}

enum class MirrorMatchReason {
    NONE,
    EXACT_SHARED_REFERENCE,
    SAME_TRANSACTION_CODE,
    COMPLEMENTARY_ACCOUNTS,
    SHARED_IDENTITY,
    COMPLEMENTARY_DIRECTION,
    ULTRA_CLOSE_DISTINCT_SOURCE
}

data class MirrorTransactionMatch(
    val confidence: MirrorMatchConfidence,
    val reason: MirrorMatchReason,
    val sharedReference: String? = null
) {
    val autoLink: Boolean
        get() = confidence == MirrorMatchConfidence.HIGH
}

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
    val direction: String? = null,
    val externalReferences: Set<String> = emptySet(),
    val accountIds: Set<String> = emptySet()
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
    ): Boolean =
        match(first, second).autoLink

    fun match(
        first: MirrorTransactionObservation,
        second: MirrorTransactionObservation
    ): MirrorTransactionMatch {
        if (
            first.amountMinor <= 0L ||
            first.amountMinor != second.amountMinor
        ) {
            return none()
        }
        if (
            first.type.equals(
                "REVERSAL",
                ignoreCase = true
            ) ||
            second.type.equals(
                "REVERSAL",
                ignoreCase = true
            )
        ) {
            return none()
        }

        val firstCode =
            normalizeReference(first.transactionCode)
        val secondCode =
            normalizeReference(second.transactionCode)

        val firstReferences =
            normalizedReferences(first)
        val secondReferences =
            normalizedReferences(second)
        val sharedReferences =
            firstReferences.intersect(secondReferences)

        if (sharedReferences.isNotEmpty()) {
            val shared = sharedReferences.first()
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.HIGH,
                reason =
                    if (
                        firstCode != null &&
                        secondCode != null &&
                        firstCode == secondCode
                    ) {
                        MirrorMatchReason
                            .SAME_TRANSACTION_CODE
                    } else {
                        MirrorMatchReason
                            .EXACT_SHARED_REFERENCE
                    },
                sharedReference = shared
            )
        }

        val receivedDelta =
            abs(
                first.receivedAtEpochMs -
                    second.receivedAtEpochMs
            )
        val occurredDelta =
            abs(
                first.occurredAtEpochMs -
                    second.occurredAtEpochMs
            )
        val bestDelta =
            minOf(receivedDelta, occurredDelta)
        if (bestDelta > MAX_TIME_DELTA_MS) {
            return none()
        }

        val exactlyOneHasStrongReference =
            firstReferences.isEmpty() !=
                secondReferences.isEmpty()

        /*
         * Two financial institutions can arrive through the same
         * Android SMS app. One coded and one codeless observation
         * of the same amount inside a very tight window is still
         * strong enough to auto-link. Fully codeless time-only
         * similarity remains review-only below.
         */
        if (
            exactlyOneHasStrongReference &&
            bestDelta <= VERY_CLOSE_MS
        ) {
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.HIGH,
                reason =
                    MirrorMatchReason
                        .ULTRA_CLOSE_DISTINCT_SOURCE
            )
        }

        val distinctSources =
            hasDistinctSources(first, second)

        val sharedAccount =
            first.accountIds
                .mapNotNull(::normalizeAccountId)
                .toSet()
                .intersect(
                    second.accountIds
                        .mapNotNull(::normalizeAccountId)
                        .toSet()
                )
                .isNotEmpty()

        val accountPathEvidence =
            sharedAccount &&
                (
                    first.accountIds.size >= 2 ||
                    second.accountIds.size >= 2
                )

        if (
            accountPathEvidence &&
            bestDelta <= MAX_TIME_DELTA_MS
        ) {
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.HIGH,
                reason =
                    MirrorMatchReason
                        .COMPLEMENTARY_ACCOUNTS
            )
        }

        val samePhone =
            normalizePhone(first.counterpartyPhone) != null &&
                normalizePhone(first.counterpartyPhone) ==
                normalizePhone(second.counterpartyPhone)

        val sharedIdentityToken =
            identityTokens(first.counterparty)
                .intersect(
                    identityTokens(second.counterparty)
                )
                .isNotEmpty()

        if (
            (samePhone || sharedIdentityToken) &&
            bestDelta <= MAX_TIME_DELTA_MS
        ) {
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.HIGH,
                reason = MirrorMatchReason.SHARED_IDENTITY
            )
        }

        val complementaryDirections =
            areComplementaryDirections(
                first.direction,
                second.direction
            )

        if (
            complementaryDirections &&
            bestDelta <= COMPLEMENTARY_DIRECTION_MS
        ) {
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.HIGH,
                reason =
                    MirrorMatchReason
                        .COMPLEMENTARY_DIRECTION
            )
        }

        if (
            distinctSources &&
            firstReferences.isEmpty() &&
            secondReferences.isEmpty() &&
            bestDelta <= CODELESS_ULTRA_CLOSE_MS
        ) {
            return MirrorTransactionMatch(
                confidence = MirrorMatchConfidence.MEDIUM,
                reason =
                    MirrorMatchReason
                        .ULTRA_CLOSE_DISTINCT_SOURCE
            )
        }

        return none()
    }

    private fun normalizedReferences(
        observation: MirrorTransactionObservation
    ): Set<String> =
        buildSet {
            normalizeReference(
                observation.transactionCode
            )?.let(::add)
            observation.externalReferences
                .mapNotNull(::normalizeReference)
                .forEach(::add)
        }

    private fun normalizeReference(
        value: String?
    ): String? =
        value
            ?.trim()
            ?.uppercase()
            ?.replace(
                Regex("""[^A-Z0-9-]"""),
                ""
            )
            ?.takeIf { it.length >= 5 }

    private fun normalizeAccountId(
        value: String?
    ): String? =
        value
            ?.trim()
            ?.lowercase()
            ?.takeIf(String::isNotBlank)

    private fun none(): MirrorTransactionMatch =
        MirrorTransactionMatch(
            confidence = MirrorMatchConfidence.NONE,
            reason = MirrorMatchReason.NONE
        )

    private fun hasDistinctSources(
        first: MirrorTransactionObservation,
        second: MirrorTransactionObservation
    ): Boolean {
        val firstPackage =
            first.sourcePackage
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotBlank)
        val secondPackage =
            second.sourcePackage
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
        setOf(first, second) ==
            setOf("INCOME", "EXPENSE") ||
            setOf(first, second) ==
            setOf("TRANSFER", "INCOME") ||
            setOf(first, second) ==
            setOf("TRANSFER", "EXPENSE")

    private fun identityTokens(
        value: String?
    ): Set<String> =
        value.orEmpty()
            .lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .split(Regex("""\s+"""))
            .asSequence()
            .map(String::trim)
            .filter { it.length >= 3 }
            .filterNot(ignoredIdentityTokens::contains)
            .toSet()

    private fun normalizePhone(
        value: String?
    ): String? {
        val digits =
            value.orEmpty().filter(Char::isDigit)
        return when {
            digits.length == 12 &&
                digits.startsWith("254") ->
                digits
            digits.length == 10 &&
                digits.startsWith("0") ->
                "254" + digits.drop(1)
            else -> null
        }
    }
}
