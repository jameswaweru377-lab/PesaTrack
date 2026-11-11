package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InvestmentLegacyIdentityTest {
    @Test
    fun confirmedShareKeyIncludesItsOldOfPrefixAlias() {
        assertEquals("instrument:OF_SCOM", legacyOfPrefixInvestmentKey("instrument:SCOM"))
        assertEquals("instrument:OF_KPLC", legacyOfPrefixInvestmentKey("instrument:KPLC"))
    }

    @Test
    fun unexpectedInstrumentKeysDoNotInventAliases() {
        assertNull(legacyOfPrefixInvestmentKey("fund:ZIIDI"))
        assertNull(legacyOfPrefixInvestmentKey("instrument:OF_SCOM"))
        assertNull(legacyOfPrefixInvestmentKey("instrument:"))
    }

    @Test
    fun historicalOfPrefixLabelIsDisplayedAsTheSameInstrument() {
        assertEquals("SCOM", canonicalInvestmentLabel("of SCOM"))
        assertEquals("Kenya Power", canonicalInvestmentLabel("Kenya Power"))
        assertEquals("instrument:SCOM", canonicalInvestmentKey("instrument:OF_SCOM"))
        assertEquals("instrument:SCOM", canonicalInvestmentKey("instrument:SCOM"))
        assertEquals("fund:OF_SCOM", canonicalInvestmentKey("fund:OF_SCOM"))
    }
}
