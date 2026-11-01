package com.kedevjimmy.pesatrack.domain.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintConflictPolicyTest {
    private val original = FingerprintFacts(
        code = "TQK12ABC34", type = "SENT_MONEY", amountMinor = 45_000L,
        feeMinor = 0L, occurredAtEpochMs = 1_000L
    )

    @Test fun identicalReplayIsNotConflict() {
        assertFalse(FingerprintConflictPolicy.isConflicting(original, original))
    }

    @Test fun laterObservationTimeAloneIsNotConflict() {
        assertFalse(FingerprintConflictPolicy.isConflicting(
            original, original.copy(occurredAtEpochMs = 2_000L)
        ))
    }

    @Test fun contradictoryAmountIsConflict() {
        assertTrue(FingerprintConflictPolicy.isConflicting(
            original, original.copy(amountMinor = 46_000L)
        ))
    }

    @Test fun contradictoryFeeIsConflict() {
        assertTrue(FingerprintConflictPolicy.isConflicting(
            original, original.copy(feeMinor = 200L)
        ))
    }

    @Test fun contradictoryTypeIsConflict() {
        assertTrue(FingerprintConflictPolicy.isConflicting(
            original, original.copy(type = "RECEIVED_MONEY")
        ))
    }

    @Test fun absentOrDifferentCodesCannotEstablishConflict() {
        assertFalse(FingerprintConflictPolicy.isConflicting(
            original, original.copy(code = null, amountMinor = 46_000L)
        ))
        assertFalse(FingerprintConflictPolicy.isConflicting(
            original, original.copy(code = "OTHER123", amountMinor = 46_000L)
        ))
    }
}
