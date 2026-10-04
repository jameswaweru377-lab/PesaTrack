package com.kedevjimmy.pesatrack.domain.identity

import org.junit.Assert.*
import org.junit.Test

class IdentityAttentionPolicyTest {
    @Test fun optionalCounterpartyLabelDoesNotBecomeFinancialWorkEvenAfterManyPayments() {
        assertFalse(IdentityGraphPolicy.requiresFinancialAttention(prompt(IdentityReviewPromptType.CLASSIFY_COUNTERPARTY)))
    }
    @Test fun ambiguousOwnershipStillNeedsAttention() {
        assertTrue(IdentityGraphPolicy.requiresFinancialAttention(prompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE)))
        assertTrue(IdentityGraphPolicy.requiresFinancialAttention(prompt(IdentityReviewPromptType.CONFIRM_SELF_NAME)))
    }
    private fun prompt(type: IdentityReviewPromptType) = IdentityReviewPrompt(
        type, "person:sample", "Sample Person", IdentityAliasType.NAME,
        "SAMPLE PERSON", 20, "Repeated payments"
    )
}
