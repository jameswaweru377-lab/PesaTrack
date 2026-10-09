package com.kedevjimmy.pesatrack.domain.onboarding

interface ConsentVersionStorage {
    fun readVersion(): Int
    fun writeVersion(version: Int): Boolean
}

class SmsAccessConsent(private val storage: ConsentVersionStorage) {
    fun hasConsent(): Boolean = storage.readVersion() == CURRENT_VERSION
    fun accept(): Boolean = storage.writeVersion(CURRENT_VERSION)
    fun decline(): Boolean = storage.writeVersion(0)
    fun canRead(smsGranted: Boolean): Boolean = hasConsent() && smsGranted

    companion object { private const val CURRENT_VERSION = 1 }
}
