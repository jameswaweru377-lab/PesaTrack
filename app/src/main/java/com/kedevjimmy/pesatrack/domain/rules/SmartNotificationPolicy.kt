package com.kedevjimmy.pesatrack.domain.rules

enum class SmartNoticeKind { ACCOUNT, ACTIVITY, PAYMENT, DISCOVERY }
data class SmartNoticeCandidate(val key: String, val revision: String, val kind: SmartNoticeKind,
    val evidenceAt: Long, val enabled: Boolean = true, val nextEligibleAt: Long = 0)
data class SmartNoticeState(val revision: String = "", val shownAt: Long = 0, val dismissed: Boolean = false)

object SmartNotificationPolicy {
    fun intentAction(key: String, revision: String, action: String) = "smart:$action:$key:$revision"
    fun dismissalApplies(candidate: SmartNoticeCandidate, state: SmartNoticeState) =
        state.dismissed && (state.revision == candidate.revision ||
            candidate.kind in setOf(SmartNoticeKind.PAYMENT,SmartNoticeKind.DISCOVERY))
    fun acceptDismissal(state: SmartNoticeState, revision: String) = state.shownAt <= 0 || state.revision == revision
    fun routineReview(accounts: List<SmartNoticeCandidate>, activity: SmartNoticeCandidate?): SmartNoticeCandidate? {
        val activeAccounts = accounts.filter { it.enabled }.sortedBy { it.key }
        val activeActivity = activity?.takeIf { it.enabled }
        if (activeAccounts.isEmpty()) return activeActivity
        val all = activeAccounts + listOfNotNull(activeActivity)
        return SmartNoticeCandidate("activity",all.joinToString("|") { "${it.key}:${it.revision}" },
            SmartNoticeKind.ACCOUNT,all.maxOf { it.evidenceAt })
    }
    fun choose(candidates: List<SmartNoticeCandidate>, states: Map<String,SmartNoticeState>,
        now: Long, hour: Int, lastBanner: Long, snoozeUntil: Long, ready: Boolean,
        permission: Boolean, foreground: Boolean, processing: Boolean): SmartNoticeCandidate? {
        if (!ready || !permission || foreground || processing || now <= 0 ||
            hour !in 9..19 || snoozeUntil > now) return null
        if (lastBanner > 0 && now - lastBanner < ReviewReminderPolicy.DAY_MS) return null
        return candidates.filter { candidate ->
            val state = states[candidate.key] ?: SmartNoticeState()
            candidate.enabled && candidate.nextEligibleAt <= now && candidate.evidenceAt > 0 &&
                now - candidate.evidenceAt >= ReviewReminderPolicy.IMPORT_SETTLE_MS &&
                !dismissalApplies(candidate,state) &&
                !(state.shownAt > 0 && (candidate.kind in setOf(SmartNoticeKind.PAYMENT,SmartNoticeKind.DISCOVERY) ||
                    now - state.shownAt < ReviewReminderPolicy.DAY_MS ||
                    (state.revision == candidate.revision &&
                        now - state.shownAt < ReviewReminderPolicy.SAME_SET_COOLDOWN_MS)))
        }.sortedWith(compareBy<SmartNoticeCandidate> { it.kind.ordinal }.thenBy { it.key }).firstOrNull()
    }
}
