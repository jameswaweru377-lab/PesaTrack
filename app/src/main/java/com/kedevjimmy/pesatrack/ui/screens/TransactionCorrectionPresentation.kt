package com.kedevjimmy.pesatrack.ui.screens

/**
 * User-facing impact previews for manual transaction corrections.
 *
 * These prompts only describe existing ledger operations; they deliberately
 * do not infer account ownership, merchant identity, or transaction direction.
 */
internal enum class TransactionCorrectionAction {
    ACCEPT_IDENTITY,
    REMOVE_REFUND_LINK,
    MARK_OWN_TRANSFER,
    EXCLUDE_ENTRY
}

internal data class TransactionCorrectionPrompt(
    val title: String,
    val explanation: String,
    val confirmLabel: String
)

internal fun transactionCorrectionPrompt(
    action: TransactionCorrectionAction
): TransactionCorrectionPrompt = when (action) {
    TransactionCorrectionAction.ACCEPT_IDENTITY -> TransactionCorrectionPrompt(
        title = "Use suggested identity?",
        explanation = "Only accept this name if you recognize it from the original message. " +
            "The displayed counterparty will change and a matching saved rule may also change its category.",
        confirmLabel = "Use this identity"
    )
    TransactionCorrectionAction.REMOVE_REFUND_LINK -> TransactionCorrectionPrompt(
        title = "Remove refund link?",
        explanation = "This entry will no longer offset the linked expense. It becomes incoming money " +
            "with an uncategorized, needs-review status. Check its category afterward.",
        confirmLabel = "Remove refund link"
    )
    TransactionCorrectionAction.MARK_OWN_TRANSFER -> TransactionCorrectionPrompt(
        title = "Mark as own-account transfer?",
        explanation = "Only continue if both sides are accounts you own. This entry becomes a transfer " +
            "rather than an expense or income. Your spending or income totals may change, " +
            "and existing split allocations may be cleared.",
        confirmLabel = "Mark as transfer"
    )
    TransactionCorrectionAction.EXCLUDE_ENTRY -> TransactionCorrectionPrompt(
        title = "Exclude from tracked activity?",
        explanation = "This is more than excluding a budget category. The entry will be hidden from " +
            "Activity and left out of financial summaries. Its stored record remains, " +
            "but this screen has no one-tap undo. Continue only if it should not count.",
        confirmLabel = "Exclude entry"
    )
}
