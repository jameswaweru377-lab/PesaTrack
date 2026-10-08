package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialWhitespaceTest {
    @Test fun normalizesCurrencyTypographyAcrossInstitutions() {
        listOf("\u00a0", "\u202f", "\u2007").forEach { space ->
            assertEquals("KES 450.00", FinancialMessageNormalizer.normalize(null, "Kshs${space}450.00").body)
            assertEquals("KES 450.00", FinancialMessageNormalizer.normalize(null, "450.00${space}KES").body)
        }
    }

    @Test fun doesNotJoinSeparateNumbersOrInventCurrency() {
        assertEquals("1 250", FinancialMessageNormalizer.normalize(null, "1\u00a0250").body)
        assertEquals(false, FinancialMessageNormalizer.normalize(null, "shop 450").hasCurrencyHint)
    }

    @Test fun bankCreditsKeepPrincipalAndBalanceSeparate() {
        listOf("My Bank", "Equity", "Cooperative Bank").forEach { title ->
            val result = FinancialMessageInterpreter.interpret(title,
                "Your account has been credited with Kshs\u202f60,000.00 from SAMPLE COMPANY on 26/09/2026. Available balance Kshs\u202f72,100.00.")
            assertEquals("Receipt count for $title", 1, result.events.size)
            assertEquals(6_000_000L, (result.events.single() as ReceiptEventFact).amountMinor)
            assertEquals(7_210_000L, result.balances.single().balanceMinor)
        }
    }

    @Test fun currencyTypographyDoesNotProveAnAmbiguousSenderIsAnOwnedBankAccount() {
        assertEquals(null, FinancialProviderRegistry.resolveOwnedMessageSourceAccount("Cooperative", bankLike = true))
        val result = FinancialMessageInterpreter.interpret("Cooperative",
            "Your account has been credited with Kshs\u202f60,000.00 from SAMPLE COMPANY on 26/09/2026. Available balance Kshs\u202f72,100.00.")
        assertEquals(0, result.events.size)
    }

    @Test fun doesNotExtendRecognitionToAmbiguousUnicodeSeparatedDigits() {
        assertEquals("Kshs\u202f1\u202f250.00",
            FinancialMessageNormalizer.normalize(null, "Kshs\u202f1\u202f250.00").body)
    }
}
