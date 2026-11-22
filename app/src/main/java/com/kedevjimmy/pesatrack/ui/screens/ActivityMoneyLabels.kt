package com.kedevjimmy.pesatrack.ui.screens

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Exact KES formatting for live Activity summaries, including fractional shillings. */
internal fun activityKesLabel(minor: Long, showCents: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (showCents || minor % 100L != 0L) 2 else 0
        maximumFractionDigits = 2
    }
    return "KES " + formatter.format(BigDecimal.valueOf(minor, 2))
}
