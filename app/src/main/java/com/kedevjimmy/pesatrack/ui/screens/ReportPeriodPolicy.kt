package com.kedevjimmy.pesatrack.ui.screens

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class ReportPeriod(val label: String) {
    WEEK("Week"), MONTH("Month"), SIX_MONTHS("6 months"), YEAR("Year")
}

internal data class ReportPeriodRange(
    val startEpochMs: Long,
    val endExclusiveEpochMs: Long,
    val previousStartEpochMs: Long,
    val label: String
)

internal data class ReportTrendBucket(
    val startEpochMs: Long,
    val endExclusiveEpochMs: Long,
    val label: String
)

/** All windows use local calendar boundaries, including leap years and DST. */
internal fun reportPeriodRange(
    period: ReportPeriod,
    selectedEpochMs: Long,
    zone: TimeZone = TimeZone.getDefault()
): ReportPeriodRange {
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = selectedEpochMs
        when (period) {
            ReportPeriod.WEEK -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            ReportPeriod.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
            else -> set(Calendar.DAY_OF_MONTH, 1)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (period == ReportPeriod.SIX_MONTHS) add(Calendar.MONTH, -5)
    }
    val end = (start.clone() as Calendar).apply { addWindow(period, forward = true) }
    val previous = (start.clone() as Calendar).apply { addWindow(period, forward = false) }
    val format = when (period) {
        ReportPeriod.WEEK -> "d MMM yyyy"
        ReportPeriod.MONTH -> "MMMM yyyy"
        ReportPeriod.SIX_MONTHS -> "MMM yyyy"
        ReportPeriod.YEAR -> "yyyy"
    }
    val formatter = SimpleDateFormat(format, Locale.getDefault()).apply { timeZone = zone }
    val label = if (period == ReportPeriod.SIX_MONTHS) {
        formatter.format(Date(start.timeInMillis)) + " – " +
            formatter.format((start.clone() as Calendar).apply { add(Calendar.MONTH, 5) }.time)
    } else {
        formatter.format(Date(start.timeInMillis))
    }
    return ReportPeriodRange(start.timeInMillis, end.timeInMillis, previous.timeInMillis, label)
}

private fun Calendar.addWindow(period: ReportPeriod, forward: Boolean) {
    val direction = if (forward) 1 else -1
    when (period) {
        ReportPeriod.WEEK -> add(Calendar.DAY_OF_MONTH, 7 * direction)
        ReportPeriod.MONTH -> add(Calendar.MONTH, direction)
        ReportPeriod.SIX_MONTHS -> add(Calendar.MONTH, 6 * direction)
        ReportPeriod.YEAR -> add(Calendar.YEAR, direction)
    }
}

internal fun reportTrendBuckets(
    period: ReportPeriod,
    selectedEpochMs: Long,
    zone: TimeZone = TimeZone.getDefault()
): List<ReportTrendBucket> {
    val anchor = Calendar.getInstance(zone).apply {
        timeInMillis = selectedEpochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (period) {
            ReportPeriod.WEEK -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            ReportPeriod.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
            else -> {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -5)
            }
        }
    }
    val isDaily = period == ReportPeriod.WEEK
    val count = when (period) {
        ReportPeriod.WEEK -> 7
        ReportPeriod.YEAR -> 12
        else -> 6
    }
    val formatter = SimpleDateFormat(if (isDaily) "EEE" else "MMM", Locale.getDefault()).apply {
        timeZone = zone
    }
    return (0 until count).map { index ->
        val start = (anchor.clone() as Calendar).apply {
            if (isDaily) add(Calendar.DAY_OF_MONTH, index) else add(Calendar.MONTH, index)
        }
        val end = (start.clone() as Calendar).apply {
            if (isDaily) add(Calendar.DAY_OF_MONTH, 1) else add(Calendar.MONTH, 1)
        }
        ReportTrendBucket(start.timeInMillis, end.timeInMillis, formatter.format(start.time))
    }
}
