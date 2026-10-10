package com.kedevjimmy.pesatrack.ui.screens

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Budget display only: preserve exact cents from the integer-minor-unit ledger.
 * This does not alter budget limits or spending calculations.
 */
internal fun formatBudgetMoney(minor: Long): String {
    val decimals = if (minor % 100L == 0L) "#,##0" else "#,##0.00"
    val numeric = DecimalFormat(decimals, DecimalFormatSymbols(Locale.ENGLISH))
        .format(BigDecimal.valueOf(minor, 2))
    return if (numeric.startsWith("-")) "-KES " + numeric.drop(1) else "KES " + numeric
}
