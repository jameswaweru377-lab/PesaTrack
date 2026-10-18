package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone

data class OnboardingHistoryRange(
    val startEpochMs: Long,
    val endExclusiveEpochMs: Long,
    val calendarMonths: Int
)

/**
 * First-run SMS import selection: N completed previous calendar months PLUS
 * current month-to-date, only when the user explicitly opts in.
 *
 * This intentionally differs from HistoryImportWindow.months(N), which means
 * N calendar months INCLUDING the current one. Do not change that legacy API:
 * Activity/reimport and existing pay-cycle tests depend on its semantics.
 */
object OnboardingHistoryWindow {
    /** Returns a validated custom date range with both selected calendar days included. */
    fun customRange(
        startDateEpochMs: Long,
        endDateEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): OnboardingHistoryRange? {
        fun startOfDay(epochMs: Long) = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val start = startOfDay(startDateEpochMs)
        val end = startOfDay(endDateEpochMs)
        val today = startOfDay(nowEpochMs)
        if (start.after(end) || end.after(today)) return null

        val endExclusive = (end.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val calendarMonths =
            (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 +
                end.get(Calendar.MONTH) - start.get(Calendar.MONTH) + 1

        return OnboardingHistoryRange(
            startEpochMs = start.timeInMillis,
            endExclusiveEpochMs = endExclusive.timeInMillis,
            calendarMonths = calendarMonths
        )
    }

    fun importStartEpochMs(
        completedPreviousMonths: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        require(completedPreviousMonths in 0..3) {
            "Select 0–3 completed previous months."
        }
        if (completedPreviousMonths == 0) return null

        return Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -completedPreviousMonths)
        }.timeInMillis
    }
}
