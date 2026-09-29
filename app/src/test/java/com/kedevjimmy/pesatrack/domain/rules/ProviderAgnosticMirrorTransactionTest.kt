package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAgnosticMirrorTransactionTest {
    @Test
    fun codelessBankObservationCanMirrorCodedMpesaObservation() {
        val bank = MirrorTransactionObservation(
            amountMinor = 500_000L,
            occurredAtEpochMs = 1_000_000L,
            receivedAtEpochMs = 1_000_000L,
            sourceKind = "interpreted_financial_notification",
            sourcePackage = "com.google.android.apps.messaging",
            transactionCode = null,
            type = "TRANSFER",
            counterparty = "M-Pesa",
            counterpartyPhone = null,
            direction = "TRANSFER"
        )
        val mpesa = MirrorTransactionObservation(
            amountMinor = 500_000L,
            occurredAtEpochMs = 1_012_000L,
            receivedAtEpochMs = 1_012_000L,
            sourceKind = "interpreted_financial_notification",
            sourcePackage = "com.google.android.apps.messaging",
            transactionCode = "TST12BANK1",
            type = "RECEIVED",
            counterparty = "CO-OP BANK",
            counterpartyPhone = null,
            direction = "TRANSFER"
        )

        assertTrue(
            MirrorTransactionMatcher.isMirror(
                bank,
                mpesa
            )
        )
    }
}
