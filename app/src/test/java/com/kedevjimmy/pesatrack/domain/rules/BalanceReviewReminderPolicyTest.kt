package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceReviewReminderPolicyTest {
    private val day = BalanceReviewReminderPolicy.DAY_MS
    private val now = 20 * day

    @Test fun firstMissingBalanceSetCanNotify() {
        assertTrue(BalanceReviewReminderPolicy.shouldPost("missing:a,b", "", 0L, now))
    }

    @Test fun repeatedOpeningCannotSpamOrRepeatBeforeSevenDays() {
        assertFalse(BalanceReviewReminderPolicy.shouldPost("same", "same", now - day / 2, now))
        assertFalse(BalanceReviewReminderPolicy.shouldPost("same", "same", now - 6 * day, now))
        assertTrue(BalanceReviewReminderPolicy.shouldPost("same", "same", now - 7 * day, now))
    }

    @Test fun changedMissingSetStillRespectsOneDayCooldown() {
        assertFalse(BalanceReviewReminderPolicy.shouldPost("new", "old", now - day / 2, now))
        assertTrue(BalanceReviewReminderPolicy.shouldPost("new", "old", now - day, now))
    }

    @Test fun resolvedMissingSetAndClockRollbackDoNotNotify() {
        assertFalse(BalanceReviewReminderPolicy.shouldPost("", "old", 0L, now))
        assertFalse(BalanceReviewReminderPolicy.shouldPost("old", "old", now + 1, now))
        assertFalse(BalanceReviewReminderPolicy.shouldPost("old", "old", 0L, 0L))
    }

    @Test fun balanceRemindersShareTheDailyBudgetWithOtherSmartAlerts() {
        assertFalse(BalanceReviewReminderPolicy.shouldPost("new", "", 0L, now, now - day / 2))
        assertTrue(BalanceReviewReminderPolicy.shouldPost("new", "", 0L, now, now - day))
        assertFalse(BalanceReviewReminderPolicy.shouldPost("new", "", 0L, now, now + 1L))
    }
}
