package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.*
import org.junit.Test

class TransactionMessageEvidencePolicyTest {
    private fun message(code: String = "UJ9ABC4H", amount: Long = 200000, body: String = "Original payment") =
        TransactionMessageEvidence("M-PESA", body, 1000, code, listOf(amount))

    @Test fun selectsOnlyExactReferenceAndAmount() {
        val original = message()
        assertEquals(original, matchingTransactionMessage("UJ9ABC4H", 200000, listOf(message("XUJ9ABC4H"), message(amount = 5000), original)))
    }

    @Test fun missingReferenceOrMessageIsExplicitlyUnavailable() {
        assertNull(matchingTransactionMessage(null, 200000, listOf(message())))
        assertNull(matchingTransactionMessage("UJ9ABC4H", 200000, emptyList()))
    }

    @Test fun ambiguousEvidenceIsNotGuessed() {
        assertNull(matchingTransactionMessage("UJ9ABC4H", 200000, listOf(message(), message(body = "Different evidence"))))
    }

    @Test fun identicalSmsCopiesDoNotCreateFalseAmbiguity() {
        val original = message()
        assertEquals(original, matchingTransactionMessage("uj9abc4h", 200000, listOf(original, original.copy(receivedAtEpochMs = 2000))))
    }
}
