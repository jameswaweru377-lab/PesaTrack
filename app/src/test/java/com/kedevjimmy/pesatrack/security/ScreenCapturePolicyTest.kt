package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCapturePolicyTest {
    @Test
    fun unlockedForegroundScreensCanBeCapturedEvenWithAppLockEnabled() {
        assertFalse(ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = true,
            appLocked = false,
            foreground = true,
            backupRecoveryBlocked = false
        ))
    }

    @Test
    fun lockedScreenStillBlocksCapture() {
        assertTrue(ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = true,
            appLocked = true,
            foreground = true,
            backupRecoveryBlocked = false
        ))
    }

    @Test
    fun backgroundedProtectedAppStillBlocksCapture() {
        assertTrue(ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = true,
            appLocked = false,
            foreground = false,
            backupRecoveryBlocked = false
        ))
    }

    @Test
    fun backupRecoveryFailureBlocksCaptureEvenWithoutAppLock() {
        assertTrue(ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = false,
            appLocked = false,
            foreground = true,
            backupRecoveryBlocked = true
        ))
    }

    @Test
    fun appWithoutLockDoesNotBlockCaptureInBackground() {
        assertFalse(ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = false,
            appLocked = false,
            foreground = false,
            backupRecoveryBlocked = false
        ))
    }
}
