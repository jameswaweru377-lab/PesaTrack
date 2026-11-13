package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.Locale
import java.util.Calendar

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

    fun headline(category: TransactionCategory): String = when (category) {
        TransactionCategory.RENT -> "Rent due soon"
        TransactionCategory.OTHER, TransactionCategory.UNCATEGORIZED -> "Bill due soon"
        else -> "${badge(category).lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }} due soon"
    }

    /** Calendar days, not 24-hour blocks, so tomorrow is not labelled today. */
    fun dueLabel(nowEpochMs: Long, dueEpochMs: Long): String {
        val today = Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val due = Calendar.getInstance().apply {
            timeInMillis = dueEpochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var days = 0
        while (today.before(due) && days < 31) {
            today.add(Calendar.DAY_OF_MONTH, 1)
            days++
        }
        return when (days) {
            0 -> "Due today"
            1 -> "Due tomorrow"
            else -> "Due in $days days"
        }
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
