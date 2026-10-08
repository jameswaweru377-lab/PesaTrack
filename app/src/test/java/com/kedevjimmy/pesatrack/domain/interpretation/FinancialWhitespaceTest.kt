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
}
