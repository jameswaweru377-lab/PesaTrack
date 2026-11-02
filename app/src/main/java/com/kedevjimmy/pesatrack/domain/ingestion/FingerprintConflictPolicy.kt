package com.kedevjimmy.pesatrack.domain.ingestion

/** Only hard contradictory facts qualify; receipt times and labels are not identity. */
data class FingerprintFacts(
    val code: String?,
    val type: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val occurredAtEpochMs: Long
)

object FingerprintConflictPolicy {
    fun isConflicting(stored: FingerprintFacts, incoming: FingerprintFacts): Boolean {
        val storedCode = stored.code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val incomingCode = incoming.code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        if (storedCode == null || storedCode != incomingCode) return false
        return stored.type != incoming.type ||
            stored.amountMinor != incoming.amountMinor ||
            stored.feeMinor != incoming.feeMinor
    }
}
