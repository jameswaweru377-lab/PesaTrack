package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import kotlin.math.abs
import java.util.Locale

/**
 * Read-only coordinate bridge. No row becomes an import candidate until the
 * existing full-document M-Pesa parser validates all rows and balances.
 * Numbers in Details are never converted to wallet movement cells.
 */
object PdfBoxStatementGeometry {
    private val receipt=Regex(
        "^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})(?:\\s+(.*))?$",RegexOption.IGNORE_CASE
    )
    private val sourceReceipt=Regex(
        "(?m)^[ \\t]*[A-Z0-9]{10}\\s+(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+[0-9]{2}:[0-9]{2}:[0-9]{2}"
    )
    private val completed=Regex("\\bCOMPLETED\\b",RegexOption.IGNORE_CASE)
    private val footer=Regex(
        "^(?:DISCLAIMER\\b|STATEMENT VERIFICATION CODE\\b|FOR SELF[- ]HELP\\b)",
        RegexOption.IGNORE_CASE
    )
    private val money=Regex("-|(?:-?[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
    private fun clean(s:String)=s.replace(Regex("\\s+")," ").trim()
    private data class VisualLine(val y:Float,val words:List<PdfTextFragment>,val text:String)

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
            val expected=sourceReceipt.findAll(source).count()
            if(expected!=completed.findAll(source).count())
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            if(expected==0) {
                if(lines.any {completed.containsMatchIn(it.text)})
                    return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
                output.add(source)
                continue
            }
            val anchors=lines.indices.filter {receipt.matches(lines[it].text)}
            if(anchors.size!=expected || anchors.zipWithNext().any {(a,b)->
                b<=a || lines[b].y-lines[a].y<5f})
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
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
            val visualPrefix=lines.subList(0,anchors.first()).joinToString("\n") { it.text }
            val prefix=listOf(visualPrefix,extractedPrefix).filter {it.isNotBlank()}
                .joinToString("\n")
            if(completed.containsMatchIn(prefix))
                return reject(page,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val rows=mutableListOf<String>()
            for((position,index) in anchors.withIndex()) {
                val stop=anchors.getOrNull(position+1) ?: lines.size
                val block=lines.subList(index,stop).takeWhile {!footer.containsMatchIn(it.text)}
                val words=block.flatMap {it.words}
                if(words.isEmpty() || words.size>260)
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val statuses=words.filter {it.text.equals("COMPLETED",true)}
                if(statuses.size!=1)
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                val status=statuses.single()
                val left=mutableListOf<PdfTextFragment>()
                val right=mutableListOf<PdfTextFragment>()
                for(word in words) {
                    if(word===status)continue
                    when {
                        word.right<=status.left+3f ->left.add(word)
                        word.left>=status.right+2f ->right.add(word)
                        else ->return reject(page,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                    }
                }
                val descriptive=clean(left.sortedWith(compareBy<PdfTextFragment> {it.top}.thenBy {it.left})
                    .joinToString(" ") {it.text})
                val head=receipt.matchEntire(descriptive)
                    ?:return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val detail=clean(head.groupValues[4])
                if(detail.length !in 1..200 || completed.containsMatchIn(detail))
                    return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val cash=right.sortedWith(compareBy<PdfTextFragment> {it.left}.thenBy {it.top})
                if(cash.size !in 2..3 || cash.any {!money.matches(it.text)} ||
                    cash.zipWithNext().any {(a,b)->b.left-a.right<3f} ||
                    cash.last().left-cash.first().right<12f)
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                if(cash.size==3 && cash[0].text!="-" && cash[1].text!="-")
                    return reject(page,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                rows.add(
                    "${head.groupValues[1].uppercase(Locale.ROOT)} ${head.groupValues[2]} " +
                        "${head.groupValues[3]} $detail COMPLETED " +
                        cash.joinToString(" ") {it.text}
                )
            }
            globalRows+=rows.size
            if(globalRows>5_000)return reject(page,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            output.add(prefix+"\n"+rows.joinToString("\n"))
        }
        return if(globalRows>0)MpesaPositionedRows.Ready(output)
        else reject(1,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
    }
}
