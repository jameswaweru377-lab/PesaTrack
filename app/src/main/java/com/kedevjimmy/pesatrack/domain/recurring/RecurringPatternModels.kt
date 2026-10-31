package com.kedevjimmy.pesatrack.domain.recurring

enum class RecurringPatternConfidence {
    HIGH,
    MEDIUM
}

enum class RecurringPatternCadence {
    WEEKLY,
    FORTNIGHTLY,
    MONTHLY_FIXED_DAY,
    MONTHLY_LAST_DAY,
    TERMLY
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
    val accountId: String?,
    val cadence: RecurringPatternCadence = RecurringPatternCadence.MONTHLY_FIXED_DAY,
    val reason: String = ""
)
