package com.kedevjimmy.pesatrack.domain.recurring

enum class RecurringPatternConfidence {
    HIGH,
    MEDIUM
}

data class RecurringPatternObservation(
    val transactionId: Long,
    val identityKey: String,
    val label: String,
    val categoryId: String,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val counterparty: String?,
    val counterpartyPhone: String?,
    val accountId: String?
)

data class RecurringPatternCandidate(
    val patternKey: String,
    val identityKey: String,
    val label: String,
    val categoryId: String,
    val typicalAmountMinor: Long,
    val occurrenceCount: Int,
    val medianIntervalDays: Int,
    val lastObservedAtEpochMs: Long,
    val nextExpectedAtEpochMs: Long,
    val confidence: RecurringPatternConfidence,
    val representativeTransactionId: Long,
    val counterparty: String?,
    val counterpartyPhone: String?,
    val accountId: String?
)
