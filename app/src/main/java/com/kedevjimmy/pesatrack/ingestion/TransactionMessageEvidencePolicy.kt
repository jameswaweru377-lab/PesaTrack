package com.kedevjimmy.pesatrack.ingestion

data class TransactionMessageEvidence(
    val sender: String,
    val body: String,
    val receivedAtEpochMs: Long,
    val transactionCode: String?,
    val amountsMinor: List<Long>
)

internal fun matchingTransactionMessage(
    code: String?,
    amountMinor: Long,
    candidates: List<TransactionMessageEvidence>
): TransactionMessageEvidence? {
    val reference = code?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return candidates.filter {
        it.transactionCode?.trim()?.equals(reference, ignoreCase = true) == true &&
            amountMinor in it.amountsMinor
    }.distinctBy { it.sender to it.body }.singleOrNull()
}
