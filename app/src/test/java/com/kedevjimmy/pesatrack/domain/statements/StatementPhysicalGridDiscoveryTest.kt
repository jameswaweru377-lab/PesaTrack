package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfGridSegment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedRows
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementGeometry
import com.kedevjimmy.pesatrack.domain.evidence.PdfStatementCheck
import org.junit.Assert.*
import org.junit.Test

/** Invented grids and receipt values only. No owner's statement is stored. */
class StatementPhysicalGridDiscoveryTest {
    private val xs=listOf(20f,100f,200f,350f,425f,475f,535f,600f)
    private val roles=listOf("Receipt","No.","Completion","Time","Details",
        "Transaction","Status","Paid","In","Withdrawn","Balance")
    private fun word(value:String,x:Float,y:Float)=PdfTextFragment(
        value,x,y,x+value.length*2.1f,y+7f)

    private fun header(top:Float):List<PdfTextFragment> = listOf(
        word("Receipt",30f,top+4f),word("No.",65f,top+4f),
        word("Completion",110f,top+4f),word("Time",157f,top+4f),
        word("Details",215f,top+4f),
        word("Transaction",365f,top+1f),word("Status",365f,top+12f),
        word("Paid",436f,top+4f),word("In",457f,top+4f),
        word("Withdrawn",482f,top+4f),word("Balance",545f,top+4f))

    private fun row(ref:String,date:String,description:String,
        paid:String,debit:String,balance:String,y:Float,
        splitStatus:Boolean=false):List<PdfTextFragment> {
        val parts=mutableListOf(word(ref,27f,y),word(date.substringBefore(' '),109f,y),
            word(date.substringAfter(' '),160f,y),word(description,213f,y))
        if(splitStatus) {
            parts.add(word("Com",366f,y))
            parts.add(word("pleted",373f,y))
        } else parts.add(word("Completed",366f,y))
        if(paid.isNotEmpty())parts.add(word(paid,437f,y))
        if(debit.isNotEmpty())parts.add(word(debit,484f,y))
        parts.add(word(balance,548f,y))
        return parts
    }
    private fun frame(top:Float,rows:Int,segmented:Boolean=false):List<PdfGridSegment> {
        val bottom=top+34f+rows*30f
        val h=(0..rows+1).map {i->
            val y=top+if(i==0)0f else 34f+(i-1)*30f
            PdfGridSegment(xs.first(),y,xs.last(),y)
        }
        val v=xs.map {x->PdfGridSegment(x,top,x,bottom)}
        return if(segmented)(h+v).flatMap {s->
            listOf(PdfGridSegment(s.x1,s.y1,(s.x1+s.x2)/2f,(s.y1+s.y2)/2f),
                PdfGridSegment((s.x1+s.x2)/2f,(s.y1+s.y2)/2f,s.x2,s.y2))
        } else h+v
    }
    private fun discover(top:Float,rows:Int,words:List<PdfTextFragment>,
        segmented:Boolean=false)=StatementPhysicalGridDiscovery.discover(1,620f,800f,
        words,frame(top,rows,segmented))

    @Test fun identifiesTrueSevenColumnGridRatherThanHeaderMidpoints() {
        val top=45f
        val words=header(top)+row("QAB1234569","2026-10-10 10:30:00",
            "Shop purchase","","-20.00","490.00",top+43f)
        val found=discover(top,1,words)
        assertEquals(1,found.size)
        val grid=found.single()
        assertEquals(xs,grid.xEdges)
        assertEquals(StatementField.DESCRIPTION,grid.owner(words.first{it.text=="Shop purchase"}))
        assertEquals(StatementField.STATUS,grid.owner(words.first{it.text=="Completed"}))
        assertNull(grid.owner(word("Outside",607f,95f)))
    }

    @Test fun rowCellsHandleWrappedTextAndIndependentStatusPieces() {
        val top=45f
        val words=header(top)+
            row("QAB1234569","2026-10-10 10:30:00","Shop purchase",
                "","-20.00","490.00",top+43f,true)+
            word("at Kiosk",218f,top+56f)+
            row("QAB1234568","2026-10-09 10:30:00","Reversal via API",
                "100.00","","510.00",top+73f)+
            row("QAB1234567","2026-10-08 10:30:00","Transport",
                "","-50.00","410.00",top+103f)
        val source="M-PESA STATEMENT\nStatement Period: 01 Oct 2026 - 11 Oct 2026\n" +
            "DETAILED STATEMENT\n" +
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n" +
            "QAB1234569 2026-10-10 10:30:00 Shop purchase at Kiosk Completed -20.00 490.00\n" +
            "QAB1234568 2026-10-09 10:30:00 Reversal via API Completed 100.00 510.00\n" +
            "QAB1234567 2026-10-08 10:30:00 Transport Completed -50.00 410.00"
        val grid=discover(top,3,words,true).single()
        val reconstructed=PdfBoxStatementGeometry.reconstruct(
            listOf(source),listOf(words),listOf(true),gridTables=listOf(listOf(grid)))
        assertTrue(reconstructed.toString(),reconstructed is MpesaPositionedRows.Ready)
        val ready=reconstructed as MpesaPositionedRows.Ready
        assertEquals(3,ready.tables.single().rows.size)
        assertEquals("Shop purchase at Kiosk",ready.tables.single().rows.first()[2])
        assertEquals("Completed",ready.tables.single().rows.first()[3])
        assertEquals(-2000L,ready.pdfBoxProof!!.receipts.first().movementMinor)
    }

    @Test fun missingPhysicalBoundaryAndAmbiguousCellAlwaysRefuse() {
        val top=45f
        val words=header(top)+row("QAB1234569","2026-10-10 10:30:00",
            "Shop purchase","","-20.00","490.00",top+43f)
        val removed=frame(top,1).filterNot {it.x1==350f && it.x2==350f}
        assertTrue(StatementPhysicalGridDiscovery.discover(1,620f,800f,words,removed).isEmpty())
        val grid=discover(top,1,words).single()
        assertNull(grid.owner(word("Dangerous",342f,top+43f)))
        assertNull(grid.owner(word("-20.00",420f,top+43f)))
    }

    @Test fun bogusHeaderOrUnsupportedCurrencyColumnsCannotBecomeTable() {
        val top=45f
        val wrong=header(top).filterNot {it.text=="Withdrawn"}+
            word("Fee",483f,top+4f)
        assertTrue(discover(top,2,wrong).isEmpty())
    }
}
