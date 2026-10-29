package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementEvidenceCorrelationPolicyTest {
    private val timestamp = 1_782_800_000_000L
    private val owned = setOf("wallet", "equity")

    private fun observation(
        id: String, source: StatementEvidenceCorrelationPolicy.Source,
        account: String = "wallet", amount: Long = -1_000L,
        reference: String? = "ABC1234567", sourceId: String = id,
        time: Long = timestamp, currency: String = "KES",
        confirmed: Boolean = true, roleProtected: Boolean = false
    ) = StatementEvidenceCorrelationPolicy.Observation(
        id, source, sourceId, account, currency, time, amount,
        reference, confirmed, roleProtected
    )

    @Test fun exactIndependentActivityAndStatementLegMayCorroborate() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY),
            observation("pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                sourceId = "file1", time = timestamp + 30_000L)
        ), owned)
        assertEquals(1, result.uniqueLinks.size)
        assertEquals(StatementEvidenceCorrelationPolicy.LinkKind.SAME_LEG, result.uniqueLinks.single().kind)
        assertTrue(result.ambiguousIds.isEmpty())
    }

    @Test fun oppositeConfirmedAccountsCanBeAnOwnedTransferCandidate() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("equity-debit", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "equity"),
            observation("mpesa-credit", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                account = "wallet", amount = 1_000L)
        ), owned)
        assertEquals(StatementEvidenceCorrelationPolicy.LinkKind.OWNED_TRANSFER,
            result.uniqueLinks.single().kind)
    }

    @Test fun missingBankReferenceNeverProvesATransfer() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "equity", reference = null),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                amount = 1_000L)
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
        assertEquals(setOf("bank"), result.missingReferenceIds)
    }

    @Test fun wrongOwnershipOrUnconfirmedSourceNeverCorroborates() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "someone-else"),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                amount = 1_000L),
            observation("unconfirmed", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "equity", confirmed = false)
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
    }

    @Test fun differentCurrencyDateAmountOrReferenceCannotCreateLinks() {
        val base = observation("first", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            base,
            observation("currency", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, currency = "USD"),
            observation("late", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                time = timestamp + 3 * 24 * 60 * 60 * 1_000L),
            observation("amount", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, amount = -1_100L),
            observation("reference", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                reference = "ZYX9876543")
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
    }

    @Test fun feeAndPrincipalWithSameReferenceRemainDifferentCashLegs() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("principal", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            observation("fee", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, amount = -40L),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        ), owned)
        assertEquals(1, result.uniqueLinks.size)
        assertFalse(result.uniqueLinks.single().ids.contains("fee"))
    }

    @Test fun competingSameLegCandidatesAreAmbiguousAndNotDeduplicated() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY),
            observation("pdf-a", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, sourceId = "a"),
            observation("pdf-b", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, sourceId = "b")
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
        assertEquals(setOf("activity", "pdf-a", "pdf-b"), result.ambiguousIds)
    }

    @Test fun tripleActivityWalletAndBankRelationsRemainUnresolved() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
            observation("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "equity", amount = 1_000L)
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
        assertTrue(result.ambiguousIds.contains("wallet"))
    }

    @Test fun protectedOrShortReferenceCannotCreateLinks() {
        val result = StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("protected", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, roleProtected = true),
            observation("statement", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
            observation("short", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                reference = "AB123"),
            observation("other", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                reference = "AB123", sourceId = "other")
        ), owned)
        assertTrue(result.uniqueLinks.isEmpty())
        assertTrue(result.missingReferenceIds.contains("short"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateObservationIdsAreRejectedBeforeCorrelation() {
        StatementEvidenceCorrelationPolicy.reconcile(listOf(
            observation("duplicate", StatementEvidenceCorrelationPolicy.Source.ACTIVITY),
            observation("duplicate", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        ), owned)
    }
}
