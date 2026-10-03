package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessageLabShapeIdentityFactoryTest {
    @Test
    fun sensitiveValueChangesKeepTheSameShapeIdentity() {
        val first =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "Equity",
                    body =
                        "Dear James, account 1234567890 was credited with KES 1,250.00 on 02/10/2026 at 10:30 AM.",
                    sourcePackage =
                        "KE.EXAMPLE.BANK",
                    sourceKind =
                        " financial_notification "
                )
        val second =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "Equity",
                    body =
                        "Dear Mary, account 9988776655 was credited with KES 9,999.00 on 03/10/2026 at 11:45 AM.",
                    sourcePackage =
                        "ke.example.bank",
                    sourceKind =
                        "financial_notification"
                )

        assertEquals(
            first.shapeId,
            second.shapeId
        )
        assertEquals(
            "ke.example.bank",
            first.sourcePackage
        )
        assertEquals(
            "financial_notification",
            first.sourceKind
        )
    }

    @Test
    fun punctuationCaseAndCurrencyVariantsShareFamilyButKeepExactShapeIds() {
        val first =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "MPESA",
                    body =
                        "ABCDEF1234 Confirmed. KES 575.00 paid to TEST SHOP.",
                    sourcePackage = null,
                    sourceKind =
                        "historical_financial_message"
                )
        val second =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "mpesa",
                    body =
                        "ZYXWVU9876 confirmed Ksh 900.00 paid to OTHER SHOP!",
                    sourcePackage = null,
                    sourceKind =
                        "historical_financial_message"
                )

        assertNotEquals(
            first.shapeId,
            second.shapeId
        )
        assertEquals(
            first.familyId,
            second.familyId
        )
    }

    @Test
    fun materiallyDifferentWordingDoesNotCollapseIntoSameFamily() {
        val payment =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "MPESA",
                    body =
                        "ABCDEF1234 Confirmed. KES 575.00 paid to TEST SHOP.",
                    sourcePackage = null,
                    sourceKind =
                        "historical_financial_message"
                )
        val received =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "MPESA",
                    body =
                        "ABCDEF1234 Confirmed. You have received KES 575.00 from TEST USER.",
                    sourcePackage = null,
                    sourceKind =
                        "historical_financial_message"
                )

        assertNotEquals(
            payment.familyId,
            received.familyId
        )
    }

    @Test
    fun differentSourcePackagesDoNotCollapseTheSameText() {
        val first =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "Bank",
                    body =
                        "Your account was debited with KES 500.00.",
                    sourcePackage =
                        "ke.example.bank.one",
                    sourceKind =
                        "financial_notification"
                )
        val second =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "Bank",
                    body =
                        "Your account was debited with KES 500.00.",
                    sourcePackage =
                        "ke.example.bank.two",
                    sourceKind =
                        "financial_notification"
                )

        assertNotEquals(
            first.shapeId,
            second.shapeId
        )
    }

    @Test
    fun differentChannelsRemainSeparateCoverageShapes() {
        val app =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "KCB",
                    body =
                        "Your account was credited with KES 1,000.00.",
                    sourcePackage =
                        "ke.example.bank",
                    sourceKind =
                        "financial_notification"
                )
        val sms =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        "KCB",
                    body =
                        "Your account was credited with KES 1,000.00.",
                    sourcePackage =
                        null,
                    sourceKind =
                        "historical_financial_message"
                )

        assertNotEquals(
            app.shapeId,
            sms.shapeId
        )
    }
}
