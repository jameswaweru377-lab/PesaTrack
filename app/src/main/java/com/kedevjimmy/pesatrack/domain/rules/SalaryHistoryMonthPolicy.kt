package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone

object SalaryHistoryMonthPolicy {
    private const val EARLY_PAYDAY_DAY = 20

    fun anchorCalendar(
        nowEpochMs: Long,
        timeZone: TimeZone =
            TimeZone.getDefault()
    ): Calendar {
        val calendar =
            Calendar.getInstance(
                timeZone
            ).apply {
                timeInMillis =
                    nowEpochMs
                set(
                    Calendar.DAY_OF_MONTH,
                    1
                )
                set(
                    Calendar.HOUR_OF_DAY,
                    12
                )
                set(
                    Calendar.MINUTE,
                    0
                )
                set(
                    Calendar.SECOND,
                    0
                )
                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val currentDay =
            Calendar.getInstance(
                timeZone
            ).apply {
                timeInMillis =
                    nowEpochMs
            }.get(
                Calendar.DAY_OF_MONTH
            )

        if (
            currentDay <
            EARLY_PAYDAY_DAY
        ) {
            calendar.add(
                Calendar.MONTH,
                -1
            )
        }

        return calendar
    }
}
