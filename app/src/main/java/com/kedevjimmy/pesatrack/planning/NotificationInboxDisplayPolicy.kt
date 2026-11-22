package com.kedevjimmy.pesatrack.planning

import java.util.Calendar
import java.util.TimeZone

/** A presentation-only grouping; the saved notice list remains the source of truth. */
data class NotificationInboxPresentation(
    val featured: LocalInboxNotice?,
    val today: List<LocalInboxNotice>,
    val earlierThisWeek: List<LocalInboxNotice>,
    val older: List<LocalInboxNotice>,
    val visibleCount: Int
)

object NotificationInboxDisplayPolicy {
    fun present(notices: List<LocalInboxNotice>, unreadOnly: Boolean,
                now: Long, timezone: TimeZone): NotificationInboxPresentation {
        val visible = notices.asSequence().filter { !unreadOnly || !it.read }
            .sortedByDescending { it.createdAt }.toList()
        // Only a delivered and still-unread Activity review can headline the inbox.
        val featured = visible.firstOrNull {
            !it.read && (it.category.equals("ACTIVITY", ignoreCase = true) ||
                it.route == "activity-attention")
        }
        val rest = visible.filterNot { it.key == featured?.key }
        val calendar = Calendar.getInstance(timezone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        val weekStart = calendar.timeInMillis
        fun isToday(notice: LocalInboxNotice) = notice.createdAt in todayStart until tomorrowStart
        return NotificationInboxPresentation(
            featured = featured,
            today = rest.filter(::isToday),
            earlierThisWeek = rest.filter { it.createdAt in weekStart until todayStart },
            older = rest.filter { it.createdAt < weekStart || it.createdAt >= tomorrowStart },
            visibleCount = visible.size
        )
    }
}
