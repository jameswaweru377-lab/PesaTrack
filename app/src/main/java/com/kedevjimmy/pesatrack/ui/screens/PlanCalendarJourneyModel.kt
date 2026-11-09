package com.kedevjimmy.pesatrack.ui.screens

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.math.BigInteger

internal data class PlanCalendarDay(
    val startEpochMs: Long,
    val weekday: String,
    val dayOfMonth: String,
    val month: String,
    val isToday: Boolean,
    val hasBill: Boolean
)

/** Calendar week (Monday–Sunday) in the device's local time zone. */
internal fun planCalendarJourneyWeek(
    todayEpochMs: Long,
    dueEpochMs: List<Long>,
    locale: Locale,
    timeZone: TimeZone
): List<PlanCalendarDay> {
    fun dayStart(epochMs: Long): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dueDays = dueEpochMs.map(::dayStart).toSet()
    val todayStart = dayStart(todayEpochMs)
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }
    fun dated(epochMs: Long, pattern: String) = SimpleDateFormat(pattern, locale).apply {
        this.timeZone = timeZone
    }.format(Date(epochMs))
    return (0 until 7).map {
        val start = calendar.timeInMillis
        val result = PlanCalendarDay(
            startEpochMs = start,
            weekday = dated(start, "EEE"),
            dayOfMonth = dated(start, "d"),
            month = dated(start, "MMM"),
            isToday = start == todayStart,
            hasBill = start in dueDays
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        result
    }
}

/** Visual shares of only these three values; never a substitute for ledger reconciliation. */
internal fun planJourneyDisplayedShares(
    committedMinor: Long, savedMinor: Long, availableMinor: Long
): List<Int>? {
    val values = listOf(committedMinor, savedMinor, availableMinor)
    if (values.any { it < 0L }) return null
    val total = values.fold(BigInteger.ZERO) { sum, value ->
        sum + BigInteger.valueOf(value)
    }
    if (total == BigInteger.ZERO) return null
    val first = values.take(2).map { amount ->
        BigInteger.valueOf(amount).multiply(BigInteger.valueOf(100L))
            .add(total.divide(BigInteger.valueOf(2L))).divide(total).toInt()
    }
    return first + (100 - first.sum())
}

/** Scheduled bills only; this is not the pay-cycle commitments total. */
internal fun planUpcomingBillsTotalMinor(remainingAmounts: List<Long>): Long? {
    if (remainingAmounts.any { it < 0L }) return null
    val total = remainingAmounts.fold(BigInteger.ZERO) { sum, value ->
        sum + BigInteger.valueOf(value)
    }
    return total.takeIf { it <= BigInteger.valueOf(Long.MAX_VALUE) }?.toLong()
}
