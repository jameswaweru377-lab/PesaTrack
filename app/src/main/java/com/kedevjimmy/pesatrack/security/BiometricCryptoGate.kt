package com.kedevjimmy.pesatrack.security

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * One biometric approval must sign a fresh challenge with an Android Keystore
 * private key. Its signature is checked independently with the public key.
 * A forged success callback (or a fake signing result) cannot grant access.
 * PIN and phone-lock recovery remain separate paths.
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
            val pair = getOrCreateKeyPair() ?: return@runCatching null
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(pair.private)
            pending = OneShotCryptoProof(signer, pair.public)
            BiometricPrompt.CryptoObject(signer)
        }.getOrNull()
    }

    fun confirm(result: BiometricPrompt.AuthenticationResult): Boolean {
        val proof = pending ?: return false
        pending = null
        return proof.verify(result.cryptoObject?.signature)
    }

    fun cancel() {
        pending = null
    }

    /** Existing invalidated key material must never be silently recreated. */
    private fun getOrCreateKeyPair(): KeyPair? {
        val store = keyStore()
        val existing = store.getKey(purpose.alias, null)
        if (existing is PrivateKey) {
            val public = store.getCertificate(purpose.alias)?.publicKey ?: return null
            return KeyPair(public, existing)
        }
        if (store.containsAlias(purpose.alias)) return null // Invalid keys fail closed.

        val builder = KeyGenParameterSpec.Builder(
            purpose.alias,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            .apply { initialize(builder.build()) }
            .generateKeyPair()
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

/** Testable single-use proof; signing requires biometrics, verification does not. */
internal class OneShotCryptoProof(
    private val expected: Signature,
    private val publicKey: PublicKey
) {
    private var attempted = false
    private val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Synchronized
    fun verify(received: Signature?): Boolean {
        if (attempted) return false
        attempted = true
        return try {
            if (received !== expected) return false
            received.update(challenge)
            val signed = received.sign() // Keystore denies signing without biometric auth.
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(challenge)
            verifier.verify(signed)
        } catch (_: Exception) {
            false
        } finally {
            challenge.fill(0)
        }
    }
}
