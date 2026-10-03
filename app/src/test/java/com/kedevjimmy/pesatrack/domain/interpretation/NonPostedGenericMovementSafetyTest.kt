package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertTrue
import org.junit.Test

class NonPostedGenericMovementSafetyTest {
    @Test
    fun pendingGenericBankDebitDoesNotCreateMovement() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "Your account XX9087 has been debited by KES 500.00 pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun pendingGenericBankCreditDoesNotCreateMovement() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "KES 825.50 has been credited to your account XX9087 pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun pendingBeneficiaryBankDebitDoesNotCreatePayment() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "EQUITY BANK",
                    body =
                        "Your Equity a/c XXX5678 has been debited with KES 2,400.00. Transfer to SAMPLE BENEFICIARY pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun pendingCounterpartyReceiptDoesNotCreateReceipt() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "Your account has been credited with KES 60,000.00 from XYZ COMPANY pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun pendingGenericTransferDoesNotCreateTransfer() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "KES 1,000.00 transferred from Savings account to Current account pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun failedGenericPaymentDoesNotCreatePayment() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "Payments",
                    body =
                        "KES 750.00 sent to SAMPLE USER failed."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }
}
