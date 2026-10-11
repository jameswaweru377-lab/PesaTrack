package com.kedevjimmy.pesatrack.domain.evidence

data class EvidenceCashFact(
    val accountId: String, val direction: String, val amountMinor: Long,
    val occurredAtEpochMs: Long, val transactionReference: String?
)
data class EvidenceCanonicalFact(
    val accountId: String?, val direction: String, val amountMinor: Long,
    val occurredAtEpochMs: Long, val transactionReference: String?, val sourceKind: String
)

/** A matched evidence row is never an independent economic event. */
object EvidenceLinkSafetyPolicy {
    private const val REFERENCE_WINDOW_MS = 72L * 60L * 60L * 1000L

    fun canLink(source: EvidenceCashFact, canonical: EvidenceCanonicalFact, reason: String): Boolean {
        if (source.accountId.isBlank() || canonical.accountId != source.accountId ||
            source.direction !in setOf("INCOME", "EXPENSE") ||
            source.amountMinor <= 0 || canonical.amountMinor != source.amountMinor ||
            source.occurredAtEpochMs <= 0 || canonical.occurredAtEpochMs <= 0) return false
        if (canonical.direction != source.direction &&
            !(source.direction == "EXPENSE" && canonical.direction == "TRANSFER")) return false
        val delta = if (source.occurredAtEpochMs >= canonical.occurredAtEpochMs)
            source.occurredAtEpochMs - canonical.occurredAtEpochMs
        else canonical.occurredAtEpochMs - source.occurredAtEpochMs
        return when (reason) {
            "CREATED" -> delta == 0L && canonical.sourceKind in setOf("manual_entry", "statement_import")
            "REFERENCE" -> delta <= REFERENCE_WINDOW_MS &&
                !source.transactionReference.isNullOrBlank() &&
                normalize(source.transactionReference) == normalize(canonical.transactionReference)
            else -> false
        }
    }
    private fun normalize(value: String?): String? = value?.trim()
        ?.uppercase(java.util.Locale.ROOT)?.takeIf { it.isNotBlank() }
}
