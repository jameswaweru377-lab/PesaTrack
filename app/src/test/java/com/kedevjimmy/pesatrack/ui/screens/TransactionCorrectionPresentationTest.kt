package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCorrectionPresentationTest {
    @Test fun eachCorrectionHasDistinctNonEmptyConfirmation() {
        val actions = TransactionCorrectionAction.values().toList()
        val prompts = actions.map(::transactionCorrectionPrompt)
        assertEquals(4, prompts.size)
        assertEquals(4, prompts.map { it.confirmLabel }.toSet().size)
        assertTrue(prompts.all { it.title.isNotBlank() && it.explanation.isNotBlank() && it.confirmLabel.isNotBlank() })
    }

    @Test fun ownershipAndCategoryChangeAreExplainedBeforeCommitting() {
        val transfer = transactionCorrectionPrompt(TransactionCorrectionAction.MARK_OWN_TRANSFER)
        assertTrue(transfer.explanation.contains("both sides"))
        assertTrue(transfer.explanation.contains("income"))
        assertTrue(transfer.explanation.contains("split allocations"))
        assertFalse(transfer.explanation.contains("guaranteed"))
    }

    @Test fun exclusionDisclosureDoesNotPretendItIsOnlyAVisualBudgetChange() {
        val exclusion = transactionCorrectionPrompt(TransactionCorrectionAction.EXCLUDE_ENTRY)
        assertTrue(exclusion.title.contains("tracked activity"))
        assertTrue(exclusion.explanation.contains("hidden from Activity"))
        assertTrue(exclusion.explanation.contains("no one-tap undo"))
    }

    @Test fun unlinkingRefundAndAcceptingIdentityDiscloseSideEffects() {
        assertTrue(transactionCorrectionPrompt(TransactionCorrectionAction.REMOVE_REFUND_LINK)
            .explanation.contains("uncategorized"))
        assertTrue(transactionCorrectionPrompt(TransactionCorrectionAction.ACCEPT_IDENTITY)
            .explanation.contains("saved rule"))
    }
}
