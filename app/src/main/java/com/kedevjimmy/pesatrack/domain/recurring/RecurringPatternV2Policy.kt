package com.kedevjimmy.pesatrack.domain.recurring

import java.math.BigInteger
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * An opt-in, read-only detector. It never creates a bill, rule or ledger entry.
 * Observed dates are calendar days in Kenya, rather than elapsed 24-hour blocks.
 */
internal object RecurringPatternV2Policy {
    private const val MIN_OBSERVATIONS = 3
    private const val MAX_AMOUNT_DEVIATION_PERCENT = 35L
    private const val HIGH_AMOUNT_DEVIATION_PERCENT = 10L
    private const val ONE_DAY_MS = 86_400_000L
    private val kenyaZone = TimeZone.getTimeZone("Africa/Nairobi")

    private data class Fit(
        val cadence: RecurringPatternCadence,
        val intervalDays: Int,
        val anchorDay: Int,
        val skipped: Boolean,
        val nextDate: Long
    )

    fun detect(
        observations: List<RecurringPatternObservation>,
        nowEpochMs: Long
    ): List<RecurringPatternCandidate> =
        observations
            .groupBy { "${it.identityKey}|${it.categoryId}" }
            .mapNotNull { (key, group) -> candidate(key, group, nowEpochMs) }
            .sortedWith(
                compareByDescending<RecurringPatternCandidate> {
                    if (it.confidence == RecurringPatternConfidence.HIGH) 2 else 1
                }.thenByDescending { it.occurrenceCount }
                    .thenBy { it.nextExpectedAtEpochMs }
            )

    private fun candidate(
        patternKey: String,
        raw: List<RecurringPatternObservation>,
        nowEpochMs: Long
    ): RecurringPatternCandidate? {
        val ordered = raw.filter { it.amountMinor > 0L }
            .sortedBy { it.occurredAtEpochMs }
        if (ordered.size < MIN_OBSERVATIONS) return null
        // Repeated shopping visits are variable spending, even when weekly.
        if (ordered.last().categoryId.equals("SHOPPING", ignoreCase = true)) return null
        val amounts = ordered.map { it.amountMinor }
        val typical = medianLong(amounts)
        val maxDeviation = amounts.maxOf {
            BigInteger.valueOf(it).subtract(BigInteger.valueOf(typical)).abs()
        }
        if (!withinPercentage(maxDeviation, typical, MAX_AMOUNT_DEVIATION_PERCENT)) {
            return null
        }
        val fit = fitCadence(ordered.map { date(it.occurredAtEpochMs) }) ?: return null
        if (ageDays(ordered.last().occurredAtEpochMs, nowEpochMs) > maxAgeDays(fit.cadence)) {
            return null
        }
        val confidence = if (
            ordered.size >= 4 && !fit.skipped &&
            withinPercentage(maxDeviation, typical, HIGH_AMOUNT_DEVIATION_PERCENT)
        ) RecurringPatternConfidence.HIGH else RecurringPatternConfidence.MEDIUM
        val last = ordered.last()
        return RecurringPatternCandidate(
            patternKey = patternKey,
            identityKey = last.identityKey,
            label = last.label,
            categoryId = last.categoryId,
            typicalAmountMinor = typical,
            occurrenceCount = ordered.size,
            medianIntervalDays = fit.intervalDays,
            lastObservedAtEpochMs = last.occurredAtEpochMs,
            nextExpectedAtEpochMs = fit.nextDate,
            confidence = confidence,
            representativeTransactionId = last.transactionId,
            counterparty = last.counterparty,
            counterpartyPhone = last.counterpartyPhone,
            accountId = last.accountId,
            cadence = fit.cadence,
            reason = reason(fit, ordered.size)
        )
    }

    private fun fitCadence(dates: List<Calendar>): Fit? =
        fitWeekBased(dates, 7, 1, RecurringPatternCadence.WEEKLY)
            ?: fitWeekBased(dates, 14, 2, RecurringPatternCadence.FORTNIGHTLY)
            ?: fitMonthly(dates, lastDay = true)
            ?: fitMonthly(dates, lastDay = false)
            ?: fitTermly(dates)

    private fun fitWeekBased(
        dates: List<Calendar>,
        intervalDays: Int,
        jitterDays: Int,
        cadence: RecurringPatternCadence
    ): Fit? {
        val days = dates.zipWithNext { first, second -> dayDifference(first, second) }
        val skipped = skipCount(
            days,
            { abs(it - intervalDays) <= jitterDays },
            { abs(it - intervalDays * 2) <= jitterDays * 2 }
        ) ?: return null
        val next = dates.last().clone() as Calendar
        next.add(Calendar.DAY_OF_MONTH, intervalDays)
        return Fit(cadence, intervalDays, dates.last().get(Calendar.DAY_OF_MONTH),
            skipped, next.timeInMillis)
    }

    private fun fitMonthly(dates: List<Calendar>, lastDay: Boolean): Fit? {
        if (lastDay && dates.any {
                it.get(Calendar.DAY_OF_MONTH) != it.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        ) return null
        val day = medianInt(dates.map { it.get(Calendar.DAY_OF_MONTH) })
        if (!lastDay && dates.any { abs(it.get(Calendar.DAY_OF_MONTH) - day) > 2 }) {
            return null
        }
        val months = dates.zipWithNext { first, second -> monthDifference(first, second) }
        val skipped = skipCount(months, { it == 1 }, { it == 2 }) ?: return null
        val next = nextMonth(dates.last(), 1, day, lastDay)
        return Fit(
            if (lastDay) RecurringPatternCadence.MONTHLY_LAST_DAY
            else RecurringPatternCadence.MONTHLY_FIXED_DAY,
            30, day, skipped, next
        )
    }

    private fun fitTermly(dates: List<Calendar>): Fit? {
        val monthGaps = dates.zipWithNext { first, second ->
            monthDifference(first, second)
        }
        val anchor = medianInt(dates.map { it.get(Calendar.DAY_OF_MONTH) })
        if (dates.any { abs(it.get(Calendar.DAY_OF_MONTH) - anchor) > 15 }) return null
        val skipped = skipCount(monthGaps, { it == 4 }, { it == 8 }) ?: return null
        return Fit(
            RecurringPatternCadence.TERMLY, 120, anchor, skipped,
            nextMonth(dates.last(), 4, anchor, false)
        )
    }

    /**
     * One absent occurrence requires at least three observed gaps, including
     * at least two normal ones; never guess a cadence from one normal gap.
     */
    private fun skipCount(
        gaps: List<Int>,
        normal: (Int) -> Boolean,
        double: (Int) -> Boolean
    ): Boolean? {
        if (gaps.any { it <= 0 }) return null
        val missed = gaps.count { double(it) && !normal(it) }
        if (missed > 1) return null
        if (gaps.any { !normal(it) && !double(it) }) return null
        if (missed == 1 && (gaps.size < 3 || gaps.count(normal) < 2)) return null
        return missed == 1
    }

    private fun nextMonth(
        last: Calendar,
        months: Int,
        day: Int,
        lastDay: Boolean
    ): Long {
        val next = last.clone() as Calendar
        next.set(Calendar.DAY_OF_MONTH, 1)
        next.add(Calendar.MONTH, months)
        val anchor = if (lastDay) next.getActualMaximum(Calendar.DAY_OF_MONTH)
        else day.coerceAtMost(next.getActualMaximum(Calendar.DAY_OF_MONTH))
        next.set(Calendar.DAY_OF_MONTH, anchor)
        return next.timeInMillis
    }

    private fun reason(fit: Fit, count: Int): String {
        val cadence = when (fit.cadence) {
            RecurringPatternCadence.WEEKLY -> "paid every week, $count payments"
            RecurringPatternCadence.FORTNIGHTLY ->
                "paid every two weeks, $count payments"
            RecurringPatternCadence.MONTHLY_FIXED_DAY ->
                "paid on or near the ${ordinal(fit.anchorDay)}, $count months observed"
            RecurringPatternCadence.MONTHLY_LAST_DAY ->
                "paid on the last day of the month, $count months observed"
            RecurringPatternCadence.TERMLY ->
                "paid roughly once per term, $count payments observed"
        }
        if (!fit.skipped) return cadence
        val unit = when (fit.cadence) {
            RecurringPatternCadence.WEEKLY -> "week"
            RecurringPatternCadence.FORTNIGHTLY -> "two-week payment"
            RecurringPatternCadence.TERMLY -> "term"
            else -> "month"
        }
        return "$cadence (one skipped $unit)"
    }

    private fun ordinal(day: Int): String {
        val suffix = if (day % 100 in 11..13) "th" else when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    private fun date(epoch: Long): Calendar =
        Calendar.getInstance(kenyaZone).apply {
            timeInMillis = epoch
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun dayDifference(first: Calendar, second: Calendar): Int =
        ((second.timeInMillis - first.timeInMillis) / ONE_DAY_MS).toInt()

    private fun monthDifference(first: Calendar, second: Calendar): Int =
        (second.get(Calendar.YEAR) - first.get(Calendar.YEAR)) * 12 +
            second.get(Calendar.MONTH) - first.get(Calendar.MONTH)

    private fun ageDays(last: Long, now: Long): Long =
        ((date(now).timeInMillis - date(last).timeInMillis).coerceAtLeast(0L)) /
            ONE_DAY_MS

    private fun maxAgeDays(cadence: RecurringPatternCadence): Int =
        when (cadence) {
            RecurringPatternCadence.WEEKLY -> 21
            RecurringPatternCadence.FORTNIGHTLY -> 35
            RecurringPatternCadence.TERMLY -> 180
            else -> 75
        }

    private fun withinPercentage(
        deviation: BigInteger,
        typical: Long,
        percent: Long
    ): Boolean =
        deviation.multiply(BigInteger.valueOf(100)) <=
            BigInteger.valueOf(typical).multiply(BigInteger.valueOf(percent))

    private fun medianLong(values: List<Long>): Long {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else
            BigInteger.valueOf(sorted[middle - 1]).add(BigInteger.valueOf(sorted[middle]))
                .divide(BigInteger.valueOf(2)).toLong()
    }

    private fun medianInt(values: List<Int>): Int {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else
            (sorted[middle - 1] + sorted[middle]) / 2
    }
}
