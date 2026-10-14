package com.kedevjimmy.pesatrack.domain.everyday

/**
 * Immutable, physically verified row for a separate Statement Activity
 * journal. A receipt is an event reference, never a unique cash-leg ID.
 * A zero movement remains an informational event rather than a cash entry.
 */
data class StatementActivityLine(
    val page: Int,
    val row: Int,
    val reference: String,
    val occurredAt: Long,
    val description: String,
    val paidInMinor: Long,
    val withdrawnMinor: Long,
    val balanceMinor: Long
) {
    init {
        require(page in 1..160 && row in 1..5_000 && occurredAt > 0L)
        require(reference.matches(Regex("[A-Z0-9]{10}")))
        require(description.trim().length in 1..200)
        require(paidInMinor >= 0L && withdrawnMinor >= 0L && balanceMinor >= 0L)
        require(paidInMinor == 0L || withdrawnMinor == 0L)
    }

    val movementMinor: Long get() = Math.subtractExact(paidInMinor, withdrawnMinor)
    val informational: Boolean get() = movementMinor == 0L
}
