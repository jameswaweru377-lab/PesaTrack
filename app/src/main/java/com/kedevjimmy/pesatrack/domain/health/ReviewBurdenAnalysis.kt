package com.kedevjimmy.pesatrack.domain.health

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

data class ReviewBurdenObservation(
    val id: Long,
    val occurredAtEpochMs: Long,
    val direction: MoneyDirection,
    val category: TransactionCategory,
    val reviewState: ReviewState,
    val type: MpesaTransactionType,
    val cycleCandidateReason: String?,
    val possibleDuplicate: Boolean
)

data class ReviewBurdenSnapshot(
    val windowDays: Int,
    val observedHistoryDays: Int,
    val eligibleTransactionCount: Int,
    val manualReviewTransactionCount: Int,
    val reviewRatePercent: Double?,
    val mature: Boolean
) {
    companion object {
        fun empty(
            windowDays: Int =
                ReviewBurdenAnalysis
                    .WINDOW_DAYS
        ) =
            ReviewBurdenSnapshot(
                windowDays = windowDays,
                observedHistoryDays = 0,
                eligibleTransactionCount = 0,
                manualReviewTransactionCount = 0,
                reviewRatePercent = null,
                mature = false
            )
    }
}

object ReviewBurdenAnalysis {
    const val WINDOW_DAYS = 30
    const val TARGET_PERCENT = 5.0

    private const val DAY_MS =
        24L * 60L * 60L * 1_000L

    fun calculate(
        observations:
            List<ReviewBurdenObservation>,
        nowEpochMs: Long,
        windowDays: Int = WINDOW_DAYS
    ): ReviewBurdenSnapshot {
        val safeWindowDays =
            windowDays.coerceAtLeast(1)
        val eligible =
            observations
                .filter(::isEligible)
        val firstEligibleEpochMs =
            eligible.minOfOrNull {
                it.occurredAtEpochMs
            }
        val observedHistoryDays =
            firstEligibleEpochMs
                ?.let {
                    (
                        (
                            nowEpochMs -
                                it
                            ).coerceAtLeast(
                                0L
                            ) /
                            DAY_MS +
                            1L
                        ).toInt()
                            .coerceAtMost(
                                safeWindowDays
                            )
                }
                ?: 0
        val mature =
            observedHistoryDays >=
                safeWindowDays
        val startEpochMs =
            windowStartEpochMs(
                nowEpochMs = nowEpochMs,
                windowDays =
                    safeWindowDays
            )
        val inWindow =
            eligible.filter {
                it.occurredAtEpochMs in
                    startEpochMs..nowEpochMs
            }
        val manualReviewCount =
            inWindow.count {
                it.reviewState ==
                    ReviewState
                        .NEEDS_REVIEW ||
                    it.reviewState ==
                    ReviewState
                        .USER_CLASSIFIED
            }
        val rate =
            if (inWindow.isEmpty()) {
                null
            } else {
                manualReviewCount *
                    100.0 /
                    inWindow.size
            }

        return ReviewBurdenSnapshot(
            windowDays =
                safeWindowDays,
            observedHistoryDays =
                observedHistoryDays,
            eligibleTransactionCount =
                inWindow.size,
            manualReviewTransactionCount =
                manualReviewCount,
            reviewRatePercent =
                rate,
            mature =
                mature
        )
    }

    fun windowStartEpochMs(
        nowEpochMs: Long,
        windowDays: Int = WINDOW_DAYS
    ): Long =
        nowEpochMs -
            (
                windowDays
                    .coerceAtLeast(1) -
                    1L
                ) *
            DAY_MS

    private fun isEligible(
        observation:
            ReviewBurdenObservation
    ): Boolean {
        if (
            observation.direction !=
            MoneyDirection.EXPENSE
        ) {
            return false
        }
        if (
            observation.reviewState ==
            ReviewState.EXCLUDED
        ) {
            return false
        }
        if (
            observation.type ==
            MpesaTransactionType.REVERSAL
        ) {
            return false
        }
        if (
            observation
                .cycleCandidateReason !=
            null
        ) {
            return false
        }
        if (
            observation
                .possibleDuplicate
        ) {
            return false
        }
        if (
            observation.category in
            STRUCTURAL_CATEGORIES
        ) {
            return false
        }

        return true
    }

    private val STRUCTURAL_CATEGORIES =
        setOf(
            TransactionCategory
                .DEBT_LOAN,
            TransactionCategory
                .INCOME,
            TransactionCategory
                .TRANSFER,
            TransactionCategory
                .CASH,
            TransactionCategory
                .SAVINGS,
            TransactionCategory
                .INVESTMENT
        )
}
