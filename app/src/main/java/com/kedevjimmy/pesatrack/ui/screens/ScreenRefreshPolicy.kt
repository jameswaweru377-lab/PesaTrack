package com.kedevjimmy.pesatrack.ui.screens

/**
 * One ledger revision is one stable presentation of a tab.
 * Navigating away and back must not repeat expensive account/plan queries when
 * no underlying financial data has changed. A retry or new ledger revision
 * always triggers a fresh read; stale data is not treated as authoritative.
 */
internal object ScreenRefreshPolicy {
    fun needsRead(
        cachedRevision: Long?,
        currentRevision: Long,
        retryOrRefresh: Int = 0
    ): Boolean = retryOrRefresh > 0 || cachedRevision != currentRevision
}
