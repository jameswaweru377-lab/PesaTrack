package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReversalMatchingPolicyTest {
    private val original = ReversalOriginal(1L, "ABC123", 10_000L, "wallet-1", 1_000L)

    @Test fun uniqueFullReversalSuppressesOnlyItsOriginal() {
        assertEquals(1L, ReversalMatchingPolicy.match(
            ReversalEvidence(" abc123 ", 10_000L, "wallet-1", 2_000L), listOf(original)
        ))
    }

    @Test fun partialReversalCannotEraseFullOriginalExpense() {
        assertNull(ReversalMatchingPolicy.match(
            ReversalEvidence("ABC123", 4_000L, "wallet-1", 2_000L), listOf(original)
        ))
    }

    @Test fun identicalCodesAcrossAccountsAreNotGloballyExcluded() {
        val another = original.copy(id = 2L, accountId = "wallet-2")
        assertEquals(1L, ReversalMatchingPolicy.match(
            ReversalEvidence("ABC123", 10_000L, "wallet-1", 2_000L), listOf(original, another)
        ))
        assertNull(ReversalMatchingPolicy.match(
            ReversalEvidence("ABC123", 10_000L, null, 2_000L), listOf(original, another)
        ))
    }

    @Test fun wrongAmountDateOrAccountCannotExcludeAnOriginal() {
        assertNull(ReversalMatchingPolicy.match(
            ReversalEvidence("ABC123", 10_000L, "wallet-2", 2_000L), listOf(original)
        ))
        assertNull(ReversalMatchingPolicy.match(
            ReversalEvidence("ABC123", 10_000L, "wallet-1", 500L), listOf(original)
        ))
    }
}
