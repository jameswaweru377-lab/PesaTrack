package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone

/** A missing message is uncertainty, never evidence that someone was not paid. */
object SalaryReminderPolicy {
    fun dueWindow(lastSalaryEpochMs: Long?, startDay: Int?, endDay: Int?, now: Long,
                  timeZone: TimeZone = TimeZone.getDefault()): ExpectedPayWindow? {
        if (lastSalaryEpochMs != null && lastSalaryEpochMs > now) return null
        val anchor = lastSalaryEpochMs ?: Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            add(Calendar.MONTH, -2)
        }.timeInMillis
        var window = SalaryExpectationPolicy.nextWindow(anchor, startDay, endDay, timeZone) ?: return null
        var due: ExpectedPayWindow? = null
        // Advance old schedules to the most recently elapsed window, without posting a backlog.
        repeat(2400) {
            if (now <= window.endEpochMs) return due
            due = window
            window = SalaryExpectationPolicy.nextWindow(window.endEpochMs, startDay, endDay, timeZone)
                ?: return due
        }
        return due
    }
}
