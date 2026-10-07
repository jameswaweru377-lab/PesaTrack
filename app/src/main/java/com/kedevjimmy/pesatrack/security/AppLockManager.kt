package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.max
import kotlin.math.min

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
            .remove(
                KEY_FAILED_ATTEMPTS
            )
            .remove(
                KEY_LOCKOUT_UNTIL_EPOCH_MS
            )
            .apply()
    }

    fun verifyPin(
        pin: String,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): Boolean {
        if (!isEnabled()) {
            return true
        }
        if (
            remainingLockoutMs(
                nowEpochMs
            ) > 0L
        ) {
            return false
        }
        if (!isSupportedStoredPin(pin)) {
            recordFailedAttempt(
                nowEpochMs
            )
            return false
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
                ?: run {
                    recordFailedAttempt(
                        nowEpochMs
                    )
                    return false
                }

        val verified =
            MessageDigest.isEqual(
                expected,
                actual
            )
        if (verified) {
            clearFailedAttempts()
        } else {
            recordFailedAttempt(
                nowEpochMs
            )
        }
        return verified
    }

    fun remainingLockoutMs(
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): Long =
        max(
            0L,
            prefs.getLong(
                KEY_LOCKOUT_UNTIL_EPOCH_MS,
                0L
            ) - nowEpochMs
        )

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
            .remove(
                KEY_FAILED_ATTEMPTS
            )
            .remove(
                KEY_LOCKOUT_UNTIL_EPOCH_MS
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

    private fun recordFailedAttempt(
        nowEpochMs: Long
    ) {
        val attempts =
            prefs.getInt(
                KEY_FAILED_ATTEMPTS,
                0
            ) + 1
        val editor =
            prefs.edit()
                .putInt(
                    KEY_FAILED_ATTEMPTS,
                    attempts
                )

        lockoutDelayMs(
            attempts
        )
            .takeIf {
                it > 0L
            }
            ?.let { delayMs ->
                editor.putLong(
                    KEY_LOCKOUT_UNTIL_EPOCH_MS,
                    nowEpochMs +
                        delayMs
                )
            }

        editor.apply()
    }

    private fun clearFailedAttempts() {
        prefs.edit()
            .remove(
                KEY_FAILED_ATTEMPTS
            )
            .remove(
                KEY_LOCKOUT_UNTIL_EPOCH_MS
            )
            .apply()
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
        private const val KEY_FAILED_ATTEMPTS =
            "failed_pin_attempts"
        private const val KEY_LOCKOUT_UNTIL_EPOCH_MS =
            "pin_lockout_until_epoch_ms"

        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private const val ITERATIONS = 120_000
        private const val LOCK_GRACE_MS =
            30_000L
        private const val NEW_PIN_MIN_LENGTH = 6
        private const val MAX_PIN_LENGTH = 8
        private const val LEGACY_PIN_MIN_LENGTH = 4
        private const val FAILED_ATTEMPTS_BEFORE_DELAY = 3
        private const val BASE_LOCKOUT_MS =
            2_000L
        private const val MAX_LOCKOUT_MS =
            60_000L

        fun requireValidPin(pin: String) {
            require(
                pin.length in
                    NEW_PIN_MIN_LENGTH..
                    MAX_PIN_LENGTH &&
                    pin.all(Char::isDigit)
            ) {
                "Use a 6–8 digit PIN."
            }
        }

        private fun isSupportedStoredPin(
            pin: String
        ): Boolean =
            pin.length in
                LEGACY_PIN_MIN_LENGTH..
                MAX_PIN_LENGTH &&
                pin.all(Char::isDigit)

        private fun derive(
            pin: String,
            salt: ByteArray
        ): ByteArray {
            require(
                isSupportedStoredPin(pin)
            ) {
                "Invalid stored PIN format."
            }
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

        private fun lockoutDelayMs(
            attempts: Int
        ): Long {
            if (
                attempts <
                FAILED_ATTEMPTS_BEFORE_DELAY
            ) {
                return 0L
            }
            val exponent =
                min(
                    attempts -
                        FAILED_ATTEMPTS_BEFORE_DELAY,
                    5
                )
            return min(
                MAX_LOCKOUT_MS,
                BASE_LOCKOUT_MS *
                    (1L shl exponent)
            )
        }
    }
}
