package com.kedevjimmy.pesatrack.data.local

data class PayCycle(
    val id: String,
    val startTransactionId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val openingIncomeMinor: Long,
    val carryoverMinor: Long,
    val endingTrackedMinor: Long?,
    val state: String
)

data class CommitmentProgress(
    val ruleId: String,
    val name: String,
    val category: String,
    val plannedMinor: Long,
    val paidMinor: Long,
    val remainingMinor: Long
)

data class SalaryCycleCandidate(
    val transactionId: Long,
    val label: String,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val reason: String
)

data class PayCycleSnapshot(
    val cycle: PayCycle?,
    val startingFundsMinor: Long,
    val additionalIncomeMinor: Long,
    val spentMinor: Long,
    val feesMinor: Long,
    val savingsMinor: Long,
    val remainingCommitmentsMinor: Long,
    val availableToLiveMinor: Long,
    val safeTodayMinor: Long?,
    val estimatedDaysRemaining: Int?,
    val commitments: List<CommitmentProgress>,
    val pendingSalary: SalaryCycleCandidate?,
    val unexplainedFundingGapMinor: Long = 0L,
    val refundsMinor: Long = 0L,
    val paydaySnapshot: PaydaySnapshot? = null
)
