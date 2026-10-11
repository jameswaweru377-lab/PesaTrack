package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementImportDatesTest {
    private fun epoch(day: String, time: String="00:00", zone: String="Africa/Nairobi") =
        SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT).apply {isLenient=false;timeZone=TimeZone.getTimeZone(zone)}.parse("$day $time")!!.time
    private val now=epoch("2026-10-11","12:00")
    private fun row(day: String,time: String="00:00",zone: String="Africa/Nairobi")=StatementRow(epoch(day,time,zone),"Synthetic payment",-10000,"FAKE-$day-$time")
    private fun input(rows:List<StatementRow>,range:StatementDateRange?=null,zone:String="Africa/Nairobi")=
        StatementImportSelection("synthetic-bank",rows,range,zone)
    private fun fails(action:()->Unit) {assertTrue(runCatching(action).exceptionOrNull() is IllegalArgumentException)}

    @Test fun allDatesPreservesAmountsReferencesOrderAndRows() {
        val rows=listOf(row("2025-02-14"),row("2024-01-01"))
        val result=StatementImportDates.select(input(rows),now)
        assertEquals(rows,result.rows);assertEquals(0,result.excludedCount)
        assertEquals("2024-01-01",result.availableFromDay);assertEquals("2025-02-14",result.availableToDay)
    }
    @Test fun customRangeIncludesEntireFirstAndLastCalendarDays() {
        val rows=listOf(row("2025-01-31","23:59"),row("2025-02-01"),row("2025-02-28","23:59"),row("2025-03-01"))
        val result=StatementImportDates.select(input(rows,StatementDateRange("2025-02-01","2025-02-28")),now)
        assertEquals(rows.subList(1,3),result.rows);assertEquals(2,result.excludedCount)
    }
    @Test fun historicalFebruary2025ThroughTodayHasNoMonthCountCap() {
        val rows=listOf(row("2025-01-31"),row("2025-02-01"),row("2026-10-11","11:59"))
        assertEquals(rows.drop(1),StatementImportDates.select(input(rows,StatementDateRange("2025-02-01","2026-10-11")),now).rows)
    }
    @Test fun oneDayAndNoMatchingRowsAreValidSelections() {
        val rows=listOf(row("2025-02-14"))
        assertEquals(rows,StatementImportDates.select(input(rows,StatementDateRange("2025-02-14","2025-02-14")),now).rows)
        val missing=StatementImportDates.select(input(rows,StatementDateRange("2025-03-01","2025-03-31")),now)
        assertTrue(missing.rows.isEmpty());assertEquals(1,missing.excludedCount);assertEquals("2025-02-14",missing.availableFromDay)
    }
    @Test fun reversedFutureAndMalformedRangesAreRejected() {
        for(range in listOf(StatementDateRange("2025-03-01","2025-02-01"),StatementDateRange("2026-10-11","2026-10-12"),
            StatementDateRange("2025-02-29","2025-03-01"),StatementDateRange("2025-2-01","2025-03-01"),StatementDateRange("2025-13-01","2025-13-02"))) {
            fails {StatementImportDates.select(input(emptyList(),range),now)}
        }
    }
    @Test fun leapDayAndDstDayBoundariesDoNotLoseTransactions() {
        val leap=row("2024-02-29","23:59")
        assertEquals(listOf(leap),StatementImportDates.select(input(listOf(leap),StatementDateRange("2024-02-29","2024-02-29")),now).rows)
        val zone="America/New_York";val rows=listOf(row("2025-03-09","00:00",zone),row("2025-03-09","23:59",zone),row("2025-03-10","00:00",zone))
        assertEquals(rows.take(2),StatementImportDates.select(input(rows,StatementDateRange("2025-03-09","2025-03-09"),zone),now).rows)
    }
    @Test fun todayUsesTheCapturedLocalZoneRatherThanUtc() {
        val localNow=epoch("2025-03-01","00:10")
        val rows=listOf(row("2025-03-01"))
        assertEquals(rows,StatementImportDates.select(input(rows,StatementDateRange("2025-03-01","2025-03-01")),localNow).rows)
        fails {StatementImportDates.select(input(rows,StatementDateRange("2025-03-01","2025-03-01"),"UTC"),localNow)}
    }
    @Test fun csvAndFilterShareTheSameCapturedZone() {
        val rows=StatementCsv.parse("date,description,amount\n2025-02-14,Synthetic,100.00",TimeZone.getTimeZone("America/New_York"))
        assertEquals(epoch("2025-02-14",zone="America/New_York"),rows.single().date)
        assertEquals(rows,StatementImportDates.select(input(rows,StatementDateRange("2025-02-14","2025-02-14"),"America/New_York"),now).rows)
    }
    @Test fun invalidRowsOutsideRangeCannotBeSilentlyDiscarded() {
        val invalid=row("2025-01-01").copy(signedMinor=0)
        fails {StatementImportDates.select(input(listOf(invalid,row("2025-02-14")),StatementDateRange("2025-02-01","2025-02-28")),now)}
        fails {StatementImportDates.select(input(listOf(row("2026-10-12"))),now)}
        fails {StatementImportDates.select(input(emptyList(),zone="Made/Up"),now)}
    }
    @Test fun changedAccountRangeFileOrZoneInvalidatesReviewedPreview() {
        val rows=listOf(row("2025-02-14"));val reviewed=input(rows)
        for(current in listOf(reviewed.copy(accountId="another-bank"),reviewed.copy(range=StatementDateRange("2025-02-01","2025-02-28")),
            reviewed.copy(rows=rows+row("2025-03-01")),reviewed.copy(zoneId="UTC"))) {
            fails {StatementImportDates.requireReviewed(current,reviewed,rows,now)}
        }
        fails {StatementImportDates.requireReviewed(reviewed,null,rows,now)}
    }
    @Test fun selectedRowsMustBeEligibleAndCannotExceedSourceMultiplicity() {
        val before=row("2025-01-31");val included=row("2025-02-14")
        val source=input(listOf(before,included),StatementDateRange("2025-02-01","2025-02-28"))
        assertEquals(listOf(included),StatementImportDates.requireReviewed(source,source,listOf(included),now))
        fails {StatementImportDates.requireReviewed(source,source,listOf(before),now)}
        fails {StatementImportDates.requireReviewed(source,source,listOf(included,included),now)}
        fails {StatementImportDates.requireReviewed(source,source,emptyList(),now)}
    }
}
