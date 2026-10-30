package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.min

class AppLockManager(
    context: Context
) {
    private val contentResolver =
        context.applicationContext.contentResolver
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

    fun isEnabled(): Boolean =
        prefs.contains(KEY_PIN_HASH) &&
            prefs.contains(KEY_PIN_SALT)

    /** Biometrics are optional; the PIN remains the required recovery path. */
    fun isBiometricUnlockEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_UNLOCK_ENABLED, true)

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_UNLOCK_ENABLED, enabled).apply()
    }

    /** Existing installations continue to use the previous 30-second grace by default. */
    fun backgroundLockGraceMs(): Long = when (
        prefs.getLong(KEY_BACKGROUND_LOCK_GRACE_MS, AppLockTimingPolicy.LOCK_GRACE_MS)
    ) {
        0L -> 0L
        60_000L -> 60_000L
        else -> AppLockTimingPolicy.LOCK_GRACE_MS
    }

    fun setBackgroundLockGraceMs(graceMs: Long) {
        require(graceMs == 0L || graceMs == 30_000L || graceMs == 60_000L)
        prefs.edit().putLong(KEY_BACKGROUND_LOCK_GRACE_MS, graceMs).apply()
    }

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
            .remove(KEY_LAST_BACKGROUND_ELAPSED_MS)
            .remove(KEY_BACKGROUND_BOOT_COUNT)
            .remove(
                KEY_FAILED_ATTEMPTS
            )
            .remove(
                KEY_LOCKOUT_UNTIL_EPOCH_MS
            )
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED_MS)
            .remove(KEY_LOCKOUT_BOOT_COUNT)
            .apply()
    }

    fun verifyPin(
        pin: String,
        nowElapsedMs: Long =
            SystemClock.elapsedRealtime()
    ): Boolean {
        if (!isEnabled()) {
            return true
        }
        if (
            remainingLockoutMs(
                nowElapsedMs
            ) > 0L
        ) {
            return false
        }
        if (!isSupportedStoredPin(pin)) {
            recordFailedAttempt(
                nowElapsedMs
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
                        nowElapsedMs
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
                nowElapsedMs
            )
        }
        return verified
    }

    fun remainingLockoutMs(
        nowElapsedMs: Long =
            SystemClock.elapsedRealtime()
    ): Long {
        val savedUntil = when {
            prefs.contains(KEY_LOCKOUT_UNTIL_ELAPSED_MS) ->
                prefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED_MS, 0L)
            prefs.contains(KEY_LOCKOUT_UNTIL_EPOCH_MS) ->
                prefs.getLong(KEY_LOCKOUT_UNTIL_EPOCH_MS, 0L)
            else -> return 0L
        }
        val bootCount = currentBootCount()
        val savedBootCount =
            prefs.getInt(KEY_LOCKOUT_BOOT_COUNT, UNKNOWN_BOOT_COUNT)
                .takeIf { prefs.contains(KEY_LOCKOUT_BOOT_COUNT) }
        val window = AppLockTimingPolicy.normalizeLockout(
            untilElapsedMs = savedUntil,
            lockoutBootCount = savedBootCount,
            nowElapsedMs = nowElapsedMs,
            currentBootCount = bootCount,
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        )
        if (window == null) {
            prefs.edit()
                .remove(KEY_LOCKOUT_UNTIL_ELAPSED_MS)
                .remove(KEY_LOCKOUT_UNTIL_EPOCH_MS)
                .remove(KEY_LOCKOUT_BOOT_COUNT)
                .apply()
            return 0L
        }
        if (
            window.untilElapsedMs != savedUntil ||
            savedBootCount != bootCount ||
            prefs.contains(KEY_LOCKOUT_UNTIL_EPOCH_MS)
        ) {
            prefs.edit()
                .putLong(KEY_LOCKOUT_UNTIL_ELAPSED_MS, window.untilElapsedMs)
                .putInt(KEY_LOCKOUT_BOOT_COUNT, bootCount)
                .remove(KEY_LOCKOUT_UNTIL_EPOCH_MS)
                .apply()
        }
        val remaining = AppLockTimingPolicy.remainingLockoutMs(window, nowElapsedMs)
        if (remaining == 0L) {
            prefs.edit()
                .remove(KEY_LOCKOUT_UNTIL_ELAPSED_MS)
                .remove(KEY_LOCKOUT_BOOT_COUNT)
                .apply()
        }
        return remaining
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
        nowElapsedMs: Long =
            SystemClock.elapsedRealtime()
    ) {
        if (!isEnabled()) {
            return
        }
        prefs.edit()
            .putLong(
                KEY_LAST_BACKGROUND_ELAPSED_MS,
                nowElapsedMs
            )
            .putInt(KEY_BACKGROUND_BOOT_COUNT, currentBootCount())
            .remove(KEY_LAST_BACKGROUND_EPOCH_MS)
            .apply()
    }

    fun markUnlocked() {
        prefs.edit()
            .remove(
                KEY_LAST_BACKGROUND_EPOCH_MS
            )
            .remove(KEY_LAST_BACKGROUND_ELAPSED_MS)
            .remove(KEY_BACKGROUND_BOOT_COUNT)
            .remove(
                KEY_FAILED_ATTEMPTS
            )
            .remove(
                KEY_LOCKOUT_UNTIL_EPOCH_MS
            )
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED_MS)
            .remove(KEY_LOCKOUT_BOOT_COUNT)
            .apply()
    }

    fun shouldRequireUnlock(
        nowElapsedMs: Long =
            SystemClock.elapsedRealtime()
    ): Boolean {
        if (!isEnabled()) {
            return false
        }
        if (prefs.contains(KEY_LAST_BACKGROUND_EPOCH_MS)) {
            // An old wall-clock timestamp cannot safely grant an unlock grace period.
            return true
        }
        return AppLockTimingPolicy.shouldRequireUnlock(
            backgroundedAtElapsedMs =
                prefs.getLong(KEY_LAST_BACKGROUND_ELAPSED_MS, 0L)
                    .takeIf { prefs.contains(KEY_LAST_BACKGROUND_ELAPSED_MS) },
            backgroundBootCount =
                prefs.getInt(KEY_BACKGROUND_BOOT_COUNT, UNKNOWN_BOOT_COUNT)
                    .takeIf { prefs.contains(KEY_BACKGROUND_BOOT_COUNT) },
            nowElapsedMs = nowElapsedMs,
            currentBootCount = currentBootCount(),
            graceMs = backgroundLockGraceMs()
        )
    }

    private fun recordFailedAttempt(
        nowElapsedMs: Long
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
                    KEY_LOCKOUT_UNTIL_ELAPSED_MS,
                    nowElapsedMs +
                        delayMs
                )
                    .putInt(KEY_LOCKOUT_BOOT_COUNT, currentBootCount())
                    .remove(KEY_LOCKOUT_UNTIL_EPOCH_MS)
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
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED_MS)
            .remove(KEY_LOCKOUT_BOOT_COUNT)
            .apply()
    }

    private fun currentBootCount(): Int =
        runCatching {
            Settings.Global.getInt(
                contentResolver,
                Settings.Global.BOOT_COUNT,
                UNKNOWN_BOOT_COUNT
            )
        }.getOrDefault(UNKNOWN_BOOT_COUNT)

    companion object {
        const val PREFS_NAME =
            "security_preferences"

        private const val KEY_PIN_HASH =
            "app_lock_pin_hash"
        private const val KEY_PIN_SALT =
            "app_lock_pin_salt"
        private const val KEY_BIOMETRIC_UNLOCK_ENABLED = "biometric_unlock_enabled"
        private const val KEY_BACKGROUND_LOCK_GRACE_MS = "background_lock_grace_ms"
        private const val KEY_LAST_BACKGROUND_EPOCH_MS =
            "last_background_epoch_ms"
        private const val KEY_LAST_BACKGROUND_ELAPSED_MS =
            "last_background_elapsed_ms"
        private const val KEY_BACKGROUND_BOOT_COUNT =
            "background_boot_count"
        private const val KEY_FAILED_ATTEMPTS =
            "failed_pin_attempts"
        private const val KEY_LOCKOUT_UNTIL_EPOCH_MS =
            "pin_lockout_until_epoch_ms"
        private const val KEY_LOCKOUT_UNTIL_ELAPSED_MS =
            "pin_lockout_until_elapsed_ms"
        private const val KEY_LOCKOUT_BOOT_COUNT =
            "lockout_boot_count"
        private const val UNKNOWN_BOOT_COUNT = Int.MIN_VALUE

        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private const val ITERATIONS = 120_000
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
            return PasswordKeyDerivation.derive(pin, salt, ITERATIONS, KEY_BITS)
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
