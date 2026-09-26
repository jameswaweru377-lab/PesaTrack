package com.kedevjimmy.pesatrack.domain.rules

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class HistoryMonth(
    val year: Int,
    val month: Int,
    val label: String,
    val startEpochMs: Long,
    val endEpochMs: Long
)

object HistoryImportWindow {
    fun months(
        count: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): List<HistoryMonth> {
        require(count in 0..3) { "History import is limited to 0-3 months." }
        if (count == 0) return emptyList()

        val current = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return (0 until count).map { offset ->
            val start = (current.clone() as Calendar).apply {
                add(Calendar.MONTH, -offset)
            }
            val end = (start.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }

            HistoryMonth(
                year = start.get(Calendar.YEAR),
                month = start.get(Calendar.MONTH),
                label = SimpleDateFormat("MMMM yyyy", locale).format(start.time),
                startEpochMs = start.timeInMillis,
                endEpochMs = end.timeInMillis
            )
        }
    }

    fun importStartEpochMs(
        count: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? =
        months(count, nowEpochMs, timeZone, Locale.ENGLISH)
            .lastOrNull()
            ?.startEpochMs

    fun epochForDay(
        month: HistoryMonth,
        dayOfMonth: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        val calendar = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, month.year)
            set(Calendar.MONTH, month.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (dayOfMonth !in 1..maxDay) return null

        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
