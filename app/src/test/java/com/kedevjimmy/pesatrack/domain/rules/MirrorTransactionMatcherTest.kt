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
    fun matchesUltraCloseMirrorEvenWhenLabelsDiffer() {
        assertTrue(
            MirrorTransactionMatcher.isMirror(
                mpesa(
                    amount = 50_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "JOHN MWANGI"
                ),
                bank(
                    amount = 50_000L,
                    receivedAt = 1_003_000L,
                    counterparty = "Mobile transfer"
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
    fun doesNotMergeTwoRealMpesaTransactions() {
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
    fun doesNotMergeTwoCodeLessBankNotifications() {
        assertFalse(
            MirrorTransactionMatcher.isMirror(
                bank(
                    amount = 10_000L,
                    receivedAt = 1_000_000L,
                    counterparty = "KCB"
                ),
                bank(
                    amount = 10_000L,
                    receivedAt = 1_002_000L,
                    counterparty = "KCB"
                )
            )
        )
    }

    private fun mpesa(
        amount: Long,
        receivedAt: Long,
        counterparty: String,
        code: String = "TQK12ABC34"
    ) = MirrorTransactionObservation(
        amountMinor = amount,
        occurredAtEpochMs = receivedAt,
        receivedAtEpochMs = receivedAt,
        sourceKind = "mpesa_notification",
        sourcePackage = "com.safaricom.mpesa",
        transactionCode = code,
        type = "PAY_BILL",
        counterparty = counterparty,
        counterpartyPhone = null
    )

    private fun bank(
        amount: Long,
        receivedAt: Long,
        counterparty: String
    ) = MirrorTransactionObservation(
        amountMinor = amount,
        occurredAtEpochMs = receivedAt,
        receivedAtEpochMs = receivedAt,
        sourceKind = "configured_notification",
        sourcePackage = "com.kcb.bank",
        transactionCode = null,
        type = "OTHER",
        counterparty = counterparty,
        counterpartyPhone = null
    )
}
