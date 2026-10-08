package com.kedevjimmy.pesatrack.domain.intelligence

import java.util.TimeZone

data class SavingObservation(val epochMs: Long, val deltaMinor: Long)
enum class ForecastStatus { REACHED, INSUFFICIENT_HISTORY, NO_RELIABLE_PROGRESS, ESTIMATE }
data class SavingForecast(val status: ForecastStatus, val typicalMonths: Int? = null,
    val fastestMonths: Int? = null, val slowestMonths: Int? = null, val sampleMonths: Int = 0)

/** Read-only calculations; no models, data access or mutation authority. */
object OfflineMoneyPolicy {
    fun completedSavings(observations: List<SavingObservation>, nowEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()): List<Long> = emptyList()
    fun forecast(targetMinor: Long, savedMinor: Long, completedMonths: List<Long>): SavingForecast =
        SavingForecast(ForecastStatus.INSUFFICIENT_HISTORY)
    fun spendingIncrease(previousMinor: Long, currentMinor: Long): Long? = null
}
