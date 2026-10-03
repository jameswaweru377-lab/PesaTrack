package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabImplementationReviewTest {
    @Test
    fun onlyEvidenceReadyShapesCanBeImplementationReviewed() {
        val ready =
            shape(
                occurrences = 2,
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT
            )
        val single =
            shape(
                occurrences = 1,
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT
            )
        val unlabeled =
            shape(
                occurrences = 3,
                semanticLabel = null
            )

        assertTrue(
            MessageLabImplementationReview
                .isReviewable(ready)
        )
        assertFalse(
            MessageLabImplementationReview
                .isReviewable(single)
        )
        assertFalse(
            MessageLabImplementationReview
                .isReviewable(unlabeled)
        )
    }

    @Test
    fun snapshotSeparatesUnreviewedNeedMoreAndReady() {
        val snapshot =
            MessageLabImplementationReview
                .snapshot(
                    listOf(
                        shape(
                            id = "unreviewed",
                            occurrences = 2,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT
                        ),
                        shape(
                            id = "more",
                            occurrences = 3,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .TRANSFER,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .NEEDS_MORE_EVIDENCE
                        ),
                        shape(
                            id = "ready",
                            occurrences = 4,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .MONEY_RECEIVED,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        ),
                        shape(
                            id = "single",
                            occurrences = 1,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        )
                    )
                )

        assertEquals(
            3,
            snapshot.evidenceReadyCount
        )
        assertEquals(
            1,
            snapshot.unreviewedCount
        )
        assertEquals(
            1,
            snapshot.needsMoreEvidenceCount
        )
        assertEquals(
            1,
            snapshot.readyForParserWorkCount
        )
    }

    @Test
    fun familyRecurrenceCanMakeOneReviewedVariantReviewable() {
        val ready =
            shape(
                id = "variant-a",
                familyId = "family",
                occurrences = 1,
                familyOccurrences = 2,
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT
            )

        assertTrue(
            MessageLabImplementationReview
                .isReviewable(ready)
        )
    }

    @Test
    fun snapshotCountsOneImplementationTaskPerFamilyAndMeaning() {
        val snapshot =
            MessageLabImplementationReview
                .snapshot(
                    listOf(
                        shape(
                            id = "variant-a",
                            familyId = "family",
                            occurrences = 1,
                            familyOccurrences = 3,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .TRANSFER
                        ),
                        shape(
                            id = "variant-b",
                            familyId = "family",
                            occurrences = 2,
                            familyOccurrences = 3,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .TRANSFER,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        )
                    )
                )

        assertEquals(
            1,
            snapshot.evidenceReadyCount
        )
        assertEquals(
            0,
            snapshot.unreviewedCount
        )
        assertEquals(
            0,
            snapshot.needsMoreEvidenceCount
        )
        assertEquals(
            1,
            snapshot.readyForParserWorkCount
        )
    }

    @Test
    fun deferredFamilyDoesNotReappearAsUnreviewedThroughSiblingVariant() {
        val snapshot =
            MessageLabImplementationReview
                .snapshot(
                    listOf(
                        shape(
                            id = "variant-a",
                            familyId = "family",
                            occurrences = 1,
                            familyOccurrences = 3,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .NEEDS_MORE_EVIDENCE
                        ),
                        shape(
                            id = "variant-b",
                            familyId = "family",
                            occurrences = 2,
                            familyOccurrences = 3,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT
                        )
                    )
                )

        assertEquals(
            1,
            snapshot.evidenceReadyCount
        )
        assertEquals(
            0,
            snapshot.unreviewedCount
        )
        assertEquals(
            1,
            snapshot.needsMoreEvidenceCount
        )
        assertEquals(
            0,
            snapshot.readyForParserWorkCount
        )
    }

    @Test
    fun implementationReadyCandidatesDeduplicateFamilyAndMeaning() {
        val ready =
            MessageLabImplementationReview
                .readyForParserWork(
                    listOf(
                        shape(
                            id = "older",
                            familyId = "family",
                            occurrences = 2,
                            familyOccurrences = 4,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .BALANCE_UPDATE,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        ),
                        shape(
                            id = "newer",
                            familyId = "family",
                            occurrences = 2,
                            familyOccurrences = 4,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .BALANCE_UPDATE,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        )
                    )
                )

        assertEquals(
            1,
            ready.size
        )
    }

    @Test
    fun readyForParserWorkRequiresFreshEvidenceReadinessToo() {
        val ready =
            MessageLabImplementationReview
                .readyForParserWork(
                    listOf(
                        shape(
                            id = "good",
                            occurrences = 2,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .BALANCE_UPDATE,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        ),
                        shape(
                            id = "stale",
                            occurrences = 1,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .BALANCE_UPDATE,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        ),
                        shape(
                            id = "more",
                            occurrences = 5,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .LOAN_RECEIVED,
                            implementationReview =
                                MessageLabImplementationReviewState
                                    .NEEDS_MORE_EVIDENCE
                        )
                    )
                )

        assertEquals(
            listOf("good"),
            ready.map {
                it.shapeId
            }
        )
    }

    private fun shape(
        id: String = "shape",
        familyId: String = id,
        occurrences: Int,
        familyOccurrences: Int =
            occurrences,
        semanticLabel:
            MessageLabSemanticLabel?,
        implementationReview:
            MessageLabImplementationReviewState? =
            null
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            familyId =
                familyId,
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody =
                "<AMOUNT> financial message",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount =
                occurrences,
            familyOccurrenceCount =
                familyOccurrences,
            userDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            semanticLabel =
                semanticLabel,
            implementationReview =
                implementationReview
        )
}
