package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

enum class RepaymentChainConfidence {
    HIGH,
    MEDIUM
}

data class RepaymentFundingCandidate(
    val transactionId: Long,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val balanceAfterMinor: Long?,
    val priorBalanceMinor: Long?
)

data class RepaymentFundingMatch(
    val transactionId: Long,
    val confidence: RepaymentChainConfidence,
    val reason: String
)

data class DebtFundingCandidate(
    val transactionId: Long,
    val occurredAtEpochMs: Long,
    val amountMinor: Long,
    val transactionFeeMinor: Long
)

object DebtFundingPairingPolicy {
    private const val MAX_DISTANCE_MS =
        3L * 60L * 1000L

    fun chooseTransactionShortfallCandidate(
        financedMinor: Long,
        financedAtEpochMs: Long,
        candidates: List<DebtFundingCandidate>
    ): Long? {
        if (financedMinor <= 0L) return null

        val plausible = candidates.filter { candidate ->
            val distance = abs(
                financedAtEpochMs -
                    candidate.occurredAtEpochMs
            )
            val required =
                candidate.amountMinor +
                    candidate.transactionFeeMinor

            distance <= MAX_DISTANCE_MS &&
                financedMinor <= required &&
                candidate.amountMinor > 0L
        }

        val exact = plausible.filter { candidate ->
            candidate.amountMinor +
                candidate.transactionFeeMinor ==
                financedMinor
        }

        if (exact.size == 1) {
            return exact.single().transactionId
        }

        if (exact.size > 1) {
            val sorted = exact.sortedBy {
                abs(
                    financedAtEpochMs -
                        it.occurredAtEpochMs
                )
            }
            if (
                sorted.size >= 2 &&
                abs(
                    financedAtEpochMs -
                        sorted[0].occurredAtEpochMs
                ) ==
                abs(
                    financedAtEpochMs -
                        sorted[1].occurredAtEpochMs
                )
            ) {
                return null
            }
            return sorted.first().transactionId
        }

        return plausible.singleOrNull()?.transactionId
    }
}

object DebtRepaymentChainPolicy {
    const val MAX_LINK_DISTANCE_MS =
        10L * 60L * 1000L
    private const val STRONG_TIME_DISTANCE_MS =
        2L * 60L * 1000L
    private const val BALANCE_TOLERANCE_MINOR = 1L

    fun chooseFundingTransaction(
        repaymentAmountMinor: Long,
        walletBalanceAfterMinor: Long?,
        repaymentAtEpochMs: Long,
        candidates: List<RepaymentFundingCandidate>
    ): RepaymentFundingMatch? {
        if (repaymentAmountMinor <= 0L) return null

        val plausible = candidates
            .filter {
                abs(
                    repaymentAtEpochMs -
                        it.occurredAtEpochMs
                ) <= MAX_LINK_DISTANCE_MS
            }
            .sortedBy {
                abs(
                    repaymentAtEpochMs -
                        it.occurredAtEpochMs
                )
            }

        if (plausible.isEmpty()) return null

        if (walletBalanceAfterMinor != null) {
            val equationMatches = plausible.filter {
                balanceEquationMatches(
                    candidate = it,
                    repaymentAmountMinor =
                        repaymentAmountMinor,
                    walletBalanceAfterMinor =
                        walletBalanceAfterMinor
                )
            }

            if (equationMatches.size == 1) {
                return RepaymentFundingMatch(
                    transactionId =
                        equationMatches.single()
                            .transactionId,
                    confidence =
                        RepaymentChainConfidence.HIGH,
                    reason =
                        "Wallet balance equation matched incoming funds, debt repayment and final balance."
                )
            }

            val postCreditMatches =
                plausible.filter { candidate ->
                    candidate.balanceAfterMinor != null &&
                        abs(
                            candidate.balanceAfterMinor -
                                repaymentAmountMinor -
                                walletBalanceAfterMinor
                        ) <= BALANCE_TOLERANCE_MINOR
                }

            if (postCreditMatches.size == 1) {
                return RepaymentFundingMatch(
                    transactionId =
                        postCreditMatches.single()
                            .transactionId,
                    confidence =
                        RepaymentChainConfidence.HIGH,
                    reason =
                        "Incoming wallet balance minus the debt repayment equals the final wallet balance."
                )
            }
        }

        val exactAmount = plausible.filter {
            it.amountMinor == repaymentAmountMinor
        }
        if (exactAmount.size == 1) {
            return RepaymentFundingMatch(
                transactionId =
                    exactAmount.single().transactionId,
                confidence =
                    RepaymentChainConfidence.MEDIUM,
                reason =
                    "Only one nearby incoming transaction matched the debt repayment amount."
            )
        }

        val veryClose = plausible.filter {
            abs(
                repaymentAtEpochMs -
                    it.occurredAtEpochMs
            ) <= STRONG_TIME_DISTANCE_MS
        }

        if (veryClose.size == 1) {
            return RepaymentFundingMatch(
                transactionId =
                    veryClose.single().transactionId,
                confidence =
                    RepaymentChainConfidence.MEDIUM,
                reason =
                    "Only one incoming transaction occurred within two minutes of the automatic debt repayment."
            )
        }

        return null
    }

    private fun balanceEquationMatches(
        candidate: RepaymentFundingCandidate,
        repaymentAmountMinor: Long,
        walletBalanceAfterMinor: Long
    ): Boolean {
        val prior = candidate.priorBalanceMinor
            ?: return false
        val expected =
            prior +
                candidate.amountMinor -
                repaymentAmountMinor

        return abs(
            expected - walletBalanceAfterMinor
        ) <= BALANCE_TOLERANCE_MINOR
    }
}
