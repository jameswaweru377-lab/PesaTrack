package com.kedevjimmy.pesatrack.domain.everyday

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class StatementDateRange(val fromDay: String, val toDay: String)
data class StatementImportSelection(val accountId: String, val rows: List<StatementRow>,
    val range: StatementDateRange?, val zoneId: String)
data class StatementDateSelection(val rows: List<StatementRow>, val excludedCount: Int,
    val availableFromDay: String?, val availableToDay: String?)

/** Calendar-day selection only: never rewrites dates, money, references or canonical identity. */
object StatementImportDates {
    private val zoneIds=TimeZone.getAvailableIDs().toSet()
    private fun zone(id: String): TimeZone {
        require(id in zoneIds) { "Close and reopen import to refresh the date settings." }
        return TimeZone.getTimeZone(id)
    }
    fun day(epoch: Long, zoneId: String): String = SimpleDateFormat("yyyy-MM-dd",Locale.ROOT)
        .apply {timeZone=zone(zoneId)}.format(Date(epoch))
    private fun requireDay(value: String) {
        require(Regex("(?:19|20|21)[0-9]{2}-[0-9]{2}-[0-9]{2}").matches(value)) { "Choose valid import dates." }
        val position=ParsePosition(0)
        val parsed=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).apply {isLenient=false;timeZone=TimeZone.getTimeZone("UTC")}.parse(value,position)
        require(parsed!=null && position.index==value.length) { "Choose valid import dates." }
    }
    fun validateRange(range: StatementDateRange?, now: Long, zoneId: String) {
        val today=day(now,zoneId)
        if(range==null)return
        requireDay(range.fromDay);requireDay(range.toDay)
        require(range.fromDay<=range.toDay) { "From date must be on or before To date." }
        require(range.toDay<=today) { "Choose dates up to today." }
    }
    fun select(input: StatementImportSelection, now: Long=System.currentTimeMillis()): StatementDateSelection {
        validateRange(input.range,now,input.zoneId)
        // Invalid source rows must not disappear just because they are outside the chosen dates.
        StatementCsv.validateRows(input.rows,now)
        val dated=input.rows.map {it to day(it.date,input.zoneId)}
        val selected=dated.filter {(_,date)->input.range?.let {date>=it.fromDay && date<=it.toDay} ?: true}.map {it.first}
        return StatementDateSelection(selected,input.rows.size-selected.size,dated.minOfOrNull {it.second},dated.maxOfOrNull {it.second})
    }
    /** Recheck the displayed snapshot and range immediately before the existing atomic write adapter. */
    fun requireReviewed(current: StatementImportSelection, reviewed: StatementImportSelection?,
        selected: List<StatementRow>, now: Long=System.currentTimeMillis()): List<StatementRow> {
        require(current==reviewed && current.accountId.isNotBlank()) { "The file, account or dates changed. Review the entries again." }
        require(selected.isNotEmpty()) { "Choose at least one entry to import." }
        val remaining=select(current,now).rows.groupingBy {it}.eachCount().toMutableMap()
        for(row in selected) {
            val count=remaining[row] ?: 0
            require(count>0) { "An entry is outside the reviewed date range. Review the entries again." }
            remaining[row]=count-1
        }
        return selected.toList()
    }
}
