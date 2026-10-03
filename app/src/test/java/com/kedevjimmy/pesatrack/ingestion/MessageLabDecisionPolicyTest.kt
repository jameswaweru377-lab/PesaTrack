package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabDecisionPolicyTest {
    @Test
    fun noDecisionUsesHeuristicBoundary() {
        assertTrue(
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported = true,
                    userDecision = null
                )
        )
        assertFalse(
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported = false,
                    userDecision = null
                )
        )
    }

    @Test
    fun confirmedFinancialKeepsEvidenceEvenIfHeuristicLaterWeakens() {
        assertTrue(
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported = false,
                    userDecision =
                        MessageLabUserDecision
                            .CONFIRMED_FINANCIAL
                )
        )
    }

    @Test
    fun notFinancialSuppressesUnsupportedNoise() {
        assertFalse(
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported = true,
                    userDecision =
                        MessageLabUserDecision
                            .NOT_FINANCIAL
                )
        )
    }

    @Test
    fun strongerCurrentInterpretationCanRetireOldNotFinancialDecision() {
        assertTrue(
            MessageLabDecisionPolicy
                .canRetireAsUnderstood(
                    recognized = true,
                    semanticallyUnderstood =
                        true,
                    userDecision =
                        MessageLabUserDecision
                            .NOT_FINANCIAL
                )
        )
    }

    @Test
    fun recognizedMessageCanRetireNormalOrConfirmedFinancialEvidence() {
        assertTrue(
            MessageLabDecisionPolicy
                .canRetireAsUnderstood(
                    recognized = true,
                    semanticallyUnderstood =
                        false,
                    userDecision = null
                )
        )
        assertTrue(
            MessageLabDecisionPolicy
                .canRetireAsUnderstood(
                    recognized = false,
                    semanticallyUnderstood =
                        true,
                    userDecision =
                        MessageLabUserDecision
                            .CONFIRMED_FINANCIAL
                )
        )
    }
}
