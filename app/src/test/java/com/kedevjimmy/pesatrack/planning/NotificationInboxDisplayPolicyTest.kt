package com.kedevjimmy.pesatrack.planning

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationInboxDisplayPolicyTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")
    private val today = GregorianCalendar(zone).apply {
        set(2026, Calendar.NOVEMBER, 22, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun notice(key: String, dayOffset: Int = 0, category: String = "BILLS",
                       read: Boolean = false, route: String = "bills"): LocalInboxNotice {
        val timestamp = GregorianCalendar(zone).apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
        return LocalInboxNotice(key, category, "Real reminder $key", "Open to review",
            route, timestamp, read)
    }

    @Test fun reviewAndReminderTabsUseActualNoticeDestinations() {
        val notices = listOf(
            notice("activity", category = "ACTIVITY", route = "activity-attention"),
            notice("bank", category = "ACCOUNTS", route = "accounts"),
            notice("smart", category = "DEBT", route = "smart-review/example?action=review&revision=1"),
            notice("bill"), notice("backup", category = "BACKUP", route = "backup-reminder"),
            notice("unknown", category = "TEST", route = "bills")
        )
        val reviews = NotificationInboxDisplayPolicy.present(
            notices, NotificationInboxFilter.NEEDS_REVIEW, today, zone
        )
        val reminders = NotificationInboxDisplayPolicy.present(
            notices, NotificationInboxFilter.REMINDERS, today, zone
        )
        assertNull(reviews.featured)
        assertEquals(setOf("activity", "bank", "smart"), reviews.today.map { it.key }.toSet())
        assertEquals(setOf("bill", "backup", "unknown"), reminders.today.map { it.key }.toSet())
        assertEquals(6, NotificationInboxDisplayPolicy.present(
            notices, NotificationInboxFilter.ALL_UPDATES, today, zone
        ).visibleCount)
    }

    @Test fun dismissedFeaturedCardReturnsToTheListWithoutChangingUnreadState() {
        val activity = notice("review", category = "ACTIVITY", route = "activity-attention")
        val shown = NotificationInboxDisplayPolicy.present(
            listOf(activity), NotificationInboxFilter.ALL_UPDATES, today, zone
        )
        val dismissed = NotificationInboxDisplayPolicy.present(
            listOf(activity), NotificationInboxFilter.ALL_UPDATES, today, zone,
            dismissedFeaturedKey = "review"
        )
        assertEquals("review", shown.featured?.key)
        assertNull(dismissed.featured)
        assertEquals(listOf("review"), dismissed.today.map { it.key })
        assertEquals(1, dismissed.visibleCount)
        assertEquals(1, LocalNotificationInboxPolicy.unreadCount(listOf(activity)))
    }

    @Test fun dismissingTheFeaturedCardDoesNotImmediatelyFeatureAnotherReview() {
        val newest = notice("newest", category = "ACTIVITY", route = "activity-attention")
        val older = notice("older", dayOffset = -1, category = "ACTIVITY",
            route = "activity-attention")
        val model = NotificationInboxDisplayPolicy.present(
            listOf(newest, older), NotificationInboxFilter.ALL_UPDATES, today, zone,
            dismissedFeaturedKey = "newest"
        )
        assertNull(model.featured)
        assertEquals(listOf("newest"), model.today.map { it.key })
        assertEquals(listOf("older"), model.earlierThisWeek.map { it.key })
    }

    @Test fun emptyCategoryShowsNoNoticesWithoutDeletingOtherCategories() {
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("read", read = true), notice("new")),
            filter = NotificationInboxFilter.NEEDS_REVIEW, now = today, timezone = zone
        )
        assertEquals(emptyList<String>(), model.today.map { it.key })
        assertEquals(0, model.visibleCount)
        assertEquals(0, NotificationInboxDisplayPolicy.present(
            listOf(notice("read", read = true)), NotificationInboxFilter.NEEDS_REVIEW, today, zone
        ).visibleCount)
    }

    @Test fun latestUnreadActivityIsFeaturedOnceAndCanBeReadNormally() {
        val activity = notice("review", category = "ACTIVITY")
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("bill"), activity), NotificationInboxFilter.ALL_UPDATES, today, zone
        )
        assertEquals("review", model.featured?.key)
        assertEquals(listOf("bill"), model.today.map { it.key })

        val readModel = NotificationInboxDisplayPolicy.present(
            listOf(activity.copy(read = true)), NotificationInboxFilter.ALL_UPDATES, today, zone
        )
        assertNull(readModel.featured)
        assertEquals(listOf("review"), readModel.today.map { it.key })
    }

    @Test fun noticesGroupByDeviceLocalDatesWithoutLosingOlderOrFutureDatedItems() {
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("older", -7), notice("today"),
                notice("week", -2), notice("future", 1)),
            NotificationInboxFilter.ALL_UPDATES, today, zone
        )
        assertEquals(listOf("today"), model.today.map { it.key })
        assertEquals(listOf("week"), model.earlierThisWeek.map { it.key })
        assertEquals(setOf("older", "future"), model.older.map { it.key }.toSet())
        assertEquals(4, model.visibleCount)
    }

    @Test fun aNoticeMovesOutOfTodayWhenThePhonePassesMidnight() {
        val notification = notice("sunday")
        val before = NotificationInboxDisplayPolicy.present(
            listOf(notification), NotificationInboxFilter.ALL_UPDATES, today, zone)
        val monday = GregorianCalendar(zone).apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val after = NotificationInboxDisplayPolicy.present(
            listOf(notification), NotificationInboxFilter.ALL_UPDATES, monday, zone)
        assertEquals(listOf("sunday"), before.today.map { it.key })
        assertEquals(emptyList<String>(), after.today.map { it.key })
        assertEquals(listOf("sunday"), after.older.map { it.key })
    }
}
