package com.kedevjimmy.pesatrack.ui.screens

import kotlin.math.roundToInt

/**
 * Bar heights represent only positive spending. A zero has zero height, rather
 * than a decorative minimum bar that falsely implies spending. Signed negative
 * adjustments retain their visible value label but do not become positive bars.
 */
internal fun spendingBarHeightsDp(weeks: List<Long>, maxHeightDp: Int = 88): List<Int> {
    require(maxHeightDp > 0)
    val maximumPositive = weeks.maxOrNull()?.coerceAtLeast(0L) ?: 0L
    return weeks.map { amount ->
        if (amount <= 0L || maximumPositive == 0L) 0
        else ((amount.toDouble() / maximumPositive.toDouble()) * maxHeightDp)
            .roundToInt()
            .coerceIn(1, maxHeightDp)
    }
}
