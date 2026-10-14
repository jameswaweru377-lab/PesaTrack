package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.kedevjimmy.pesatrack.domain.evidence.PdfStatementCheck
import com.kedevjimmy.pesatrack.domain.evidence.PdfColumnDiagnostic
import kotlin.math.abs
import java.util.Locale
import java.math.BigDecimal
import com.kedevjimmy.pesatrack.domain.statements.StatementField
import com.kedevjimmy.pesatrack.domain.statements.StatementPdfColumns
import com.kedevjimmy.pesatrack.domain.statements.StatementPdfColumnDetector
import com.kedevjimmy.pesatrack.domain.statements.StatementFormatProfile
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import com.kedevjimmy.pesatrack.domain.statements.StatementTableDiscovery
import com.kedevjimmy.pesatrack.domain.statements.StatementPdfLines
import com.kedevjimmy.pesatrack.domain.statements.StatementPhysicalLine

/**
 * Coordinate-to-table adapter. No row becomes an import candidate until the
 * full-document receipt, financial and metadata gates have passed.
 * Numbers in Details are never converted to wallet movement cells.
 */
object PdfBoxStatementGeometry {
    private val receiptToken=Regex("[A-Z0-9]{10}",RegexOption.IGNORE_CASE)
    private val clock=Regex("\\b[0-9]{2}:[0-9]{2}:[0-9]{2}\\b")
    private val sourceReceipt=Regex(
        "(?m)^[ \\t]*([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})",
        RegexOption.IGNORE_CASE
    )
    private val completed=Regex("\\bCOMPLETED\\b",RegexOption.IGNORE_CASE)
    private val footer=Regex(
        "^(?:DISCLAIMER\\b|STATEMENT VERIFICATION CODE\\b|FOR SELF[- ]HELP\\b)",
        RegexOption.IGNORE_CASE
    )
    private val money=Regex("-|(?:-?[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
    private fun clean(s:String)=s.replace(Regex("\\s+")," ").trim()
    private val fields=setOf(StatementField.REFERENCE,StatementField.DATE,
        StatementField.DESCRIPTION,StatementField.STATUS,StatementField.CREDIT,
        StatementField.DEBIT,StatementField.BALANCE)
    private data class HeaderBand(val start:Int,val end:Int,val layout:StatementPdfColumns)

    private fun headers(lines:List<StatementPhysicalLine>):List<HeaderBand> =
        StatementTableDiscovery.headers(lines,fields).map {HeaderBand(it.start,it.end,it.columns)}

    /** Recognized cover summary table, using the same column evidence rules. */
    private fun summaryTotals(lines:List<StatementPhysicalLine>):List<String>? {
        val section=lines.indexOfFirst {it.text.equals("SUMMARY",true)}
        if(section<0)return emptyList()
        val total=lines.indices.filter {it>section &&
            lines[it].words.any {word -> Regex("(?i)TOTALS?:?").matches(word.text)}}
        if(total.size!=1)return null
        val index=total.single()
        val profile=StatementFormatProfile(mapOf("Transaction Type" to StatementField.DESCRIPTION))
        val roles=setOf(StatementField.DESCRIPTION,StatementField.CREDIT,StatementField.DEBIT)
        val candidates=(section+1 until index).flatMap {start ->
            (start until minOf(index,start+3)).mapNotNull {end ->
                StatementPdfColumnDetector.detect(lines.subList(start,end+1).flatMap {it.words},roles,profile)
            }
        }
        val columns=candidates.maxByOrNull {it.columns.sumOf {c->c.heading.size}} ?: return null
        val cells=mutableMapOf<StatementField,MutableList<PdfTextFragment>>()
        for(word in lines[index].words) {
            val field=columns.owner(word) ?: return null
            cells.getOrPut(field){mutableListOf()}.add(word)
        }
        if(cells[StatementField.DESCRIPTION]?.singleOrNull()?.text?.let {
                Regex("(?i)TOTALS?:?").matches(it)
            }!=true)return null
        fun value(field:StatementField):String? {
            val cell=cells[field]?.singleOrNull()?.text ?: return null
            if(cell=="-")return "0.00"
            if(EverydayMoneyPolicy.money(cell)==null)return null
            return cell
        }
        val paid=value(StatementField.CREDIT) ?: return null
        val debit=value(StatementField.DEBIT) ?: return null
        return listOf("Total Paid In: $paid","Total Withdrawn: $debit")
    }

    /**
     * Header midpoints are not guarantees of body-cell widths. Some PDFBox
     * text rectangles for a genuine *source-verified* date or time can spill
     * slightly over the inferred Date/time–Description boundary. Only that
     * exact isolated source token may be retained in Date/time; never assign
     * an arbitrary straddling fragment by its centre or by a majority area.
     *
     * The DATE cell is still checked verbatim against the source receipt and
     * its unique completion time after reconstruction. Any competing glyph
     * rectangle or large overrun fails closed.
     */
    private fun verifiedDateBoundaryWord(
        word:PdfTextFragment,
        rowWords:List<PdfTextFragment>,
        layout:StatementPdfColumns,
        receiptDate:String,
        uniqueReceiptTime:String?
    ):Boolean {
        if(word.text != receiptDate && (uniqueReceiptTime == null ||
                word.text != uniqueReceiptTime))return false
        val date=layout.columns.singleOrNull {it.field==StatementField.DATE} ?:return false
        val detail=layout.columns.singleOrNull {it.field==StatementField.DESCRIPTION} ?:return false
        val boundary=date.right
        // Only neighboring Date/time and Description, in that physical order.
        if(boundary != detail.left || word.left<date.left ||
            word.left>=boundary || word.right<=boundary ||
            word.right>detail.right || word.right-boundary>24f)return false
        val intersecting=layout.columns.filter {
            word.left<it.right && word.right>it.left
        }.map {it.field}.toSet()
        if(intersecting!=setOf(StatementField.DATE,StatementField.DESCRIPTION))
            return false
        // A broad rectangle is not proof that adjacent text was absorbed.
        // Never accept if its original glyph bounds physically touch another
        // text rectangle on the same line.
        val centre=(word.top+word.bottom)/2f
        if(rowWords.any {other ->
                other !== word &&
                    abs((other.top+other.bottom)/2f-centre)<=3.4f &&
                    other.left<word.right+1f && other.right>word.left-1f
            })return false
        return true
    }

    /**
     * Very narrow, source-backed exception for Details text extending LEFT of
     * a heading-midpoint boundary into Date/time. Never reassign an arbitrary
     * rectangle by its midpoint. This is not applied to money or status cells.
     *
     * The description fragment must appear exactly once in the original
     * receipt AFTER its unique completion time and BEFORE Completed, must
     * overlap only the two neighboring physical column ranges, and must not
     * physically intersect any other word on the same visual line.
     * The full existing receipt and financial proof is always required later.
     */
    private fun verifiedDescriptionBoundaryWord(
        word:PdfTextFragment,
        rowWords:List<PdfTextFragment>,
        layout:StatementPdfColumns,
        sourceBlock:String,
        receiptDate:String,
        uniqueReceiptTime:String?
    ):Boolean {
        val time=uniqueReceiptTime ?:return false
        val date=layout.columns.singleOrNull {it.field==StatementField.DATE} ?:return false
        val detail=layout.columns.singleOrNull {it.field==StatementField.DESCRIPTION} ?:return false
        val boundary=detail.left
        if(date.right!=boundary || word.left<date.left ||
            word.left>=boundary || word.right<=boundary ||
            word.right>detail.right || boundary-word.left>24f)return false
        if(word.text.length !in 3..200 || word.text!=word.text.trim() ||
            word.text.any(Char::isISOControl) ||
            word.text==receiptDate || word.text==time ||
            word.text.equals("Completed",true) ||
            money.matches(word.text))return false
        if(layout.columns.filter {word.left<it.right && word.right>it.left}
                .map {it.field}.toSet()!=setOf(StatementField.DATE,StatementField.DESCRIPTION))
            return false
        val sourceTime=sourceBlock.indexOf(time)
        val sourceStatus=sourceBlock.indexOf("Completed",ignoreCase=true)
        val index=sourceBlock.indexOf(word.text)
        if(sourceTime<0 || sourceStatus<0 || index<sourceTime+time.length ||
            index>=sourceStatus || sourceBlock.indexOf(word.text,index+1)>=0)return false
        // Prevent a shifted rectangle from stealing text that occupies the
        // Date/time physical space on the same row, even if source text agrees.
        val centre=(word.top+word.bottom)/2f
        if(rowWords.any {other ->
                other !== word &&
                    abs((other.top+other.bottom)/2f-centre)<=3.4f &&
                    other.left<word.right+1f && other.right>word.left-1f
            })return false
        return true
    }

    /**
     * The output is one of a small fixed vocabulary of authored messages.
     * No PDF text, bounds, source values, or nearby financial details escape.
     */
    private fun classifyUnownedFragment(
        word:PdfTextFragment,
        rowWords:List<PdfTextFragment>,
        layout:StatementPdfColumns,
        receiptDate:String,
        sourceTime:String?
    ):PdfColumnDiagnostic {
        val date=layout.columns.singleOrNull {it.field==StatementField.DATE}
            ?:return PdfColumnDiagnostic.OTHER_RECTANGLE
        val description=layout.columns.singleOrNull {it.field==StatementField.DESCRIPTION}
            ?:return PdfColumnDiagnostic.OTHER_RECTANGLE
        val verified=word.text==receiptDate || (sourceTime!=null && word.text==sourceTime)
        if(verified) {
            if(word.left>=date.left && word.left<date.right &&
                word.right-date.right>24f)
                return PdfColumnDiagnostic.VERIFIED_TIMESTAMP_TOO_WIDE
            val mid=(word.top+word.bottom)/2f
            if(rowWords.any { other ->
                    other !== word &&
                        abs((other.top+other.bottom)/2f-mid)<=3.4f &&
                        other.left<word.right+1f && other.right>word.left-1f
                })return PdfColumnDiagnostic.VERIFIED_TIMESTAMP_TOUCHES_NEIGHBOR
            return PdfColumnDiagnostic.VERIFIED_TIMESTAMP_LAYOUT_MISMATCH
        }
        val crosses=(word.left<description.left && word.right>description.left)
        return if(crosses && word.left>=date.left && word.right<=description.right) {
            // Classification by source confirmation, not by publishing text.
            val looksJoined=word.text.startsWith(receiptDate) ||
                (sourceTime!=null && word.text.startsWith(sourceTime))
            if(looksJoined)PdfColumnDiagnostic.COMBINED_OR_UNVERIFIED_TEXT
            else PdfColumnDiagnostic.DESCRIPTION_CROSSES_BOUNDARY
        } else PdfColumnDiagnostic.OTHER_RECTANGLE
    }

    fun reconstruct(
        pages:List<String>, positioned:List<List<PdfTextFragment>>, complete:List<Boolean>,
        /** Compatibility output for legacy tests/callers, never production review. */
        emitCompatibilityText:Boolean=true
    ):MpesaPositionedRows {
        fun reject(p:Int,reason:MpesaPositionedRows.Reason,row:Int?=null,
            check:PdfStatementCheck?=null,columns:Set<StatementField> = emptySet(),
            diagnostic:PdfColumnDiagnostic?=null)=
            MpesaPositionedRows.Unsupported(p,reason,row,check ?: when(reason) {
                MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY -> PdfStatementCheck.INCOMPLETE_GEOMETRY
                MpesaPositionedRows.Reason.UNRECOGNIZED_TABLE -> PdfStatementCheck.HEADER_DISCOVERY
                MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT -> PdfStatementCheck.RECEIPT_COVERAGE
                MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT -> PdfStatementCheck.ROW_BOUNDARY
                MpesaPositionedRows.Reason.UNVERIFIED_FINANCIAL_EVIDENCE -> PdfStatementCheck.FINANCIAL_RECONCILIATION
                else -> null
            },columns,diagnostic)
        if(pages.isEmpty() || pages.size>PdfTextReviewPolicy.MAX_PAGES ||
            positioned.size!=pages.size || complete.size!=pages.size)
            return reject(1,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
        val output=mutableListOf<String>()
        val evidence=mutableListOf<PdfBoxReceiptEvidence>()
        val tables=mutableListOf<StatementTablePage>()
        val metadata=mutableListOf<String>()
        var globalRows=0
        var globalWords=0
        for(i in pages.indices) {
            val page=i+1
            val source=pages[i]
            // Summary/boundary declarations can appear after the table or a
            // disclaimer. Audit the whole source, not just the display prefix.
            metadata.add(source)
            globalWords+=positioned[i].size
            if(!complete[i] || globalWords>32_000 || source.isBlank())
                return reject(page,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
            val lines=StatementPdfLines.group(positioned[i])
                ?:return reject(page,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
            val totals=summaryTotals(lines)
                ?:return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_FINANCIAL_EVIDENCE,
                    check=PdfStatementCheck.SUMMARY_EVIDENCE)
            metadata.addAll(totals)
            val sourceRows=sourceReceipt.findAll(source).toList()
            val expected=sourceRows.size
            if(expected!=completed.findAll(source).count())
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            if(expected==0) {
                if(lines.any {completed.containsMatchIn(it.text)})
                    return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
                output.add(source)
                metadata.add(source)
                continue
            }
            val bands=headers(lines)
            fun layoutAt(index:Int)=bands.filter {it.end<index}.maxWithOrNull(
                compareBy<HeaderBand> {it.end}.thenBy {band ->
                    band.layout.columns.sumOf {it.heading.size}
                }
            )?.layout
            // Receipt references, not flattened date/time lines, anchor rows.
            // Completion timestamps may wrap within their own date column.
            val anchors=lines.indices.filter {index ->
                val layout=layoutAt(index)
                layout!=null && lines[index].words.any {
                    receiptToken.matches(it.text) && !footer.containsMatchIn(it.text) &&
                        layout.owner(it)==StatementField.REFERENCE
                }
            }
            if(anchors.size!=expected || anchors.zipWithNext().any {(a,b)->
                b<=a || lines[b].y-lines[a].y<5f})
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT,
                    check=if(bands.isEmpty())PdfStatementCheck.HEADER_DISCOVERY else PdfStatementCheck.RECEIPT_COVERAGE)
            if(lines.indexOfFirst {footer.containsMatchIn(it.text)}.let {
                    it>=0 && it<anchors.last()
                }) return reject(page,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
            val headerWords=bands.flatMap {band ->
                lines.subList(band.start,band.end+1).flatMap {it.words}
            }.toSet()
            val rowStarts=anchors.map {index ->
                val layout=layoutAt(index)!!
                var start=index
                while(start>0 && lines[index].y-lines[start-1].y<=16f &&
                    lines[start-1].words.all {layout.owner(it)==StatementField.DATE})start--
                start
            }
            val prefixStart=sourceReceipt.find(source)?.range?.first
                ?:return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val extractedPrefix=source.substring(0,prefixStart)
            // PDFBox's text-stream ordering and physical reading order can
            // differ. The row geometry is trustworthy only if all source
            // receipts/statuses reconcile; retain the *actual* text visible
            // above the first receipt as independent header evidence as well.
            //
            // Never construct "Currency: KES" or invent official statement
            // labels: only observed, positioned page words may authorize the
            // Kenya-currency fallback in PdfStatementReview.
            val visualPrefix=lines.subList(0,rowStarts.first()).joinToString("\n") { it.text }
            val prefix=listOf(visualPrefix,extractedPrefix).filter {it.isNotBlank()}
                .joinToString("\n")
            if(completed.containsMatchIn(prefix))
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            metadata.add(prefix)
            val rows=mutableListOf<String>()
            for((position,index) in anchors.withIndex()) {
                val layout=layoutAt(index)
                    ?:return reject(page,MpesaPositionedRows.Reason.UNRECOGNIZED_TABLE,position+1)
                val stop=rowStarts.getOrNull(position+1) ?: lines.size
                val block=lines.subList(rowStarts[position],stop).takeWhile {!footer.containsMatchIn(it.text)}
                val words=block.flatMap {it.words}.filterNot {it in headerWords}
                if(words.isEmpty() || words.size>260)
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.ROW_SIZE)
                val original=sourceRows[position]
                val sourceBlock=source.substring(original.range.first,
                    sourceRows.getOrNull(position+1)?.range?.first ?: source.length)
                val times=clock.findAll(sourceBlock).map {it.value}.toList()
                val verifiedClock=times.singleOrNull()
                val cells=mutableMapOf<StatementField,MutableList<PdfTextFragment>>()
                for(word in words) {
                    val field=layout.owner(word) ?:
                        StatementField.DATE.takeIf {
                            verifiedDateBoundaryWord(
                                word,words,layout,original.groupValues[2],verifiedClock
                            )
                        } ?: StatementField.DESCRIPTION.takeIf {
                            verifiedDescriptionBoundaryWord(
                                word,words,layout,sourceBlock,original.groupValues[2],verifiedClock
                            )
                        } ?:return reject(page,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT,position+1,
                            PdfStatementCheck.COLUMN_OWNERSHIP,layout.columns.filter {
                                word.left<it.right && word.right>it.left
                            }.map {it.field}.toSet(), classifyUnownedFragment(
                                word,words,layout,original.groupValues[2],verifiedClock
                            ))
                    cells.getOrPut(field) {mutableListOf()}.add(word)
                }
                fun text(field:StatementField)=clean(cells[field].orEmpty()
                    .sortedWith(compareBy<PdfTextFragment> {it.top}.thenBy {it.left})
                    .joinToString(" ") {it.text})
                val reference=text(StatementField.REFERENCE).uppercase(Locale.ROOT)
                val date=text(StatementField.DATE)
                if(reference!=original.groupValues[1].uppercase(Locale.ROOT))
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.REFERENCE_CELL)
                if(times.size!=1)
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.SOURCE_TIMESTAMP_COUNT)
                if(date!="${original.groupValues[2]} ${times.single()}")
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.TIMESTAMP_CELL)
                if(text(StatementField.STATUS).uppercase(Locale.ROOT)!="COMPLETED")
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,PdfStatementCheck.STATUS_CELL)
                val detail=text(StatementField.DESCRIPTION)
                if(detail.length !in 1..200)
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.DESCRIPTION_LENGTH)
                if(completed.containsMatchIn(detail))
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,position+1,PdfStatementCheck.DESCRIPTION_STATUS)
                fun amount(field:StatementField,blankAllowed:Boolean):Long? {
                    val parts=cells[field].orEmpty()
                    if(parts.size>1)return null
                    if(parts.isEmpty())return if(blankAllowed)0L else null
                    val value=parts.single().text
                    if(value=="-")return if(blankAllowed)0L else null
                    if(!money.matches(value))return null
                    return EverydayMoneyPolicy.money(value)
                }
                val paid=amount(StatementField.CREDIT,true)
                val debit=amount(StatementField.DEBIT,true)
                val balance=amount(StatementField.BALANCE,false)
                if(paid==null || paid<0)return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,
                    if(cells[StatementField.CREDIT].orEmpty().size>1)PdfStatementCheck.CREDIT_PARTS else PdfStatementCheck.CREDIT_AMOUNT)
                if(debit==null || debit==Long.MIN_VALUE)return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,
                    if(cells[StatementField.DEBIT].orEmpty().size>1)PdfStatementCheck.DEBIT_PARTS else PdfStatementCheck.DEBIT_AMOUNT)
                if(balance==null || balance<0)return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,
                    if(cells[StatementField.BALANCE].orEmpty().size>1)PdfStatementCheck.BALANCE_PARTS else PdfStatementCheck.BALANCE_AMOUNT)
                if(paid>0 && debit!=0L)return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,
                    PdfStatementCheck.MOVEMENT_DIRECTION)
                val withdrawn=abs(debit)
                if(paid==0L && withdrawn==0L &&
                    listOf(StatementField.CREDIT,StatementField.DEBIT).none {field ->
                        cells[field]?.singleOrNull()?.text?.let {
                            it!="-" && EverydayMoneyPolicy.money(it)==0L
                        }==true
                    }) return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY,position+1,PdfStatementCheck.ZERO_EVIDENCE)
                evidence.add(PdfBoxReceiptEvidence(page,position+1,reference,date,detail,
                    cells.mapValues {it.value.toList()},paid,withdrawn,balance))
                // Keep physical column order and cell rectangles in the shared
                // V2 model. No summaries or customer metadata become rows.
                val headings=layout.columns.map {column->column.heading.sortedWith(
                    compareBy<PdfTextFragment> {it.top}.thenBy {it.left}).joinToString(" "){it.text}}
                val row=layout.columns.map {text(it.field)}
                val provenance=layout.columns.map {cells[it.field].orEmpty().toList()}
                val previous=tables.lastOrNull()
                if(previous?.pageNumber==page && previous.headers==headings) {
                    tables[tables.lastIndex]=previous.copy(rows=previous.rows+listOf(row),
                        cellEvidence=previous.cellEvidence+listOf(provenance))
                } else tables.add(StatementTablePage(page,headings,listOf(row),listOf(provenance),
                    if(previous?.pageNumber==page)previous.tableNumber+1 else 1))
                // Normalize proved column cells for the existing review parser.
                // For zero records, explicit zero plus complete empty geometry
                // proves no cash; two empty cells alone never prove zero.
                val creditText=if(paid==0L && withdrawn==0L)"0.00" else
                    cells[StatementField.CREDIT]?.singleOrNull()?.text ?: "-"
                val debitText=if(paid==0L && withdrawn==0L)"0.00" else
                    if(withdrawn>0L)BigDecimal.valueOf(withdrawn,2).negate().toPlainString() else "-"
                if(emitCompatibilityText)rows.add(
                    "$reference $date $detail COMPLETED $creditText $debitText " +
                        cells.getValue(StatementField.BALANCE).single().text
                )
            }
            globalRows+=anchors.size
            if(globalRows>5_000)return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            // Include merged *observed* heading words, including stacked labels.
            val headings=bands.firstOrNull()?.layout?.originalHeadings.orEmpty()
            // Row cells above are always emitted in credit/debit/balance order.
            // Normalize the observed heading order to the same compatibility
            // representation. The original layout and cell rectangles stay in
            // proof; no currency or document authenticity is inferred here.
            val reviewPrefix=(prefix+"\n"+headings).replace(
                Regex("(?i)WITHDRAWN\\s+PAID IN(?=\\s+BALANCE)"),"Paid In Withdrawn")
            output.add(reviewPrefix+"\n"+rows.joinToString("\n"))
        }
        if(globalRows==0)return reject(1,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
        val proof=PdfBoxStatementFinancialAudit.verify(evidence,metadata)
            ?:return reject(1,MpesaPositionedRows.Reason.UNVERIFIED_FINANCIAL_EVIDENCE)
        return MpesaPositionedRows.Ready(output,proof,tables)
    }
}
