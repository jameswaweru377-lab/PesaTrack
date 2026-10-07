package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppLockManager(
    context: Context
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

    fun isEnabled(): Boolean =
        prefs.contains(KEY_PIN_HASH) &&
            prefs.contains(KEY_PIN_SALT)

    fun setPin(pin: String) {
        requireValidPin(pin)
        val salt =
            ByteArray(SALT_BYTES)
                .also {
                    SecureRandom()
                        .nextBytes(it)
                }
        val hash =
            derive(
                pin = pin,
                salt = salt
            )

        prefs.edit()
            .putString(
                KEY_PIN_SALT,
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            )
            .putString(
                KEY_PIN_HASH,
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
            )
            .remove(
                KEY_LAST_BACKGROUND_EPOCH_MS
            )
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!isEnabled()) {
            return true
        }

        val salt =
            prefs.getString(
                KEY_PIN_SALT,
                null
            )
                ?.let {
                    runCatching {
                        Base64.decode(
                            it,
                            Base64.NO_WRAP
                        )
                    }.getOrNull()
                }
                ?: return false
        val expected =
            prefs.getString(
                KEY_PIN_HASH,
                null
            )
                ?.let {
                    runCatching {
                        Base64.decode(
                            it,
                            Base64.NO_WRAP
                        )
                    }.getOrNull()
                }
                ?: return false

        val actual =
            runCatching {
                derive(
                    pin = pin,
                    salt = salt
                )
            }.getOrNull()
                ?: return false

        return MessageDigest.isEqual(
            expected,
            actual
        )
    }

    fun disable(pin: String): Boolean {
        if (!verifyPin(pin)) {
            return false
        }
        prefs.edit()
            .clear()
            .apply()
        return true
    }

    fun markBackgrounded(
        nowEpochMs: Long =
            System.currentTimeMillis()
    ) {
        if (!isEnabled()) {
            return
        }
        prefs.edit()
            .putLong(
                KEY_LAST_BACKGROUND_EPOCH_MS,
                nowEpochMs
            )
            .apply()
    }

    fun markUnlocked() {
        prefs.edit()
            .remove(
                KEY_LAST_BACKGROUND_EPOCH_MS
            )
            .apply()
    }

    fun shouldRequireUnlock(
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): Boolean {
        if (!isEnabled()) {
            return false
        }
        val backgroundedAt =
            prefs.getLong(
                KEY_LAST_BACKGROUND_EPOCH_MS,
                0L
            )
        if (backgroundedAt <= 0L) {
            return false
        }
        return nowEpochMs -
            backgroundedAt >=
            LOCK_GRACE_MS
    }

    companion object {
        const val PREFS_NAME =
            "security_preferences"

        private const val KEY_PIN_HASH =
            "app_lock_pin_hash"
        private const val KEY_PIN_SALT =
            "app_lock_pin_salt"
        private const val KEY_LAST_BACKGROUND_EPOCH_MS =
            "last_background_epoch_ms"

        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private const val ITERATIONS = 120_000
        private const val LOCK_GRACE_MS =
            30_000L

        fun requireValidPin(pin: String) {
            require(
                pin.length in 4..8 &&
                    pin.all(Char::isDigit)
            ) {
                "Use a 4–8 digit PIN."
            }
        }

        private fun derive(
            pin: String,
            salt: ByteArray
        ): ByteArray {
            requireValidPin(pin)
            val spec =
                PBEKeySpec(
                    pin.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_BITS
                )
            return try {
                SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
                )
                    .generateSecret(spec)
                    .encoded
            } finally {
                spec.clearPassword()
            }
        }
    }
}
