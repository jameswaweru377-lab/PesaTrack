package com.kedevjimmy.pesatrack.domain.rules

data class RefundCandidate(
    val transactionId: Long,
    val occurredAtEpochMs: Long,
    val amountMinor: Long,
    val counterparty: String?
)

enum class RefundReviewReason { PARTIAL, AMBIGUOUS }

object RefundMatchingPolicy {
    const val MAX_REFUND_DISTANCE_MS = 14L * 24L * 60L * 60L * 1000L

    fun chooseCandidate(
        receivedAmountMinor: Long,
        receivedAtEpochMs: Long,
        receivedCounterparty: String?,
        candidates: List<RefundCandidate>
    ): Long? {
        if (receivedAmountMinor <= 0L) return null
        val incomingIdentity = merchantIdentity(receivedCounterparty)
            ?: return null

        val plausible = candidates
            .asSequence()
            .filter {
                it.amountMinor == receivedAmountMinor &&
                    it.occurredAtEpochMs <= receivedAtEpochMs &&
                    receivedAtEpochMs - it.occurredAtEpochMs <=
                        MAX_REFUND_DISTANCE_MS
            }
            .filter {
                val outgoingIdentity = merchantIdentity(it.counterparty)
                    ?: return@filter false
                identitiesMatch(
                    incomingIdentity,
                    outgoingIdentity
                )
            }
            .sortedByDescending { it.occurredAtEpochMs }
            .toList()

        if (plausible.isEmpty()) return null
        if (plausible.size == 1) {
            return plausible.single().transactionId
        }

        // Multiple identical payments to the same merchant are ambiguous.
        // Auto-link only when the newest candidate is clearly separated.
        val newest = plausible[0]
        val second = plausible[1]
        val newestDistance =
            receivedAtEpochMs - newest.occurredAtEpochMs
        val secondDistance =
            receivedAtEpochMs - second.occurredAtEpochMs

        return if (
            secondDistance - newestDistance >=
            MIN_CLEAR_SEPARATION_MS
        ) {
            newest.transactionId
        } else {
            null
        }
    }

    /** Partial and competing evidence must never create an automatic ledger link. */
    fun reviewReason(
        receivedAmountMinor: Long,
        receivedAtEpochMs: Long,
        receivedCounterparty: String?,
        candidates: List<RefundCandidate>
    ): RefundReviewReason? {
        val plausible = plausibleCandidates(
            receivedAmountMinor, receivedAtEpochMs, receivedCounterparty, candidates
        )
        return when {
            plausible.size > 1 -> RefundReviewReason.AMBIGUOUS
            plausible.singleOrNull()?.amountMinor?.let { it > receivedAmountMinor } == true ->
                RefundReviewReason.PARTIAL
            else -> null
        }
    }

    private fun plausibleCandidates(
        amount: Long,
        at: Long,
        counterparty: String?,
        candidates: List<RefundCandidate>
    ): List<RefundCandidate> {
        if (amount <= 0L) return emptyList()
        val identity = merchantIdentity(counterparty) ?: return emptyList()
        return candidates.filter { candidate ->
            candidate.amountMinor >= amount && candidate.occurredAtEpochMs <= at &&
                at - candidate.occurredAtEpochMs <= MAX_REFUND_DISTANCE_MS &&
                merchantIdentity(candidate.counterparty) == identity
        }
    }

    internal fun merchantIdentity(raw: String?): String? {
        val tokens = raw
            ?.uppercase()
            ?.replace(Regex("""[^A-Z0-9]+"""), " ")
            ?.trim()
            ?.split(Regex("""\s+"""))
            ?.filter { token ->
                token.isNotBlank() &&
                    token.any(Char::isLetter) &&
                    token !in STOP_WORDS
            }
            .orEmpty()

        return tokens
            .joinToString(" ")
            .takeIf { it.length >= 3 }
    }

    private fun identitiesMatch(
        first: String,
        second: String
    ): Boolean {
        return first == second
    }

    private val STOP_WORDS = setOf(
        "KENYA",
        "LIMITED",
        "LTD",
        "PLC",
        "COMPANY",
        "CO",
        "ACCOUNT",
        "PAYMENT",
        "PAYMENTS"
    )

    private const val MIN_CLEAR_SEPARATION_MS =
        60L * 60L * 1000L
}
