package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SalaryDatePickerPolicyTest {
    private fun date(value: String, zone: TimeZone) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = zone }.parse(value)!!.time
    @Test fun calendarDateRoundTripsInKenyaAndAcrossDstWithoutChangingLedgerDay() {
        val utc = TimeZone.getTimeZone("UTC")
        for (id in listOf("Africa/Nairobi", "America/Los_Angeles", "Pacific/Auckland")) {
            val zone = TimeZone.getTimeZone(id)
            for (day in listOf("2025-03-09", "2025-11-02", "2025-09-28")) {
                val picker = SalaryDatePickerPolicy.toPickerMillis(date("$day 23:45", zone), zone)
                assertEquals(date("$day 00:00", utc), picker)
                assertEquals(date("$day 00:00", zone), SalaryDatePickerPolicy.toReceiptMillis(picker, zone))
            }
        }
    }
}
