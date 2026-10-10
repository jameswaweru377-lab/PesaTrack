package com.kedevjimmy.pesatrack.domain.identity

import org.junit.Assert.*
import org.junit.Test

class OwnershipReviewPolicyTest {
    @Test fun providerNameAloneDoesNotQualify() {
        assertFalse(OwnershipReviewEvidence().canAsk)
    }
    @Test fun qualifyingBalanceOrTransferCanAskWithoutProvingOwnership() {
        assertTrue(OwnershipReviewEvidence(hasBalance = true).canAsk)
        assertTrue(OwnershipReviewEvidence(transferCount = 1).canAsk)
        assertFalse(OwnershipReviewEvidence(transferCount = 0).canAsk)
    }
    @Test fun selfAliasesForOneIdentityBecomeOneDecisionButDifferentIdentitiesRemain() {
        val phone = prompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE, "person:one")
        val name = prompt(IdentityReviewPromptType.CONFIRM_SELF_NAME, "person:one")
        val other = prompt(IdentityReviewPromptType.CONFIRM_SELF_NAME, "person:two")
        assertEquals(listOf(phone, other), OwnershipReviewPolicy.distinctDecisions(listOf(phone, name, other)))
    }
    @Test fun optionalClassificationDoesNotHideOwnershipQuestion() {
        val classification = prompt(IdentityReviewPromptType.CLASSIFY_COUNTERPARTY, "person:one")
        val phone = prompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE, "person:one")
        assertEquals(listOf(classification, phone), OwnershipReviewPolicy.distinctDecisions(listOf(classification, phone)))
    }
    @Test fun rejectionAndDeferralStillSuppressExistingEvidence() {
        val phone = prompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE, "person:one")
        val deferred = IdentityReviewDecision(phone.promptType, phone.identityId, IdentityReviewDecisionType.DEFERRED, 2, 1)
        assertFalse(IdentityGraphPolicy.shouldPresentReviewPrompt(phone, listOf(deferred)))
        assertTrue(IdentityGraphPolicy.shouldPresentReviewPrompt(phone.copy(observationCount = 3), listOf(deferred)))
        val rejected = deferred.copy(decision = IdentityReviewDecisionType.REJECTED_SELF)
        assertFalse(IdentityGraphPolicy.shouldPresentReviewPrompt(phone.copy(observationCount = 20), listOf(rejected)))
    }
    @Test fun deferredPhoneDoesNotImmediatelyBecomeDuplicateNameQuestion() {
        val phone = prompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE, "person:one")
        val name = phone.copy(promptType = IdentityReviewPromptType.CONFIRM_SELF_NAME)
        val deferred = IdentityReviewDecision(phone.promptType, phone.identityId, IdentityReviewDecisionType.DEFERRED, 2, 1)
        val visible = OwnershipReviewPolicy.distinctDecisions(listOf(phone, name))
            .filter { IdentityGraphPolicy.shouldPresentReviewPrompt(it, listOf(deferred)) }
        assertTrue(visible.isEmpty())
    }
    private fun prompt(type: IdentityReviewPromptType, id: String) = IdentityReviewPrompt(
        type, id, "Sample Person", IdentityAliasType.NAME, "SAMPLE PERSON", 2, "Possible own-account transfer"
    )
}
