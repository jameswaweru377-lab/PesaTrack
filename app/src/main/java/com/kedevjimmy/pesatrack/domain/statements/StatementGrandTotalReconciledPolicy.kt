package com.kedevjimmy.pesatrack.domain.statements

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A read-only report for one bank PDF whose printed grand totals were checked at admission. */
object StatementGrandTotalReconciledPolicy {
    data class Row(
        val sourceId: String,
        val number: Int,
        val occurredAt: Long,
        val movementMinor: Long
    )

    data class Month(
        val key: String,
        val rows: Int,
        val moneyInMinor: Long,
        val moneyOutMinor: Long
    )

    data class Report(
        val rows: Int,
        val moneyInMinor: Long,
        val moneyOutMinor: Long,
        val netMinor: Long,
        val months: List<Month>
    )

    fun project(
        sourceId: String,
        expectedRows: Int,
        expectedMoneyInMinor: Long,
        expectedMoneyOutMinor: Long,
        rows: List<Row>
    ): Report? {
        if (sourceId.isBlank() || expectedRows !in 1..5_000 || rows.size != expectedRows ||
            expectedMoneyInMinor < 0 || expectedMoneyOutMinor < 0) return null

        return try {
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("Africa/Nairobi")
                isLenient = false
            }
            val months = linkedMapOf<String, Month>()
            var moneyIn = 0L
            var moneyOut = 0L
            rows.forEachIndexed { number, row ->
                if (row.sourceId != sourceId || row.number != number ||
                    row.occurredAt <= 0L || row.movementMinor == 0L) return null

                val incoming = if (row.movementMinor > 0L) row.movementMinor else 0L
                val outgoing = if (row.movementMinor < 0L)
                    Math.negateExact(row.movementMinor) else 0L
                moneyIn = Math.addExact(moneyIn, incoming)
                moneyOut = Math.addExact(moneyOut, outgoing)

                val key = monthFormat.format(Date(row.occurredAt))
                val month = months[key] ?: Month(key, 0, 0L, 0L)
                months[key] = month.copy(
                    rows = Math.addExact(month.rows, 1),
                    moneyInMinor = Math.addExact(month.moneyInMinor, incoming),
                    moneyOutMinor = Math.addExact(month.moneyOutMinor, outgoing)
                )
            }
            if (moneyIn != expectedMoneyInMinor || moneyOut != expectedMoneyOutMinor) null
            else Report(expectedRows, moneyIn, moneyOut, Math.subtractExact(moneyIn, moneyOut),
                months.values.sortedByDescending { it.key })
        } catch (_: ArithmeticException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
