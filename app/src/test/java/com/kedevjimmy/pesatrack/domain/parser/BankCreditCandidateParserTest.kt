package com.kedevjimmy.pesatrack.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankCreditCandidateParserTest {
    @Test
    fun findsIncomingCreditAndOriginator() {
        val parsed = BankCreditCandidateParser.parse(
            "Your account has been credited with KES 60,000.00 from XYZ COMPANY on 26/09/2026."
        )

        assertEquals(6_000_000L, parsed?.amountMinor)
        assertEquals("XYZ COMPANY", parsed?.originator)
    }

    @Test
    fun ignoresOrdinaryDebitAlert() {
        assertNull(
            BankCreditCandidateParser.parse(
                "KES 2,500.00 debited from your account for card purchase."
            )
        )
    }
}
