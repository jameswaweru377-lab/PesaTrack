package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object PayCyclePolicy {
    private const val MIN_CYCLE_DAYS = 14

    fun candidateReason(
        previousCycleStartEpochMs: Long?,
        currentSalaryEpochMs: Long,
        payWindowStartDay: Int?,
        payWindowEndDay: Int?,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String? {
        if (previousCycleStartEpochMs == null) return null

        val days = TimeUnit.MILLISECONDS.toDays(
            currentSalaryEpochMs - previousCycleStartEpochMs
        )

        if (days >= 0L && days < MIN_CYCLE_DAYS.toLong()) {
            return "Another salary-like credit arrived only " +
                days +
                " days after the current pay cycle started."
        }

        if (payWindowStartDay != null && payWindowEndDay != null) {
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = currentSalaryEpochMs
            }
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            if (!dayInWindow(day, payWindowStartDay, payWindowEndDay)) {
                return "This salary arrived outside your usual pay window."
            }
        }

        return null
    }

    fun dayInWindow(
        day: Int,
        startDay: Int,
        endDay: Int
    ): Boolean =
        if (startDay <= endDay) {
            day in startDay..endDay
        } else {
            day >= startDay || day <= endDay
        }
}
