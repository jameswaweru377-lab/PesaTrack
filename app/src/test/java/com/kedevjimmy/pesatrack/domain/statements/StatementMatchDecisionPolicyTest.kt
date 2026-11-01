package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementMatchDecisionPolicyTest {
    private fun row(id: String, account: String, amount: Long) =
        StatementEvidenceCorrelationPolicy.Observation(id, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
            id, account, "KES", 1_700_000_000_000L, amount, "ABC1234567", true, sourceTotalsVerified = true)
    private val out = row("out", "bank", -500)
    private val incoming = row("in", "wallet", 500)
    private val accounts = mapOf(
        "bank" to StatementMatchDecisionPolicy.Ownership(1L, "BANK", "001234567890", "Invented bank"),
        "wallet" to StatementMatchDecisionPolicy.Ownership(2L, "MOBILE_WALLET", "0712345678", "Invented wallet"))

    @Test fun stableIdentityDoesNotDependOnObservationOrder() {
        val a = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts)
        val b = StatementMatchDecisionPolicy.fingerprint(listOf(incoming, out), accounts)
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test fun historicalMessagesAndWalletStatementsNeedAccountCheck() {
        assertFalse(StatementMatchDecisionPolicy.requiresAccountAttestation(listOf(out, incoming)))
        assertTrue(StatementMatchDecisionPolicy.requiresAccountAttestation(listOf(out,
            incoming.copy(source = StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT))))
        assertTrue(StatementMatchDecisionPolicy.requiresAccountAttestation(listOf(incoming,
            out.copy(source = StatementEvidenceCorrelationPolicy.Source.ACTIVITY))))
    }

    @Test fun changedEvidenceOrOwnershipInvalidatesDecision() {
        val original = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts)
        val changedAmount = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming.copy(movementMinor = 600)), accounts)
        val changedOwnership = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts + ("wallet" to accounts.getValue("wallet").copy(revision = 3L)))
        val changedIdentifier = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts + ("wallet" to accounts.getValue("wallet").copy(identifier = "0799999999")))
        val changedType = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts + ("wallet" to accounts.getValue("wallet").copy(type = "BANK")))
        val changedName = StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts + ("wallet" to accounts.getValue("wallet").copy(name = "Another account")))
        assertNotEquals(original, changedAmount)
        assertNotEquals(original, changedOwnership)
        assertNotEquals(original, changedIdentifier)
        assertNotEquals(original, changedType)
        assertNotEquals(original, changedName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingOwnershipRevisionCannotAuthorizeSavedDecision() {
        StatementMatchDecisionPolicy.fingerprint(listOf(out, incoming), accounts.filterKeys { it == "bank" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun repeatedSourceIdentityIsRejected() {
        StatementMatchDecisionPolicy.fingerprint(listOf(out, out), accounts)
    }
}
