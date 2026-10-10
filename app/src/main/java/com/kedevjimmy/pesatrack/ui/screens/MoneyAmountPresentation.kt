package com.kedevjimmy.pesatrack.ui.screens

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Summary money display only. The ledger stays in exact minor units.
 * Show two decimal places whenever cents are present; never round cents away.
 * Transaction-detail evidence may deliberately show two places for every amount.
 */
internal fun formatKesAmount(minor: Long): String =
    (if (minor < 0L) "-KES " else "KES ") + formatKesNumber(minor).removePrefix("-")

/** Numeric label for compact visualizations, including signed adjustments. */
internal fun formatKesNumber(minor: Long): String {
    val pattern = if (minor % 100L == 0L) "#,##0" else "#,##0.00"
    return DecimalFormat(pattern, DecimalFormatSymbols(Locale.ENGLISH))
        .format(BigDecimal.valueOf(minor, 2))
}
