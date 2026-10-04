package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageInspectorParserHandoffPolicyTest {
    @Test
    fun noShapeExposesNoDeveloperHandoff() {
        val availability =
            MessageInspectorParserHandoffPolicy
                .availabilityFor(null)

        assertFalse(
            availability
                .parserDraftAvailable
        )
        assertFalse(
            availability
                .implementationPacketAvailable
        )
    }

    @Test
    fun singleObservationCannotExposeDraftOrPacket() {
        val availability =
            MessageInspectorParserHandoffPolicy
                .availabilityFor(
                    shape(
                        occurrences = 1,
                        familyOccurrences = 1,
                        review = null
                    )
                )

        assertFalse(
            availability
                .parserDraftAvailable
        )
        assertFalse(
            availability
                .implementationPacketAvailable
        )
    }

    @Test
    fun evidenceReadyExposesDraftButNotImplementationPacket() {
        val availability =
            MessageInspectorParserHandoffPolicy
                .availabilityFor(
                    shape(
                        occurrences = 2,
                        familyOccurrences = 2,
                        review = null
                    )
                )

        assertTrue(
            availability
                .parserDraftAvailable
        )
        assertFalse(
            availability
                .implementationPacketAvailable
        )
    }

    @Test
    fun readyForParserWorkExposesDraftAndImplementationPacket() {
        val availability =
            MessageInspectorParserHandoffPolicy
                .availabilityFor(
                    shape(
                        occurrences = 1,
                        familyOccurrences = 3,
                        review =
                            MessageLabImplementationReviewState
                                .READY_FOR_PARSER_WORK
                    )
                )

        assertTrue(
            availability
                .parserDraftAvailable
        )
        assertTrue(
            availability
                .implementationPacketAvailable
        )
    }

    @Test
    fun needsMoreEvidenceNeverExposesImplementationPacket() {
        val availability =
            MessageInspectorParserHandoffPolicy
                .availabilityFor(
                    shape(
                        occurrences = 3,
                        familyOccurrences = 3,
                        review =
                            MessageLabImplementationReviewState
                                .NEEDS_MORE_EVIDENCE
                    )
                )

        assertTrue(
            availability
                .parserDraftAvailable
        )
        assertFalse(
            availability
                .implementationPacketAvailable
        )
    }

    private fun shape(
        occurrences: Int,
        familyOccurrences: Int,
        review:
            MessageLabImplementationReviewState?
    ) =
        UnsupportedMessageShape(
            shapeId = "shape",
            familyId = "family",
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "manual_message_inspector",
            redactedBody =
                "KES <AMOUNT> paid to <ENTITY>.",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount =
                occurrences,
            userDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            semanticLabel =
                MessageLabSemanticLabel
                    .PAYMENT,
            familyOccurrenceCount =
                familyOccurrences,
            familyVariantCount = 1,
            implementationReview =
                review
        )
}
