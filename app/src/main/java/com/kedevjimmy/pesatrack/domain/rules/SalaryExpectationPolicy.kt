package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone

data class ExpectedPayWindow(
    val startEpochMs: Long,
    val endEpochMs: Long
)

object SalaryExpectationPolicy {
    fun nextWindow(
        lastSalaryEpochMs: Long,
        payWindowStartDay: Int?,
        payWindowEndDay: Int?,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ExpectedPayWindow? {
        val startDay = payWindowStartDay ?: return null
        val endDay = payWindowEndDay ?: return null
        if (startDay !in 1..31 || endDay !in 1..31) {
            return null
        }

        val lastSalary = Calendar.getInstance(timeZone).apply {
            timeInMillis = lastSalaryEpochMs
        }
        val crossesMonth = endDay < startDay

        val previousWindowStartMonth =
            Calendar.getInstance(timeZone).apply {
                clear()
                set(
                    lastSalary.get(Calendar.YEAR),
                    lastSalary.get(Calendar.MONTH),
                    1,
                    0,
                    0,
                    0
                )
                set(Calendar.MILLISECOND, 0)

                // For a window such as 24th–1st, a salary that lands
                // before the 24th belongs to the window that began in
                // the previous calendar month.
                if (
                    crossesMonth &&
                    lastSalary.get(Calendar.DAY_OF_MONTH) < startDay
                ) {
                    add(Calendar.MONTH, -1)
                }
            }

        val nextStart = (previousWindowStartMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            setDaySafely(startDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextEnd = (nextStart.clone() as Calendar).apply {
            if (crossesMonth) {
                add(Calendar.MONTH, 1)
            }
            setDaySafely(endDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return ExpectedPayWindow(
            startEpochMs = nextStart.timeInMillis,
            endEpochMs = nextEnd.timeInMillis
        )
    }

    fun overdueWindow(
        lastSalaryEpochMs: Long,
        payWindowStartDay: Int?,
        payWindowEndDay: Int?,
        nowEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ExpectedPayWindow? {
        val window = nextWindow(
            lastSalaryEpochMs = lastSalaryEpochMs,
            payWindowStartDay = payWindowStartDay,
            payWindowEndDay = payWindowEndDay,
            timeZone = timeZone
        ) ?: return null

        return window.takeIf {
            nowEpochMs > it.endEpochMs
        }
    }

    private fun Calendar.setDaySafely(day: Int) {
        set(Calendar.DAY_OF_MONTH, 1)
        set(
            Calendar.DAY_OF_MONTH,
            day.coerceAtMost(
                getActualMaximum(Calendar.DAY_OF_MONTH)
            )
        )
    }
}
