package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0/A2 R03: prevent same-amount, near-simultaneous *real purchases*
 * from disappearing as an automatically merged mirror transaction.
 * Synthetic observations, with no personal financial data.
 */
class MirrorCounterpartyConflictTest {
    @Test
    fun differentMerchantsSameAmountAndDirectionMustNotMergeOnTimeAlone() {
        val first = sample(
            code = "TSTCODE100",
            counterparty = "RIVER MARKET",
            direction = "EXPENSE",
            at = 1_000_000L
        )
        val second = sample(
            code = null,
            counterparty = "HILL TAXI",
            direction = "EXPENSE",
            at = 1_018_000L,
            sourcePackage = "com.example.otherbank"
        )

        val match = MirrorTransactionMatcher.match(first, second)
        assertFalse("Two independent purchases must not collapse", match.autoLink)
        assertEquals(MirrorMatchConfidence.NONE, match.confidence)
        assertFalse(MirrorTransactionMatcher.isMirror(second, first))
    }

    @Test
    fun sameMerchantCodedAndCodelessCopyCanStillLink() {
        val first = sample(
            code = "TSTCODE101",
            counterparty = "RIVER MARKET",
            direction = "EXPENSE",
            at = 1_000_000L
        )
        val second = sample(
            code = null,
            counterparty = "River Market Ltd",
            direction = "EXPENSE",
            at = 1_018_000L,
            sourcePackage = "com.example.otherbank"
        )
        // Token overlap proves this isn't a positively conflicting merchant.
        assertTrue(MirrorTransactionMatcher.match(first, second).autoLink)
    }

    @Test
    fun exactSharedExternalReferenceStillBeatsDifferentlyNamedMerchantLabels() {
        val first = sample(
            code = "TSTCODE102",
            counterparty = "RIVER MARKET",
            direction = "EXPENSE",
            at = 1_000_000L
        ).copy(externalReferences = setOf("LINK12345"))
        val second = sample(
            code = null,
            counterparty = "HILL TAXI",
            direction = "EXPENSE",
            at = 1_018_000L,
            sourcePackage = "com.example.otherbank"
        ).copy(externalReferences = setOf("LINK12345"))

        val match = MirrorTransactionMatcher.match(first, second)
        assertTrue(match.autoLink)
        assertEquals(MirrorMatchReason.EXACT_SHARED_REFERENCE, match.reason)
    }

    @Test
    fun distinctKnownPhonesBlockTimeOnlyAutoLinkEvenWithSameMerchantName() {
        val first = sample(
            code = "TSTCODE103",
            counterparty = "SAMPLE SHOP",
            direction = "EXPENSE",
            at = 1_000_000L
        ).copy(counterpartyPhone = "0712000000")
        val second = sample(
            code = null,
            counterparty = "Sample Shop",
            direction = "EXPENSE",
            at = 1_005_000L,
            sourcePackage = "com.example.otherbank"
        ).copy(counterpartyPhone = "0712000001")
        assertFalse(MirrorTransactionMatcher.match(first, second).autoLink)
    }

    @Test
    fun complementaryBankWalletMovementIsNotRejectedForDifferentNames() {
        val bank = sample(
            code = "EQA123456",
            counterparty = "Bank Sender",
            direction = "EXPENSE",
            at = 1_000_000L
        )
        val wallet = sample(
            code = null,
            counterparty = "Wallet Recipient",
            direction = "INCOME",
            at = 1_012_000L,
            sourcePackage = "com.example.otherbank"
        )
        assertTrue(MirrorTransactionMatcher.match(bank, wallet).autoLink)
    }

    private fun sample(
        code: String?,
        counterparty: String,
        direction: String,
        at: Long,
        sourcePackage: String = "com.example.wallet"
    ) = MirrorTransactionObservation(
        amountMinor = 98_500L,
        occurredAtEpochMs = at,
        receivedAtEpochMs = at,
        sourceKind = "financial_notification",
        sourcePackage = sourcePackage,
        transactionCode = code,
        type = "PAYMENT",
        counterparty = counterparty,
        counterpartyPhone = null,
        direction = direction
    )
}
