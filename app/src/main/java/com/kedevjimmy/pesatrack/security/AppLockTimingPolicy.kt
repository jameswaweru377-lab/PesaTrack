package com.kedevjimmy.pesatrack.security

import kotlin.math.max

/** Monotonic, boot-scoped durations for the optional app lock. */
internal object AppLockTimingPolicy {
    const val LOCK_GRACE_MS = 30_000L
    const val REBOOT_LOCKOUT_MS = 60_000L

    data class LockoutWindow(
        val untilElapsedMs: Long,
        val bootCount: Int
    )

    fun shouldRequireUnlock(
        backgroundedAtElapsedMs: Long?,
        backgroundBootCount: Int?,
        nowElapsedMs: Long,
        currentBootCount: Int,
        graceMs: Long = LOCK_GRACE_MS
    ): Boolean {
        if (backgroundedAtElapsedMs == null) return false
        return backgroundBootCount != currentBootCount ||
            nowElapsedMs < backgroundedAtElapsedMs ||
            nowElapsedMs - backgroundedAtElapsedMs >= graceMs.coerceIn(0L, 60_000L)
    }

    /** A deadline from an earlier boot (or old wall-clock format) cannot be trusted. */
    fun normalizeLockout(
        untilElapsedMs: Long?,
        lockoutBootCount: Int?,
        nowElapsedMs: Long,
        currentBootCount: Int,
        failedAttempts: Int
    ): LockoutWindow? {
        if (untilElapsedMs == null) return null
        if (lockoutBootCount != currentBootCount) {
            return if (failedAttempts >= 3) {
                LockoutWindow(nowElapsedMs + REBOOT_LOCKOUT_MS, currentBootCount)
            } else {
                null
            }
        }
        return LockoutWindow(untilElapsedMs, currentBootCount)
    }

    fun remainingLockoutMs(window: LockoutWindow?, nowElapsedMs: Long): Long =
        max(0L, (window?.untilElapsedMs ?: nowElapsedMs) - nowElapsedMs)
}
