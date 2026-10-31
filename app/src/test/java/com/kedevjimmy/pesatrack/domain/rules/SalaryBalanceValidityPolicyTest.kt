package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryBalanceValidityPolicyTest {
    private val now = 1_000_000_000L

    @Test fun manualBalanceIsNotTrustedWhenLedgerChangesOrCycleRolls() {
        assertFalse(SalaryBalanceValidityPolicy.validManualBalance(
            confirmedAtEpochMs = now - 60_000L, nowEpochMs = now,
            savedCycleStartId = 11L, currentCycleStartId = 11L,
            savedLedgerFingerprint = "10:14", currentLedgerFingerprint = "11:15"
        ))
        assertFalse(SalaryBalanceValidityPolicy.validManualBalance(
            confirmedAtEpochMs = now - 60_000L, nowEpochMs = now,
            savedCycleStartId = 11L, currentCycleStartId = 12L,
            savedLedgerFingerprint = "10:14", currentLedgerFingerprint = "10:14"
        ))
    }

    @Test fun manualBalanceExpiresAndRejectsFutureClockEvidence() {
        assertFalse(SalaryBalanceValidityPolicy.validManualBalance(
            confirmedAtEpochMs = now - 86_400_001L, nowEpochMs = now,
            savedCycleStartId = 11L, currentCycleStartId = 11L,
            savedLedgerFingerprint = "10:14", currentLedgerFingerprint = "10:14"
        ))
        assertFalse(SalaryBalanceValidityPolicy.validManualBalance(
            confirmedAtEpochMs = now + 1L, nowEpochMs = now,
            savedCycleStartId = 11L, currentCycleStartId = 11L,
            savedLedgerFingerprint = "10:14", currentLedgerFingerprint = "10:14"
        ))
    }

    @Test fun unchangedFreshConfirmedEvidenceRemainsAvailable() {
        assertTrue(SalaryBalanceValidityPolicy.validManualBalance(
            confirmedAtEpochMs = now - 86_400_000L, nowEpochMs = now,
            savedCycleStartId = 11L, currentCycleStartId = 11L,
            savedLedgerFingerprint = "10:14", currentLedgerFingerprint = "10:14"
        ))
    }
}
