package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType

/**
 * Temporary compatibility mapping for the legacy transactions.type
 * column. Generic interpretation must not depend on these values.
 * Remove this once persistence uses canonical event kinds directly.
 */
object LegacyTransactionTypeCompatibility {
    fun resolve(
        kind: CanonicalFinancialRecordKind,
        paymentRouteHint: PaymentRouteHint? = null,
        sourceIsMpesa: Boolean = false,
        destinationIsMpesa: Boolean = false
    ): MpesaTransactionType =
        when (kind) {
            CanonicalFinancialRecordKind.PAYMENT ->
                when (
                    requireNotNull(paymentRouteHint) {
                        "Payment movement requires a route hint."
                    }
                ) {
                    PaymentRouteHint.PERSON_TO_PERSON ->
                        MpesaTransactionType.SENT_MONEY
                    PaymentRouteHint.MERCHANT ->
                        MpesaTransactionType.BUY_GOODS
                    PaymentRouteHint.BILL ->
                        MpesaTransactionType.PAY_BILL
                    PaymentRouteHint.AIRTIME ->
                        MpesaTransactionType.AIRTIME
                }

            CanonicalFinancialRecordKind.RECEIPT ->
                if (destinationIsMpesa) {
                    MpesaTransactionType.RECEIVED
                } else {
                    MpesaTransactionType.OTHER
                }

            CanonicalFinancialRecordKind.TRANSFER ->
                when {
                    sourceIsMpesa ->
                        MpesaTransactionType.SENT_MONEY
                    destinationIsMpesa ->
                        MpesaTransactionType.RECEIVED
                    else ->
                        MpesaTransactionType.OTHER
                }

            CanonicalFinancialRecordKind.CASH_WITHDRAWAL ->
                if (sourceIsMpesa) {
                    MpesaTransactionType.WITHDRAWAL
                } else {
                    MpesaTransactionType.OTHER
                }

            CanonicalFinancialRecordKind.CASH_DEPOSIT ->
                if (destinationIsMpesa) {
                    MpesaTransactionType.DEPOSIT
                } else {
                    MpesaTransactionType.OTHER
                }

            CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT ->
                MpesaTransactionType.OTHER

            CanonicalFinancialRecordKind.FEE ->
                MpesaTransactionType.OTHER

            CanonicalFinancialRecordKind.REVERSAL ->
                MpesaTransactionType.REVERSAL

            else ->
                error(
                    "Unsupported canonical movement kind: $kind"
                )
        }
}
