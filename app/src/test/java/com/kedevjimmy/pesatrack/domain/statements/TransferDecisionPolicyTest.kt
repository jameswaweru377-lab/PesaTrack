package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Test
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Decision
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Result

class TransferDecisionPolicyTest {
    private val out = StatementEvidenceCorrelationPolicy.Observation("out", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
        "sms1", "bank", "KES", 1000, -50000, "ABC123456", true)
    private val incoming = out.copy(id = "in", sourceId = "sms2", accountId = "wallet", movementMinor = 50000)
    private val pair = TransferReconciliationPolicy.Pair(listOf(out), listOf(incoming),
        TransferReconciliationPolicy.Basis.SHARED_REFERENCE, "current")

    @Test fun changedEvidenceCannotSaveOldChoice() {
        assertEquals(Result.STALE, TransferDecisionPolicy.validate("old", null, Decision.CONFIRMED,
            listOf(pair), emptyMap(), emptySet(), true))
    }
    @Test fun secondReviewerCannotOverwriteChangedDecision() {
        assertEquals(Result.STALE, TransferDecisionPolicy.validate("current", null, Decision.NOT_SAME,
            listOf(pair), mapOf("current" to Decision.CONFIRMED), emptySet(), true))
    }
    @Test fun legacyConfirmationBlocksReusingObservation() {
        assertEquals(Result.CONFLICT, TransferDecisionPolicy.validate("current", null, Decision.CONFIRMED,
            listOf(pair), emptyMap(), setOf("out"), true))
    }
    @Test fun explicitSourceAccountCheckRequired() {
        assertEquals(Result.ACCOUNT_CHECK_REQUIRED, TransferDecisionPolicy.validate("current", null, Decision.CONFIRMED,
            listOf(pair), emptyMap(), emptySet(), false))
    }
    @Test fun undoStillWorksIfLegacyConflictAppears() {
        assertEquals(Result.SAVED, TransferDecisionPolicy.validate("current", Decision.CONFIRMED, null,
            listOf(pair), mapOf("current" to Decision.CONFIRMED), setOf("out"), false))
    }
    @Test fun rejectDoesNotRequireAccountAttestation() {
        assertEquals(Result.SAVED, TransferDecisionPolicy.validate("current", null, Decision.NOT_SAME,
            listOf(pair), emptyMap(), emptySet(), false))
    }
    @Test fun validReviewCanBeSaved() {
        assertEquals(Result.SAVED, TransferDecisionPolicy.validate("current", null, Decision.CONFIRMED,
            listOf(pair), emptyMap(), emptySet(), true))
    }
}
