package com.kedevjimmy.pesatrack.domain.everyday

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Read-only aggregation of one saved PDF statement. This never classifies a
 * withdrawal as an expense: wallet outflows include internal transfers,
 * savings movements, fees, loans and other non-purchase transactions.
 *
 * Reports are scoped to one original PDF batch to avoid silently double-
 * counting overlapping statement imports.
 */
object StatementMonthlyUsage {
    private val nairobi: TimeZone = TimeZone.getTimeZone("Africa/Nairobi")

    data class Month(
        val year: Int,
        val month: Int,
        val sourceRows: Int,
        val financialMovements: Int,
        val informationalRows: Int,
        val paidInMinor: Long,
        val paidOutMinor: Long
    ) {
        init {
            require(year in 1900..2200 && month in 1..12)
            require(sourceRows == financialMovements + informationalRows)
            require(paidInMinor >= 0 && paidOutMinor >= 0)
        }

        val key: String get() = String.format(Locale.ROOT, "%04d-%02d", year, month)
        val label: String get() = DateFormatSymbols(Locale.ENGLISH).months[month - 1] + " " + year
        val netMinor: Long get() = Math.subtractExact(paidInMinor, paidOutMinor)
    }

    private class Totals(val year: Int, val month: Int) {
        var sourceRows: Int = 0
        var financialMovements: Int = 0
        var informationalRows: Int = 0
        var paidInMinor: Long = 0
        var paidOutMinor: Long = 0

        fun add(line: StatementActivityLine) {
            sourceRows = Math.addExact(sourceRows, 1)
            if (line.informational) informationalRows = Math.addExact(informationalRows, 1)
            else financialMovements = Math.addExact(financialMovements, 1)
            paidInMinor = Math.addExact(paidInMinor, line.paidInMinor)
            paidOutMinor = Math.addExact(paidOutMinor, line.withdrawnMinor)
        }

        fun freeze() = Month(
            year = year,
            month = month,
            sourceRows = sourceRows,
            financialMovements = financialMovements,
            informationalRows = informationalRows,
            paidInMinor = paidInMinor,
            paidOutMinor = paidOutMinor
        )
    }

    /** Financial timestamps from M-Pesa statements are interpreted in Nairobi time. */
    fun monthKey(occurredAt: Long): String {
        require(occurredAt > 0L)
        val calendar = Calendar.getInstance(nairobi, Locale.ROOT).apply {
            timeInMillis = occurredAt
        }
        return String.format(Locale.ROOT, "%04d-%02d",
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    fun summarize(lines: List<StatementActivityLine>): List<Month> {
        if (lines.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance(nairobi, Locale.ROOT)
        val byMonth = mutableMapOf<String, Totals>()
        lines.forEach { line ->
            calendar.timeInMillis = line.occurredAt
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val key = String.format(Locale.ROOT, "%04d-%02d", year, month)
            byMonth.getOrPut(key) { Totals(year, month) }.add(line)
        }
        return byMonth.toSortedMap(compareByDescending { it }).values.map { it.freeze() }
    }
}
