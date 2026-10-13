package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedRows
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementGeometry
import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxTableReview
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementDeclarations
import com.kedevjimmy.pesatrack.domain.statements.StatementTableWire
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/**
 * Entirely invented PDFBox word coordinates, dates and transaction values.
 * These tests cannot replace a private-device PDF acceptance run.
 */
class PdfBoxStatementGeometryTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val heading="""
        M-PESA STATEMENT
        Statement Period: 01 Oct 2026 - 11 Oct 2026
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val first="QAB1234569 2026-10-10 10:30:00 Shop purchase Completed -20.00 490.00"
    private val second="QAB1234568 2026-10-09 10:30:00 Reversal via API Completed 100.00 510.00"
    private val oldest="QAB1234567 2026-10-08 10:30:00 Transport Completed -50.00 410.00"

    private fun word(value:String,x:Float,y:Float):PdfTextFragment =
        PdfTextFragment(value,x,y,x+(value.length*3.0f),y+8f)
    private fun textRow(code:String,day:String,time:String,description:String,
                        movement:String,balance:String,y:Float):List<PdfTextFragment> = listOf(
        word(code,35f,y),word(day,99f,y),word(time,166f,y),
        word(description,221f,y),word("Completed",330f,y),
        word(movement,if(movement.startsWith("-"))465f else 406f,y),word(balance,522f,y)
    )
    private val headerGeometry=visualStatementHeadings()
    private fun geometry():List<PdfTextFragment> = headerGeometry+
        textRow("QAB1234569","2026-10-10","10:30:00","Shop purchase","-20.00","490.00",100f)+
        textRow("QAB1234568","2026-10-09","10:30:00","Reversal via API","100.00","510.00",140f)+
        listOf(word("from",221f,152f),word("-",241f,152f),word("071***123456",248f,152f))+
        textRow("QAB1234567","2026-10-08","10:30:00","Transport","-50.00","410.00",190f)

    /**
     * PDFBox can reconstruct top-of-page display headings from positioned
     * words even when its text-stream prefix omits those headings. Header
     * evidence must come from original PDF glyphs, never a guessed KES string.
     */
    private fun visualStatementHeadings():List<PdfTextFragment> = listOf(
        word("M-PESA",30f,8f),word("STATEMENT",72f,8f),
        word("Statement",30f,20f),word("Period:",89f,20f),
        word("01",135f,20f),word("Oct",149f,20f),word("2026",168f,20f),
        word("-",199f,20f),word("11",213f,20f),word("Oct",227f,20f),
        word("2026",250f,20f),
        word("DETAILED",30f,35f),word("STATEMENT",87f,35f),
        word("Receipt",30f,60f),word("No.",72f,60f),
        word("Completion",97f,60f),word("Time",168f,60f),
        word("Details",221f,60f),word("Transaction",330f,48f),
        word("Status",330f,60f),word("Paid",400f,60f),
        word("In",416f,60f),word("Withdrawn",444f,60f),
        word("Balance",516f,60f)
    )

    private fun recoveredLayout(withTitle:Boolean = true):List<PdfTextFragment> =
        visualStatementHeadings().filter {withTitle || it.top!=8f} +
            geometry().drop(headerGeometry.size)

    private fun refused(words:List<PdfTextFragment>,source:String=heading+first+"\n"+second+"\n"+oldest)=
        PdfBoxStatementGeometry.reconstruct(listOf(source),listOf(words),listOf(true)) as MpesaPositionedRows.Unsupported

    @Test fun rowRefusalsIdentifyTheFailingCheckWithoutValues() {
        assertEquals(PdfStatementCheck.TIMESTAMP_CELL,refused(geometry().map {
            if(it.top==100f && it.text=="2026-10-10")it.copy(text="2026-10-11") else it}).check)
        assertEquals(PdfStatementCheck.SOURCE_TIMESTAMP_COUNT,refused(geometry(),
            heading+first.replace("Shop purchase","Shop purchase at 09:15:00")+"\n"+second+"\n"+oldest).check)
        assertEquals(PdfStatementCheck.STATUS_CELL,refused(geometry().map {
            if(it.top==100f && it.text=="Completed")it.copy(text="Pending") else it}).check)
        assertEquals(PdfStatementCheck.DESCRIPTION_LENGTH,refused(geometry().map {
            if(it.top==100f && it.text=="Shop purchase")it.copy(text="x".repeat(201)) else it}).check)
        assertEquals(PdfStatementCheck.CREDIT_PARTS,refused(geometry()+
            word("0.00",406f,112f)+word("0.00",406f,124f)).check)
        assertEquals(PdfStatementCheck.DEBIT_PARTS,refused(geometry()+word("-1.00",465f,112f)).check)
        assertEquals(PdfStatementCheck.BALANCE_AMOUNT,refused(geometry().map {
            if(it.top==100f && it.text=="490.00")it.copy(text="??") else it}).check)
        assertEquals(PdfStatementCheck.MOVEMENT_DIRECTION,refused(geometry()+word("10.00",406f,100f)).check)
    }

    @Test fun crossingColumnsReportOnlySemanticRolesAndSourceLocation() {
        val output=refused(geometry().map {if(it.text=="Shop purchase")it.copy(right=340f) else it})
        assertEquals(PdfStatementCheck.COLUMN_OWNERSHIP,output.check)
        assertEquals(setOf(com.kedevjimmy.pesatrack.domain.statements.StatementField.DESCRIPTION,
            com.kedevjimmy.pesatrack.domain.statements.StatementField.STATUS),output.columns)
        assertEquals(1,output.page)
        assertEquals(1,output.row)
        val message=PdfReadException(PdfFailure.TABLE_EVIDENCE,output.page,output.row,output.check,output.columns).safeExplanation
        assertTrue(message.contains("Description / Status"))
        for(privateText in listOf("Shop purchase","QAB1234569","-20.00","490.00","2026-10-10"))
            assertFalse(message.contains(privateText))
    }

    @Test fun structuredProductionReviewConsumesCellsWithoutLegacyTextRoundTrip() {
        val output=PdfBoxStatementGeometry.reconstruct(listOf(heading+first+"\n"+second+"\n"+oldest),
            listOf(geometry()),listOf(true)) as MpesaPositionedRows.Ready
        val proof=output.pdfBoxProof!!
        val report=PdfBoxVerificationReport(proof.receipts.size,3,proof.verifiedTransitions,
            proof.totalsVerified,proof.openingVerified,proof.closingVerified)
        val tables=StatementTableWire.decode(StatementTableWire.encode(output.tables))
        val review=PdfBoxTableReview.read(tables,report,
            PdfStatementReview.reviewMetadata(output.pages,true).currencyReview,zone)
        assertEquals(review.problems.joinToString(),3,review.rows.size)
        assertEquals(-2000L,review.rows.first().signedMinor)
        assertTrue(review.rows[1].description.contains("071***123456"))
        assertEquals(output.pdfBoxProof!!.receipts.first().cells.values.flatten().toSet(),
            output.tables.first().cellEvidence.first().flatten().toSet())
        val truncated=PdfBoxTableReview.read(tables.map {it.copy(rows=it.rows.dropLast(1))},report,null,zone)
        assertTrue(truncated.rows.isEmpty())
        assertTrue(truncated.problems.isNotEmpty())
    }

    @Test fun changedMoneyCellOrProofCountBlocksWholeStructuredReview() {
        val output=PdfBoxStatementGeometry.reconstruct(listOf(heading+first+"\n"+second+"\n"+oldest),
            listOf(geometry()),listOf(true)) as MpesaPositionedRows.Ready
        val table=output.tables.single()
        val debit=table.headers.indexOf("Withdrawn")
        val changed=table.rows.first().toMutableList().also {it[debit]="-21.00"}
        val report=PdfBoxVerificationReport(3,3,2,false,false,false)
        assertTrue(PdfBoxTableReview.read(listOf(table.copy(rows=listOf(changed)+table.rows.drop(1))),
            report,null,zone).rows.isEmpty())
        assertTrue(PdfBoxTableReview.read(output.tables,report.copy(cashRows=2),null,zone).rows.isEmpty())
    }

    @Test fun singleEventRetainsObservedOpeningProofAfterTransfer() {
        val output=PdfBoxStatementGeometry.reconstruct(listOf(heading+first+"\n"+second+"\n"+oldest),
            listOf(geometry()),listOf(true)) as MpesaPositionedRows.Ready
        val table=output.tables.single().copy(rows=listOf(output.tables.single().rows.first()),cellEvidence=emptyList())
        val report=PdfBoxVerificationReport(1,1,0,true,true,true)
        val declarations=PdfBoxStatementDeclarations(51000,49000,0,2000)
        val review=PdfBoxTableReview.read(listOf(table),report,null,zone,declarations=declarations)
        assertEquals(review.problems.joinToString(),1,review.rows.size)
        assertTrue(PdfBoxTableReview.read(listOf(table),report,null,zone).rows.isEmpty())
    }

    @Test fun oldestAmountAndBalanceShiftCannotEvadeSummaryAndBoundaryProof() {
        val output=PdfBoxStatementGeometry.reconstruct(listOf(heading+first+"\n"+second+"\n"+oldest),
            listOf(geometry()),listOf(true)) as MpesaPositionedRows.Ready
        val table=output.tables.single()
        val report=PdfBoxVerificationReport(3,3,2,true,true,true)
        val declarations=PdfBoxStatementDeclarations(46000,49000,10000,7000)
        assertEquals(3,PdfBoxTableReview.read(listOf(table),report,null,zone,declarations=declarations).rows.size)
        val debit=table.headers.indexOf("Withdrawn")
        val changed=table.rows.last().toMutableList().also {it[debit]="-500.00"}
        assertTrue(PdfBoxTableReview.read(listOf(table.copy(rows=table.rows.dropLast(1)+listOf(changed))),
            report,null,zone,declarations=declarations).rows.isEmpty())
        val balance=table.headers.indexOf("Balance")
        val shifted=table.rows.mapIndexed {i,row->row.toMutableList().also {
            it[balance]=listOf("590.00","610.00","510.00")[i]}}
        assertTrue(PdfBoxTableReview.read(listOf(table.copy(rows=shifted)),report,null,zone,
            declarations=declarations).rows.isEmpty())
        assertTrue(PdfBoxTableReview.read(listOf(table),report.copy(totalsVerified=false),null,zone,
            declarations=declarations).rows.isEmpty())
    }

    @Test fun declarationTransportIsBoundedAndLossless() {
        val declarations=PdfBoxStatementDeclarations(46000,49000,10000,null)
        assertEquals(declarations,PdfBoxStatementDeclarations.decode(declarations.encode()))
        for(values in listOf(longArrayOf(1,2),longArrayOf(-2,0,0,0))) {
            assertTrue(runCatching {PdfBoxStatementDeclarations.decode(values)}.isFailure)
        }
    }

    @Test fun productionTablesDoNotNeedCompatibilityTransactionProse() {
        val output=PdfBoxStatementGeometry.reconstruct(listOf(heading+first+"\n"+second+"\n"+oldest),
            listOf(geometry()),listOf(true),emitCompatibilityText=false) as MpesaPositionedRows.Ready
        assertFalse(output.pages.joinToString().contains("QAB1234569"))
        assertTrue(PdfStatementReview.reviewMetadata(output.pages,true).problems.isEmpty())
        val proof=output.pdfBoxProof!!
        val review=PdfBoxTableReview.read(output.tables,PdfBoxVerificationReport(3,3,2,false,false,false),
            null,zone,declarations=proof.declarations)
        assertEquals(3,review.rows.size)
    }

    @Test fun physicallyPresentHeadersVerifyCurrencyEvenIfPdfBoxTextStreamPrefixIsIncomplete() {
        // The original source prefix is intentionally missing several
        // headings. The PDFBox glyph geometry contains all of them.
        val incompletePrefix="MOBILE MONEY REPORT\n"
        val input=incompletePrefix+first+"\n"+second+"\n"+
            "from - 071***123456\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout()),listOf(true)
        )
        assertTrue(output.toString(),output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertEquals(verified.problems.joinToString(),3,verified.rows.size)
        assertNotNull(verified.currencyReview)
        assertEquals(2,output.pdfBoxProof!!.verifiedTransitions)
    }

    @Test fun completePdfBoxGeometryCanReplaceOnlyMissingDecorativeDetailedHeading() {
        val noSection=heading.replace("DETAILED STATEMENT\n","")
        val input=noSection+first+"\n"+second+"\n"+oldest
        val positioned=recoveredLayout().filterNot {it.top==35f}
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(positioned),listOf(true)
        )
        assertTrue(output.toString(),output is MpesaPositionedRows.Ready)
        val recovered=(output as MpesaPositionedRows.Ready).pages
        val legacy=PdfStatementReview.read(recovered,zone)
        assertTrue(legacy.rows.isEmpty())
        assertTrue(legacy.problems.any {it.contains("detailed statement section")})
        // Only the proven PDFBox path may offer candidates without the
        // decorative heading; the user still confirms source/KES and balances.
        val review=PdfStatementReview.read(
            recovered,zone,completePdfBoxGeometry=true
        )
        assertEquals(review.problems.joinToString(),3,review.rows.size)
        assertNotNull(review.currencyReview)
        assertTrue(review.currencyReview!!.contains("does NOT establish currency"))
        assertEquals(2,output.pdfBoxProof!!.verifiedTransitions)
    }

    @Test fun completePdfBoxProofDoesNotReplaceStatementTitle() {
        val noSection="MOBILE MONEY REPORT\n" +
            heading.replace("M-PESA STATEMENT\n","")
                .replace("DETAILED STATEMENT\n","") +
            first+"\n"+second+"\n"+oldest
        val positioned=recoveredLayout(withTitle=false).filterNot {it.top==35f}
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(noSection),listOf(positioned),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val review=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone,
            completePdfBoxGeometry=true
        )
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any {it.contains("M-Pesa statement title")})
    }

    @Test fun completePdfBoxProofCannotReplaceStatementPeriodOrMoneyHeaders() {
        val withoutPeriod=heading.replace("Statement Period: 01 Oct 2026 - 11 Oct 2026\n","")
            .replace("DETAILED STATEMENT\n","") +
            first+"\n"+second+"\n"+oldest
        val positioned=recoveredLayout().filterNot {
            it.top==20f || it.top==35f
        }
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(withoutPeriod),listOf(positioned),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val review=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone,
            completePdfBoxGeometry=true
        )
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any {it.contains("statement period")})

        val withoutMoneyHeaders=heading
            .replace("DETAILED STATEMENT\n","")
            .replace("Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n","")+
            first+"\n"+second+"\n"+oldest
        val noHeaders=recoveredLayout().filterNot {it.top==35f || it.top==60f}
        val normalized=PdfBoxStatementGeometry.reconstruct(
            listOf(withoutMoneyHeaders),listOf(noHeaders),listOf(true)
        )
        // Production PDFBox now refuses missing physical money headings
        // before a normalized document can reach the legacy review parser.
        assertTrue(normalized is MpesaPositionedRows.Unsupported)
    }

    @Test fun foreignCurrencyCannotBeAcceptedByCompletePdfBoxGeometryException() {
        val input="Currency: USD\n" + heading.replace("DETAILED STATEMENT\n","")+
            first+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),
            listOf(recoveredLayout().filterNot {it.top==35f}),
            listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Unsupported)
    }

    @Test fun missingOriginalTitleStillCannotAuthorizeInferredKes() {
        val input="MOBILE MONEY REPORT\n"+first+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout(withTitle=false)),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(verified.rows.isEmpty())
        assertTrue(verified.problems.any {it.contains("KES could not be verified")})
        assertTrue(verified.problems.any {it.contains("Missing statement headings: M-Pesa statement title")})
        assertFalse(verified.problems.joinToString().contains("QAB1234569"))
    }

    @Test fun explicitForeignCurrencyCannotBeHiddenByRecoveredKenyanHeaders() {
        val input="Currency: USD\n"+first+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout()),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Unsupported)
    }

    @Test fun literalStatementBrandInsideTransactionDoesNotInventDocumentTitle() {
        val input="MOBILE MONEY REPORT\n"+
            first.replace("Shop purchase","M-PESA STATEMENT")+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout(withTitle=false).map {
                if(it.text=="Shop purchase") it.copy(text="M-PESA STATEMENT") else it
            }),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(verified.rows.isEmpty())
    }

    @Test fun wrappedMaskedAccountInDetailsCannotBecomeAPaidInColumn() {
        val original=heading+first+"\n"+second+"\nfrom - 071***123456\n"+oldest
        val normalized=PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(true)
        )
        assertTrue(normalized.toString(),normalized is MpesaPositionedRows.Ready)
        val candidates=PdfStatementReview.read(
            (normalized as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(candidates.problems.joinToString(),candidates.problems.isEmpty())
        assertEquals(3,candidates.rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),candidates.rows.map {it.signedMinor})
        assertTrue(candidates.rows[1].description.contains("071***123456"))
        assertEquals(2,normalized.pdfBoxProof!!.verifiedTransitions)
    }

    @Test fun unknownMoneyToTheRightOfCompletedRejectsEntireDocument() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val moved=geometry()+word("70.00",467f,140f)
        assertTrue(
            PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(moved),listOf(true))
                is MpesaPositionedRows.Unsupported
        )
    }

    @Test fun negativeAmountInPaidInCannotBeReinterpretedAsWithdrawal() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val wrong=geometry().map {
            if(it.text=="-20.00" && it.top==100f)word(it.text,406f,it.top) else it
        }
        assertTrue("Sign cannot override physical Paid In ownership",
            PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(wrong),listOf(true))
                is MpesaPositionedRows.Unsupported)
    }

    @Test fun missingPositionedMoneyHeadingCannotAuthorizeColumnOwnership() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val wrong=geometry().filterNot {it.text=="Withdrawn"}
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(wrong),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun positiveWithdrawalIsDebitBecauseOfItsColumn() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val positive=geometry().map {
            if(it.text=="-20.00")it.copy(text="20.00") else it
        }
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(positive),listOf(true))
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        val review=PdfStatementReview.read((result as MpesaPositionedRows.Ready).pages,zone)
        assertEquals(review.problems.joinToString(),-2000L,review.rows.first().signedMinor)
    }

    @Test fun fullyPopulatedMoneyCellsStillRequireRunningBalanceProof() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val broken=geometry().map {
            if(it.text=="490.00")it.copy(text="491.00") else it
        }
        assertTrue("Every row shape needs balance verification",
            PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(broken),listOf(true))
                is MpesaPositionedRows.Unsupported)
    }

    @Test fun explicitSummaryTotalsCannotBeIgnored() {
        val original="Total Paid In: 999.00\nTotal Withdrawn: 70.00\n"+
            heading+first+"\n"+second+"\n"+oldest
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun sourceReceiptIdentityMustAgreeWithPhysicalReceipt() {
        val original=heading+first.replace("QAB1234569","QAB9999999")+"\n"+second+"\n"+oldest
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun pageFootersCannotHideAnotherReceipt() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val boxes=geometry()+word("DISCLAIMER",20f,155f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(boxes),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun declaredTotalsAfterFooterCannotBeDroppedFromFinancialAudit() {
        val original=heading+first+"\n"+second+"\n"+oldest+
            "\nDISCLAIMER\nTotal Paid In: 101.00\nTotal Withdrawn: 70.00"
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),
            listOf(geometry()+word("DISCLAIMER",30f,700f)),listOf(true))
        assertTrue(result is MpesaPositionedRows.Unsupported)
    }

    @Test fun ordinaryLeftAlignedDisclaimerIsNotAnotherReceipt() {
        val original=heading+first+"\n"+second+"\n"+oldest+"\nDISCLAIMER"
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),
            listOf(geometry()+word("DISCLAIMER",20f,700f)),listOf(true))
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        assertEquals(3,(result as MpesaPositionedRows.Ready).pdfBoxProof!!.receipts.size)
    }

    @Test fun verifiedTotalsAndBalancesKeepTheirProofAndOriginalMoneyRectangles() {
        val summary="Total Paid In: 100.00\nTotal Withdrawn: 70.00\n"+
            "Opening Balance: 460.00\nClosing Balance: 490.00\n"
        val result=PdfBoxStatementGeometry.reconstruct(
            listOf(summary+heading+first+"\n"+second+"\n"+oldest),listOf(geometry()),listOf(true)
        )
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        val proof=(result as MpesaPositionedRows.Ready).pdfBoxProof!!
        assertTrue(proof.totalsVerified && proof.openingVerified && proof.closingVerified)
        assertEquals(3,proof.receipts.size)
        assertEquals(465f,proof.receipts.first().cells.getValue(
            com.kedevjimmy.pesatrack.domain.statements.StatementField.DEBIT).single().left)
    }

    @Test fun physicalSummaryTableTotalsAreComparedWithAllReceiptLegs() {
        val cover="SUMMARY\nTransaction Type Paid In Withdrawn\nTOTAL 100.00 70.00\n"
        fun coverBoxes(total:String)=listOf(word("SUMMARY",35f,20f),
            word("Transaction Type",35f,45f),word("Paid In",400f,45f),
            word("Withdrawn",459f,45f),word("TOTAL",35f,70f),
            word(total,406f,70f),word("70.00",465f,70f))
        val page=heading+first+"\n"+second+"\n"+oldest
        val valid=PdfBoxStatementGeometry.reconstruct(listOf(cover,page),
            listOf(coverBoxes("100.00"),geometry()),listOf(true,true))
        assertTrue(valid.toString(),valid is MpesaPositionedRows.Ready)
        assertTrue((valid as MpesaPositionedRows.Ready).pdfBoxProof!!.totalsVerified)
        val invalid=PdfBoxStatementGeometry.reconstruct(listOf(cover.replace("100.00","101.00"),page),
            listOf(coverBoxes("101.00"),geometry()),listOf(true,true))
        assertTrue(invalid is MpesaPositionedRows.Unsupported)
    }

    @Test fun repeatedPageHeaderDoesNotBecomeWrappedDescriptionOrMoney() {
        val original=heading+first+"\n"+heading+second+"\n"+oldest
        val repeated=geometry()+visualStatementHeadings().filter {it.top>=48f}
            .map {it.copy(top=it.top+65f,bottom=it.bottom+65f)}
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(repeated),listOf(true))
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        assertEquals(3,(result as MpesaPositionedRows.Ready).pdfBoxProof!!.receipts.size)
    }

    @Test fun swappedPaidInWithdrawnColumnsAreNormalizedFromEvidenceOnThatPage() {
        val original=heading.replace("Paid In Withdrawn","Withdrawn Paid In")+
            first+"\n"+second+"\n"+oldest
        val shifted=geometry().map {
            when(it.text) {
                "Paid"->word("Paid",459f,it.top)
                "In"->word("In",475f,it.top)
                "Withdrawn"->word("Withdrawn",400f,it.top)
                "-20.00","-50.00"->word(it.text,406f,it.top)
                "100.00"->word(it.text,465f,it.top)
                else->it
            }
        }
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(shifted),listOf(true))
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        val review=PdfStatementReview.read((result as MpesaPositionedRows.Ready).pages,zone)
        assertEquals(review.problems.joinToString(),listOf(-2000L,10000L,-5000L),
            review.rows.map {it.signedMinor})
    }

    @Test fun oneReceiptWithoutOpeningBalanceCannotOfferFinancialProof() {
        val result=PdfBoxStatementGeometry.reconstruct(listOf(heading+first),
            listOf(headerGeometry+textRow("QAB1234569","2026-10-10","10:30:00",
                "Shop purchase","-20.00","490.00",100f)),listOf(true))
        assertTrue(result is MpesaPositionedRows.Unsupported)
    }

    @Test fun completionDateAndTimeCanWrapInsideTheirPhysicalColumn() {
        val original=heading+listOf(first,second,oldest).joinToString("\n") {
            it.replace(" 10:30:00","")+"\n10:30:00"
        }
        val wrapped=geometry().map {
            if(it.text=="10:30:00")it.copy(left=99f,right=123f,top=it.top+9f,bottom=it.bottom+9f) else it
        }
        val result=PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(wrapped),listOf(true))
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        assertEquals(3,(result as MpesaPositionedRows.Ready).pdfBoxProof!!.receipts.size)
    }

    @Test fun syntheticTwentySixPagesAccountForAll881RowsAndExactTotals() {
        val pages=mutableListOf<String>()
        val coordinates=mutableListOf<List<PdfTextFragment>>()
        var index=0
        repeat(26) {page ->
            val text=mutableListOf<String>()
            val boxes=headerGeometry.toMutableList()
            repeat(if(page==25)31 else 34) {row ->
                val code=String.format(java.util.Locale.ROOT,"T%09d",index)
                val seconds=36000+880-index
                val time=String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",
                    seconds/3600,seconds/60%60,seconds%60)
                val balance="${120+index}.00"
                text.add("$code 2026-10-10 $time Synthetic withdrawal Completed -1.00 $balance")
                boxes.addAll(textRow(code,"2026-10-10",time,"Synthetic withdrawal",
                    "-1.00",balance,100f+row*16f))
                index++
            }
            val summary=if(page==0)"Total Paid In: 0.00\nTotal Withdrawn: 881.00\n"+
                "Opening Balance: 1001.00\nClosing Balance: 120.00\n" else ""
            pages.add(summary+heading+text.joinToString("\n"));coordinates.add(boxes)
        }
        val result=PdfBoxStatementGeometry.reconstruct(pages,coordinates,List(26){true})
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        val proof=(result as MpesaPositionedRows.Ready).pdfBoxProof!!
        assertEquals(881,proof.receipts.size)
        assertEquals(880,proof.verifiedTransitions)
        assertTrue(proof.totalsVerified && proof.openingVerified && proof.closingVerified)
        val review=PdfStatementReview.read(result.pages,zone)
        assertEquals(review.problems.joinToString(),881,review.rows.size)
    }

    @Test fun missingStatusOrUnknownNumericFinancialFragmentCannotCreateCash() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val dropped=geometry().filterNot {it.text=="Completed" && it.top==140f}
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(dropped),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
        val bad=geometry()+word("45.00",365f,152f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(bad),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun missingOrInvalidGeometryAndMissingReceiptRejectAllPages() {
        val original=heading+first+"\n"+second+"\n"+oldest
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(false)
        ) is MpesaPositionedRows.Unsupported)
        val countMismatch=original+"\nQAB1234500 2026-10-07 10:30:00 Unknown Completed -10.00 400.00"
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(countMismatch),listOf(geometry()),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
        val invalid=geometry()+PdfTextFragment("Unsupported",Float.NaN,24f,120f,32f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(invalid),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun secondPageFailurePreventsFirstPageFinancialReview() {
        val page1=heading+first
        val page2=heading+second
        val boxes1=headerGeometry+textRow(
            "QAB1234569","2026-10-10","10:30:00","Shop purchase","-20.00","490.00",100f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(page1,page2),listOf(boxes1,emptyList()),listOf(true,false)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun repeatedReceiptMayContainTwoDistinctStatementMovements() {
        val fee="QAB1234569 2026-10-10 10:30:00 Service fee Completed -5.00 495.00"
        val payment="QAB1234569 2026-10-10 10:30:00 Shop purchase Completed -20.00 485.00"
        val page=heading+payment+"\n"+fee+"\n"+second+"\n"+oldest
        val paymentBoxes=geometry().map {
            if(it.text=="490.00" && it.top==100f)word("485.00",522f,100f)
            else it
        }
        val boxes=paymentBoxes+textRow(
            "QAB1234569","2026-10-10","10:30:00","Service fee","-5.00","490.00",118f)
        val reconstructed=PdfBoxStatementGeometry.reconstruct(
            listOf(page),listOf(boxes),listOf(true)
        )
        assertTrue(reconstructed.toString(),reconstructed is MpesaPositionedRows.Ready)
        val candidates=PdfStatementReview.read(
            (reconstructed as MpesaPositionedRows.Ready).pages,zone
        )
        assertEquals(candidates.problems.joinToString(),4,candidates.rows.size)
        assertEquals(2,candidates.rows.count {it.reference=="QAB1234569"})
    }
}
