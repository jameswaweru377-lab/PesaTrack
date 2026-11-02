package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class BackupReminderPolicyTest {
    private val day = TimeUnit.DAYS.toMillis(1)

    @Test fun waitsThirtyDaysAfterEnabling() {
        assertFalse(BackupReminderPolicy.isDue(now = 29 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 0, hour = 10))
        assertTrue(BackupReminderPolicy.isDue(now = 31 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 0, hour = 10))
    }

    @Test fun verifiedBackupRestartsThirtyDayWindow() {
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 20 * day, lastShownAt = 0, hour = 10))
        assertTrue(BackupReminderPolicy.isDue(now = 51 * day, enabledAt = 1, lastBackupAt = 20 * day, lastShownAt = 0, hour = 10))
    }

    @Test fun sentNotificationIsRateLimitedAndQuietOvernight() {
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 35 * day, hour = 10))
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 0, hour = 21))
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 0, hour = 7))
        assertTrue(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 0, lastShownAt = 0, hour = 9))
    }

    @Test fun unsetOrFutureTimestampsNeverTrigger() {
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 0, lastBackupAt = 0, lastShownAt = 0, hour = 10))
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 41 * day, lastBackupAt = 0, lastShownAt = 0, hour = 10))
        assertFalse(BackupReminderPolicy.isDue(now = 40 * day, enabledAt = 1, lastBackupAt = 41 * day, lastShownAt = 0, hour = 10))
    }
}
