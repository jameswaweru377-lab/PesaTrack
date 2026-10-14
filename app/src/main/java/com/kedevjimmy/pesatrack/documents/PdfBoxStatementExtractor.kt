package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.os.ParcelFileDescriptor
import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedRows
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementGeometry
import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import com.kedevjimmy.pesatrack.domain.statements.StatementPhysicalGridDiscovery
import com.kedevjimmy.pesatrack.domain.statements.StatementPhysicalGridTable
import com.kedevjimmy.pesatrack.domain.evidence.PdfGridSegment
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxWordBounds
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxVerificationReport
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

/**
 * User-opt-in PDFBox -> shared evidence tables -> financial review.
 *
 * Must be called only inside the existing isolated PDF service. The complete
 * document is checked before any text is returned; there is no ledger access,
 * remote processing, OCR, diagnostic logging or persistent PDF storage.
 */
internal object PdfBoxStatementExtractor {
    data class ReviewedDocument(val pages:List<String>,val verification:PdfBoxVerificationReport,
        val tables:List<StatementTablePage>,val currencyReview:String?,
        val declarations:com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementDeclarations)
    fun extract(context:Context,fd:ParcelFileDescriptor,password:String?):List<String> =
        extractForReview(context,fd,password,legacyText=true).pages

    fun extractForReview(context:Context,fd:ParcelFileDescriptor,password:String?,legacyText:Boolean=false):ReviewedDocument {
        PdfTextReviewPolicy.requireBytes(fd.statSize)
        PDFBoxResourceLoader.init(context.applicationContext)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(fd).use {input ->
                PDDocument.load(input,password.orEmpty()).use {document ->
                    val count=document.numberOfPages
                    if(count !in 1..PdfTextReviewPolicy.MAX_PAGES)
                        throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
                    val pages=mutableListOf<String>()
                    val boxes=mutableListOf<List<PdfTextFragment>>()
                    val complete=mutableListOf<Boolean>()
                    val physicalGrids=mutableListOf<List<StatementPhysicalGridTable>>()
                    var characters=0L
                    var totalWords=0
                    for(i in 0 until count) {
                        val page=document.getPage(i)
                        val crop=page.cropBox
                        val rotated=(page.rotation % 180 + 180) % 180 == 90
                        val width=if(rotated)crop.height else crop.width
                        val height=if(rotated)crop.width else crop.height
                        val stripper=PositionedWords(width,height)
                        stripper.setStartPage(i+1)
                        stripper.setEndPage(i+1)
                        stripper.setSortByPosition(true)
                        val text=stripper.getText(document)
                        characters+=text.length
                        totalWords+=stripper.words.size
                        if(characters>PdfTextReviewPolicy.MAX_CHARACTERS ||
                            totalWords>32_000)
                            throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                        pages.add(text)
                        boxes.add(stripper.words.toList())
                        complete.add(stripper.complete)
                        // PDF graphics use a bottom-left origin; PDFTextStripper's
                        // word bounds use top-left coordinates. Normalize inside
                        // the isolated process before touching grid discovery.
                        // Rotated layouts must not silently use wrong cells.
                        val layout=if((page.rotation%360+360)%360==0) {
                            val paths=PdfBoxGridProbe.capture(page)
                            val lines=(paths.stroked+paths.filledRules).map {segment->
                                PdfGridSegment(
                                    segment.x1-crop.lowerLeftX,crop.upperRightY-segment.y1,
                                    segment.x2-crop.lowerLeftX,crop.upperRightY-segment.y2
                                )
                            }
                            StatementPhysicalGridDiscovery.discover(
                                i+1,width,height,stripper.words,lines)
                        } else emptyList()
                        physicalGrids.add(layout)
                    }
                    PdfTextReviewPolicy.preview(pages)
                    val output=PdfBoxStatementGeometry.reconstruct(pages,boxes,complete,
                        emitCompatibilityText=legacyText,gridTables=physicalGrids)
                    if(output !is MpesaPositionedRows.Ready)
                        // The detail of the rejection is deliberately fixed,
                        // not derived from the private statement's text.
                        throw PdfReadException(if(output is MpesaPositionedRows.Unsupported &&
                            output.reason==MpesaPositionedRows.Reason.UNVERIFIED_FINANCIAL_EVIDENCE)
                            PdfFailure.FINANCIAL_EVIDENCE else PdfFailure.TABLE_EVIDENCE,
                            (output as? MpesaPositionedRows.Unsupported)?.takeIf {
                                it.check!=com.kedevjimmy.pesatrack.domain.evidence.PdfStatementCheck.FINANCIAL_RECONCILIATION
                            }?.page,
                            (output as? MpesaPositionedRows.Unsupported)?.row,
                            (output as? MpesaPositionedRows.Unsupported)?.check,
                            (output as? MpesaPositionedRows.Unsupported)?.columns.orEmpty())
                    PdfTextReviewPolicy.preview(output.pages)
                    val proof=output.pdfBoxProof ?: throw PdfReadException(PdfFailure.FINANCIAL_EVIDENCE)
                    val metadataReview=PdfStatementReview.reviewMetadata(output.pages,true)
                    if(metadataReview.problems.isNotEmpty())throw PdfReadException(PdfFailure.METADATA_EVIDENCE)
                    return ReviewedDocument(output.pages,PdfBoxVerificationReport(
                        proof.receipts.size,proof.receipts.count {it.movementMinor!=0L},
                        proof.verifiedTransitions,proof.totalsVerified,
                        proof.openingVerified,proof.closingVerified
                    ),output.tables,metadataReview.currencyReview,proof.declarations)
                }
            }
        } catch (_:InvalidPasswordException) {
            throw PdfReadException(PdfFailure.PASSWORD)
        } catch (e:PdfReadException) {
            throw e
        } catch (_:IOException) {
            throw PdfReadException(PdfFailure.PARSE)
        }
    }

    private class PositionedWords(
        private val pageWidth:Float,private val pageHeight:Float
    ):PDFTextStripper() {
        val words=mutableListOf<PdfTextFragment>()
        var complete=true
            private set

        override fun writeString(text:String,textPositions:MutableList<TextPosition>) {
            if(text.isNotBlank()) {
                if(textPositions.isEmpty())complete=false
                val tokens=mutableListOf<MutableList<TextPosition>>()
                var run=mutableListOf<TextPosition>()
                var previous:TextPosition?=null
                fun flush() {
                    if(run.isNotEmpty()) {
                        tokens.add(run)
                        run=mutableListOf()
                    }
                }
                for(glyph in textPositions) {
                    val value=glyph.unicode
                    if(value.isBlank()) {
                        flush()
                        previous=null
                        continue
                    }
                    // A glyph holding both words and whitespace cannot be
                    // placed precisely in a single money-cell column.
                    if(value.any(Char::isWhitespace) || value.any(Char::isISOControl)) {
                        complete=false
                        flush()
                        previous=null
                        continue
                    }
                    val old=previous
                    if(old!=null) {
                        val gap=glyph.xDirAdj-(old.xDirAdj+old.widthDirAdj)
                        if(gap>maxOf(3f,old.widthDirAdj*1.5f))flush()
                    }
                    run.add(glyph)
                    previous=glyph
                }
                flush()
                for(group in tokens) {
                    val value=group.joinToString("") {it.unicode}
                    if(value.length>1200 || value.any(Char::isISOControl)) {
                        complete=false
                        continue
                    }
                    val glyphBounds=group.map {glyph ->
                        val x=glyph.xDirAdj
                        val y=glyph.yDirAdj
                        PdfBoxWordBounds.Rect(
                            x,y,x+glyph.widthDirAdj,y+glyph.heightDir
                        )
                    }
                    val bounded=PdfBoxWordBounds.fromCharacters(
                        glyphBounds,pageHeight,pageWidth
                    )
                    if(bounded==null)complete=false
                    else words.add(PdfTextFragment(
                        value,bounded.left,bounded.top,bounded.right,bounded.bottom
                    ))
                    if(words.size>32_000)throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                }
            }
            super.writeString(text,textPositions)
        }
    }
}
