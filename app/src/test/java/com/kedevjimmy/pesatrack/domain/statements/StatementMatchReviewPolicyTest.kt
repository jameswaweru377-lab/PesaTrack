package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementMatchReviewPolicyTest {
    private fun observation(id: String, account: String, amount: Long,
        source: StatementEvidenceCorrelationPolicy.Source) =
        StatementEvidenceCorrelationPolicy.Observation(id, source, id, account, "KES",
            1_700_000_000_000L, amount, "ABC1234567", true, sourceTotalsVerified = true)

    @Test fun combinesCopiesIntoOneTransferSuggestionWithoutRepeatingThem() {
        val rows = listOf(observation("bank", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            observation("wallet", "wallet", 500, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT),
            observation("sms", "wallet", 500, StatementEvidenceCorrelationPolicy.Source.ACTIVITY))
        val proof = StatementCrossSourceEventPolicy.Result(listOf(setOf("wallet", "sms")),
            listOf(StatementCrossSourceEventPolicy.TransferCandidate(setOf("bank"), setOf("wallet", "sms"))),
            emptySet(), emptySet())
        val result = StatementMatchReviewPolicy.project(rows, proof)
        assertEquals(1, result.transfers.size)
        assertTrue(result.sameMovements.isEmpty())
        assertEquals(2, result.transfers.single().incoming.size)
    }

    @Test fun incompleteOrAmbiguousEvidenceNeverBecomesSuggestion() {
        val rows = listOf(observation("bank", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT))
        val proof = StatementCrossSourceEventPolicy.Result(listOf(setOf("bank", "missing")),
            listOf(StatementCrossSourceEventPolicy.TransferCandidate(setOf("bank"), setOf("missing"))),
            setOf("bank"), emptySet())
        val result = StatementMatchReviewPolicy.project(rows, proof)
        assertTrue(result.transfers.isEmpty())
        assertTrue(result.sameMovements.isEmpty())
        assertEquals(1, result.ambiguousCount)
    }

    @Test fun transferAcrossYearBoundaryRemainsVisibleInBothRelevantPeriods() {
        val bank = observation("bank", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
        val wallet = observation("wallet", "wallet", 500, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
            .copy(occurredAt = bank.occurredAt + 1_000)
        val proof = StatementCrossSourceEventPolicy.Result(emptyList(),
            listOf(StatementCrossSourceEventPolicy.TransferCandidate(setOf("bank"), setOf("wallet"))),
            emptySet(), emptySet())
        assertEquals(1, StatementMatchReviewPolicy.project(listOf(bank, wallet), proof,
            bank.occurredAt, wallet.occurredAt).transfers.size)
        assertEquals(1, StatementMatchReviewPolicy.project(listOf(bank, wallet), proof,
            wallet.occurredAt, wallet.occurredAt + 1_000).transfers.size)
        assertTrue(StatementMatchReviewPolicy.project(listOf(bank, wallet), proof,
            wallet.occurredAt + 1_000, wallet.occurredAt + 2_000).transfers.isEmpty())
    }

    @Test fun inconsistentTransferCannotAppearEvenIfProofIsMalformed() {
        val rows = listOf(observation("bank", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            observation("wallet", "wallet", 600, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT))
        val proof = StatementCrossSourceEventPolicy.Result(emptyList(),
            listOf(StatementCrossSourceEventPolicy.TransferCandidate(setOf("bank"), setOf("wallet"))), emptySet(), emptySet())
        assertTrue(StatementMatchReviewPolicy.project(rows, proof).transfers.isEmpty())
    }

    @Test fun yearBoundarySearchIncludesCompetitorOfAnOutsideCandidate() {
        val yearEnd = 1_800_000_000_000L
        val hour = 60L * 60 * 1_000
        val first = observation("first", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
            .copy(occurredAt = yearEnd - hour)
        val middle = observation("middle", "wallet", 500, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
            .copy(occurredAt = yearEnd + 46 * hour)
        val competitor = observation("competitor", "other", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
            .copy(occurredAt = yearEnd + 92 * hour)
        val window = StatementMatchReviewPolicy.evidenceWindow(yearEnd - 365L * 24 * hour, yearEnd)
        assertTrue(competitor.occurredAt < window.endExclusive)
        val rows = listOf(first, middle, competitor)
        val proof = StatementCrossSourceEventPolicy.reconcile(rows, setOf("bank", "wallet", "other"))
        val review = StatementMatchReviewPolicy.project(rows, proof, yearEnd - 365L * 24 * hour, yearEnd)
        assertTrue(review.transfers.isEmpty())
        assertTrue(review.ambiguousCount > 0)
    }
}
