package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidenceLevel
import org.junit.Assert.*
import org.junit.Test

class OwnedTransferReviewPolicyTest {
    private val bank = TransferReviewObservation(1, "bank", "BANK", "EXPENSE", 10000, 100,
        "KES", 1000, setOf("SYNTHETIC01"), "James Mwangi Waweru", null, "bank snapshot", primaryReference = "BANKCODE01")
    private val wallet = TransferReviewObservation(2, "account-mpesa", "MOBILE_WALLET", "INCOME", 10000, 0,
        "KES", 1000, setOf("SYNTHETIC01"), "James Waweru", "0712345678", "wallet snapshot", primaryReference = "SYNTHETIC01")
    private val owned = mapOf("bank" to 1L, "account-mpesa" to 2L)
    private fun candidates(rows: List<TransferReviewObservation> = listOf(bank, wallet),
        consent: Map<String, Long> = owned) = OwnedTransferReviewPolicy.candidates(
        rows, consent, "James Mwangi Waweru", "0712345678")

    @Test fun orderedNameAndCorrelatedConfirmedAccountsOfferReviewOnly() {
        val candidate = candidates().single()
        assertEquals("1:2", candidate.pairKey)
        assertEquals(SelfNameEvidenceLevel.EXACT_THREE_NAMES, candidate.nameEvidence)
        assertEquals(1L, candidate.sourceOwnershipRevision)
        assertEquals(2L, candidate.destinationOwnershipRevision)
    }
    @Test fun missingConsentNeverOffersTransfer() {
        assertTrue(candidates(consent = mapOf("bank" to 1L)).isEmpty())
        assertTrue(candidates(consent = emptyMap()).isEmpty())
    }
    @Test fun twoNamesRequireReviewAndPhoneIsOnlyAHint() {
        val candidate = candidates(listOf(bank.copy(name = "James Waweru"), wallet.copy(phone = null))).single()
        assertEquals(SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW, candidate.nameEvidence)
        assertFalse(candidate.phoneEvidence)
        assertTrue(candidates(listOf(bank.copy(name = "Stranger"), wallet.copy(name = "Stranger", phone = "***5678"))).isEmpty())
    }
    @Test fun contradictionsProtectedFactsAndIncomingFeesStayIndependent() {
        val invalid = listOf(wallet.copy(amountMinor = 20000), wallet.copy(amountMinor = 0),
            wallet.copy(currency = "USD"), wallet.copy(epochMs = 172802000L),
            wallet.copy(references = setOf("DIFFERENT01"), primaryReference = "DIFFERENT01"),
            wallet.copy(references = emptySet(), primaryReference = null),
            wallet.copy(protected = true), wallet.copy(feeMinor = 1), wallet.copy(identityRejected = true),
            wallet.copy(accountId = "bank"), wallet.copy(direction = "TRANSFER"))
        invalid.forEach { assertTrue("Must not pair $it", candidates(listOf(bank, it)).isEmpty()) }
        assertTrue(candidates(listOf(bank.copy(protected = true), wallet)).isEmpty())
    }
    @Test fun ambiguityIncludesTwinsWithoutProfileHints() {
        val twin = wallet.copy(id = 3, name = "Different Person", phone = null)
        assertTrue(candidates(listOf(bank, wallet, twin)).isEmpty())
        assertTrue(candidates(listOf(bank, bank.copy(id = 4, name = "Other Person"), wallet)).isEmpty())
    }
    @Test fun timeAndMoneyBoundariesAreConservative() {
        assertEquals(1, candidates(listOf(bank, wallet.copy(epochMs = bank.epochMs))).size)
        assertTrue(candidates(listOf(bank, wallet.copy(epochMs = bank.epochMs + 1))).isEmpty())
        assertTrue(candidates(listOf(bank, wallet.copy(epochMs = bank.epochMs + 172800000L))).isEmpty())
        assertTrue(candidates(listOf(bank, wallet.copy(epochMs = bank.epochMs + 172800001L))).isEmpty())
        assertTrue(candidates(listOf(bank.copy(epochMs = -1), wallet)).isEmpty())
        assertTrue(candidates(listOf(bank.copy(feeMinor = -1), wallet)).isEmpty())
        assertTrue(candidates(listOf(bank.copy(amountMinor = Long.MAX_VALUE, feeMinor = 1),
            wallet.copy(amountMinor = Long.MAX_VALUE))).isEmpty())
    }
    @Test fun ambiguityWithProtectedTwinDoesNotAuthorizeAConvenientPair() {
        assertTrue(candidates(listOf(bank, wallet, wallet.copy(id = 3, protected = true))).isEmpty())
        assertTrue(candidates(listOf(bank, wallet, wallet.copy(id = 3, epochMs = bank.epochMs + 1))).isEmpty())
    }
    @Test fun sharedCustomerReferenceWithoutTransactionCrossReferenceIsNotCorrelation() {
        assertTrue(candidates(listOf(bank.copy(references = setOf("CLIENT01")),
            wallet.copy(references = setOf("CLIENT01"), primaryReference = "WALLETCODE01"))).isEmpty())
        assertTrue(candidates(listOf(bank.copy(primaryReference = null), wallet.copy(primaryReference = null))).isEmpty())
        assertEquals(1, candidates().size) // Bank's cross-reference is the wallet's primary code.
    }
}
