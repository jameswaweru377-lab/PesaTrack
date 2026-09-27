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
    val counterpartyPhone: String?
)

object MirrorTransactionMatcher {
    const val MAX_TIME_DELTA_MS = 120_000L
    private const val ULTRA_CLOSE_MS = 5_000L

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

        val firstHasCode = !first.transactionCode.isNullOrBlank()
        val secondHasCode = !second.transactionCode.isNullOrBlank()
        if (firstHasCode == secondHasCode) {
            return false
        }

        val receivedDelta = abs(
            first.receivedAtEpochMs - second.receivedAtEpochMs
        )
        val occurredDelta = abs(
            first.occurredAtEpochMs - second.occurredAtEpochMs
        )
        if (
            receivedDelta > MAX_TIME_DELTA_MS &&
            occurredDelta > MAX_TIME_DELTA_MS
        ) {
            return false
        }

        val samePhone = normalizePhone(first.counterpartyPhone) != null &&
            normalizePhone(first.counterpartyPhone) ==
                normalizePhone(second.counterpartyPhone)

        val sharedIdentityToken =
            identityTokens(first.counterparty)
                .intersect(identityTokens(second.counterparty))
                .isNotEmpty()

        val ultraClose = receivedDelta <= ULTRA_CLOSE_MS

        return samePhone || sharedIdentityToken || ultraClose
    }

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
