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

enum class NotificationInboxFilter { ALL_UPDATES, NEEDS_REVIEW, REMINDERS }

object NotificationInboxDisplayPolicy {
    fun needsReview(notice: LocalInboxNotice): Boolean =
        notice.route == "activity-attention" || notice.route == "accounts" ||
            notice.route == "data-health" || notice.route.startsWith("smart-review/") ||
            notice.category.equals("ACTIVITY", ignoreCase = true)

    fun present(notices: List<LocalInboxNotice>, filter: NotificationInboxFilter,
                now: Long, timezone: TimeZone,
                dismissedFeaturedKey: String? = null): NotificationInboxPresentation {
        val visible = notices.asSequence().filter {
            when (filter) {
                NotificationInboxFilter.ALL_UPDATES -> true
                NotificationInboxFilter.NEEDS_REVIEW -> needsReview(it)
                NotificationInboxFilter.REMINDERS -> !needsReview(it)
            }
        }
            .sortedByDescending { it.createdAt }.toList()
        // The featured treatment is optional UI. Dismissing it leaves the notice in the list.
        val candidate = if (filter == NotificationInboxFilter.ALL_UPDATES) {
            visible.firstOrNull {
                !it.read &&
                    (it.category.equals("ACTIVITY", ignoreCase = true) ||
                        it.route == "activity-attention")
            }
        } else null
        val featured = candidate?.takeUnless { it.key == dismissedFeaturedKey }
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
