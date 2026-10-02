package com.kedevjimmy.pesatrack.domain.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityAliasNormalizerTest {
    @Test
    fun normalizesKenyanPhoneVariantsToOneIdentity() {
        assertEquals(
            "254712345678",
            IdentityAliasNormalizer
                .normalizeKenyanPhone(
                    "0712 345 678"
                )
        )
        assertEquals(
            "254712345678",
            IdentityAliasNormalizer
                .normalizeKenyanPhone(
                    "+254 712 345 678"
                )
        )
        assertEquals(
            "254712345678",
            IdentityAliasNormalizer
                .normalizeKenyanPhone(
                    "712345678"
                )
        )
    }

    @Test
    fun acceptsNewerKenyan01MobilePrefixes() {
        assertEquals(
            "254112345678",
            IdentityAliasNormalizer
                .normalizeKenyanPhone(
                    "0112 345 678"
                )
        )
        assertTrue(
            IdentityAliasNormalizer
                .looksLikeKenyanPhone(
                    "0112345678"
                )
        )
    }

    @Test
    fun normalizesNamesWithoutDecidingEntityKind() {
        assertEquals(
            "GEOFFREY KERONGO",
            IdentityAliasNormalizer
                .normalize(
                    IdentityAliasType.NAME,
                    "  Geoffrey   Kerongo "
                )
        )
        assertEquals(
            "NAIVAS FOODMARKET 001",
            IdentityAliasNormalizer
                .normalize(
                    IdentityAliasType.NAME,
                    "Naivas Foodmarket-001"
                )
        )
    }

    @Test
    fun randomLongNumbersAreNotTreatedAsKenyanPhones() {
        assertFalse(
            IdentityAliasNormalizer
                .looksLikeKenyanPhone(
                    "123456789012345"
                )
        )
    }
}
