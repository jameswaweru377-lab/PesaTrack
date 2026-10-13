package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementTableWireTest {
    private val table=StatementTablePage(2,listOf("Date","Description","Amount"),
        listOf(listOf("2026-01-01","Invented : shop\nwrapped, \"label\"","-10.00")))
    @Test fun roundTripPreservesCellsAndPhysicalPageWithoutFlattening() {
        assertEquals(listOf(table),StatementTableWire.decode(StatementTableWire.encode(listOf(table))))
    }
    @Test fun partialOrMalformedPayloadNeverExposesPartialTables() {
        val good=StatementTableWire.encode(listOf(table))
        for(bad in listOf(good.dropLast(1),good+"trailing",good.replace("PESA_TABLE_1","PESA_TABLE_2"),
            "PESA_TABLE_1:9999:ab", "PESA_TABLE_1:1:0")) {
            assertTrue(runCatching {StatementTableWire.decode(bad)}.isFailure)
        }
    }
    @Test fun cellsAndTotalRowsAreBoundedBeforeAllocation() {
        assertTrue(runCatching {StatementTableWire.encode(listOf(table.copy(
            rows=listOf(listOf("date","a".repeat(1201),"money")))))}.isFailure)
        assertTrue(runCatching {StatementTableWire.encode(List(5001){table})}.isFailure)
    }
    @Test fun largeStatementRetainsAll881RowsAcross26Pages() {
        val tables=(1..26).map {page->table.copy(pageNumber=page,
            rows=List(if(page==26)31 else 34){table.rows.single()})}
        val restored=StatementTableWire.decode(StatementTableWire.encode(tables))
        assertEquals(881,restored.sumOf {it.rows.size})
        assertEquals(26,restored.last().pageNumber)
    }
}
