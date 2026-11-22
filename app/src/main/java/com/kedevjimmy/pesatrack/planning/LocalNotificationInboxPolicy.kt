package com.kedevjimmy.pesatrack.planning

/** Small, local-only record of alerts PesaTrack actually sent to Android. */
data class LocalInboxNotice(
    val key: String,
    val category: String,
    val title: String,
    val summary: String,
    val route: String,
    val createdAt: Long,
    val read: Boolean
)

object LocalNotificationInboxPolicy {
    private const val MAX_NOTICES = 40
    private val smartReviewRoute = Regex(
        "smart-review/[A-Za-z0-9%._~-]{1,256}\\?action=review&revision=[A-Za-z0-9%._~-]{1,256}"
    )

    fun supportsRoute(route: String): Boolean = route in setOf(
        "accounts", "activity-attention", "salary", "bills", "budgets",
        "saving-goals", "debts", "backup-reminder", "data-health"
    ) || smartReviewRoute.matches(route)

    fun record(current: List<LocalInboxNotice>, alert: LocalInboxNotice): List<LocalInboxNotice> =
        (listOf(alert) + current.filterNot { it.key == alert.key })
            .sortedByDescending { it.createdAt }.take(MAX_NOTICES)

    fun markRead(current: List<LocalInboxNotice>, key: String): List<LocalInboxNotice> =
        current.map { if (it.key == key) it.copy(read = true) else it }

    fun markAllRead(current: List<LocalInboxNotice>): List<LocalInboxNotice> =
        current.map { it.copy(read = true) }

    fun unreadCount(current: List<LocalInboxNotice>): Int = current.count { !it.read }
}
