package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs
import java.util.Locale

/**
 * Strict read-only receipt correlation for a posted outgoing loan payment.
 * No name-only matching, account guessing, or silent edits to user decisions.
 */
object DebtReceiptCorrelationPolicy {
    private const val MAX_GAP_MS = 15L * 60L * 1000L
    data class Candidate(
        val transactionId: Long,
        val transactionCode: String?,
        val accountId: String?,
        val amountMinor: Long,
        val occurredAtEpochMs: Long,
        val direction: String,
        val category: String?,
        val reviewState: String?,
        val ruleId: String?,
        val relationType: String?,
        val reversalCode: String?,
        val balanceAfterMinor: Long?
    )

    fun validCode(code: String?): Boolean =
        code?.trim()?.matches(Regex("[A-Z0-9]{10}", RegexOption.IGNORE_CASE)) == true

    fun choose(
        receipt: String,
        accountId: String,
        amountMinor: Long,
        occurredAtEpochMs: Long,
        walletBalanceAfterMinor: Long?,
        candidates: List<Candidate>
    ): Candidate? {
        // The code cannot belong to two cash events. Reject ambiguity,
        // even if only one of the transactions would otherwise qualify.
        if (!validCode(receipt) || candidates.size != 1 || amountMinor <= 0L)
            return null
        val row = candidates.single()
        if (!receipt.trim().uppercase(Locale.ROOT)
                .equals(row.transactionCode?.trim()?.uppercase(Locale.ROOT))) return null
        if (row.accountId != accountId || row.amountMinor != amountMinor ||
            abs(row.occurredAtEpochMs - occurredAtEpochMs) > MAX_GAP_MS) return null
        if (row.direction !in setOf("EXPENSE", "TRANSFER") ||
            row.category !in setOf("OTHER", "DEBT_LOAN") ||
            row.reviewState !in setOf("NEEDS_REVIEW", "AUTO_CLASSIFIED") ||
            row.ruleId != null || row.relationType != null ||
            row.reversalCode != null) return null
        if (walletBalanceAfterMinor != null && row.balanceAfterMinor != null &&
            walletBalanceAfterMinor != row.balanceAfterMinor) return null
        return row
    }
}
