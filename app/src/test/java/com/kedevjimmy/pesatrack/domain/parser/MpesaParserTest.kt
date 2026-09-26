package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MpesaParserTest {

    @Test
    fun parsesSentMoneyMessage() {
        val message = "TQK12ABC34 Confirmed. Ksh450.00 sent to ELIZABETH W. 0712345678 on 21/9/26 at 1:24 PM. New M-PESA balance is Ksh12,850.00. Transaction cost, Ksh0.00."

        val parsed = MpesaParser.parse(message)

        assertNotNull(parsed)
        assertEquals("TQK12ABC34", parsed?.transactionCode)
        assertEquals(MpesaTransactionType.SENT_MONEY, parsed?.type)
        assertEquals(45_000L, parsed?.amountMinor)
        assertEquals("ELIZABETH W", parsed?.counterparty)
        assertEquals("+254712345678", parsed?.counterpartyPhone)
        assertEquals(0L, parsed?.feeMinor)
        assertEquals(1_285_000L, parsed?.balanceMinor)
    }

    @Test
    fun parsesPayBillMessage() {
        val message = "TQK98XYZ76 Confirmed. Ksh1,200.00 paid to KPLC PREPAID for account 123456 on 20/9/26 at 2:20 PM. New M-PESA balance is Ksh8,900.00. Transaction cost, Ksh23.00."

        val parsed = MpesaParser.parse(message)

        assertNotNull(parsed)
        assertEquals(MpesaTransactionType.PAY_BILL, parsed?.type)
        assertEquals(120_000L, parsed?.amountMinor)
        assertEquals("KPLC PREPAID", parsed?.counterparty)
        assertEquals(2_300L, parsed?.feeMinor)
    }

    @Test
    fun stripsMaskedPhoneFromPersonCounterparty() {
        val message = "TQK12DEF56 Confirmed. Ksh900.00 sent to JANE NJERI 2547****321 on 22/9/26 at 8:10 AM. New M-PESA balance is Ksh11,950.00."

        val parsed = MpesaParser.parse(message)

        assertNotNull(parsed)
        assertEquals("JANE NJERI", parsed?.counterparty)
        assertEquals(1_195_000L, parsed?.balanceMinor)
    }

    @Test
    fun keepsMerchantNameWithoutTryingToTreatItAsAPhone() {
        val message = "TQK55ABC77 Confirmed. Ksh1,500.00 paid to PLANET INTERNET 5G on 22/9/26 at 9:32 AM. New M-PESA balance is Ksh10,450.00."

        val parsed = MpesaParser.parse(message)

        assertNotNull(parsed)
        assertEquals("PLANET INTERNET 5G", parsed?.counterparty)
    }


    @Test
    fun normalizesReceivedMoneyPhoneIdentity() {
        val message = "TQK66ABC88 Confirmed. You have received Ksh2,000.00 from JOSEPH MWANGO 0711223344 on 23/9/26 at 7:45 PM. New M-PESA balance is Ksh14,000.00."

        val parsed = MpesaParser.parse(message)

        assertNotNull(parsed)
        assertEquals("JOSEPH MWANGO", parsed?.counterparty)
        assertEquals("+254711223344", parsed?.counterpartyPhone)
    }

    @Test
    fun ignoresNonMpesaText() {
        assertEquals(null, MpesaParser.parse("Your parcel has been delivered."))
    }
}
