package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialNotificationCapturePolicyTest {
    @Test
    fun transactionAlertWithFinancialBodyIsAccepted() {
        val result =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        "Transaction alert",
                    body =
                        "KES 2,500.00 was debited from account 1234 for a card purchase.",
                    sourcePackage =
                        "ke.example.bank",
                    selfPackage =
                        "com.kedevjimmy.pesatrack"
                )

        assertTrue(
            result.shouldIngest
        )
    }

    @Test
    fun financialActionInTitleCanSupportShortNotificationBody() {
        val result =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        "Money received",
                    body =
                        "KES 500.00",
                    sourcePackage =
                        "ke.example.bank",
                    selfPackage =
                        "com.kedevjimmy.pesatrack"
                )

        assertTrue(
            result.shouldIngest
        )
    }

    @Test
    fun otpNotificationIsRejected() {
        val result =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        "Security code",
                    body =
                        "Your OTP is 123456. Do not share this code.",
                    sourcePackage =
                        "ke.example.bank",
                    selfPackage =
                        "com.kedevjimmy.pesatrack"
                )

        assertFalse(
            result.shouldIngest
        )
    }

    @Test
    fun loanPromotionIsRejected() {
        val result =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        "Loan offer",
                    body =
                        "You qualify for KES 10,000. Apply now.",
                    sourcePackage =
                        "ke.example.lender",
                    selfPackage =
                        "com.kedevjimmy.pesatrack"
                )

        assertFalse(
            result.shouldIngest
        )
    }

    @Test
    fun ownAppNotificationIsNeverIngested() {
        val result =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        "Transaction alert",
                    body =
                        "KES 2,500.00 was debited from account 1234.",
                    sourcePackage =
                        "com.kedevjimmy.pesatrack",
                    selfPackage =
                        "com.kedevjimmy.pesatrack"
                )

        assertFalse(
            result.shouldIngest
        )
    }

    @Test
    fun genericNotificationTitleUsesAppLabelForInterpretation() {
        assertEquals(
            "Example Bank",
            FinancialNotificationCapturePolicy
                .interpretationTitle(
                    notificationTitle =
                        "Transaction alert",
                    appLabel =
                        "Example Bank"
                )
        )
    }

    @Test
    fun meaningfulNotificationTitleIsPreserved() {
        assertEquals(
            "Money received",
            FinancialNotificationCapturePolicy
                .interpretationTitle(
                    notificationTitle =
                        "Money received",
                    appLabel =
                        "Example Bank"
                )
        )
    }
}
