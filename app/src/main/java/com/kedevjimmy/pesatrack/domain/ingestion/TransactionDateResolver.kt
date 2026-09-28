package com.kedevjimmy.pesatrack.domain.ingestion

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TransactionDateResolver {
    private val patterns = listOf(
        "d/M/yy 'at' h:mm a",
        "d/M/yyyy 'at' h:mm a",
        "d-M-yy 'at' h:mm a",
        "d-M-yyyy 'at' h:mm a",
        "d/M/yy 'at' H:mm",
        "d/M/yyyy 'at' H:mm",
        "d/M/yy h:mm a",
        "d/M/yyyy h:mm a",
        "d-M-yy h:mm a",
        "d-M-yyyy h:mm a",
        "d/M/yy H:mm",
        "d/M/yyyy H:mm"
    )

    fun resolve(
        occurredAtText: String?,
        fallbackEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val value = occurredAtText
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return fallbackEpochMs

        for (pattern in patterns) {
            val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                isLenient = false
                this.timeZone = timeZone
            }
            try {
                val parsed = parser.parse(value)
                if (parsed != null) return parsed.time
            } catch (_: ParseException) {
                // Try the next known M-Pesa date pattern.
            }
        }

        return fallbackEpochMs
    }
}
