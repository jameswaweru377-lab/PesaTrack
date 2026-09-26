package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

data class FulizaPaymentCandidate(
    val transactionId: Long,
    val occurredAtEpochMs: Long,
    val amountMinor: Long,
    val mpesaFeeMinor: Long
)

object FulizaPairingPolicy {
    private const val MAX_DISTANCE_MS = 3 * 60 * 1000L

    fun chooseCandidate(
        fulizaFinancedMinor: Long,
        fulizaOccurredAtEpochMs: Long,
        candidates: List<FulizaPaymentCandidate>
    ): Long? {
        if (fulizaFinancedMinor <= 0L) return null

        val plausible = candidates.filter { candidate ->
            val distance = abs(fulizaOccurredAtEpochMs - candidate.occurredAtEpochMs)
            val totalRequired = candidate.amountMinor + candidate.mpesaFeeMinor
            distance <= MAX_DISTANCE_MS &&
                fulizaFinancedMinor <= totalRequired &&
                candidate.amountMinor > 0L
        }

        val exact = plausible.filter { candidate ->
            candidate.amountMinor + candidate.mpesaFeeMinor == fulizaFinancedMinor
        }

        if (exact.size == 1) return exact.single().transactionId

        if (exact.size > 1) {
            return exact
                .sortedBy { abs(fulizaOccurredAtEpochMs - it.occurredAtEpochMs) }
                .let { sorted ->
                    if (
                        sorted.size >= 2 &&
                        abs(fulizaOccurredAtEpochMs - sorted[0].occurredAtEpochMs) ==
                        abs(fulizaOccurredAtEpochMs - sorted[1].occurredAtEpochMs)
                    ) {
                        null
                    } else {
                        sorted.first().transactionId
                    }
                }
        }

        return plausible.singleOrNull()?.transactionId
    }
}
