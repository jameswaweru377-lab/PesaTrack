package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatementBankHistoryPolicyTest {
    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
            set(year, month - 1, day, 13, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun leg(source: String, day: Int, movement: Long, account: String = "mine") =
        StatementBankHistoryPolicy.SourceLeg(source, account, "KENYAN_BANK_EQUITY",
            date(2026, 9, 30), date(2026, 9, day), movement)

    @Test fun neverAddsTheSameMonthFromOverlappingDocuments() {
        val month = StatementBankHistoryPolicy.selectForAccount(listOf(
            leg("first", 1, 10_000), leg("first", 3, -2_000),
            leg("second", 1, 10_000), leg("second", 3, -2_000), leg("second", 5, -1_000)
        ), "mine").single()
        assertEquals("2026-09", month.key)
        assertEquals("second", month.sourceId)
        assertEquals(1, month.alternativeSourceCount)
        assertEquals(10_000L, month.moneyInMinor)
        assertEquals(3_000L, month.moneyOutMinor)
        assertTrue(month.containsOverlappingImports)
        assertTrue(month.classificationUnverified)
    }

    @Test fun anotherAccountAndUnimportedMonthsNeverAppearAsZero() {
        val months = StatementBankHistoryPolicy.selectForAccount(listOf(
            leg("mine-file", 2, -2_000), leg("other-file", 2, 99_000, "other")
        ), "mine")
        assertEquals(1, months.size)
        assertEquals(2_000L, months.single().moneyOutMinor)
        assertEquals(0L, months.single().moneyInMinor)
        assertFalse(months.single().containsOverlappingImports)
    }
}
