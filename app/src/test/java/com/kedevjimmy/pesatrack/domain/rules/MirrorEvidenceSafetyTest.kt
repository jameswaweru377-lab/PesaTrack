package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorEvidenceSafetyTest {
    private fun row(code: String?, source: String, direction: String = "EXPENSE") = MirrorTransactionObservation(
        amountMinor = 50_000L, occurredAtEpochMs = 1_780_000_000_000L,
        receivedAtEpochMs = 1_780_000_000_000L, sourceKind = source, sourcePackage = source,
        transactionCode = code, type = "SENT_MONEY", counterparty = "Invented shop",
        counterpartyPhone = "0712345678", direction = direction, accountIds = setOf("bank", "wallet"))

    @Test fun equalAmountCloseTogetherWithoutSharedReferenceNeverAutoLinks() {
        val coded = row("ABC1234567", "sms")
        val codeless = row(null, "notification")
        assertFalse(MirrorTransactionMatcher.match(coded, codeless).autoLink)
        assertFalse(MirrorTransactionMatcher.match(codeless, coded).autoLink)
        assertFalse(MirrorTransactionMatcher.match(coded, codeless.copy(sourceKind = "sms", sourcePackage = "sms")).autoLink)
        assertFalse(MirrorTransactionMatcher.match(coded, codeless.copy(direction = "INCOME")).autoLink)
        assertFalse(MirrorTransactionMatcher.match(codeless, codeless.copy(sourceKind = "other", sourcePackage = "other")).autoLink)
    }

    @Test fun differentReferencesAndSameAccountPathAreNotProof() {
        val first = row("ABC1234567", "bank")
        val second = row("XYZ7654321", "wallet", "INCOME")
        assertFalse(MirrorTransactionMatcher.match(first, second).autoLink)
    }

    @Test fun exactSharedReferenceStillAllowsVerifiedMirrorCandidate() {
        val first = row("ABC1234567", "bank")
        val second = row(null, "wallet", "INCOME").copy(externalReferences = setOf("ABC1234567"))
        assertTrue(MirrorTransactionMatcher.match(first, second).autoLink)
        assertTrue(MirrorTransactionMatcher.match(second, first).autoLink)
    }

    @Test fun reusedReferenceWithDistantTimeOrContradictoryPayeeIsNotProof() {
        val first = row("ABC1234567", "bank")
        val second = row(null, "wallet").copy(externalReferences = setOf("ABC1234567"))
        val far = second.copy(occurredAtEpochMs = first.occurredAtEpochMs + 49L * 60 * 60 * 1_000,
            receivedAtEpochMs = first.receivedAtEpochMs + 49L * 60 * 60 * 1_000)
        assertFalse(MirrorTransactionMatcher.match(first, far).autoLink)
        assertFalse(MirrorTransactionMatcher.match(first, second.copy(counterparty = "Distinct transport", counterpartyPhone = null)).autoLink)
    }
}
