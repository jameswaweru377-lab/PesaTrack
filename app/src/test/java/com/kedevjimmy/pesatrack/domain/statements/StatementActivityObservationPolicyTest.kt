package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementActivityObservationPolicyTest {
    private fun row(
        id: Long = 42L,
        account: String? = "wallet",
        source: String = "historical_sms",
        type: String = "SENT_MONEY",
        direction: String = "EXPENSE",
        reference: String? = "ABC1234567",
        amount: Long = 12_000L,
        state: String = "AUTO_CLASSIFIED",
        category: String = "OTHER",
        linked: Boolean = false,
        reversed: Boolean = false,
        epoch: Long = 1_782_800_000_000L
    ) = StatementActivityObservationPolicy.ActivityLeg(
        transactionId = id, accountId = account, sourceKind = source,
        type = type, direction = direction, currency = "KES",
        amountMinor = amount, occurredAt = epoch, reference = reference,
        reviewState = state, category = category,
        structurallyLinked = linked,
        reversalOfCode = if (reversed) "XYZ9876543" else null
    )

    private val owned = setOf("wallet", "equity")

    @Test fun messageCashLegsProduceSignedIndependentActivityObservations() {
        val result = StatementActivityObservationPolicy.project(listOf(
            row(id = 2, source = "configured_notification", type = "RECEIVED",
                direction = "INCOME", account = "equity", amount = 9_000),
            row(id = 1, type = "PAY_BILL", amount = 3_000),
            row(id = 3, direction = "TRANSFER", type = "RECEIVED", amount = 8_000)
        ), owned)
        assertEquals(listOf("activity:1", "activity:2", "activity:3"), result.map { it.id })
        assertEquals(listOf(-3_000L, 9_000L, 8_000L), result.map { it.movementMinor })
        assertTrue(result.all { it.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY })
        assertTrue(result.all { it.ownershipConfirmed && !it.sourceTotalsVerified })
    }

    @Test fun pdfOriginManualAndUnownedRowsCannotConfirmTheirOwnStatement() {
        val result = StatementActivityObservationPolicy.project(listOf(
            row(id = 1, source = "statement_import"),
            row(id = 2, source = "manual_entry"),
            row(id = 3, source = "manual_sms_import"),
            row(id = 4, account = "unknown"),
            row(id = 5, account = null),
            row(id = 6, source = "historical_sms")
        ), owned)
        assertEquals(listOf("activity:6"), result.map { it.id })
    }

    @Test fun contradictorySignAndUnknownTransferDirectionCannotBecomeProof() {
        val projected = StatementActivityObservationPolicy.project(listOf(
            row(id = 1, type = "RECEIVED", direction = "EXPENSE"),
            row(id = 2, type = "OTHER", direction = "TRANSFER"),
            row(id = 3, type = "REVERSAL", direction = "INCOME"),
            row(id = 4, amount = 0),
            row(id = 5, amount = Long.MAX_VALUE),
            row(id = 6, type = "OTHER", direction = "INCOME")
        ), owned)
        assertEquals(listOf("activity:5", "activity:6"), projected.map { it.id })
        assertEquals(listOf(-Long.MAX_VALUE, 12_000L), projected.map { it.movementMinor })
    }

    @Test fun protectedRolesRemainVisibleButNeverQualifyForAutomaticMatching() {
        val protected = listOf(
            row(id = 1, state = "EXCLUDED"),
            row(id = 2, category = "DEBT_LOAN"),
            row(id = 3, category = "INVESTMENT"),
            row(id = 4, category = "SAVINGS"),
            row(id = 5, category = "REFUND"),
            row(id = 6, linked = true),
            row(id = 7, reversed = true),
            row(id = 9, category = "FEES")
        )
        val result = StatementActivityObservationPolicy.project(protected + row(id = 8), owned)
        assertEquals(9, result.size)
        assertTrue(result.filterNot { it.id == "activity:8" }.all { it.roleProtected })
        assertFalse(result.single { it.id == "activity:8" }.roleProtected)
    }

    @Test fun absentReferenceRemainsEvidenceButCannotCreateExactLink() {
        val projected = StatementActivityObservationPolicy.project(listOf(row(reference = null)), owned)
        assertEquals(1, projected.size)
        assertNull(projected.single().reference)
        val pdf = projected.single().copy(id = "pdf:1",
            source = StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
            sourceId = "pdf:1")
        assertTrue(StatementEvidenceCorrelationPolicy.reconcile(projected + pdf, owned)
            .uniqueLinks.isEmpty())
    }
}
