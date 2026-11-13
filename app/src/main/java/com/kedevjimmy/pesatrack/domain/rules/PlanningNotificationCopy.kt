package com.kedevjimmy.pesatrack.domain.rules

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Display-only copy sourced from existing trusted Plan progress. */
object PlanningNotificationCopy {
    fun budgetHeadline(name: String, level: String): String {
        val label = name.trim().ifBlank { "Your" }
        val withBudget = if (label.endsWith("budget", ignoreCase = true)) label else "$label budget"
        return if (level == "over") "$withBudget limit reached" else "$withBudget almost full"
    }

    fun budgetUsage(spentMinor: Long, limitMinor: Long): String =
        "KES ${amount(spentMinor)} of KES ${amount(limitMinor)} used"

    private fun amount(minor: Long): String =
        NumberFormat.getNumberInstance(Locale.UK).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(minor, 2))
}
