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
    val balanceMinor: Long,
    /** Seven original PDF cells, retaining original date, signs and wrapped details. */
    val sourceCells: List<String> = emptyList()
) {
    init {
        require(page in 1..160 && row in 1..5_000 && occurredAt > 0L)
        require(reference.matches(Regex("[A-Z0-9]{10}")))
        require(description.trim().length in 1..200)
        require(paidInMinor >= 0L && withdrawnMinor >= 0L && balanceMinor >= 0L)
        require(paidInMinor == 0L || withdrawnMinor == 0L)
        require(sourceCells.isEmpty() || (sourceCells.size == 7 &&
            sourceCells.all { it.length <= 1200 }))
    }

    val movementMinor: Long get() = Math.subtractExact(paidInMinor, withdrawnMinor)
    val informational: Boolean get() = movementMinor == 0L
}

/** Count-only admission contract for the separate statement archive.
 * A future offline Python extractor must supply these only after whole-file proof;
 * this type does not infer source authenticity or create canonical transactions.
 */
data class StatementActivityProof(
    val receiptRows: Int,
    val cashRows: Int,
    val totalsVerified: Boolean
) {
    init { require(receiptRows in 1..5000 && cashRows in 0..receiptRows) }
}
