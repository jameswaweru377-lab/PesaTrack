package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialMessageInterpreterTest {
    @Test
    fun understandsMshwariToMpesaTransferAndBothBalances() {
        val message =
            "RQK12ABC34 Confirmed. Ksh 7,000.00 transferred from M-Shwari account on 28/9/26 at 6:00 PM. M-Shwari balance is Ksh 543.00. M-PESA balance is Ksh 7,789.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        assertTrue(result.recognized)
        assertTrue(result.consumesLegacyTransactionParsing)
        assertEquals("RQK12ABC34", result.transactionCode)
        assertEquals(2, result.balances.size)

        val transfer = result.events.single() as TransferEventFact
        assertEquals(700_000L, transfer.amountMinor)
        assertEquals(
            "NCBA_MSHWARI_SAVINGS",
            transfer.source.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            transfer.destination.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            transfer.source.ownershipHint
        )

        val mshwariBalance = result.balances.first {
            it.account.providerKey ==
                "NCBA_MSHWARI_SAVINGS"
        }
        val mpesaBalance = result.balances.first {
            it.account.providerKey ==
                "SAFARICOM_MPESA"
        }
        assertEquals(54_300L, mshwariBalance.balanceMinor)
        assertEquals(778_900L, mpesaBalance.balanceMinor)
    }

    @Test
    fun understandsMpesaToMshwariTransfer() {
        val message =
            "RQK12ABC35 Confirmed. Ksh 2,500.00 transferred to M-Shwari account on 28/9/26 at 6:15 PM. M-Shwari balance is Ksh 3,043.00. New M-PESA balance is Ksh 5,289.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        val transfer = result.events.single() as TransferEventFact
        assertEquals(
            "SAFARICOM_MPESA",
            transfer.source.providerKey
        )
        assertEquals(
            "NCBA_MSHWARI_SAVINGS",
            transfer.destination.providerKey
        )
        assertEquals(250_000L, transfer.amountMinor)
    }

    @Test
    fun unknownInstitutionIsUnderstoodWithoutPretendingOwnership() {
        val message =
            "RQK12ABC36 Confirmed. Ksh 5,000.00 transferred from PesaNova account on 28/9/26 at 7:00 PM. PesaNova balance is Ksh 12,400.00. M-PESA balance is Ksh 8,100.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        val transfer = result.events.single() as TransferEventFact
        assertEquals("PesaNova", transfer.source.displayName)
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            transfer.source.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            transfer.confidence
        )
        assertEquals(2, result.balances.size)
    }

    @Test
    fun understandsReceivedFromNamedAccountGrammar() {
        val message =
            "RQK12ABC38 Confirmed. Received Ksh 4,000.00 from PesaNova account on 28/9/26 at 7:05 PM. M-PESA balance is Ksh 12,100.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        val transfer = result.events.single() as TransferEventFact
        assertEquals("PesaNova", transfer.source.displayName)
        assertEquals(
            "SAFARICOM_MPESA",
            transfer.destination.providerKey
        )
        assertEquals(400_000L, transfer.amountMinor)
    }

    @Test
    fun understandsOrdinaryMpesaPaymentGenerically() {
        val message =
            "RQK12ABC37 Confirmed. Ksh 450.00 sent to SAMPLE PERSON 0712345678 on 28/9/26 at 7:10 PM. New M-PESA balance is Ksh 7,650.00. Transaction cost, Ksh 7.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        assertTrue(result.recognized)
        assertTrue(result.consumesLegacyTransactionParsing)
        assertEquals(1, result.balances.size)

        val payment =
            result.events.single() as PaymentEventFact
        assertEquals(45_000L, payment.amountMinor)
        assertEquals("SAMPLE PERSON", payment.counterparty)
        assertEquals(
            "+254712345678",
            payment.counterpartyPhone
        )
        assertEquals(700L, payment.feeMinor)
        assertEquals(
            "SAFARICOM_MPESA",
            payment.source.providerKey
        )
        assertTrue(
            payment.evidence.any {
                it.field == FinancialField.AMOUNT
            }
        )
        assertTrue(
            payment.evidence.any {
                it.field == FinancialField.COUNTERPARTY
            }
        )
        assertEquals(
            765_000L,
            result.balances.single().balanceMinor
        )
    }

    @Test
    fun understandsOrdinaryMpesaReceiptGenerically() {
        val message =
            "RQK12ABC39 Confirmed. You have received Ksh 2,000.00 from SAMPLE PERSON 0712345679 on 28/9/26 at 7:20 PM. New M-PESA balance is Ksh 9,650.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "MPESA",
            body = message
        )

        assertTrue(result.consumesLegacyTransactionParsing)

        val receipt =
            result.events.single() as ReceiptEventFact
        assertEquals(200_000L, receipt.amountMinor)
        assertEquals("SAMPLE PERSON", receipt.counterparty)
        assertEquals(
            "+254712345679",
            receipt.counterpartyPhone
        )
        assertEquals(
            "SAFARICOM_MPESA",
            receipt.destination.providerKey
        )
    }

    @Test
    fun ignoresNonFinancialAirtimeBalance() {
        val message =
            "Your airtime balance is Ksh 73.00."

        val result = FinancialMessageInterpreter.interpret(
            title = "Safaricom",
            body = message
        )

        assertFalse(result.recognized)
    }
}
