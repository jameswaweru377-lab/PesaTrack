package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsupportedFinancialCandidatePolicyTest {
    @Test
    fun weakCreditLanguageDoesNotPolluteMessageLab() {
        val relevance =
            assess(
                title = "Service",
                body =
                    "Your credit score improved after 3 months."
            )

        assertTrue(relevance.isRelevant)
        assertFalse(
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        )
    }

    @Test
    fun weakLoanLanguageDoesNotPolluteMessageLab() {
        val relevance =
            assess(
                title = "Service",
                body =
                    "Loan services are now available in the app."
            )

        assertTrue(relevance.isRelevant)
        assertFalse(
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        )
    }

    @Test
    fun explicitDebtBalanceIsKeptForLearning() {
        val relevance =
            assess(
                title = "Future Lender",
                body =
                    "Your Nova Loan outstanding balance is KES 200.00."
            )

        assertTrue(
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        )
    }

    @Test
    fun explicitUnknownPaymentIsKeptForLearning() {
        val relevance =
            assess(
                title = "Future Wallet",
                body =
                    "KES 450 paid to SAMPLE STORE. Ref ABC123."
            )

        assertTrue(
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        )
    }

    @Test
    fun noAmountFinancialConfirmationIsStillKept() {
        val relevance =
            assess(
                title = "Future Finance",
                body =
                    "Your transfer of funds has been processed."
            )

        assertTrue(
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        )
    }

    private fun assess(
        title: String?,
        body: String
    ): FinancialMessageRelevance =
        FinancialMessageRelevanceGate.assess(
            FinancialMessageNormalizer.normalize(
                title = title,
                body = body
            )
        )
}
