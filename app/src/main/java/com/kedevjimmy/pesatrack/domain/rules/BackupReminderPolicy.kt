package com.kedevjimmy.pesatrack.domain.rules

/** A friendly opt-in nudge, at most once per month and never after a recent verified backup. */
object BackupReminderPolicy {
    const val INTERVAL_MS = 30L * 24 * 60 * 60 * 1000

    fun isDue(
        now: Long,
        enabledAt: Long,
        lastBackupAt: Long,
        lastShownAt: Long,
        hour: Int
    ): Boolean {
        if (enabledAt <= 0 || now < enabledAt || hour !in 9..19) return false
        if (lastBackupAt > now || lastShownAt > now) return false
        val reference = maxOf(enabledAt, lastBackupAt, lastShownAt)
        return now - reference >= INTERVAL_MS
    }
}
