package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTimingPolicyTest {
    @Test
    fun backgroundGraceUsesElapsedTimeAndFailsClosedAcrossBoots() {
        assertFalse(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = 100_000L,
                backgroundBootCount = 12,
                nowElapsedMs = 129_999L,
                currentBootCount = 12
            )
        )
        assertTrue(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = 100_000L,
                backgroundBootCount = 12,
                nowElapsedMs = 130_000L,
                currentBootCount = 12
            )
        )
        assertTrue(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = 100_000L,
                backgroundBootCount = 12,
                nowElapsedMs = 300_000L,
                currentBootCount = 13
            )
        )
        assertTrue(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = 1_798_000_000_000L,
                backgroundBootCount = null,
                nowElapsedMs = 101_000L,
                currentBootCount = 12
            )
        )
        assertTrue(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = 100_000L,
                backgroundBootCount = 12,
                nowElapsedMs = 99_999L,
                currentBootCount = 12
            )
        )
        assertFalse(
            AppLockTimingPolicy.shouldRequireUnlock(
                backgroundedAtElapsedMs = null,
                backgroundBootCount = null,
                nowElapsedMs = 100_000L,
                currentBootCount = 12
            )
        )
    }

    @Test
    fun rebootOrLegacyDeadlineRestartsBoundedLockoutInsteadOfClearingIt() {
        val rebooted = AppLockTimingPolicy.normalizeLockout(
            untilElapsedMs = 152_000L,
            lockoutBootCount = 12,
            nowElapsedMs = 3_000L,
            currentBootCount = 13,
            failedAttempts = 3
        )
        assertEquals(
            AppLockTimingPolicy.LockoutWindow(63_000L, 13),
            rebooted
        )
        assertEquals(60_000L, AppLockTimingPolicy.remainingLockoutMs(rebooted, 3_000L))

        val oldWallClockDeadline = AppLockTimingPolicy.normalizeLockout(
            untilElapsedMs = 1_798_000_000_000L,
            lockoutBootCount = null,
            nowElapsedMs = 7_000L,
            currentBootCount = 13,
            failedAttempts = 4
        )
        assertEquals(
            AppLockTimingPolicy.LockoutWindow(67_000L, 13),
            oldWallClockDeadline
        )
    }

    @Test
    fun lockoutExpiresWithElapsedTimeButNotWithAReboot() {
        val active = AppLockTimingPolicy.normalizeLockout(
            untilElapsedMs = 12_000L,
            lockoutBootCount = 13,
            nowElapsedMs = 10_000L,
            currentBootCount = 13,
            failedAttempts = 3
        )
        assertEquals(2_000L, AppLockTimingPolicy.remainingLockoutMs(active, 10_000L))
        assertEquals(0L, AppLockTimingPolicy.remainingLockoutMs(active, 12_000L))

        val rebooted = AppLockTimingPolicy.normalizeLockout(
            untilElapsedMs = 12_000L,
            lockoutBootCount = 13,
            nowElapsedMs = 10_000L,
            currentBootCount = 14,
            failedAttempts = 3
        )
        assertEquals(60_000L, AppLockTimingPolicy.remainingLockoutMs(rebooted, 10_000L))
        assertEquals(0L, AppLockTimingPolicy.remainingLockoutMs(rebooted, 70_000L))
    }

    @Test
    fun noLockoutRecordOrSubthresholdFailuresDoNotCreateARebootPenalty() {
        assertEquals(
            null,
            AppLockTimingPolicy.normalizeLockout(
                untilElapsedMs = null,
                lockoutBootCount = null,
                nowElapsedMs = 5_000L,
                currentBootCount = 2,
                failedAttempts = 2
            )
        )
        assertEquals(
            null,
            AppLockTimingPolicy.normalizeLockout(
                untilElapsedMs = 15_000L,
                lockoutBootCount = 1,
                nowElapsedMs = 5_000L,
                currentBootCount = 2,
                failedAttempts = 2
            )
        )
    }
}
