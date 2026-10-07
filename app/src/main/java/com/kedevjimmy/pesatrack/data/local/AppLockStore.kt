package com.kedevjimmy.pesatrack.data.local

import android.content.Context

/**
 * Stores only whether app lock is enabled.
 *
 * PesaTrack never stores a biometric template, device PIN, password or secret.
 * Authentication itself is delegated to Android's BiometricPrompt/device
 * credential flow.
 */
class AppLockStore private constructor(
    context: Context
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                "pesatrack_security",
                Context.MODE_PRIVATE
            )

    fun isEnabled(): Boolean =
        prefs.getBoolean(
            KEY_ENABLED,
            false
        )

    fun setEnabled(
        enabled: Boolean
    ) {
        prefs.edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }

    companion object {
        private const val KEY_ENABLED =
            "app_lock_enabled"

        @Volatile
        private var instance:
            AppLockStore? = null

        fun get(
            context: Context
        ): AppLockStore =
            instance ?: synchronized(this) {
                instance
                    ?: AppLockStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
