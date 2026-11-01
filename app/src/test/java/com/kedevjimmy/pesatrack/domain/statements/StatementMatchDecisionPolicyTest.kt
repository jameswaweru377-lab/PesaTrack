package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementMatchDecisionPolicyTest {
    private fun row(id: String, account: String, amount: Long) =
        StatementEvidenceCorrelationPolicy.Observation(id, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
            id, account, "KES", 1_700_000_000_000L, amount, "ABC1234567", true, sourceTotalsVerified = true)
    private val out = row("out", "bank", -500)
    private val incoming = row("in", "wallet", 500)

    @Test fun stableIdentityDoesNotDependOnObservationOrder() {
        val a = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), mapOf("bank" to 1L, "wallet" to 2L))
        val b = StatementMatchDecisionPolicy.fingerprint(listOf(incoming, out), mapOf("wallet" to 2L, "bank" to 1L))
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test fun changedEvidenceOrOwnershipInvalidatesDecision() {
        val original = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), mapOf("bank" to 1L, "wallet" to 2L))
        val changedAmount = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming.copy(movementMinor = 600)), mapOf("bank" to 1L, "wallet" to 2L))
        val changedOwnership = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), mapOf("bank" to 1L, "wallet" to 3L))
        assertNotEquals(original, changedAmount)
        assertNotEquals(original, changedOwnership)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingOwnershipRevisionCannotAuthorizeSavedDecision() {
        StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), mapOf("bank" to 1L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun repeatedSourceIdentityIsRejected() {
        StatementMatchDecisionPolicy.fingerprint(listOf(out, out), mapOf("bank" to 1L))
    }
}
