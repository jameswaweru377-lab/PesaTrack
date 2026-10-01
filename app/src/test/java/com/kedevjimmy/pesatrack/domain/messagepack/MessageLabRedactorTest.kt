package com.kedevjimmy.pesatrack.domain.messagepack

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabRedactorTest {
    @Test
    fun removesSensitiveValuesButKeepsFinancialShape() {
        val redacted =
            MessageLabRedactor.redact(
                safeSenderLabel = "M-Pesa",
                body =
                    "UINFT7V1JD Confirmed. Ksh985.00 sent to JOHN DOE 0712345678 on 23/9/26 at 5:27 PM. New M-PESA balance is Ksh1,250.00. Ref 123456789."
            )

        assertFalse(
            redacted.body.contains(
                "UINFT7V1JD"
            )
        )
        assertFalse(
            redacted.body.contains(
                "0712345678"
            )
        )
        assertFalse(
            redacted.body.contains(
                "985.00"
            )
        )
        assertFalse(
            redacted.body.contains(
                "1,250.00"
            )
        )
        assertFalse(
            redacted.body.contains(
                "JOHN DOE"
            )
        )
        assertTrue(
            redacted.body.contains(
                "<TX_CODE>"
            )
        )
        assertTrue(
            redacted.body.contains(
                "<PHONE>"
            )
        )
        assertTrue(
            redacted.body.contains(
                "<AMOUNT>"
            )
        )
        assertTrue(
            redacted.body.contains(
                "<ENTITY>"
            )
        )
        assertTrue(
            redacted.body.contains(
                "M-PESA balance",
                ignoreCase = true
            )
        )
    }

    @Test
    fun unknownSenderIsNotPersistedAsIdentity() {
        val redacted =
            MessageLabRedactor.redact(
                safeSenderLabel = null,
                body =
                    "KES 640.00 processed through your account."
            )

        assertTrue(
            redacted.sender ==
                "<SENDER>"
        )
    }
}
