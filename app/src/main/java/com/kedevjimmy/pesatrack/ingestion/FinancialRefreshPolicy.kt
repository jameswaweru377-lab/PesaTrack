package com.kedevjimmy.pesatrack.ingestion

/** Recheck financial screens after an APK update or a meaningful foreground gap. */
internal object FinancialRefreshPolicy {
    private const val MIN_GAP_MS = 5L * 60L * 1000L

    fun shouldRefresh(
        nowMs: Long,
        lastRefreshMs: Long,
        currentVersionCode: Long,
        previousVersionCode: Long
    ): Boolean = currentVersionCode != previousVersionCode ||
        lastRefreshMs <= 0L ||
        nowMs < lastRefreshMs ||
        nowMs - lastRefreshMs >= MIN_GAP_MS
}
