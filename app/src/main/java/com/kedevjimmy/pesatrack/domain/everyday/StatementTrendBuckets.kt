package com.kedevjimmy.pesatrack.domain.everyday

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Exact daily/weekly/monthly/yearly spending chart bins, never counting transfers as expenses. */
object StatementTrendBuckets {
    data class Bucket(val index: Int, val label: String, val amountMinor: Long, val observed: Boolean)
    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")

    fun build(
        items: List<StatementClassified>,
        period: StatementFinancialPolicy.Period,
        importedMonths: Set<Int> = emptySet()
    ): List<Bucket> {
        val titles = when (period) {
            StatementFinancialPolicy.Period.DAY -> listOf("00–03","04–07","08–11","12–15","16–19","20–23")
            StatementFinancialPolicy.Period.WEEK -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
            StatementFinancialPolicy.Period.MONTH -> listOf("1–7","8–14","15–21","22–28","29–31")
            StatementFinancialPolicy.Period.YEAR -> listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        }
        val total = LongArray(titles.size)
        val seen = BooleanArray(titles.size)
        val calendar = Calendar.getInstance(nairobi, Locale.ROOT)
        items.forEach { row ->
            calendar.timeInMillis = row.line.occurredAt
            val slot = when (period) {
                StatementFinancialPolicy.Period.DAY -> calendar.get(Calendar.HOUR_OF_DAY) / 4
                StatementFinancialPolicy.Period.WEEK ->
                    (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                StatementFinancialPolicy.Period.MONTH ->
                    (calendar.get(Calendar.DAY_OF_MONTH) - 1) / 7
                StatementFinancialPolicy.Period.YEAR -> calendar.get(Calendar.MONTH)
            }
            seen[slot] = true
            if (row.flow == StatementFlow.EXPENSE)
                total[slot] = Math.addExact(total[slot], row.amountMinor)
        }
        return titles.indices.map { index ->
            Bucket(index, titles[index], total[index],
                if (period == StatementFinancialPolicy.Period.YEAR)
                    index + 1 in importedMonths else seen[index])
        }
    }
}
