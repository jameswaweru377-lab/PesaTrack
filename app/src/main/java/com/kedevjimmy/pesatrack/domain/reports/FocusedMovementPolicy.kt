package com.kedevjimmy.pesatrack.domain.reports

/** Persisted, canonical movements only. Amounts use integer minor units. */
data class FocusedMovement(
    val id: String,
    val group: String,
    val kind: String,
    val amountMinor: Long?,
    val feeMinor: Long = 0L,
    val atEpochMs: Long,
    val transactionId: Long? = null
)

data class FocusedMovementTotals(val amountMinor: Long?, val records: List<FocusedMovement>) {
    val unknownCount: Int get() = records.count { it.amountMinor == null }
    val hasRecords: Boolean get() = records.isNotEmpty()
}

object FocusedMovementPolicy {
    fun total(rows: List<FocusedMovement>, kind: String, start: Long, endExclusive: Long): FocusedMovementTotals {
        val selected = rows.filter { it.kind == kind && it.atEpochMs >= start && it.atEpochMs < endExclusive }
            .distinctBy { it.id }
        val amount = if (selected.any { it.amountMinor == null }) null else
            runCatching { selected.fold(0L) { sum, row -> Math.addExact(sum, row.amountMinor!!) } }.getOrNull()
        return FocusedMovementTotals(amount, selected)
    }
}
