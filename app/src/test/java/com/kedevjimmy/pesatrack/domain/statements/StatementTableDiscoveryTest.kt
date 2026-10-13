package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import org.junit.Assert.*
import org.junit.Test

class StatementTableDiscoveryTest {
    private fun box(text:String,x:Float,y:Float,width:Float=40f)=
        PdfTextFragment(text,x,y,x+width,y+8f)
    private fun header(y:Float=50f)=listOf(box("Date",10f,y),box("Narration",120f,y),
        box("Debit",300f,y),box("Credit",380f,y),box("Balance",460f,y))
    private fun row(y:Float=80f,date:String="2026-01-01")=listOf(
        box(date,10f,y,70f),box("Invented shop",120f,y,100f),
        box("10.00",300f,y),box("-",380f,y,5f),box("90.00",460f,y))
    private fun found(words:List<PdfTextFragment>):StatementTableDiscoveryResult.Found =
        StatementTableDiscovery.discover(1,words) as StatementTableDiscoveryResult.Found

    @Test fun discoversBorderlessTableAndRetainsEveryCellRectangle() {
        val table=found(listOf(box("Private name",10f,10f))+header()+row()).tables.single()
        assertEquals(listOf("Date","Narration","Debit","Credit","Balance"),table.headers)
        assertEquals("10.00",table.rows.single()[2])
        assertEquals(row()[2],table.cellEvidence.single()[2].single())
        assertFalse(table.rows.flatten().any {it.contains("Private name")})
    }
    @Test fun wrappedMaskedIdentityStaysInDescription() {
        val table=found(header()+row()+box("from 07***12345",120f,92f,110f)).tables.single()
        assertEquals("Invented shop from 07***12345",table.rows.single()[1])
        assertEquals("10.00",table.rows.single()[2])
    }
    @Test fun reorderedColumnsRemainInObservedOrder() {
        val words=listOf(box("Balance",10f,50f),box("Credit",120f,50f),
            box("Description",220f,50f,70f),box("Date",350f,50f),box("Debit",460f,50f),
            box("90.00",10f,80f),box("-",120f,80f,5f),box("Shop",220f,80f),
            box("2026-01-01",350f,80f,70f),box("10.00",460f,80f))
        assertEquals("Balance",found(words).tables.single().headers.first())
    }
    @Test fun repeatedHeadersCreateSeparateRegionsWithSameSourcePage() {
        val tables=found(header()+row()+header(130f)+row(160f,"2026-01-02")).tables
        assertEquals(2,tables.size)
        assertEquals(1,tables[1].pageNumber)
        assertEquals("2026-01-02",tables[1].rows.single().first())
    }
    @Test fun splitHeadingUsesSharedSemanticAliases() {
        val heads=header().filterNot {it.text=="Debit"}+
            box("Paid",300f,50f,22f)+box("Out",325f,50f,20f)
        assertEquals("Paid Out",found(heads+row()).tables.single().headers[2])
    }
    @Test fun noTableIsExplicitFailure() {
        assertTrue(StatementTableDiscovery.discover(1,row()) is StatementTableDiscoveryResult.Blocked)
    }
    @Test fun unknownHeadingCannotBeSilentlyDropped() {
        assertTrue(StatementTableDiscovery.discover(1,header()+box("Unknown",550f,50f)+row())
            is StatementTableDiscoveryResult.Blocked)
    }
    @Test fun dateBeforeHeaderCannotBeSilentlyDropped() {
        assertTrue(StatementTableDiscovery.discover(1,row(20f)+header()+row())
            is StatementTableDiscoveryResult.Blocked)
    }
    @Test fun overflowingRectangleAndNonFiniteGeometryAreBlocked() {
        assertTrue(StatementTableDiscovery.discover(1,header()+row()+
            box("Unassigned",220f,90f,100f)) is StatementTableDiscoveryResult.Blocked)
        assertTrue(StatementTableDiscovery.discover(1,header()+row()+
            box("Bad",Float.NaN,90f)) is StatementTableDiscoveryResult.Blocked)
    }
    @Test fun trailingFooterIsNotGuessedAway() {
        val table=found(header()+row()+box("10.00",300f,120f)).tables.single()
        // Unknown numeric lines are retained. Interpretation must reject a
        // multi-number debit instead of reporting a falsely complete row.
        assertEquals("10.00 10.00",table.rows.single()[2])
    }

    @Test fun repeatedTableRegionsFeedTheExistingFinancialInterpreter() {
        val second=row(160f,"2026-01-02").map {if(it.text=="90.00")it.copy(text="80.00") else it}
        val tables=found(header()+row()+header(130f)+second).tables
        val accepted=UniversalStatementTableInterpreter.interpret(tables,openingBalanceMinor=10000)
            as StatementTableResult.Accepted
        assertEquals(2,accepted.sourceRows)
        assertEquals(listOf(-1000L,-1000L),accepted.legs.map {it.movementMinor})
    }

    @Test fun physicalAndCsvTablesShareCellsAndInterpretation() {
        val tables=found(header()+row()+row(110f,"2026-01-02").map {
            if(it.text=="90.00")it.copy(text="80.00") else it}).tables
        val csv=UniversalStatementCsv.decode("Date,Narration,Debit,Credit,Balance\n"+
            "2026-01-01,Invented shop,10.00,-,90.00\n2026-01-02,Invented shop,10.00,-,80.00")!!
        assertEquals(csv.rows,tables.single().rows)
        assertEquals(UniversalStatementTableInterpreter.interpret(listOf(csv),openingBalanceMinor=10000),
            UniversalStatementTableInterpreter.interpret(tables,openingBalanceMinor=10000))
    }

    @Test fun inconsistentProvenanceBlocksInterpretation() {
        val table=found(header()+row()).tables.single()
        val damaged=table.copy(cellEvidence=listOf(listOf(emptyList())))
        assertTrue(UniversalStatementTableInterpreter.interpret(listOf(damaged),openingBalanceMinor=10000)
            is StatementTableResult.Blocked)
    }

    @Test fun discovers881RowsAcross26PagesWithoutDroppingDescriptions() {
        val tables=(1..26).flatMap {page->
            val n=if(page==26)31 else 34
            val words=header()+(0 until n).flatMap {i->row(80f+i*18f)+
                box("from 07***12345",120f,90f+i*18f,110f)}
            (StatementTableDiscovery.discover(page,words) as StatementTableDiscoveryResult.Found).tables
        }
        assertEquals(881,tables.sumOf {it.rows.size})
        assertTrue(tables.flatMap {it.rows}.all {it[1]=="Invented shop from 07***12345"})
    }
}
