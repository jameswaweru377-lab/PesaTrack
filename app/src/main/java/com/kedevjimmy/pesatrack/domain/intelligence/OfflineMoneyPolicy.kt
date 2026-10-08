package com.kedevjimmy.pesatrack.domain.intelligence

import java.util.TimeZone
import java.util.Calendar
import java.math.BigInteger

data class SavingObservation(val epochMs: Long, val deltaMinor: Long)
enum class ForecastStatus { REACHED, INSUFFICIENT_HISTORY, NO_RELIABLE_PROGRESS, ESTIMATE }
data class SavingForecast(val status: ForecastStatus, val typicalMonths: Int? = null,
    val fastestMonths: Int? = null, val slowestMonths: Int? = null, val sampleMonths: Int = 0)

/** Read-only calculations; no models, data access or mutation authority. */
object OfflineMoneyPolicy {
    fun completedSavings(observations: List<SavingObservation>, nowEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()): List<Long> {
        if (nowEpochMs <= 0L) return emptyList()
        fun month(epoch: Long) = Calendar.getInstance(timeZone).apply {
            timeInMillis = epoch
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = month(nowEpochMs)
        val recorded = observations.filter { it.epochMs >= 0L && it.epochMs < end.timeInMillis && it.deltaMinor != 0L }
        val earliest = recorded.minOfOrNull { it.epochMs } ?: return emptyList()
        val window = (end.clone() as Calendar).apply { add(Calendar.MONTH, -6) }
        val cursor = month(earliest).apply {
            if (timeInMillis < window.timeInMillis) timeInMillis = window.timeInMillis
        }
        val result = mutableListOf<Long>()
        while (cursor.timeInMillis < end.timeInMillis) {
            val start = cursor.timeInMillis
            cursor.add(Calendar.MONTH, 1)
            val next = cursor.timeInMillis
            val total = recorded.filter { it.epochMs in start until next }
                .fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(item.deltaMinor) }
            if (total < BigInteger.valueOf(Long.MIN_VALUE) || total > BigInteger.valueOf(Long.MAX_VALUE)) return emptyList()
            result += total.toLong()
        }
        return result
    }

    fun forecast(targetMinor: Long, savedMinor: Long, completedMonths: List<Long>): SavingForecast {
        if (targetMinor <= 0L || savedMinor < 0L) return SavingForecast(ForecastStatus.INSUFFICIENT_HISTORY)
        if (savedMinor >= targetMinor) return SavingForecast(ForecastStatus.REACHED)
        val history = completedMonths.takeLast(6)
        if (history.size < 3) return SavingForecast(ForecastStatus.INSUFFICIENT_HISTORY)
        val ordered = history.sorted()
        val middle = ordered.size / 2
        val median = if (ordered.size % 2 == 1) ordered[middle] else
            ((BigInteger.valueOf(ordered[middle - 1]) + BigInteger.valueOf(ordered[middle])) / BigInteger.valueOf(2L)).toLong()
        val remaining = targetMinor - savedMinor
        fun months(rate: Long): Int? {
            if (rate <= 0L) return null
            val count = remaining / rate + if (remaining % rate == 0L) 0L else 1L
            return count.takeIf { it in 1L..1200L }?.toInt()
        }
        val typical = months(median) ?: return SavingForecast(ForecastStatus.NO_RELIABLE_PROGRESS, sampleMonths = history.size)
        return SavingForecast(ForecastStatus.ESTIMATE, typical, months(ordered.last()), months(ordered.first()), history.size)
    }

    /** Recorded net principal spending; requires KSh 100 and a 20% increase. */
    fun spendingIncrease(previousMinor: Long, currentMinor: Long): Long? {
        if (previousMinor <= 0L || currentMinor <= previousMinor) return null
        val difference = currentMinor - previousMinor
        if (difference < 10_000L || BigInteger.valueOf(difference) * BigInteger.valueOf(5L) < BigInteger.valueOf(previousMinor)) return null
        return difference
    }
}
