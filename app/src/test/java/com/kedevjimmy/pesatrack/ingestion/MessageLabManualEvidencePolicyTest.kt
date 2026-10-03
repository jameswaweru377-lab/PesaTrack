package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabManualEvidencePolicyTest {
    @Test
    fun uniqueStoredMatchIsReused() {
        val target =
            MessageLabManualEvidencePolicy
                .target(
                    inspectionMatch =
                        MessageLabInspectionMatch(
                            shape =
                                shape(
                                    "existing"
                                ),
                            ambiguousSourceMatch =
                                false,
                            matchedShapeCount = 1
                        ),
                    manualShapeId =
                        "manual"
                )

        assertEquals(
            "existing",
            target.shapeId
        )
        assertTrue(
            target.reuseExistingShape
        )
    }

    @Test
    fun ambiguousSourceMatchUsesSeparateManualShape() {
        val target =
            MessageLabManualEvidencePolicy
                .target(
                    inspectionMatch =
                        MessageLabInspectionMatch(
                            shape = null,
                            ambiguousSourceMatch =
                                true,
                            matchedShapeCount = 2
                        ),
                    manualShapeId =
                        "manual"
                )

        assertEquals(
            "manual",
            target.shapeId
        )
        assertFalse(
            target.reuseExistingShape
        )
    }

    @Test
    fun noStoredMatchUsesSeparateManualShape() {
        val target =
            MessageLabManualEvidencePolicy
                .target(
                    inspectionMatch =
                        MessageLabInspectionMatch(
                            shape = null,
                            ambiguousSourceMatch =
                                false,
                            matchedShapeCount = 0
                        ),
                    manualShapeId =
                        "manual"
                )

        assertEquals(
            "manual",
            target.shapeId
        )
        assertFalse(
            target.reuseExistingShape
        )
    }

    @Test
    fun repeatedManualRegistrationNeverIncrementsEvidenceCount() {
        assertEquals(
            1,
            MessageLabManualEvidencePolicy
                .stableOccurrenceCount(
                    null
                )
        )
        assertEquals(
            1,
            MessageLabManualEvidencePolicy
                .stableOccurrenceCount(
                    1
                )
        )
        assertEquals(
            7,
            MessageLabManualEvidencePolicy
                .stableOccurrenceCount(
                    7
                )
        )
    }

    private fun shape(
        id: String
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody =
                "KES <AMOUNT> paid.",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount = 1
        )
}
