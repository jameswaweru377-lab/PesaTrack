package com.kedevjimmy.pesatrack.domain.health

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewBurdenAnalysisTest {
    private val day =
        24L * 60L * 60L * 1_000L
    private val now =
        100L * day

    @Test
    fun countsNeedsReviewAndUserClassifiedAsManualBurden() {
        val snapshot =
            ReviewBurdenAnalysis
                .calculate(
                    observations =
                        listOf(
                            observation(
                                id = 1,
                                daysAgo = 2,
                                reviewState =
                                    ReviewState
                                        .AUTO_CLASSIFIED
                            ),
                            observation(
                                id = 2,
                                daysAgo = 3,
                                reviewState =
                                    ReviewState
                                        .NEEDS_REVIEW
                            ),
                            observation(
                                id = 3,
                                daysAgo = 4,
                                reviewState =
                                    ReviewState
                                        .USER_CLASSIFIED
                            )
                        ),
                    nowEpochMs = now
                )

        assertEquals(
            3,
            snapshot
                .eligibleTransactionCount
        )
        assertEquals(
            2,
            snapshot
                .manualReviewTransactionCount
        )
        assertEquals(
            66.666,
            snapshot
                .reviewRatePercent!!,
            0.01
        )
    }

    @Test
    fun excludesStructuralAndNonOrdinaryRows() {
        val snapshot =
            ReviewBurdenAnalysis
                .calculate(
                    observations =
                        listOf(
                            observation(
                                id = 1,
                                daysAgo = 1,
                                direction =
                                    MoneyDirection
                                        .TRANSFER,
                                category =
                                    TransactionCategory
                                        .TRANSFER
                            ),
                            observation(
                                id = 2,
                                daysAgo = 1,
                                category =
                                    TransactionCategory
                                        .DEBT_LOAN
                            ),
                            observation(
                                id = 3,
                                daysAgo = 1,
                                type =
                                    MpesaTransactionType
                                        .REVERSAL
                            ),
                            observation(
                                id = 4,
                                daysAgo = 1,
                                reviewState =
                                    ReviewState
                                        .EXCLUDED
                            ),
                            observation(
                                id = 5,
                                daysAgo = 1,
                                possibleDuplicate =
                                    true
                            ),
                            observation(
                                id = 6,
                                daysAgo = 1,
                                cycleCandidateReason =
                                    "salary-cycle"
                            )
                        ),
                    nowEpochMs = now
                )

        assertEquals(
            0,
            snapshot
                .eligibleTransactionCount
        )
        assertEquals(
            0,
            snapshot
                .manualReviewTransactionCount
        )
        assertNull(
            snapshot
                .reviewRatePercent
        )
    }

    @Test
    fun usesRollingThirtyDayWindow() {
        val snapshot =
            ReviewBurdenAnalysis
                .calculate(
                    observations =
                        listOf(
                            observation(
                                id = 1,
                                daysAgo = 5,
                                reviewState =
                                    ReviewState
                                        .AUTO_CLASSIFIED
                            ),
                            observation(
                                id = 2,
                                daysAgo = 40,
                                reviewState =
                                    ReviewState
                                        .NEEDS_REVIEW
                            )
                        ),
                    nowEpochMs = now
                )

        assertEquals(
            1,
            snapshot
                .eligibleTransactionCount
        )
        assertEquals(
            0,
            snapshot
                .manualReviewTransactionCount
        )
        assertEquals(
            0.0,
            snapshot
                .reviewRatePercent!!,
            0.0
        )
    }

    @Test
    fun becomesMatureOnlyAfterFullHistoryWindow() {
        val learning =
            ReviewBurdenAnalysis
                .calculate(
                    observations =
                        listOf(
                            observation(
                                id = 1,
                                daysAgo = 10
                            )
                        ),
                    nowEpochMs = now
                )

        assertFalse(
            learning.mature
        )
        assertEquals(
            11,
            learning
                .observedHistoryDays
        )

        val mature =
            ReviewBurdenAnalysis
                .calculate(
                    observations =
                        listOf(
                            observation(
                                id = 2,
                                daysAgo = 40
                            ),
                            observation(
                                id = 3,
                                daysAgo = 2
                            )
                        ),
                    nowEpochMs = now
                )

        assertTrue(
            mature.mature
        )
        assertEquals(
            30,
            mature
                .observedHistoryDays
        )
    }

    @Test
    fun windowStartIsInclusiveThirtyDaySpan() {
        assertEquals(
            now - 29L * day,
            ReviewBurdenAnalysis
                .windowStartEpochMs(
                    now
                )
        )
    }

    private fun observation(
        id: Long,
        daysAgo: Int,
        direction:
            MoneyDirection =
            MoneyDirection.EXPENSE,
        category:
            TransactionCategory =
            TransactionCategory.OTHER,
        reviewState:
            ReviewState =
            ReviewState.AUTO_CLASSIFIED,
        type:
            MpesaTransactionType =
            MpesaTransactionType
                .BUY_GOODS,
        cycleCandidateReason:
            String? = null,
        possibleDuplicate:
            Boolean = false
    ) =
        ReviewBurdenObservation(
            id = id,
            occurredAtEpochMs =
                now -
                    daysAgo.toLong() *
                    day,
            direction = direction,
            category = category,
            reviewState = reviewState,
            type = type,
            cycleCandidateReason =
                cycleCandidateReason,
            possibleDuplicate =
                possibleDuplicate
        )
}
