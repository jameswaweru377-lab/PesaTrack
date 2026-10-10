package com.kedevjimmy.pesatrack.domain.everyday
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class EverydayMoneyPolicyTest {
 @Test fun exactMoneyRejectsFractionsOverflowAndScientificNotation() {
  assertEquals(120050L, EverydayMoneyPolicy.money("1,200.50"))
  assertNull(EverydayMoneyPolicy.money("1.001")); assertNull(EverydayMoneyPolicy.money("1e3"))
  assertNull(EverydayMoneyPolicy.money("92233720368547758.08"))
 }
 @Test fun splitMustEqualPrincipalAndUseDistinctCategories() {
  assertTrue(EverydayMoneyPolicy.validSplit(100, listOf(MoneyPart("food",60),MoneyPart("home",40))))
  assertFalse(EverydayMoneyPolicy.validSplit(100,listOf(MoneyPart("food",60),MoneyPart("home",41))))
  assertFalse(EverydayMoneyPolicy.validSplit(100,listOf(MoneyPart("food",50),MoneyPart("food",50))))
 }
 @Test fun refundRoundingPreservesEveryCent() {
  val parts=listOf(MoneyPart("a",1),MoneyPart("b",1),MoneyPart("c",1))
  val refund=EverydayMoneyPolicy.apportion(2,parts)
  assertEquals(2L,refund.sumOf { it.minor });assertEquals(listOf(1L,1L,0L),refund.map { it.minor })
 }
 @Test fun apportionDoesNotOverflowIntermediateProducts() {
  val parts=listOf(MoneyPart("a",Long.MAX_VALUE-1),MoneyPart("b",1))
  assertEquals(Long.MAX_VALUE,EverydayMoneyPolicy.apportion(Long.MAX_VALUE,parts).sumOf {it.minor})
 }
 @Test fun csvHandlesQuotesAndRejectsAmbiguousAmounts() {
  val rows=StatementCsv.parse("date,description,amount,reference\n2026-10-01,\"Shop, Nairobi\",-120.50,ABC\n")
  assertEquals(1,rows.size);assertEquals(-12050L,rows[0].signedMinor);assertEquals("Shop, Nairobi",rows[0].description)
  assertThrows(IllegalArgumentException::class.java) { StatementCsv.parse("date,description,debit,credit\n2026-10-01,Shop,10,20") }
 }
 @Test fun csvDoesNotSilentlyDropInvalidDatesOrRows() {
  assertThrows(IllegalArgumentException::class.java) { StatementCsv.parse("date,description,amount\n2026-02-30,Shop,10") }
  assertThrows(IllegalArgumentException::class.java) { StatementCsv.parse("date,description,amount\n01/10/2026,Shop,1.001") }
  assertThrows(IllegalArgumentException::class.java) { StatementCsv.parse("date,description,amount\n01/10/2026,Shop,0") }
 }
 @Test fun quotedNewlineAndBomWork() {
  assertEquals("Shop\nNairobi",StatementCsv.parse("\uFEFFdate,description,amount\r\n01/10/2026,\"Shop\nNairobi\",10\r\n")[0].description)
 }
 private fun at(y:Int,m:Int,d:Int)=Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {clear();set(y,m-1,d)}.timeInMillis
 @Test fun paydayClampsShortMonthsWithoutDrifting() {
  val z=TimeZone.getTimeZone("Africa/Nairobi")
  val p=BudgetPeriods.containing(at(2026,3,1),BudgetCadence.PAYDAY,31,z)
  assertEquals(at(2026,2,28),p.start);assertEquals(at(2026,3,31),p.end)
 }
 @Test fun weeklyAnchorAndEndExclusive() {
  val z=TimeZone.getTimeZone("Africa/Nairobi")
  val p=BudgetPeriods.containing(at(2026,10,9),BudgetCadence.WEEKLY,Calendar.MONDAY,z)
  assertEquals(at(2026,10,5),p.start);assertEquals(at(2026,10,12),p.end)
 }
 @Test fun carryCannotBecomeNegativeOrOverflow() {
  assertEquals(30L,BudgetPeriods.carryNext(100,20,90))
  assertEquals(0L,BudgetPeriods.carryNext(100,0,150))
  assertThrows(ArithmeticException::class.java) {BudgetPeriods.carryNext(Long.MAX_VALUE,1,0)}
 }
}
