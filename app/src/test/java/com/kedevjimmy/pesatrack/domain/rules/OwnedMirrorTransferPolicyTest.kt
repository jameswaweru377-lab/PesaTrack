package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test

class OwnedMirrorTransferPolicyTest {
    private val owned = setOf("bank", "wallet")
    private val debit = OwnedMirrorObservation(20_000L, "EXPENSE", "bank", null, setOf("SHARED12345"))
    private val credit = OwnedMirrorObservation(20_000L, "INCOME", "wallet", null, setOf("shared12345"))

    @Test fun sharedReferenceAndTwoOwnedAccountsResolveInEitherOrder() {
        val expected = OwnedMirrorTransfer("bank", "wallet")
        assertEquals(expected, OwnedMirrorTransferPolicy.resolve(debit, credit, owned))
        assertEquals(expected, OwnedMirrorTransferPolicy.resolve(credit, debit, owned))
    }
    @Test fun equalAmountOrProviderNameCannotEstablishOwnTransfer() {
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(references = setOf("OTHER12345")), owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(accountId = "someone-else"), owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(accountId = "bank"), owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(amountMinor = 19_000L), owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(direction = "EXPENSE"), owned))
    }
    @Test fun explicitCorrectionsAreNotOverwritten() {
        assertNull(OwnedMirrorTransferPolicy.resolve(debit.copy(protected = true), credit, owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(debit, credit.copy(protected = true), owned))
    }
    @Test fun replayPreservesResolvedPathButOnlyForTheMatchingSide() {
        val transfer = debit.copy(direction = "TRANSFER", linkedAccountId = "wallet")
        assertEquals(OwnedMirrorTransfer("bank", "wallet"), OwnedMirrorTransferPolicy.resolve(transfer, credit, owned))
        assertEquals(OwnedMirrorTransfer("bank", "wallet"), OwnedMirrorTransferPolicy.resolve(debit, transfer, owned))
        assertNull(OwnedMirrorTransferPolicy.resolve(transfer, credit.copy(accountId = "bank"), owned))
    }
}
