package com.kedevjimmy.pesatrack.domain.rules

import java.util.Locale

data class ReversalOriginal(
    val id: Long,
    val code: String,
    val amountMinor: Long,
    val accountId: String?,
    val occurredAtEpochMs: Long
)

data class ReversalEvidence(
    val originalCode: String,
    val amountMinor: Long,
    val accountId: String?,
    val occurredAtEpochMs: Long
)

/** An unproven or partial reversal must not erase an original cash movement. */
object ReversalMatchingPolicy {
    fun match(reversal: ReversalEvidence, candidates: List<ReversalOriginal>): Long? {
        if (reversal.amountMinor <= 0L || reversal.originalCode.isBlank()) return null
        val matching = candidates.filter { original ->
            original.amountMinor == reversal.amountMinor &&
                original.occurredAtEpochMs <= reversal.occurredAtEpochMs &&
                original.code.trim().uppercase(Locale.ROOT) ==
                    reversal.originalCode.trim().uppercase(Locale.ROOT) &&
                (original.accountId == null || reversal.accountId == null ||
                    original.accountId == reversal.accountId)
        }
        // Prefer a verified same-account identity over unknown account metadata.
        val exactAccount = matching.filter {
            reversal.accountId != null && it.accountId == reversal.accountId
        }
        return (exactAccount.ifEmpty { matching }).singleOrNull()?.id
    }
}
