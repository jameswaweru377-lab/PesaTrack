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
                       read: Boolean = false): LocalInboxNotice {
        val timestamp = GregorianCalendar(zone).apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
        return LocalInboxNotice(key, category, "Real reminder $key", "Open to review",
            "bills", timestamp, read)
    }

    @Test fun unreadFilterKeepsOnlyUnreadNoticesAndAnEmptyState() {
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("read", read = true), notice("new")),
            unreadOnly = true, now = today, timezone = zone
        )
        assertEquals(listOf("new"), model.today.map { it.key })
        assertEquals(1, model.visibleCount)
        assertEquals(0, NotificationInboxDisplayPolicy.present(
            listOf(notice("read", read = true)), true, today, zone
        ).visibleCount)
    }

    @Test fun latestUnreadActivityIsFeaturedOnceAndCanBeReadNormally() {
        val activity = notice("review", category = "ACTIVITY")
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("bill"), activity), false, today, zone
        )
        assertEquals("review", model.featured?.key)
        assertEquals(listOf("bill"), model.today.map { it.key })

        val readModel = NotificationInboxDisplayPolicy.present(
            listOf(activity.copy(read = true)), false, today, zone
        )
        assertNull(readModel.featured)
        assertEquals(listOf("review"), readModel.today.map { it.key })
    }

    @Test fun noticesGroupByDeviceLocalDatesWithoutLosingOlderOrFutureDatedItems() {
        val model = NotificationInboxDisplayPolicy.present(
            listOf(notice("older", -7), notice("today"),
                notice("week", -2), notice("future", 1)),
            false, today, zone
        )
        assertEquals(listOf("today"), model.today.map { it.key })
        assertEquals(listOf("week"), model.earlierThisWeek.map { it.key })
        assertEquals(setOf("older", "future"), model.older.map { it.key }.toSet())
        assertEquals(4, model.visibleCount)
    }
}
