package com.kedevjimmy.pesatrack.data.local

data class StoredTransaction(
    val id: Long,
    val transactionCode: String?,
    val fingerprint: String,
    val type: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val balanceMinor: Long?,
    val counterparty: String?,
    val counterpartyPhone: String?,
    val occurredAtText: String?,
    val occurredAtEpochMs: Long,
    val receivedAtEpochMs: Long,
    val sourcePackage: String?,
    val sourceKind: String,
    val direction: String,
    val category: String,
    val reviewState: String,
    val ruleId: String?,
    val planningMonth: String?,
    val reversalOfCode: String?,
    val accountId: String?,
    val linkedAccountId: String?,
    val payCycleId: String?,
    val cycleCandidateReason: String?,
    val cycleOverride: String?,
    val fulizaFinancedMinor: Long = 0L,
    val fulizaAccessFeeMinor: Long = 0L,
    val fulizaOutstandingMinor: Long? = null,
    val fulizaEventCode: String? = null,
    val evidenceLabel: String? = null,
    val evidenceSource: String? = null,
    val evidenceConfidence: String? = null,
    val evidenceTimeDeltaMs: Long? = null
)

data class LedgerSnapshot(
    val transactionCount: Int,
    val latestTransaction: StoredTransaction?,
    val latestBalanceMinor: Long?,
    val latestBalanceObservedAtEpochMs: Long?
)

data class ActivitySummary(
    val incomeMinor: Long,
    val spentMinor: Long,
    val feesMinor: Long,
    val transferMinor: Long,
    val netMinor: Long,
    val needsReviewCount: Int
)

data class ReviewGroup(
    val key: String,
    val label: String,
    val representativeTransactionId: Long,
    val transactionCount: Int,
    val totalMinor: Long,
    val latestEpochMs: Long
)

data class ActivityLedgerSnapshot(
    val transactions: List<StoredTransaction>,
    val summary: ActivitySummary,
    val reviewGroups: List<ReviewGroup> = emptyList(),
    val possibleDuplicates: List<PossibleDuplicateGroup> = emptyList()
)

data class PossibleDuplicateGroup(
    val key: String,
    val first: StoredTransaction,
    val second: StoredTransaction,
    val timeDeltaMs: Long
) {
    val amountMinor: Long
        get() = first.amountMinor
}

data class ActivityMonthBucket(
    val monthStartEpochMs: Long,
    val transactionCount: Int
)

data class ActivityReviewOverview(
    val reviewGroups: List<ReviewGroup>,
    val possibleDuplicates: List<PossibleDuplicateGroup>
)
