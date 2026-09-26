package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PlanningPeriodResolver {
    fun resolve(
        observedAtEpochMs: Long,
        monthOffset: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = observedAtEpochMs
            add(Calendar.MONTH, monthOffset)
        }
        return formatYearMonth(
            year = calendar.get(Calendar.YEAR),
            monthZeroBased = calendar.get(Calendar.MONTH)
        )
    }

    fun current(
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val calendar = Calendar.getInstance(timeZone)
        return formatYearMonth(
            year = calendar.get(Calendar.YEAR),
            monthZeroBased = calendar.get(Calendar.MONTH)
        )
    }

    private fun formatYearMonth(
        year: Int,
        monthZeroBased: Int
    ): String = String.format(
        Locale.US,
        "%04d-%02d",
        year,
        monthZeroBased + 1
    )
}
