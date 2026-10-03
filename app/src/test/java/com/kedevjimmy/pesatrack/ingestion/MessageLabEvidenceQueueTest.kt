package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLabEvidenceQueueTest {
    @Test
    fun confirmedFinancialShapesRankBeforeUnconfirmed() {
        val ranked =
            MessageLabEvidenceQueue
                .prioritize(
                    listOf(
                        shape(
                            id = "a",
                            occurrences = 20,
                            lastSeen = 300L,
                            decision = null
                        ),
                        shape(
                            id = "b",
                            occurrences = 1,
                            lastSeen = 100L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        )
                    )
                )

        assertEquals(
            listOf("b", "a"),
            ranked.map {
                it.shapeId
            }
        )
    }

    @Test
    fun semanticLabelBreaksPriorityWithinConfirmedFinancialEvidence() {
        val ranked =
            MessageLabEvidenceQueue
                .prioritize(
                    listOf(
                        shape(
                            id = "unlabeled",
                            occurrences = 9,
                            lastSeen = 900L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        ),
                        shape(
                            id = "labeled",
                            occurrences = 1,
                            lastSeen = 100L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL,
                            semanticLabel =
                                MessageLabSemanticLabel
                                    .LOAN_REPAYMENT
                        )
                    )
                )

        assertEquals(
            listOf(
                "labeled",
                "unlabeled"
            ),
            ranked.map {
                it.shapeId
            }
        )
    }

    @Test
    fun occurrenceCountBreaksPriorityWithinSameDecisionClass() {
        val ranked =
            MessageLabEvidenceQueue
                .prioritize(
                    listOf(
                        shape(
                            id = "low",
                            occurrences = 2,
                            lastSeen = 500L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        ),
                        shape(
                            id = "high",
                            occurrences = 7,
                            lastSeen = 100L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        )
                    )
                )

        assertEquals(
            listOf("high", "low"),
            ranked.map {
                it.shapeId
            }
        )
    }

    @Test
    fun familyRecurrenceBreaksPriorityWithinSameDecisionClass() {
        val ranked =
            MessageLabEvidenceQueue
                .prioritize(
                    listOf(
                        shape(
                            id = "exact-high",
                            occurrences = 4,
                            familyOccurrences = 4,
                            lastSeen = 900L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        ),
                        shape(
                            id = "family-high",
                            occurrences = 1,
                            familyOccurrences = 8,
                            lastSeen = 100L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        )
                    )
                )

        assertEquals(
            listOf(
                "family-high",
                "exact-high"
            ),
            ranked.map {
                it.shapeId
            }
        )
    }

    @Test
    fun recencyBreaksEqualOccurrenceTies() {
        val ranked =
            MessageLabEvidenceQueue
                .prioritize(
                    listOf(
                        shape(
                            id = "older",
                            occurrences = 3,
                            lastSeen = 100L,
                            decision = null
                        ),
                        shape(
                            id = "newer",
                            occurrences = 3,
                            lastSeen = 900L,
                            decision = null
                        )
                    )
                )

        assertEquals(
            listOf("newer", "older"),
            ranked.map {
                it.shapeId
            }
        )
    }

    @Test
    fun confirmedFinancialFiltersOnlyExplicitFinancialEvidence() {
        val confirmed =
            MessageLabEvidenceQueue
                .confirmedFinancial(
                    listOf(
                        shape(
                            id = "financial",
                            occurrences = 1,
                            lastSeen = 1L,
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        ),
                        shape(
                            id = "unknown",
                            occurrences = 1,
                            lastSeen = 2L,
                            decision = null
                        ),
                        shape(
                            id = "ignored",
                            occurrences = 1,
                            lastSeen = 3L,
                            decision =
                                MessageLabUserDecision
                                    .NOT_FINANCIAL
                        )
                    )
                )

        assertEquals(
            listOf("financial"),
            confirmed.map {
                it.shapeId
            }
        )
    }

    private fun shape(
        id: String,
        occurrences: Int,
        lastSeen: Long,
        decision:
            MessageLabUserDecision?,
        semanticLabel:
            MessageLabSemanticLabel? = null,
        familyOccurrences: Int = 0
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            sender = "Source",
            sourcePackage = null,
            sourceKind = "historical_financial_message",
            redactedBody = "<AMOUNT>",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs =
                lastSeen,
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
