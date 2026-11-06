package com.kedevjimmy.pesatrack.ui.screens

import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Read-only display arithmetic. No money is written or reclassified by these helpers. */
internal fun selectedBudgetSpentPercent(limitMinor: Long, spentMinor: Long): Int? {
    if (limitMinor <= 0L || spentMinor < 0L) return null
    return BigInteger.valueOf(spentMinor).multiply(BigInteger.valueOf(100L))
        .add(BigInteger.valueOf(limitMinor / 2L))
        .divide(BigInteger.valueOf(limitMinor))
        .coerceAtMost(BigInteger.valueOf(100L)).toInt()
}

/** The next 25% milestone is a visual goal marker, not an invented deposit. */
internal fun selectedGoalNextMilestone(targetMinor: Long, savedMinor: Long): Long? {
    if (targetMinor <= 0L || savedMinor < 0L || savedMinor >= targetMinor) return null
    val target = BigInteger.valueOf(targetMinor)
    val quarter = BigInteger.valueOf(4L)
    for (step in 1L..4L) {
        val milestone = target.multiply(BigInteger.valueOf(step))
            .add(BigInteger.valueOf(3L)).divide(quarter).longValueExact()
        if (milestone > savedMinor) return milestone
    }
    return null
}

/** Date difference for bill labels, independent of time-of-day and DST shifts. */
internal fun selectedBillDaysUntil(dueEpochMs: Long, todayEpochMs: Long,
    timeZone: TimeZone): Long {
    fun civilDay(epochMs: Long): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis / 86_400_000L
    }
    return civilDay(dueEpochMs) - civilDay(todayEpochMs)
}

/** A real two-week calendar, anchored to Monday in the selected local month. */
internal fun selectedBillsFortnight(
    viewedMonthEpochMs: Long,
    todayEpochMs: Long,
    dueEpochMs: List<Long>,
    locale: Locale,
    timeZone: TimeZone
): List<PlanCalendarDay> {
    fun dayStart(epoch: Long): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = epoch
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val viewed = Calendar.getInstance(timeZone).apply { timeInMillis = viewedMonthEpochMs }
    val now = Calendar.getInstance(timeZone).apply { timeInMillis = todayEpochMs }
    val sameMonth = viewed.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        viewed.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    val start = Calendar.getInstance(timeZone).apply {
        timeInMillis = if (sameMonth) todayEpochMs else viewedMonthEpochMs
        if (!sameMonth) set(Calendar.DAY_OF_MONTH, 1)
        val mondayOffset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
        add(Calendar.DAY_OF_MONTH, -mondayOffset)
        timeInMillis = dayStart(timeInMillis)
    }
    val dueDays = dueEpochMs.map(::dayStart).toSet()
    val today = dayStart(todayEpochMs)
    fun text(epoch: Long, format: String) = SimpleDateFormat(format, locale).apply {
        this.timeZone = timeZone
    }.format(Date(epoch))
    return (0 until 14).map {
        val startOfDay = start.timeInMillis
        val value = PlanCalendarDay(
            startEpochMs = startOfDay,
            weekday = text(startOfDay, "EEE"),
            dayOfMonth = text(startOfDay, "d"),
            month = text(startOfDay, "MMM"),
            isToday = startOfDay == today,
            hasBill = startOfDay in dueDays
        )
        start.add(Calendar.DAY_OF_MONTH, 1)
        value
    }
}
