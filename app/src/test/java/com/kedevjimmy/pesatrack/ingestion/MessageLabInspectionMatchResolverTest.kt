package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabInspectionMatchResolverTest {
    @Test
    fun oneSourceSpecificShapeCanProvideSavedDecision() {
        val shape =
            shape(
                id = "sms",
                sourcePackage = null,
                sourceKind =
                    "historical_financial_message",
                decision =
                    MessageLabUserDecision
                        .NOT_FINANCIAL
            )

        val result =
            MessageLabInspectionMatchResolver
                .resolve(
                    listOf(shape)
                )

        assertFalse(
            result
                .ambiguousSourceMatch
        )
        assertEquals(
            1,
            result
                .matchedShapeCount
        )
        assertEquals(
            shape,
            result.shape
        )
    }

    @Test
    fun sameWordingAcrossDifferentSourceChannelsIsAmbiguous() {
        val result =
            MessageLabInspectionMatchResolver
                .resolve(
                    listOf(
                        shape(
                            id = "sms",
                            sourcePackage =
                                null,
                            sourceKind =
                                "historical_financial_message",
                            decision =
                                MessageLabUserDecision
                                    .NOT_FINANCIAL
                        ),
                        shape(
                            id =
                                "notification",
                            sourcePackage =
                                "ke.example.bank",
                            sourceKind =
                                "notification",
                            decision =
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        )
                    )
                )

        assertTrue(
            result
                .ambiguousSourceMatch
        )
        assertEquals(
            2,
            result
                .matchedShapeCount
        )
        assertNull(
            result.shape
        )
    }

    @Test
    fun noStoredShapeReturnsNoDecisionWithoutAmbiguity() {
        val result =
            MessageLabInspectionMatchResolver
                .resolve(
                    emptyList()
                )

        assertFalse(
            result
                .ambiguousSourceMatch
        )
        assertEquals(
            0,
            result
                .matchedShapeCount
        )
        assertNull(
            result.shape
        )
    }

    private fun shape(
        id: String,
        sourcePackage: String?,
        sourceKind: String?,
        decision:
            MessageLabUserDecision
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            sender = "Finance",
            sourcePackage =
                sourcePackage,
            sourceKind =
                sourceKind,
            redactedBody =
                "KES <AMOUNT> adjustment on your account.",
            firstSeenAtEpochMs =
                1L,
            lastSeenAtEpochMs =
                2L,
            occurrenceCount = 1,
            userDecision = decision
        )
}
