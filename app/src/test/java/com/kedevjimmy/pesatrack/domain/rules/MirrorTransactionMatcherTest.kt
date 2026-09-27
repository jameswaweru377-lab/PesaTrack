package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorTransactionMatcherTest {
    @Test
    fun matchesSameAmountSameMerchantAcrossMpesaAndBankMessages() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "KCB ACCOUNT"
                ),
                bank(
                    amount = 10_000L,
                    receivedAt = 1_035_000L,
                    counterparty = "KCB"
                )
            )
        )
    }

    @Test
    fun matchesCodedAndCodeLessCopiesWithinTwentySeconds() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 98_500L,
                    receivedAt = 1_000_000L,
                    counterparty = null
                ),
                bank(
                    amount = 98_500L,
                    receivedAt = 1_018_000L,
                    counterparty = null
                )
            )
        )
    }

    @Test
    fun complementaryDirectionsExtendSafeCorrelationWindow() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 12_500L,
                    receivedAt = 1_000_000L,
                    counterparty = null,
                    direction = "EXPENSE"
                ),
                bank(
                    amount = 12_500L,
                    receivedAt = 1_052_000L,
                    counterparty = null,
                    direction = "INCOME"
                )
            )
        )
    }

    @Test
    fun sameAmountWithoutEvidenceOutsideStrongWindowDoesNotMerge() {
        assertFalse(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 98_500L,
                    receivedAt = 1_000_000L,
                    counterparty = null,
                    direction = "EXPENSE"
                ),
                bank(
                    amount = 98_500L,
                    receivedAt = 1_090_000L,
                    counterparty = null,
                    direction = "NEUTRAL"
                )
            )
        )
    }

    @Test
    fun codeLessCopiesFromDifferentAppsCanMergeWhenUltraClose() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                bank(
                    amount = 2_000L,
                    receivedAt = 1_000_000L,
                    counterparty = null,
                    sourcePackage = "com.google.android.apps.messaging"
                ),
                bank(
                    amount = 2_000L,
                    receivedAt = 1_003_000L,
                    counterparty = null,
                    sourcePackage = "com.truecaller"
                )
            )
        )
    }

    @Test
    fun codeLessCopiesFromSameSourceDoNotMergeOnTimeAlone() {
        assertFalse(
            MirrorTransactionMatcher.isMirror(
                bank(
                    amount = 2_000L,
                    receivedAt = 1_000_000L,
                    counterparty = null,
                    sourcePackage = "com.bank.app"
                ),
                bank(
                    amount = 2_000L,
                    receivedAt = 1_003_000L,
                    counterparty = null,
                    sourcePackage = "com.bank.app"
                )
            )
        )
    }

    @Test
    fun doesNotMergeSameAmountFarApart() {
        assertFalse(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "KCB"
                ),
                bank(
                    amount = 10_000L,
                    receivedAt = 1_300_001L,
                    counterparty = "KCB"
                )
            )
        )
    }

    @Test
    fun doesNotMergeTwoRealMpesaTransactionsWithDifferentCodes() {
        assertFalse(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "KCB",
                    code = "AAA111"
                ),
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_002_000L,
                    counterparty = "KCB",
                    code = "BBB222"
                )
            )
        )
    }

    @Test
    fun sameTransactionCodeIsAlwaysTheSameObservation() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "KCB",
                    code = "AAA111"
                ),
                mpesa(
                    amount = 10_000L,
                    receivedAt = 1_040_000L,
                    counterparty = null,
                    code = "AAA111"
                )
            )
        )
    }

    private fun mpesa(
        amount: Long,
        receivedAt: Long,
        counterparty: String?,
        code: String = "TQK12ABC34",
        direction: String = "EXPENSE"
    ) = MirrorTransactionObservation(
        amountMinor = amount,
        occurredAtEpochMs = receivedAt,
        receivedAtEpochMs = receivedAt,
        sourceKind = "mpesa_notification",
        sourcePackage = "com.safaricom.mpesa",
        transactionCode = code,
        type = "PAY_BILL",
        counterparty = counterparty,
        counterpartyPhone = null,
        direction = direction
    )

    private fun bank(
        amount: Long,
        receivedAt: Long,
        counterparty: String?,
        direction: String = "NEUTRAL",
        sourcePackage: String = "com.kcb.bank"
    ) = MirrorTransactionObservation(
        amountMinor = amount,
        occurredAtEpochMs = receivedAt,
        receivedAtEpochMs = receivedAt,
        sourceKind = "configured_notification",
        sourcePackage = sourcePackage,
        transactionCode = null,
        type = "OTHER",
        counterparty = counterparty,
        counterpartyPhone = null,
        direction = direction
    )
}
