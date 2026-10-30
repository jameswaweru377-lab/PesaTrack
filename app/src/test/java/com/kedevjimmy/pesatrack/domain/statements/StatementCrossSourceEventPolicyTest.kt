package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementCrossSourceEventPolicyTest {
    private val at = 1_782_800_000_000L
    private val owned = setOf("wallet", "bank")
    private fun observation(
        id: String, source: StatementEvidenceCorrelationPolicy.Source,
        account: String = "wallet", movement: Long = 5_000L,
        reference: String? = "ABC1234567", atTime: Long = at,
        documentId: String = id, verified: Boolean = true,
        roleProtected: Boolean = false, currency: String = "KES", confirmed: Boolean = true
    ) = StatementEvidenceCorrelationPolicy.Observation(
        id, source, documentId, account, currency, atTime, movement,
        reference, confirmed, roleProtected,
        sourceTotalsVerified = verified
    )

    @Test fun exactActivityAndPdfAreOneCashLeg() {
        val activity = observation("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
            verified = false)
        val statement = observation("wallet-pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
            atTime = at + 30_000)
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(statement, activity), owned)
        assertEquals(listOf(setOf("activity", "wallet-pdf")), result.sameLegGroups)
        assertTrue(result.transferCandidates.isEmpty())
        assertTrue(result.ambiguousIds.isEmpty())
    }

    @Test fun bankWalletTransferIsDetectedAfterCollapsingActivityAndWalletPdf() {
        val activity = observation("activity-wallet", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
            verified = false)
        val wallet = observation("wallet-pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        val bank = observation("bank-pdf", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
            account = "bank", movement = -5_000L, atTime = at + 120_000)
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(bank, wallet, activity), owned)
        assertEquals(listOf(setOf("activity-wallet", "wallet-pdf")), result.sameLegGroups)
        assertEquals(1, result.transferCandidates.size)
        assertEquals(setOf("bank-pdf"), result.transferCandidates.single().outgoingIds)
        assertEquals(setOf("activity-wallet", "wallet-pdf"), result.transferCandidates.single().incomingIds)
        assertTrue(result.ambiguousIds.isEmpty())
    }

    @Test fun twoVerifiedStatementSidesCanDescribeOneTransferWithoutActivity() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("bank-out", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -200L),
            observation("wallet-in", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                movement = 200L)
        ), owned)
        assertEquals(1, result.transferCandidates.size)
        assertTrue(result.sameLegGroups.isEmpty())
    }

    @Test fun duplicateActivityCandidatesAndCompetingSameDocumentLegsStayAmbiguous() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("activity-a", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
                documentId = "activity-ledger"),
            observation("activity-b", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
                documentId = "activity-ledger"),
            observation("pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        ), owned)
        assertTrue(result.sameLegGroups.isEmpty())
        assertTrue(result.transferCandidates.isEmpty())
        assertEquals(setOf("activity-a", "activity-b", "pdf"), result.ambiguousIds)
    }

    @Test fun duplicatePdfCopiesCanCorroborateOnlyWithExactTimestampAndDifferentFiles() {
        val activity = observation("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
            verified = false, atTime = at + 60_000)
        val doc1 = observation("pdf-1", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
            documentId = "file-1")
        val doc2 = observation("pdf-2", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
            documentId = "file-2")
        val good = StatementCrossSourceEventPolicy.reconcile(listOf(activity, doc1, doc2), owned)
        assertEquals(listOf(setOf("activity", "pdf-1", "pdf-2")), good.sameLegGroups)

        val moved = StatementCrossSourceEventPolicy.reconcile(listOf(
            activity, doc1, doc2.copy(occurredAt = at + 61_000)
        ), owned)
        assertTrue(moved.sameLegGroups.isEmpty())
        assertEquals(setOf("activity", "pdf-1", "pdf-2"), moved.ambiguousIds)
    }

    @Test fun matchingMoneyAndTimeWithoutBankReferenceNeverProveOwnedTransfer() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, reference = null),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        ), owned)
        assertTrue(result.sameLegGroups.isEmpty())
        assertTrue(result.transferCandidates.isEmpty())
        assertEquals(setOf("bank", "wallet"), result.unlinkedIds)
    }

    @Test fun missingOwnershipProtectedRolesWrongDateAndCurrencyNeverBecomeProof() {
        val base = observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        val other = listOf(
            observation("unowned", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "not-mine", movement = -5_000L),
            observation("protected", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, roleProtected = true),
            observation("unverified", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, verified = false),
            observation("late", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, atTime = at + 3L * 86_400_000L),
            observation("usd", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, currency = "USD"),
            observation("same-sign", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank")
        )
        for (candidate in other) {
            val result = StatementCrossSourceEventPolicy.reconcile(listOf(base, candidate), owned)
            assertTrue(candidate.id, result.sameLegGroups.isEmpty())
            assertTrue(candidate.id, result.transferCandidates.isEmpty())
        }
    }

    @Test fun threeAccountCollisionCannotChooseARecipient() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
            observation("bank-out", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L),
            observation("third", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "third", movement = -5_000L)
        ), owned + "third")
        assertTrue(result.transferCandidates.isEmpty())
        assertEquals(setOf("wallet", "bank-out", "third"), result.ambiguousIds)
    }

    @Test fun disputedTransferSideCannotPartiallyConfirmTheOtherSide() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("wallet-pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
            observation("wallet-activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
                verified = false),
            observation("bank-1", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, documentId = "file-a"),
            observation("bank-2", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, documentId = "file-a")
        ), owned)
        assertTrue(result.sameLegGroups.isEmpty())
        assertTrue(result.transferCandidates.isEmpty())
        assertEquals(4, result.ambiguousIds.size)
    }

    @Test fun feeAndPrincipalWithSameReferenceRemainSeparate() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("bank-principal", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L),
            observation("bank-fee", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -200L),
            observation("wallet-credit", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        ), owned)
        assertEquals(1, result.transferCandidates.size)
        assertEquals(setOf("bank-principal"), result.transferCandidates.single().outgoingIds)
        assertTrue(result.unlinkedIds.contains("bank-fee"))
    }

    @Test fun protectedActivityLegVetoesOtherwiseMatchingBankToWalletProposal() {
        for (reason in listOf("excluded", "loan", "refund", "reversal", "structural")) {
            val result = StatementCrossSourceEventPolicy.reconcile(listOf(
                observation("protected-activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
                    verified = false, roleProtected = true),
                observation("wallet-pdf", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
                observation("bank-pdf", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                    account = "bank", movement = -5_000L)
            ), owned)
            assertTrue(reason, result.transferCandidates.isEmpty())
            assertTrue(reason, result.sameLegGroups.isEmpty())
            assertTrue(reason, result.ambiguousIds.contains("wallet-pdf"))
        }
    }

    @Test fun conflictingIndependentPdfAmountsVetoReferenceTransferProposal() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("wallet-a", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                documentId = "wallet-file-a"),
            observation("wallet-b", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                movement = 5_200L, documentId = "wallet-file-b"),
            observation("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                movement = -5_000L, account = "bank", documentId = "bank-file")
        ), owned)
        assertTrue(result.transferCandidates.isEmpty())
        assertTrue(result.sameLegGroups.isEmpty())
        assertEquals(setOf("wallet-a", "wallet-b", "bank"), result.ambiguousIds)
    }

    @Test fun principalAndFeeInOnePdfDoNotConflictAcrossReferenceAmountBuckets() {
        val result = StatementCrossSourceEventPolicy.reconcile(listOf(
            observation("principal", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -5_000L, documentId = "bank-file"),
            observation("fee", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                account = "bank", movement = -200L, documentId = "bank-file"),
            observation("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                documentId = "wallet-file")
        ), owned)
        assertEquals(1, result.transferCandidates.size)
        assertTrue(result.ambiguousIds.isEmpty())
    }
}
