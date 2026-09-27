package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionEvidenceResolverTest {
    @Test fun exactAmountAndNearbyBankMessageCreatesLikelyMatch() {
        val evidence = TransactionEvidenceResolver.parseEvidence(
            "KCB", "KES 10,000 transfer received successfully", 1_060_000L
        )
        val match = TransactionEvidenceResolver.match(sample(), 1_000_000L, evidence!!)
        assertNotNull(match)
        assertEquals("KCB", match?.label)
        assertEquals(EvidenceConfidence.LIKELY, match?.confidence)
    }

    @Test fun veryCloseEvidenceIsHighConfidenceAndCanEnrichUnknownIdentity() {
        val evidence = TransactionEvidenceResolver.parseEvidence(
            "Equity", "KES 10,000 transferred to KCB ACCOUNT", 1_006_000L
        )
        val match = TransactionEvidenceResolver.match(sample(), 1_000_000L, evidence!!)
        assertEquals(EvidenceConfidence.HIGH, match?.confidence)
        assertEquals("KCB ACCOUNT", TransactionEvidenceResolver.enrich(sample(), match).counterparty)
    }

    @Test fun mismatchedAmountIsRejected() {
        val evidence = TransactionEvidenceResolver.parseEvidence(
            "KCB", "KES 9,000 transfer received", 1_003_000L
        )
        assertNull(TransactionEvidenceResolver.match(sample(), 1_000_000L, evidence!!))
    }

    @Test fun nonFinancialNotificationIsIgnored() {
        assertNull(TransactionEvidenceResolver.parseEvidence(
            "Shopping", "Your KES 10,000 wishlist is ready", 1_000_000L
        ))
    }

    @Test fun matchingReferenceIsHighConfidence() {
        val evidence = TransactionEvidenceResolver.parseEvidence(
            "KCB", "KES 10,000 transaction TQK12ABC34 received", 1_090_000L
        )
        val match = TransactionEvidenceResolver.match(sample(), 1_000_000L, evidence!!)
        assertEquals(EvidenceConfidence.HIGH, match?.confidence)
    }

    private fun sample() = ParsedMpesaTransaction(
        transactionCode = "TQK12ABC34",
        type = MpesaTransactionType.OTHER,
        amountMinor = 1_000_000L,
        counterparty = null,
        counterpartyPhone = null,
        feeMinor = 0L,
        balanceMinor = null,
        occurredAtText = null,
        rawMessage = "sample"
    )
}
