package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementPossibleTransferPolicyTest {
    private val hour = 3_600_000L
    private val base = 1_780_000_000_000L
    private fun row(id: String, account: String, amount: Long, offsetHours: Long = 0,
                    reference: String? = null, totals: Boolean = true, protected: Boolean = false) =
        StatementEvidenceCorrelationPolicy.Observation(id, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
            "source-$id", account, "KES", base + offsetHours * hour, amount, reference,
            ownershipConfirmed = true, roleProtected = protected, sourceTotalsVerified = totals)
    private fun emptyProof() = StatementCrossSourceEventPolicy.Result(emptyList(), emptyList(), emptySet(), emptySet())

    @Test fun uniqueOppositeAmountsAcrossAccountsAreOnlySuggestions() {
        val out = row("out", "bank", -500, reference = "BANK123456")
        val incoming = row("in", "wallet", 500, 24, "WALLET1234")
        val result = StatementPossibleTransferPolicy.suggest(listOf(out, incoming), emptyProof(), base, base + 365 * 24 * hour)
        assertEquals(1, result.size)
        assertEquals(out.id, result.single().outgoing.id)
        assertEquals(incoming.id, result.single().incoming.id)
    }

    @Test fun competingAmountsAreNotPresentedAsUnique() {
        val out = row("out", "bank", -500)
        val incoming = row("in", "wallet", 500, 1)
        val competitor = row("other", "wallet", 500, 2)
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming, competitor), emptyProof(), base, base + 10 * hour).isEmpty())
    }

    @Test fun neverSuggestUnverifiedProtectedOrAlreadyMatchedRows() {
        val out = row("out", "bank", -500)
        val incoming = row("in", "wallet", 500, 1)
        val proof = StatementCrossSourceEventPolicy.Result(emptyList(), emptyList(), setOf("out"), emptySet())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming), proof, base, base + 10 * hour).isEmpty())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming.copy(sourceTotalsVerified = false)), emptyProof(), base, base + 10 * hour).isEmpty())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out.copy(roleProtected = true), incoming), emptyProof(), base, base + 10 * hour).isEmpty())
        val linked = StatementCrossSourceEventPolicy.Result(emptyList(),
            listOf(StatementCrossSourceEventPolicy.TransferCandidate(setOf("out"), setOf("in"))), emptySet(), emptySet())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming), linked, base, base + 10 * hour).isEmpty())
        val rival = row("other", "wallet", 500, 2, totals = false)
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming, rival), emptyProof(), base, base + 10 * hour).isEmpty())
    }

    @Test fun sameAccountAndLongTimeApartAreNotTransfers() {
        val out = row("out", "bank", -500)
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, row("in", "bank", 500)), emptyProof(), base, base + 100 * hour).isEmpty())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, row("in", "wallet", 500, 49)), emptyProof(), base, base + 100 * hour).isEmpty())
    }

    @Test fun periodEdgesAndInvalidAmountsAreHandledSafely() {
        val out = row("out", "bank", -500, -1)
        val incoming = row("in", "wallet", 500, 1)
        assertEquals(1, StatementPossibleTransferPolicy.suggest(listOf(out, incoming), emptyProof(), base, base + 24 * hour).size)
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out.copy(movementMinor = Long.MIN_VALUE), incoming), emptyProof(), base, base + 24 * hour).isEmpty())
        assertTrue(StatementPossibleTransferPolicy.suggest(listOf(out, incoming), emptyProof(), base + 10 * hour, base + 24 * hour).isEmpty())
    }
}
