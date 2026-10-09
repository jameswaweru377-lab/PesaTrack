package com.kedevjimmy.pesatrack.domain.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAccessConsentTest {
    private class SavedVersion(var version: Int = 0, var writable: Boolean = true) : ConsentVersionStorage {
        override fun readVersion() = version
        override fun writeVersion(version: Int): Boolean {
            if (!writable) return false
            this.version = version
            return true
        }
    }
    @Test fun preExistingAndroidPermissionDoesNotAuthorizeReading() {
        assertFalse(SmsAccessConsent(SavedVersion()).canRead(true))
    }
    @Test fun acceptanceSurvivesRecreatingTheReader() {
        val saved = SavedVersion()
        val consent = SmsAccessConsent(saved)
        assertFalse(consent.canRead(true))
        assertTrue(consent.accept())
        assertTrue(SmsAccessConsent(saved).canRead(true))
        assertFalse(SmsAccessConsent(saved).canRead(false))
    }
    @Test fun skippingRevokesEarlierConsentAcrossReaders() {
        val saved = SavedVersion()
        SmsAccessConsent(saved).accept()
        assertTrue(SmsAccessConsent(saved).decline())
        assertFalse(SmsAccessConsent(saved).canRead(true))
    }
    @Test fun failedSaveNeverAuthorizesAccess() {
        val consent = SmsAccessConsent(SavedVersion(writable = false))
        assertFalse(consent.accept())
        assertFalse(consent.canRead(true))
    }
    @Test fun unknownDisclosureVersionRequiresNewAcceptance() {
        assertFalse(SmsAccessConsent(SavedVersion(version = 99)).canRead(true))
    }
}
