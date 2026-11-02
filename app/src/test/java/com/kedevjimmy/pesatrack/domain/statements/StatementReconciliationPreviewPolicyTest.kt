package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementReconciliationPreviewPolicyTest {
    private fun observation(id: String, source: StatementEvidenceCorrelationPolicy.Source) =
        StatementEvidenceCorrelationPolicy.Observation(id, source, id, "wallet", "KES", 1000,
            -300, "ABC123456", true, sourceTotalsVerified = true)

    @Test fun decisionAndUndoOnlyChangeProposedCount() {
        val group = listOf(observation("sms", StatementEvidenceCorrelationPolicy.Source.ACTIVITY),
            observation("pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT))
        val review = StatementMatchReviewPolicy.Review(emptyList(), listOf(group), 0)
        assertEquals(0, StatementReconciliationPreviewPolicy.project(review, emptyMap()).confirmedCopies)
        val decision = mapOf(group.map { it.id }.toSet() to StatementReconciliationPreviewPolicy.Decision.CONFIRMED)
        assertEquals(1, StatementReconciliationPreviewPolicy.project(review, decision).confirmedCopies)
        assertEquals(0, StatementReconciliationPreviewPolicy.project(review,
            mapOf(group.map { it.id }.toSet() to StatementReconciliationPreviewPolicy.Decision.NOT_SAME)).confirmedCopies)
        assertEquals(0, StatementReconciliationPreviewPolicy.project(review, emptyMap()).confirmedCopies)
    }

    @Test fun protectedOrConflictingGroupsNeverProduceProposals() {
        val group = listOf(observation("a", StatementEvidenceCorrelationPolicy.Source.ACTIVITY).copy(roleProtected = true),
            observation("b", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT))
        val review = StatementMatchReviewPolicy.Review(emptyList(), listOf(group), 1)
        val result = StatementReconciliationPreviewPolicy.project(review,
            mapOf(setOf("a", "b") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED))
        assertEquals(0, result.confirmedCopies)
        assertEquals(1, result.unresolvedEvidenceCount)
    }
}
