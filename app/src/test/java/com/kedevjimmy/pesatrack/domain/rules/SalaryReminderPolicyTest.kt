package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.TimeZone

class SalaryReminderPolicyTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")
    private fun at(date: String): Long = SimpleDateFormat("yyyy-MM-dd HH:mm").apply { timeZone = zone }.parse(date)!!.time
    @Test fun checkStartsAfterWholeWindowAndStopsAfterActualReceipt() {
        val last = at("2026-08-28 09:00")
        assertNull(SalaryReminderPolicy.dueWindow(last, 24, 28, at("2026-09-28 23:59"), zone))
        assertNotNull(SalaryReminderPolicy.dueWindow(last, 24, 28, at("2026-09-29 00:00"), zone))
        assertNull(SalaryReminderPolicy.dueWindow(at("2026-09-29 09:00"), 24, 28, at("2026-09-30 00:00"), zone))
    }
    @Test fun firstCycleCanBeMissingWithoutInventingSalary() {
        val due = SalaryReminderPolicy.dueWindow(null, 24, 28, at("2026-10-04 12:00"), zone)!!
        assertEquals(at("2026-09-24 00:00"), due.startEpochMs)
        assertNull(SalaryReminderPolicy.dueWindow(null, null, 28, at("2026-10-04 12:00"), zone))
    }
    @Test fun crossMonthAndShortMonthBoundaries() {
        val last = at("2026-08-28 09:00")
        assertNull(SalaryReminderPolicy.dueWindow(last, 28, 2, at("2026-10-02 23:59"), zone))
        assertEquals(at("2026-09-28 00:00"), SalaryReminderPolicy.dueWindow(last, 28, 2, at("2026-10-03 00:00"), zone)!!.startEpochMs)
        assertNotNull(SalaryReminderPolicy.dueWindow(at("2026-01-31 09:00"), 31, 31, at("2026-03-01 00:00"), zone))
    }
    @Test fun oldHistoryCreatesOneCurrentWindowAndEarlySalarySuppressesReminder() {
        assertEquals(at("2026-09-24 00:00"), SalaryReminderPolicy.dueWindow(at("2026-01-28 09:00"),24,28,at("2026-10-04 12:00"),zone)!!.startEpochMs)
        assertNull(SalaryReminderPolicy.dueWindow(at("2026-09-20 09:00"),24,28,at("2026-09-30 12:00"),zone))
    }
}
