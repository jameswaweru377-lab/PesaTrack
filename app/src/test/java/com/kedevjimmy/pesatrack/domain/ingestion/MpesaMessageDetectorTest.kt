package com.kedevjimmy.pesatrack.domain.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaMessageDetectorTest {
    @Test
    fun acceptsMpesaSenderTitle() {
        assertTrue(
            MpesaMessageDetector.isLikelyMpesa(
                title = "M-PESA",
                body = "TQK12ABC34 Confirmed. Ksh450.00 sent to ELIZABETH W."
            )
        )
    }

    @Test
    fun acceptsConfirmationBodyWithoutSenderTitle() {
        assertTrue(
            MpesaMessageDetector.isLikelyMpesa(
                title = "Messages",
                body = "TQK12ABC34 Confirmed. Ksh450.00 sent to ELIZABETH W."
            )
        )
    }

    @Test
    fun rejectsUnrelatedNotification() {
        assertFalse(
            MpesaMessageDetector.isLikelyMpesa(
                title = "Shopping",
                body = "Your parcel has been delivered."
            )
        )
    }
}
