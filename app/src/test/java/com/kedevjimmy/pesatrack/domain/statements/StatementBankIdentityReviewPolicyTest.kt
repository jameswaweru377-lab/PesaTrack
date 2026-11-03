package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementBankIdentityReviewPolicyTest {
    @Test fun missingOrMaskedNumberIsUnverifiableNotAnIdentityMismatch() {
        for (identifier in listOf(null, "", "****7890", "1234567", "0012AB345678")) {
            val result = StatementBankIdentityReviewPolicy.review(
                sourceIds = setOf("saved-a", "saved-b"), matchingIds = emptySet(),
                currentIdentifier = identifier
            )
            assertEquals(emptySet<String>(), result.mismatchedSourceIds)
            assertEquals(setOf("saved-a", "saved-b"), result.unverifiableSourceIds)
        }
    }

    @Test fun differentFullNumberFlagsOnlyTheUnmatchedSourceForReview() {
        val result = StatementBankIdentityReviewPolicy.review(
            sourceIds = setOf("new", "old"), matchingIds = setOf("new", "unrelated"),
            currentIdentifier = "001234567890"
        )
        assertEquals(setOf("new"), result.matchedSourceIds)
        assertEquals(setOf("old"), result.mismatchedSourceIds)
        assertTrue(result.unverifiableSourceIds.isEmpty())
        assertFalse(result.matches("old"))
    }

    @Test fun formattedFullNumberIsStillVerifiable() {
        assertEquals("001234567890", StatementBankIdentityReviewPolicy.normalizeFullIdentifier("0012 3456-7890"))
        val result = StatementBankIdentityReviewPolicy.review(
            setOf("saved"), setOf("saved"), "0012 3456-7890"
        )
        assertTrue(result.matches("saved"))
        assertTrue(result.mismatchedSourceIds.isEmpty())
    }
}
