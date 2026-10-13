package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import kotlin.math.abs
import java.util.Locale
import java.math.BigDecimal
import com.kedevjimmy.pesatrack.domain.statements.StatementField
import com.kedevjimmy.pesatrack.domain.statements.StatementPdfColumns
import com.kedevjimmy.pesatrack.domain.statements.StatementPdfColumnDetector
import com.kedevjimmy.pesatrack.domain.statements.StatementFormatProfile

/**
 * Read-only coordinate bridge. No row becomes an import candidate until the
 * existing full-document M-Pesa parser validates all rows and balances.
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
    private data class VisualLine(val y:Float,val words:List<PdfTextFragment>,val text:String)
    private val fields=setOf(StatementField.REFERENCE,StatementField.DATE,
        StatementField.DESCRIPTION,StatementField.STATUS,StatementField.CREDIT,
        StatementField.DEBIT,StatementField.BALANCE)
    private data class HeaderBand(val start:Int,val end:Int,val layout:StatementPdfColumns)

    private fun headers(lines:List<VisualLine>):List<HeaderBand> = lines.indices.flatMap {start ->
        (start until minOf(lines.size,start+3)).mapNotNull {end ->
            if(lines[end].y-lines[start].y>24f)null else {
                val layout=StatementPdfColumnDetector.detect(
                    lines.subList(start,end+1).flatMap {it.words},fields
                )
                layout?.let {HeaderBand(start,end,it)}
            }
        }
    }

    /** Recognized cover summary table, using the same column evidence rules. */
    private fun summaryTotals(lines:List<VisualLine>):List<String>? {
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

    private fun physicalLines(fragments:List<PdfTextFragment>):List<VisualLine>? {
        if(fragments.isEmpty() || fragments.size>32_000 || fragments.any {
            it.text.isBlank() || it.text.length>1200 || it.text.any(Char::isISOControl) ||
                !it.left.isFinite() || !it.right.isFinite() ||
                !it.top.isFinite() || !it.bottom.isFinite() ||
                it.left>=it.right || it.top>=it.bottom
        }) return null
        val groups=mutableListOf<MutableList<PdfTextFragment>>()
        for(word in fragments.sortedWith(compareBy<PdfTextFragment> {(it.top+it.bottom)/2f}.thenBy {it.left})) {
            val prev=groups.lastOrNull()
            val center=(word.top+word.bottom)/2f
            val old=prev?.map {(it.top+it.bottom)/2f}?.average()?.toFloat()
            val tolerance=maxOf(1.5f,minOf(3.4f,(word.bottom-word.top)*0.38f))
            if(prev==null || old==null || abs(center-old)>tolerance)
                groups.add(mutableListOf(word))
            else prev.add(word)
            if(groups.size>20_000)return null
        }
        return groups.map {g ->
            val sorted=g.sortedBy {it.left}
            VisualLine(
                g.map {(it.top+it.bottom)/2f}.average().toFloat(),
                sorted,clean(sorted.joinToString(" ") {it.text})
            )
        }
    }

    fun reconstruct(
        pages:List<String>, positioned:List<List<PdfTextFragment>>, complete:List<Boolean>
    ):MpesaPositionedRows {
        fun reject(p:Int,reason:MpesaPositionedRows.Reason)=
            MpesaPositionedRows.Unsupported(p,reason)
        if(pages.isEmpty() || pages.size>PdfTextReviewPolicy.MAX_PAGES ||
            positioned.size!=pages.size || complete.size!=pages.size)
            return reject(1,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
        val output=mutableListOf<String>()
        val evidence=mutableListOf<PdfBoxReceiptEvidence>()
        val metadata=mutableListOf<String>()
        var globalRows=0
        var globalWords=0
        for(i in pages.indices) {
            val page=i+1
            val source=pages[i]
            globalWords+=positioned[i].size
            if(!complete[i] || globalWords>32_000 || source.isBlank())
                return reject(page,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
            val lines=physicalLines(positioned[i])
                ?:return reject(page,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
            val totals=summaryTotals(lines)
                ?:return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_FINANCIAL_EVIDENCE)
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
                    receiptToken.matches(it.text) &&
                        layout.owner(it)==StatementField.REFERENCE
                }
            }
            if(anchors.size!=expected || anchors.zipWithNext().any {(a,b)->
                b<=a || lines[b].y-lines[a].y<5f})
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
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
                    ?:return reject(page,MpesaPositionedRows.Reason.UNRECOGNIZED_TABLE)
                val stop=rowStarts.getOrNull(position+1) ?: lines.size
                val block=lines.subList(rowStarts[position],stop).takeWhile {!footer.containsMatchIn(it.text)}
                val words=block.flatMap {it.words}.filterNot {it in headerWords}
                if(words.isEmpty() || words.size>260)
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val cells=mutableMapOf<StatementField,MutableList<PdfTextFragment>>()
                for(word in words) {
                    val field=layout.owner(word)
                        ?:return reject(page,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                    cells.getOrPut(field) {mutableListOf()}.add(word)
                }
                fun text(field:StatementField)=clean(cells[field].orEmpty()
                    .sortedWith(compareBy<PdfTextFragment> {it.top}.thenBy {it.left})
                    .joinToString(" ") {it.text})
                val reference=text(StatementField.REFERENCE).uppercase(Locale.ROOT)
                val date=text(StatementField.DATE)
                val original=sourceRows[position]
                val sourceBlock=source.substring(original.range.first,
                    sourceRows.getOrNull(position+1)?.range?.first ?: source.length)
                val times=clock.findAll(sourceBlock).map {it.value}.toList()
                if(reference!=original.groupValues[1].uppercase(Locale.ROOT) ||
                    times.size!=1 || date!="${original.groupValues[2]} ${times.single()}")
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                if(text(StatementField.STATUS).uppercase(Locale.ROOT)!="COMPLETED")
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                val detail=text(StatementField.DESCRIPTION)
                if(detail.length !in 1..200 || completed.containsMatchIn(detail))
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
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
                if(paid==null || paid<0 || debit==null || debit==Long.MIN_VALUE ||
                    balance==null || balance<0 || paid>0 && debit!=0L)
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                val withdrawn=abs(debit)
                if(paid==0L && withdrawn==0L &&
                    listOf(StatementField.CREDIT,StatementField.DEBIT).none {field ->
                        cells[field]?.singleOrNull()?.text?.let {
                            it!="-" && EverydayMoneyPolicy.money(it)==0L
                        }==true
                    }) return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                evidence.add(PdfBoxReceiptEvidence(page,position+1,reference,date,detail,
                    cells.mapValues {it.value.toList()},paid,withdrawn,balance))
                // Normalize proved column cells for the existing review parser.
                // For zero records, explicit zero plus complete empty geometry
                // proves no cash; two empty cells alone never prove zero.
                val creditText=if(paid==0L && withdrawn==0L)"0.00" else
                    cells[StatementField.CREDIT]?.singleOrNull()?.text ?: "-"
                val debitText=if(paid==0L && withdrawn==0L)"0.00" else
                    if(withdrawn>0L)BigDecimal.valueOf(withdrawn,2).negate().toPlainString() else "-"
                rows.add(
                    "$reference $date $detail COMPLETED $creditText $debitText " +
                        cells.getValue(StatementField.BALANCE).single().text
                )
            }
            globalRows+=rows.size
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
        return MpesaPositionedRows.Ready(output,proof)
    }
}
