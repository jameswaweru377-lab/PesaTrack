package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.Locale

/** Display-only copy: never changes the user's rule, category or ledger. */
object BillNotificationCopy {
    fun badge(category: TransactionCategory): String = when (category) {
        TransactionCategory.OTHER, TransactionCategory.UNCATEGORIZED -> "BILL"
        else -> category.name.replace('_', ' ')
    }

    fun description(category: TransactionCategory): String = when (category) {
        TransactionCategory.RENT -> "Your scheduled rent is coming up."
        else -> "Your scheduled bill is coming up."
    }

    fun merchant(displayLabel: String?, name: String): String {
        val raw = (displayLabel?.takeIf { it.isNotBlank() } ?: name)
            .replace(Regex("(^|\\s+)for account\\b.*$", RegexOption.IGNORE_CASE), "")
            .trim().trimEnd('.', '…', '-', ' ')
        if (raw.isEmpty()) return "Scheduled bill"
        if (raw.any { it.isLetter() } && raw.filter { it.isLetter() }.all { it.isUpperCase() }) {
            return raw.lowercase(Locale.ROOT).split(Regex("\\s+"))
                .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase(Locale.ROOT) } }
        }
        return raw
    }
}
