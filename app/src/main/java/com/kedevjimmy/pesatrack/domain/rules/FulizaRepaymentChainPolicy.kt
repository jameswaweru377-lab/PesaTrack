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

object FulizaRepaymentChainPolicy {
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
            val equationMatches = plausible.filter { candidate ->
                balanceEquationMatches(
                    candidate = candidate,
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
                        "M-PESA balance equation matched incoming funds, Fuliza repayment and final wallet balance."
                )
            }

            val postCreditBalanceMatches =
                plausible.filter { candidate ->
                    candidate.balanceAfterMinor != null &&
                        abs(
                            candidate.balanceAfterMinor -
                                repaymentAmountMinor -
                                walletBalanceAfterMinor
                        ) <= BALANCE_TOLERANCE_MINOR
                }

            if (postCreditBalanceMatches.size == 1) {
                return RepaymentFundingMatch(
                    transactionId =
                        postCreditBalanceMatches.single()
                            .transactionId,
                    confidence =
                        RepaymentChainConfidence.HIGH,
                    reason =
                        "Incoming M-PESA balance minus the Fuliza repayment equals the final wallet balance."
                )
            }
        }

        val exactAmountMatches = plausible.filter {
            it.amountMinor == repaymentAmountMinor
        }
        if (exactAmountMatches.size == 1) {
            return RepaymentFundingMatch(
                transactionId =
                    exactAmountMatches.single()
                        .transactionId,
                confidence =
                    RepaymentChainConfidence.MEDIUM,
                reason =
                    "Only one nearby incoming transaction matched the Fuliza repayment amount."
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
                    "Only one incoming transaction occurred within two minutes of the automatic Fuliza repayment."
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
