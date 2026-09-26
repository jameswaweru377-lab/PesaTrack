package com.kedevjimmy.pesatrack.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FulizaParserTest {
    @Test
    fun parsesFulizaDrawdownWithoutTurningItIntoPurchase() {
        val message = "UFAGA7JQOD Confirmed. Fuliza M-PESA amount is KSh 167.00. Access Fee charged KSh 1.67. Total Fuliza M-PESA outstanding amount is KSh 1086.51 due on 18/05/26."

        val parsed = FulizaParser.parse(message)

        assertNotNull(parsed)
        assertEquals("UFAGA7JQOD", parsed?.transactionCode)
        assertEquals(16_700L, parsed?.financedAmountMinor)
        assertEquals(167L, parsed?.accessFeeMinor)
        assertEquals(108_651L, parsed?.outstandingAmountMinor)
    }

    @Test
    fun acceptsAccessFeeChargeOfVariant() {
        val message = "ABCD12EFGH Confirmed. Fuliza M-PESA amount Ksh90.00. Access Fee charge of Ksh0.90. Total Fuliza M-PESA outstanding amount is Ksh90.90."

        val parsed = FulizaParser.parse(message)

        assertNotNull(parsed)
        assertEquals(9_000L, parsed?.financedAmountMinor)
        assertEquals(90L, parsed?.accessFeeMinor)
        assertEquals(9_090L, parsed?.outstandingAmountMinor)
    }
}
