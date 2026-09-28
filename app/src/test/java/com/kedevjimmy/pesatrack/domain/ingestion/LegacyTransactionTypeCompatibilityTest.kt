package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyTransactionTypeCompatibilityTest {
    @Test
    fun paymentRouteKeepsLegacyTypeOnlyAtStorageBoundary() {
        assertEquals(
            MpesaTransactionType.PAY_BILL,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.PAYMENT,
                paymentRouteHint =
                    PaymentRouteHint.BILL
            )
        )
    }

    @Test
    fun genericReceiptDoesNotBecomeMpesaWithoutMpesaDestination() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.RECEIPT,
                destinationIsMpesa = false
            )
        )
    }

    @Test
    fun transferFromMpesaKeepsOutgoingLegacyCompatibility() {
        assertEquals(
            MpesaTransactionType.SENT_MONEY,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.TRANSFER,
                sourceIsMpesa = true,
                destinationIsMpesa = false
            )
        )
    }

    @Test
    fun transferIntoMpesaKeepsIncomingLegacyCompatibility() {
        assertEquals(
            MpesaTransactionType.RECEIVED,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.TRANSFER,
                sourceIsMpesa = false,
                destinationIsMpesa = true
            )
        )
    }

    @Test
    fun nonMpesaTransferRemainsGenericInLegacyColumn() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.TRANSFER
            )
        )
    }
}
