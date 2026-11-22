package com.kedevjimmy.pesatrack.planning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNotificationInboxPolicyTest {
    private fun note(key: String, at: Long = 1) = LocalInboxNotice(
        key, "Reminder", "Review your account", "Tap to review", "accounts", at, false
    )

    @Test fun repeatedDeliveryReplacesExistingAlertInsteadOfDuplicating() {
        val result = LocalNotificationInboxPolicy.record(listOf(note("account", 1)), note("account", 2))
        assertEquals(1, result.size)
        assertEquals(2, result.single().createdAt)
        assertFalse(result.single().read)
    }

    @Test fun inboxIsNewestFirstAndBoundedToForty() {
        val notices = (1..45).fold(emptyList<LocalInboxNotice>()) { existing, i ->
            LocalNotificationInboxPolicy.record(existing, note("note-$i", i.toLong()))
        }
        assertEquals(40, notices.size)
        assertEquals("note-45", notices.first().key)
        assertEquals("note-6", notices.last().key)
    }

    @Test fun readingOneAlertLeavesOtherUnread() {
        val notices = listOf(note("one"), note("two"))
        val updated = LocalNotificationInboxPolicy.markRead(notices, "one")
        assertTrue(updated.first().read)
        assertFalse(updated.last().read)
        assertEquals(1, LocalNotificationInboxPolicy.unreadCount(updated))
    }

    @Test fun smartReviewLinkPreservesTheExactReminderIdentity() {
        assertTrue(LocalNotificationInboxPolicy.supportsRoute(
            "smart-review/payment%3Aabc?action=review&revision=hash123"))
        assertFalse(LocalNotificationInboxPolicy.supportsRoute("https://example.com"))
        assertFalse(LocalNotificationInboxPolicy.supportsRoute("smart-review/payment%3Aabc?action=gift"))
    }
}
