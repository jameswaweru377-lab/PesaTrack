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
    @Test
    fun mpesaCashWithdrawalKeepsLegacyWithdrawalType() {
        assertEquals(
            MpesaTransactionType.WITHDRAWAL,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind
                        .CASH_WITHDRAWAL,
                sourceIsMpesa = true
            )
        )
    }

    @Test
    fun bankCashWithdrawalDoesNotPretendToBeMpesa() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind
                        .CASH_WITHDRAWAL,
                sourceIsMpesa = false
            )
        )
    }

    @Test
    fun cashDepositIntoMpesaKeepsLegacyDepositType() {
        assertEquals(
            MpesaTransactionType.DEPOSIT,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind
                        .CASH_DEPOSIT,
                destinationIsMpesa = true
            )
        )
    }

    @Test
    fun cashDepositIntoBankStaysGenericInLegacyColumn() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind
                        .CASH_DEPOSIT,
                destinationIsMpesa = false
            )
        )
    }

    @Test
    fun unresolvedMovementStaysGenericAtLegacyBoundary() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind
                        .UNRESOLVED_MOVEMENT
            )
        )
    }

    @Test
    fun canonicalFeeUsesGenericLegacyTypeOnlyAtStorageBoundary() {
        assertEquals(
            MpesaTransactionType.OTHER,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.FEE
            )
        )
    }

    @Test
    fun reversalKeepsLegacyReversalTypeOnlyAtStorageBoundary() {
        assertEquals(
            MpesaTransactionType.REVERSAL,
            LegacyTransactionTypeCompatibility.resolve(
                kind =
                    CanonicalFinancialRecordKind.REVERSAL
            )
        )
    }

}
