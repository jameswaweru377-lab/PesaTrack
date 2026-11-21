package com.kedevjimmy.pesatrack.domain.rules

/** Avoid notifying on every Home visit while keeping newly missing balances actionable. */
object BalanceReviewReminderPolicy {
    const val DAY_MS = 24L * 60 * 60 * 1000
    private const val REPEAT_MS = 7 * DAY_MS

    fun shouldPost(signature: String, previousSignature: String, lastShownAt: Long, now: Long): Boolean {
        if (signature.isBlank() || now <= 0 || lastShownAt > now) return false
        if (lastShownAt == 0L) return true
        val elapsed = now - lastShownAt
        return elapsed >= DAY_MS && (signature != previousSignature || elapsed >= REPEAT_MS)
    }
}
