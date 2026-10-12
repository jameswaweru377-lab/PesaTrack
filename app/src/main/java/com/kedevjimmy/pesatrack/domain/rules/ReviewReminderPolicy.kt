package com.kedevjimmy.pesatrack.domain.rules

/**
 * Shared anti-nagging gate for a routine, opt-in Activity review notification.
 * Candidate IDs refer to already-identified local ledger records, never raw SMS.
 * No fixed hourly push: the daily scheduler must re-check the unresolved records.
 */
object ReviewReminderPolicy {
    const val MIN_ACTIONABLE = 2
    const val DAY_MS = 24L * 60L * 60L * 1_000L
    const val SAME_SET_COOLDOWN_MS = 7L * DAY_MS
    const val IMPORT_SETTLE_MS = 2L * 60L * 60L * 1_000L

    fun shouldNotify(
        enabled: Boolean,
        setupComplete: Boolean,
        permissionsAllow: Boolean,
        hourOfDay: Int,
        nowEpochMs: Long,
        lastShownEpochMs: Long,
        latestEvidenceReceivedEpochMs: Long,
        pendingIds: Set<Long>,
        lastShownFingerprint: String
    ): Boolean {
        if (!enabled || !setupComplete || !permissionsAllow) return false
        if (hourOfDay !in 9..19 || pendingIds.size < MIN_ACTIONABLE) return false
        if (nowEpochMs <= 0L) return false
        // A fresh import may still be deduplicating and applying user corrections.
        if (latestEvidenceReceivedEpochMs > 0L &&
            nowEpochMs - latestEvidenceReceivedEpochMs < IMPORT_SETTLE_MS) return false
        if (lastShownEpochMs > 0L) {
            if (nowEpochMs - lastShownEpochMs < DAY_MS) return false
            if (lastShownFingerprint == fingerprint(pendingIds) &&
                nowEpochMs - lastShownEpochMs < SAME_SET_COOLDOWN_MS) return false
        }
        return true
    }

    fun fingerprint(pendingIds: Set<Long>): String =
        pendingIds.sorted().joinToString(",")
}
