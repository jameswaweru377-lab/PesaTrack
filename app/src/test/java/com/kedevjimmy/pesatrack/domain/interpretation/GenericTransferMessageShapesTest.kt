package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Invented messages, no real names, phones or reference IDs. */
class GenericTransferMessageShapesTest {
    @Test fun genericBankToWalletTransferIdentifiesTwoAccountLegs() {
        val result = FinancialMessageInterpreter.interpret(
            title = "Example Bank",
            body = "Bank to M-Pesa transfer of KES 5,000 to +254700000001 " +
                "(SAMPLE OWNER) successfully processed. Reference ID TXN123456; " +
                "M-Pesa reference ID ABC123XYZ9."
        )
        val transfer = result.events.single() as TransferEventFact
        assertEquals(500_000L, transfer.amountMinor)
        assertEquals("Example Bank", transfer.source.displayName)
        assertEquals("SAFARICOM_MPESA", transfer.destination.providerKey)
        assertTrue(result.semanticAssessment!!.references.any { it.value == "ABC123XYZ9" })
    }

    @Test fun genericReceivedMessageStopsCounterpartyAtReferenceMetadata() {
        val result = FinancialMessageInterpreter.interpret(
            title = "Example Wallet",
            body = "You've received KES 3,000 from SAMPLE SENDER. " +
                "Transaction reference ID TXN567890, M-Pesa reference ID ABC123XYZ9."
        )
        val receipt = result.events.single() as ReceiptEventFact
        assertEquals(300_000L, receipt.amountMinor)
        assertEquals("SAMPLE SENDER", receipt.counterparty)
    }

    @Test fun genericPaidToMessageSeparatesBeneficiaryAccountFromMerchantName() {
        val result = FinancialMessageInterpreter.interpret(
            title = "Example Bank",
            body = "KES 5,000 paid to SAMPLE MERCHANT account 5391 on 27/10/2026 at 11:42. " +
                "Reference ABC123XYZ9."
        )
        val payment = result.events.single() as PaymentEventFact
        assertEquals(500_000L, payment.amountMinor)
        assertEquals("SAMPLE MERCHANT", payment.counterparty)
        assertEquals("5391", payment.accountReference)
    }

    @Test fun aBankToWalletRequestWithoutCompletionDoesNotPostTransfer() {
        val result = FinancialMessageInterpreter.interpret(
            title = "Example Bank",
            body = "Bank to M-Pesa transfer of KES 5,000 to +254700000001 " +
                "is pending approval. Reference ID TXN123456."
        )
        assertFalse(result.events.any { it is TransferEventFact })
    }

    @Test fun bankWordingFromUnidentifiedSourceDoesNotProveItsAccount() {
        val result = FinancialMessageInterpreter.interpret(
            title = "Unidentified Service",
            body = "Bank to M-Pesa transfer of KES 5,000 to +254700000001 " +
                "successfully processed. Reference ID TXN123456."
        )
        assertFalse(result.events.any { it is TransferEventFact })
    }
}
