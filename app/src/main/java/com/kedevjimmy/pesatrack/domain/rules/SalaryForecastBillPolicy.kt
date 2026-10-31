package com.kedevjimmy.pesatrack.domain.rules

import java.util.Calendar
import java.util.TimeZone

enum class SalaryForecastBillCadence { MONTHLY, WEEKLY }

data class SalaryForecastBill(
    val id: String,
    val name: String,
    val amountPerOccurrenceMinor: Long,
    val cadence: SalaryForecastBillCadence,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val paidMinorThisMonth: Long = 0L
)

/** Creates distinct, unpaid due occurrences from user-confirmed schedules. */
object SalaryForecastBillPolicy {
    fun occurrences(
        bill: SalaryForecastBill,
        nowEpochMs: Long,
        throughEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<SalaryForecastCommitment> {
        if (bill.amountPerOccurrenceMinor <= 0L || bill.id.isBlank() || throughEpochMs < nowEpochMs) {
            return emptyList()
        }
        val startMonth = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            clearTime()
        }
        return buildList {
            repeat(3) { offset ->
                val month = (startMonth.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
                if (month.timeInMillis > throughEpochMs) return@repeat
                var paymentApplied = if (offset == 0) bill.paidMinorThisMonth.coerceAtLeast(0L) else 0L
                datesForMonth(bill, month).forEach { due ->
                    val outstanding = (bill.amountPerOccurrenceMinor - paymentApplied.coerceAtMost(
                        bill.amountPerOccurrenceMinor)).coerceAtLeast(0L)
                    paymentApplied = (paymentApplied - bill.amountPerOccurrenceMinor).coerceAtLeast(0L)
                    if (outstanding > 0L && due <= throughEpochMs) {
                        add(SalaryForecastCommitment(
                            id = "${bill.id}:$due", label = bill.name,
                            amountMinor = outstanding, dueEpochMs = maxOf(nowEpochMs, due)
                        ))
                    }
                }
            }
        }
    }

    private fun datesForMonth(bill: SalaryForecastBill, month: Calendar): List<Long> {
        return when (bill.cadence) {
            SalaryForecastBillCadence.MONTHLY -> {
                val day = bill.dayOfMonth ?: return emptyList()
                if (day !in 1..31) return emptyList()
                listOf((month.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                }.timeInMillis)
            }
            SalaryForecastBillCadence.WEEKLY -> {
                val weekDay = bill.dayOfWeek ?: return emptyList()
                if (weekDay !in Calendar.SUNDAY..Calendar.SATURDAY) return emptyList()
                val days = mutableListOf<Long>()
                val cursor = month.clone() as Calendar
                val targetMonth = month.get(Calendar.MONTH)
                while (cursor.get(Calendar.MONTH) == targetMonth) {
                    if (cursor.get(Calendar.DAY_OF_WEEK) == weekDay) days += cursor.timeInMillis
                    cursor.add(Calendar.DAY_OF_MONTH, 1)
                }
                days
            }
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
