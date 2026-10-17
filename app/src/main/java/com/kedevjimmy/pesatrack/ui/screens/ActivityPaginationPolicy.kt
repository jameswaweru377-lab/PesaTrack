package com.kedevjimmy.pesatrack.ui.screens

/**
 * Activity scroll batches. The underlying monthly ledger remains the single
 * source for financial totals, review states and duplicate suppression.
 * These functions control display materialization only; they do not count as
 * database paging or change which records belong to an account/period.
 */
internal const val ACTIVITY_FIRST_BATCH = 24
internal const val ACTIVITY_NEXT_BATCH = 24

internal fun activityVisibleCount(
    current: Int,
    total: Int,
    batchSize: Int = ACTIVITY_NEXT_BATCH
): Int {
    require(current >= 0 && total >= 0 && batchSize > 0)
    return (current.toLong() + batchSize).coerceAtMost(total.toLong()).toInt()
}

internal fun <T> activityVisibleRows(rows: List<T>, visibleCount: Int): List<T> =
    rows.take(visibleCount.coerceAtLeast(0))
