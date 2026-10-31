package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementFallbackProbeGateTest {
    @Test fun unsupportedLayoutMayBeInspectedInMemoryOnly() {
        assertTrue(StatementFallbackProbeGate.mayRunReadOnlyProbe(
            StatementKnownImportOutcome.UNSUPPORTED_LAYOUT))
    }

    @Test fun accountFinancialPasswordDeviceAndUnknownRefusalsRemainClosed() {
        val forbidden = listOf(
            StatementKnownImportOutcome.ISSUER_REJECTED,
            StatementKnownImportOutcome.ACCOUNT_REJECTED,
            StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED,
            StatementKnownImportOutcome.PASSWORD_REJECTED,
            StatementKnownImportOutcome.UNSUPPORTED_DEVICE,
            StatementKnownImportOutcome.OTHER_FAILURE
        )
        assertTrue(forbidden.all { !StatementFallbackProbeGate.mayRunReadOnlyProbe(it) })
    }

    @Test fun equityUnverifiedIssuerNeverBecomesFallbackAdmission() {
        assertFalse(StatementFallbackProbeGate.mayRunReadOnlyProbe(
            StatementKnownImportOutcome.ISSUER_REJECTED))
    }
}
