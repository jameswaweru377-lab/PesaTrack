package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

enum class RepaymentChainConfidence {
    HIGH,
    MEDIUM,
    USER_CONFIRMED
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

data class DebtProceedsEventCandidate(
    val debtEventId: Long,
    val amountMinor: Long,
    val occurredAtEpochMs: Long
)

data class DebtProceedsEventMatch(
    val debtEventId: Long,
    val confidence: RepaymentChainConfidence,
    val reason: String
)

object DebtProceedsRepaymentLinkPolicy {
    const val MAX_LINK_DISTANCE_MS =
        10L * 60L * 1000L
    private const val STRONG_DISTANCE_MS =
        2L * 60L * 1000L

    fun choose(
        repaymentAmountMinor: Long,
        repaymentAtEpochMs: Long,
        candidates: List<DebtProceedsEventCandidate>
    ): DebtProceedsEventMatch? {
        if (repaymentAmountMinor <= 0L) {
            return null
        }

        val exact =
            candidates.filter { candidate ->
                candidate.amountMinor ==
                    repaymentAmountMinor &&
                    abs(
                        repaymentAtEpochMs -
                            candidate.occurredAtEpochMs
                    ) <= MAX_LINK_DISTANCE_MS
            }

        if (exact.size != 1) {
            return null
        }

        val candidate = exact.single()
        val distance =
            abs(
                repaymentAtEpochMs -
                    candidate.occurredAtEpochMs
            )

        return DebtProceedsEventMatch(
            debtEventId = candidate.debtEventId,
            confidence =
                if (distance <= STRONG_DISTANCE_MS) {
                    RepaymentChainConfidence.HIGH
                } else {
                    RepaymentChainConfidence.MEDIUM
                },
            reason =
                if (distance <= STRONG_DISTANCE_MS) {
                    "Only one nearby loan disbursement into the funding account matched the debt repayment amount."
                } else {
                    "Only one loan disbursement into the funding account matched the debt repayment amount within ten minutes."
                }
        )
    }
}

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


data class RepaymentBalanceCandidate(
    val observationId: Long,
    val balanceMinor: Long,
    val observedAtEpochMs: Long
)

data class RepaymentBalanceMatch(
    val observationId: Long,
    val confidence: RepaymentChainConfidence,
    val reason: String
)

/**
 * Final step of a repayment event chain.
 *
 * The policy knows nothing about M-Pesa, a bank, a lender or any
 * provider name. It links a balance observation only when one unique
 * observation satisfies an already-observed final balance, or a
 * deterministic funding equation.
 */
object DebtRepaymentBalancePolicy {
    const val MAX_LINK_DISTANCE_MS =
        5L * 60L * 1000L
    private const val STRONG_DISTANCE_MS =
        2L * 60L * 1000L

    fun choose(
        explicitBalanceAfterMinor: Long?,
        computedBalanceAfterMinor: Long?,
        repaymentAtEpochMs: Long,
        candidates: List<RepaymentBalanceCandidate>
    ): RepaymentBalanceMatch? {
        val target =
            explicitBalanceAfterMinor
                ?: computedBalanceAfterMinor
                ?: return null
        if (target < 0L) {
            return null
        }

        val matching =
            candidates.filter { candidate ->
                candidate.balanceMinor == target &&
                    abs(
                        candidate.observedAtEpochMs -
                            repaymentAtEpochMs
                    ) <= MAX_LINK_DISTANCE_MS
            }

        if (matching.size != 1) {
            return null
        }

        val candidate = matching.single()
        val distance =
            abs(
                candidate.observedAtEpochMs -
                    repaymentAtEpochMs
            )
        val explicit =
            explicitBalanceAfterMinor != null

        return RepaymentBalanceMatch(
            observationId = candidate.observationId,
            confidence =
                if (
                    explicit ||
                    distance <= STRONG_DISTANCE_MS
                ) {
                    RepaymentChainConfidence.HIGH
                } else {
                    RepaymentChainConfidence.MEDIUM
                },
            reason =
                when {
                    explicit ->
                        "Observed funding-account balance matched the debt repayment final balance."
                    distance <= STRONG_DISTANCE_MS ->
                        "Observed funding-account balance matched the deterministic incoming-minus-repayment equation."
                    else ->
                        "Only one nearby funding-account balance matched the deterministic incoming-minus-repayment equation."
                }
        )
    }
}


data class LiabilityPaymentCandidate(
    val debtAccountId: String,
    val displayName: String,
    val providerKey: String,
    val outstandingMinor: Long,
    val hasObservedOutstanding: Boolean
)

data class LiabilityPaymentMatch(
    val debtAccountId: String,
    val confidence: RepaymentChainConfidence,
    val reason: String
)

/**
 * Provider-neutral bridge from an outgoing payment counterparty to an
 * already-known liability.
 *
 * The policy compares normalized institution identity only. Generic
 * words such as BANK, LOAN, FINANCE, LIMITED and KENYA are ignored.
 * It never contains provider names.
 */
object LiabilityPaymentLinkPolicy {
    fun candidates(
        counterparty: String?,
        amountMinor: Long,
        candidates: List<LiabilityPaymentCandidate>
    ): List<LiabilityPaymentMatch> {
        if (
            counterparty.isNullOrBlank() ||
            amountMinor <= 0L
        ) {
            return emptyList()
        }

        val counterpartyTokens =
            identityTokens(counterparty)
        if (counterpartyTokens.isEmpty()) {
            return emptyList()
        }

        val matches =
            candidates.mapNotNull { candidate ->
                // Product identity, never the institution prefix in an internal key.
                // SAFARICOM_FULIZA must not turn a Safaricom airtime purchase
                // into a proposed repayment.
                val identity = identityTokens(candidate.displayName)
                val exactIdentity = identity.isNotEmpty() && identity == counterpartyTokens
                if (!exactIdentity) return@mapNotNull null

                val amountFitsObservedDebt =
                    candidate.hasObservedOutstanding &&
                        candidate.outstandingMinor > 0L &&
                        amountMinor <=
                            candidate.outstandingMinor

                LiabilityPaymentMatch(
                    debtAccountId =
                        candidate.debtAccountId,
                    confidence =
                        if (
                            exactIdentity &&
                            amountFitsObservedDebt
                        ) {
                            RepaymentChainConfidence.HIGH
                        } else {
                            RepaymentChainConfidence.MEDIUM
                        },
                    reason =
                        when {
                            exactIdentity &&
                                amountFitsObservedDebt ->
                                "Counterparty identity exactly matched one known liability and the payment did not exceed its observed outstanding balance."
                            exactIdentity ->
                                "Counterparty identity exactly matched a known liability, but the outstanding balance is not strong enough for automatic linking."
                            else ->
                                "Counterparty identity strongly overlaps a known liability, but requires confirmation."
                        }
                )
            }

        /*
         * If two liabilities resolve to the same identity, automatic
         * certainty is impossible even if one individual candidate
         * scored HIGH. Downgrade the whole set for review.
         */
        return if (matches.size <= 1) {
            matches
        } else {
            matches.map {
                it.copy(
                    confidence =
                        RepaymentChainConfidence.MEDIUM,
                    reason =
                        "More than one known liability matches this counterparty identity; choose the correct debt explicitly."
                )
            }
        }
    }

    fun chooseAutomatic(
        counterparty: String?,
        amountMinor: Long,
        candidates: List<LiabilityPaymentCandidate>
    ): LiabilityPaymentMatch? =
        candidates(
            counterparty = counterparty,
            amountMinor = amountMinor,
            candidates = candidates
        )
            .singleOrNull {
                it.confidence ==
                    RepaymentChainConfidence.HIGH
            }

    private fun identityTokens(
        raw: String
    ): Set<String> =
        raw.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .trim()
            .split(Regex("""\s+"""))
            .asSequence()
            .filter(String::isNotBlank)
            .filterNot(GENERIC_IDENTITY_WORDS::contains)
            .filter { token ->
                token.length >= 2 ||
                    token.any(Char::isDigit)
            }
            .toSet()

    private val GENERIC_IDENTITY_WORDS =
        setOf(
            "LOAN",
            "BANK",
            "FINANCE",
            "FINANCIAL",
            "CREDIT",
            "LENDING",
            "LIMITED",
            "LTD",
            "PLC",
            "INTERNATIONAL",
            "KENYA",
            "APP",
            "MOBILE",
            "PAYMENT",
            "PAYMENTS",
            "ACCOUNT",
            "COMPANY",
            "CO",
            "GROUP",
            "SERVICES"
        )
}

