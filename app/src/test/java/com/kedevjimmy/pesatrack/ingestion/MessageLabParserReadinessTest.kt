package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLabParserReadinessTest {
    @Test
    fun unconfirmedShapeNeedsFinancialConfirmation() {
        assertEquals(
            MessageLabParserReadinessState
                .NEEDS_FINANCIAL_CONFIRMATION,
            MessageLabParserReadiness
                .stateFor(
                    shape(
                        decision = null,
                        semanticLabel = null,
                        occurrences = 5
                    )
                )
        )
    }

    @Test
    fun confirmedShapeNeedsMeaningBeforeParserReview() {
        assertEquals(
            MessageLabParserReadinessState
                .NEEDS_MEANING_LABEL,
            MessageLabParserReadiness
                .stateFor(
                    shape(
                        decision =
                            MessageLabUserDecision
                                .CONFIRMED_FINANCIAL,
                        semanticLabel = null,
                        occurrences = 5
                    )
                )
        )
    }

    @Test
    fun singleLabeledObservationIsNotCalledEvidenceReady() {
        assertEquals(
            MessageLabParserReadinessState
                .LABELED_SINGLE_OBSERVATION,
            MessageLabParserReadiness
                .stateFor(
                    shape(
                        decision =
                            MessageLabUserDecision
                                .CONFIRMED_FINANCIAL,
                        semanticLabel =
                            MessageLabSemanticLabel
                                .LOAN_REPAYMENT,
                        occurrences = 1
                    )
                )
        )
    }

    @Test
    fun conservativeFamilyRecurrenceCanSatisfyRepeatedEvidenceFloor() {
        assertEquals(
            MessageLabParserReadinessState
                .EVIDENCE_READY,
            MessageLabParserReadiness
                .stateFor(
                    shape(
                        decision =
                            MessageLabUserDecision
                                .CONFIRMED_FINANCIAL,
                        semanticLabel =
                            MessageLabSemanticLabel
                                .PAYMENT,
                        occurrences = 1,
                        familyOccurrences = 3
                    )
                )
        )
    }

    @Test
    fun repeatedLabeledConfirmedShapeIsEvidenceReady() {
        assertEquals(
            MessageLabParserReadinessState
                .EVIDENCE_READY,
            MessageLabParserReadiness
                .stateFor(
                    shape(
                        decision =
                            MessageLabUserDecision
                                .CONFIRMED_FINANCIAL,
                        semanticLabel =
                            MessageLabSemanticLabel
                                .LOAN_REPAYMENT,
                        occurrences = 2
                    )
                )
        )
    }

    @Test
    fun snapshotSeparatesMeaningAndEvidenceReadiness() {
        val snapshot =
            MessageLabParserReadiness
                .snapshot(
                    listOf(
                        shape(
                            id = "ready",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT,
                            occurrences = 3
                        ),
                        shape(
                            id = "single",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .TRANSFER,
                            occurrences = 1
                        ),
                        shape(
                            id = "needs-meaning",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel = null,
                            occurrences = 4
                        ),
                        shape(
                            id = "unknown",
                            decision = null,
                            semanticLabel = null,
                            occurrences = 4
                        )
                    )
                )

        assertEquals(
            3,
            snapshot.confirmedFinancialCount
        )
        assertEquals(
            2,
            snapshot.meaningLabeledCount
        )
        assertEquals(
            1,
            snapshot.evidenceReadyCount
        )
        assertEquals(
            1,
            snapshot.needsMeaningCount
        )
    }

    @Test
    fun evidenceReadyFiltersOutSingleObservationAndUnlabeledShapes() {
        val ready =
            MessageLabParserReadiness
                .evidenceReady(
                    listOf(
                        shape(
                            id = "ready",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .MONEY_RECEIVED,
                            occurrences = 2
                        ),
                        shape(
                            id = "single",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT,
                            occurrences = 1
                        ),
                        shape(
                            id = "unlabeled",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel = null,
                            occurrences = 9
                        ),
                        shape(
                            id = "ignored",
                            decision =
                                MessageLabUserDecision
                                    .NOT_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .PAYMENT,
                            occurrences = 9
                        )
                    )
                )

        assertEquals(
            listOf("ready"),
            ready.map {
                it.shapeId
            }
        )
    }

    private fun shape(
        id: String = "shape",
        decision:
            MessageLabUserDecision?,
        semanticLabel:
            MessageLabSemanticLabel?,
        occurrences: Int,
        familyOccurrences: Int = 0
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody = "<AMOUNT>",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount =
                occurrences,
            userDecision =
                decision,
            semanticLabel =
                semanticLabel,
            familyOccurrenceCount =
                familyOccurrences
        )
}
