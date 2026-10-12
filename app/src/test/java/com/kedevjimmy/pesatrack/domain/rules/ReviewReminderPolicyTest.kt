package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewReminderPolicyTest {
    private val now = 30L * ReviewReminderPolicy.DAY_MS
    private fun eligible(
        enabled: Boolean = true,
        setupComplete: Boolean = true,
        permissionsAllow: Boolean = true,
        hourOfDay: Int = 11,
        lastShownEpochMs: Long = 0L,
        latestEvidenceReceivedEpochMs: Long = now - 3L * 60L * 60L * 1000L,
        pendingIds: Set<Long> = setOf(3L, 7L),
        lastShownFingerprint: String = ""
    ) = ReviewReminderPolicy.shouldNotify(
        enabled, setupComplete, permissionsAllow, hourOfDay, now,
        lastShownEpochMs, latestEvidenceReceivedEpochMs, pendingIds, lastShownFingerprint
    )

    @Test fun optInPermissionAndSetupAreAllMandatory() {
        assertTrue(eligible())
        assertFalse(eligible(enabled = false))
        assertFalse(eligible(setupComplete = false))
        assertFalse(eligible(permissionsAllow = false))
    }

    @Test fun resolvedOrSingleTransactionNeverSendsANaggingDigest() {
        assertFalse(eligible(pendingIds = emptySet()))
        assertFalse(eligible(pendingIds = setOf(3L)))
    }

    @Test fun respectsQuietHoursAndAllowsImportToSettle() {
        assertFalse(eligible(hourOfDay = 8))
        assertFalse(eligible(hourOfDay = 21))
        assertFalse(eligible(latestEvidenceReceivedEpochMs = now - 30L * 60_000L))
        assertTrue(eligible(hourOfDay = 10, latestEvidenceReceivedEpochMs = now - 3L * 60L * 60_000L))
    }

    @Test fun doesNotRepeatSameUnresolvedWorkEveryDay() {
        val old = now - 2L * ReviewReminderPolicy.DAY_MS
        val fingerprint = ReviewReminderPolicy.fingerprint(setOf(7L, 3L))
        assertEquals("3,7", fingerprint)
        assertFalse(eligible(lastShownEpochMs = old, lastShownFingerprint = fingerprint))
        assertTrue(eligible(lastShownEpochMs = now - 8L * ReviewReminderPolicy.DAY_MS,
            lastShownFingerprint = fingerprint))
    }

    @Test fun newReviewWorkCannotBypassGlobalDailyBudget() {
        val last = now - 30L * 60L * 60_000L
        assertTrue(eligible(lastShownEpochMs = last, lastShownFingerprint = "3,7",
            pendingIds = setOf(3L, 8L)))
        assertFalse(eligible(lastShownEpochMs = now - 4L * 60L * 60_000L,
            lastShownFingerprint = "3,7", pendingIds = setOf(3L, 8L)))
    }
}
