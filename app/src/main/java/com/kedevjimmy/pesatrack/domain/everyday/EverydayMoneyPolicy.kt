package com.kedevjimmy.pesatrack.domain.everyday

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale

data class MoneyPart(val category: String, val minor: Long)
object EverydayMoneyPolicy {
 fun money(text: String): Long? {
  val raw=text.trim()
  if (!Regex("[+-]?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?").matches(raw)) return null
  return runCatching { BigDecimal(raw.replace(",", "")).movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).longValueExact() }.getOrNull()
 }
 fun validSplit(total: Long, parts: List<MoneyPart>): Boolean = total>0 && parts.size in 2..20 && parts.all {it.minor>0 && it.category.isNotBlank()} && parts.map {it.category}.distinct().size==parts.size && parts.fold(BigInteger.ZERO) {sum,p->sum+BigInteger.valueOf(p.minor)}==BigInteger.valueOf(total)
 /** Largest-remainder allocation. No floating money and no lost refund cents. */
 fun apportion(total: Long, parts: List<MoneyPart>): List<MoneyPart> {
  require(total>=0 && parts.isNotEmpty() && parts.all {it.minor>0})
  val denominator=parts.fold(BigInteger.ZERO) {s,p->s+BigInteger.valueOf(p.minor)}
  val values=parts.map {BigInteger.valueOf(total).multiply(BigInteger.valueOf(it.minor)).divideAndRemainder(denominator)}
  val amounts=values.map {it[0].toLong()}.toMutableList()
  val remainder=total-amounts.sum()
  values.indices.sortedWith(compareByDescending<Int>{values[it][1]}.thenBy {it}).take(remainder.toInt()).forEach {amounts[it]++}
  return parts.mapIndexed {i,p->p.copy(minor=amounts[i])}
 }
}
enum class BudgetCadence { MONTHLY, WEEKLY, PAYDAY }
data class BudgetWindow(val start:Long,val end:Long)
object BudgetPeriods {
 fun containing(now:Long,cadence:BudgetCadence,anchor:Int,zone:TimeZone=TimeZone.getDefault()):BudgetWindow {
  val c=Calendar.getInstance(zone).apply {timeInMillis=now;set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
  if(cadence==BudgetCadence.WEEKLY) {
   require(anchor in 1..7);c.add(Calendar.DAY_OF_MONTH,-((c.get(Calendar.DAY_OF_WEEK)-anchor+7)%7));val start=c.timeInMillis;c.add(Calendar.DAY_OF_MONTH,7);return BudgetWindow(start,c.timeInMillis)
  }
  require(anchor in 1..31)
  val day=if(cadence==BudgetCadence.MONTHLY)1 else anchor
  c.set(Calendar.DAY_OF_MONTH,minOf(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)))
  if(c.timeInMillis>now) {c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,-1);c.set(Calendar.DAY_OF_MONTH,minOf(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)))}
  val start=c.timeInMillis;c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,1);c.set(Calendar.DAY_OF_MONTH,minOf(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)))
  return BudgetWindow(start,c.timeInMillis)
 }
 fun carryNext(base:Long,carry:Long,spent:Long):Long {require(base>0 && carry>=0 && spent>=0);return Math.subtractExact(Math.addExact(base,carry),spent).coerceAtLeast(0)}
}
data class StatementRow(val date:Long,val description:String,val signedMinor:Long,val reference:String?)
object StatementCsv {
 const val MAX_BYTES=2*1024*1024
 const val MAX_ROWS=2000
 fun parse(text:String):List<StatementRow> {
  require(text.toByteArray(Charsets.UTF_8).size<=MAX_BYTES) {"Choose a CSV smaller than 2 MB."}
  val rows=csv(text.removePrefix("\uFEFF"));require(rows.size>=2) {"This file has no entries."}
  val header=rows.first().map {it.trim().lowercase(Locale.ROOT)}
  require(header.distinct().size==header.size) {"Repeated column names."}
  val date=header.indexOf("date");val description=header.indexOf("description");val amount=header.indexOf("amount");val debit=header.indexOf("debit");val credit=header.indexOf("credit");val ref=header.indexOf("reference")
  require(date>=0 && description>=0 && ((amount>=0 && debit<0 && credit<0)||(amount<0 && debit>=0 && credit>=0))) {"Use date, description, amount and optional reference, or debit and credit instead of amount."}
  require(rows.size-1<=MAX_ROWS) {"Choose a file with up to 2,000 entries."}
  return rows.drop(1).mapIndexed {i,row->
   require(row.size==header.size) {"Row ${i+2}: columns do not match."}
   val label=row[description].trim();require(label.isNotEmpty() && label.length<=200) {"Row ${i+2}: check the description."}
   val epoch=listOf("yyyy-MM-dd","dd/MM/yyyy").firstNotNullOfOrNull {pattern->
    val value=row[date].trim();val expected=if(pattern=="yyyy-MM-dd")Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")else Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}")
    if(!expected.matches(value))null else SimpleDateFormat(pattern,Locale.ROOT).apply {isLenient=false}.let {f->val pos=ParsePosition(0);f.parse(value,pos)?.time?.takeIf {pos.index==value.length}}
   };require(epoch!=null && epoch>0 && epoch<=System.currentTimeMillis()) {"Row ${i+2}: use a valid past date (yyyy-MM-dd or dd/MM/yyyy)."}
   val signed=if(amount>=0)EverydayMoneyPolicy.money(row[amount]) else {
    val d=if(row[debit].isBlank())0L else EverydayMoneyPolicy.money(row[debit]);val c=if(row[credit].isBlank())0L else EverydayMoneyPolicy.money(row[credit])
    require(d!=null && c!=null && d>=0 && c>=0 && !(d>0 && c>0)) {"Row ${i+2}: use one debit or credit."};Math.subtractExact(c,d)
   }
   require(signed!=null && signed!=0L && signed!=Long.MIN_VALUE) {"Row ${i+2}: check the amount."}
   val reference=if(ref>=0)row[ref].trim().takeIf {it.isNotEmpty()} else null
   require(reference==null || reference.length<=100) {"Row ${i+2}: reference is too long."}
   StatementRow(epoch,label,signed,reference)
  }
 }
 private fun csv(text:String):List<List<String>> {
  val rows=mutableListOf<List<String>>();val row=mutableListOf<String>();val field=StringBuilder();var quoted=false;var endedQuote=false;var i=0
  fun cell(){row.add(field.toString());field.setLength(0);endedQuote=false}
  fun line(){cell();if(row.any {it.isNotBlank()})rows.add(row.toList());row.clear();require(rows.size<=MAX_ROWS+1){"Too many entries."}}
  while(i<text.length){val ch=text[i];if(quoted){if(ch=='"'){if(i+1<text.length && text[i+1]=='"'){field.append('"');i++}else{quoted=false;endedQuote=true}}else field.append(ch)}else when(ch){'"'->{require(field.isEmpty() && !endedQuote){"Check CSV quotes."};quoted=true};','->cell();'\n'->line();'\r'->{line();if(i+1<text.length && text[i+1]=='\n')i++};else->{require(!endedQuote){"Check text after CSV quotes."};field.append(ch)}};i++}
  require(!quoted){"A quoted field is unfinished."};if(field.isNotEmpty()||row.isNotEmpty()||endedQuote)line();return rows
 }
}
