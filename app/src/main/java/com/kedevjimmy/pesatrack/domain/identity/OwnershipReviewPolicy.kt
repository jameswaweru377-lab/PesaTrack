package com.kedevjimmy.pesatrack.domain.identity

data class OwnershipReviewEvidence(
    val hasBalance: Boolean = false,
    val transferCount: Int = 0
) {
    val canAsk: Boolean get() = hasBalance || transferCount > 0
}

object OwnershipReviewPolicy {
    fun distinctDecisions(prompts: List<IdentityReviewPrompt>): List<IdentityReviewPrompt> =
        prompts.distinctBy {
            // Confirming self already applies to all aliases on this stable identity.
            val decision = if (it.promptType == IdentityReviewPromptType.CLASSIFY_COUNTERPARTY) "classify" else "self"
            decision to it.identityId
        }
}
