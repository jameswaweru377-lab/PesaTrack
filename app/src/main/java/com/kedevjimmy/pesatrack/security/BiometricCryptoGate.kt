package com.kedevjimmy.pesatrack.security

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * One biometric approval must complete an Android Keystore operation for this
 * particular request. A success callback without the matching CryptoObject
 * never grants access. PIN and phone-lock recovery remain separate paths.
 */
@RequiresApi(Build.VERSION_CODES.P)
class BiometricCryptoGate(private val purpose: Purpose) {
    enum class Purpose(val alias: String) {
        APP_UNLOCK("pesatrack_biometric_app_unlock_v1"),
        BACKUP_RESTORE("pesatrack_biometric_restore_v1")
    }

    private var pending: OneShotCryptoProof? = null

    fun prepare(): BiometricPrompt.CryptoObject? {
        pending = null
        return runCatching {
            val key = getOrCreateKey() ?: return@runCatching null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            pending = OneShotCryptoProof(cipher)
            BiometricPrompt.CryptoObject(cipher)
        }.getOrNull()
    }

    fun confirm(result: BiometricPrompt.AuthenticationResult): Boolean {
        val proof = pending ?: return false
        pending = null
        return proof.verify(result.cryptoObject?.cipher)
    }

    fun cancel() {
        pending = null
    }

    /** Existing invalidated key material must never be silently recreated. */
    private fun getOrCreateKey(): SecretKey? {
        val store = keyStore()
        val existing = store.getKey(purpose.alias, null)
        if (existing is SecretKey) return existing
        if (store.containsAlias(purpose.alias)) return null // Invalid keys fail closed.

        val builder = KeyGenParameterSpec.Builder(
            purpose.alias,
            KeyProperties.PURPOSE_ENCRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(builder.build()) }
            .generateKey()
    }

    companion object {
        /** Invalidated keys cannot silently enroll new fingerprints to unlock data. */
        fun rearmAfterTrustedFallback(purpose: Purpose) {
            runCatching { keyStore().deleteEntry(purpose.alias) }
        }

        private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore")
            .apply { load(null) }
    }
}

/** Testable, single-use proof. The Keystore cipher cannot finish until biometrics authorize it. */
internal class OneShotCryptoProof(private val expected: Cipher) {
    private var attempted = false

    @Synchronized
    fun verify(received: Cipher?): Boolean {
        if (attempted) return false
        attempted = true
        if (received !== expected) return false

        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return try {
            // GCM adds a 128-bit tag; merely receiving a callback is insufficient.
            received.doFinal(challenge).size == challenge.size + 16
        } catch (_: Exception) {
            false
        } finally {
            challenge.fill(0)
        }
    }
}
