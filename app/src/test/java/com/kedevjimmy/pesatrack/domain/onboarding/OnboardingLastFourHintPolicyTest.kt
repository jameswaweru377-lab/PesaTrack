package com.kedevjimmy.pesatrack.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingLastFourHintPolicyTest {
    @Test fun fourDigitSuffixIsAnOptionalLocalHint() {
        val valid = OnboardingLastFourHintPolicy.validate(
            setOf("owned-equity"), mapOf("owned-equity" to "6789")
        )
        assertNull(valid.error)
        assertEquals("6789", valid.entries["owned-equity"])
        assertEquals(emptyMap<String, String>(),
            OnboardingLastFourHintPolicy.validate(setOf("owned-equity"), emptyMap()).entries)
    }

    @Test fun incompleteOrForeignSuffixCannotBecomeVerifiedAccountIdentity() {
        assertNotNull(OnboardingLastFourHintPolicy.validate(
            setOf("owned-equity"), mapOf("owned-equity" to "678")).error)
        assertNotNull(OnboardingLastFourHintPolicy.validate(
            setOf("owned-equity"), mapOf("not-owned" to "1234")).error)
    }
}
