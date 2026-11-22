package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationUnreadBadgeTest {
    @Test fun hidesBadgeWhenNothingIsUnread() {
        assertNull(unreadNotificationBadge(0))
        assertNull(unreadNotificationBadge(-1))
    }

    @Test fun showsTheActualCountForOneThroughNine() {
        assertEquals("1", unreadNotificationBadge(1))
        assertEquals("2", unreadNotificationBadge(2))
        assertEquals("9", unreadNotificationBadge(9))
    }

    @Test fun abbreviatesDoubleDigitNotificationCounts() {
        assertEquals("9+", unreadNotificationBadge(10))
        assertEquals("9+", unreadNotificationBadge(25))
    }
}
