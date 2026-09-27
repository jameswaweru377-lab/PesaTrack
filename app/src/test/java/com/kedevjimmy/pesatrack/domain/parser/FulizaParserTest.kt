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
    fun parsesAutomaticFulizaRepayment() {
        val message =
            "UIQFT85YNE Confirmed. Ksh 220.00 from your M-PESA has been used to partially pay your outstanding Fuliza M-PESA. Your available Fuliza M-PESA limit is Ksh 589.09. Your M-PESA balance is 0.00."

        val parsed = FulizaParser.parseRepayment(message)

        assertNotNull(parsed)
        assertEquals("UIQFT85YNE", parsed?.transactionCode)
        assertEquals(22_000L, parsed?.repaidAmountMinor)
        assertEquals(58_909L, parsed?.availableLimitMinor)
        assertEquals(0L, parsed?.mpesaBalanceMinor)
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
