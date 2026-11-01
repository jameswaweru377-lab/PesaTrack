package com.kedevjimmy.pesatrack.security

/** Allows deliberate captures of unlocked screens without exposing locked or recovery UI. */
internal object ScreenCapturePolicy {
    fun shouldBlockCapture(
        appLockEnabled: Boolean,
        appLocked: Boolean,
        foreground: Boolean,
        backupRecoveryBlocked: Boolean
    ): Boolean = backupRecoveryBlocked ||
        (appLockEnabled && (appLocked || !foreground))
}
