package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import java.util.Locale
import kotlin.math.abs

/**
 * Reconstruct known mobile-wallet PDF receipts from *physical* Details and
 * Status/Money regions, never by guessing that a naked number is an amount.
 * Original values remain in memory and are not logged or exported.
 */
sealed class MpesaPositionedRows {
    data class Ready(val pages: List<String>) : MpesaPositionedRows()
    data class Unsupported(val page: Int, val reason: Reason) : MpesaPositionedRows()
    enum class Reason {
        INCOMPLETE_GEOMETRY, UNRECOGNIZED_TABLE, AMBIGUOUS_RECEIPT,
        MISSING_STATUS_OR_MONEY, OUT_OF_ROW_FRAGMENT, UNVERIFIED_ROW_COUNT
    }
}

object MpesaPositionedReceiptReader {
    private val anchor=Regex(
        "^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})(?:\\s+(.*))?$",
        setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL)
    )
    private val sourceStart=Regex(
        "(?m)^[ \\t]*[A-Z0-9]{10}\\s+(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+[0-9]{2}:[0-9]{2}:[0-9]{2}"
    )
    private val completed=Regex("\\bCOMPLETED\\b",RegexOption.IGNORE_CASE)
    private val statusStart=Regex("^COMPLETED(?:\\s|$)",RegexOption.IGNORE_CASE)
    private val rightSide=Regex("^COMPLETED\\s+(.+)$",RegexOption.IGNORE_CASE)
    private val moneyCell=Regex("(?:-|(?:-?[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?))")
    private val footer=Regex("(?i)^(?:DISCLAIMER\\b|STATEMENT VERIFICATION CODE\\b|FOR SELF[- ]HELP\\b)")
    private val header=Regex("(?i)^(?:RECEIPT NO\\.?|COMPLETION TIME\\b|TRANSACTION STATUS\\b|PAID IN\\b|WITHDRAWN\\b|BALANCE\\b)")
    private fun clean(s:String)=s.replace(Regex("\\s+")," ").trim()
    private fun valid(f:PdfTextFragment)=
        f.left.isFinite() && f.right.isFinite() && f.top.isFinite() &&
            f.bottom.isFinite() && f.left<f.right && f.top<f.bottom && f.text.isNotBlank()

    fun reconstruct(
        pages:List<String>,
        positionedPages:List<List<PdfTextFragment>>,
        completePages:List<Boolean>
    ):MpesaPositionedRows {
        fun unsupported(p:Int,reason:MpesaPositionedRows.Reason)=
            MpesaPositionedRows.Unsupported(p,reason)
        if(pages.isEmpty() || pages.size!=positionedPages.size || pages.size!=completePages.size)
            return unsupported(1,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)
        val reconstructed=mutableListOf<String>()
        var count=0
        for((pageIndex,original) in pages.withIndex()) {
            val pageNo=pageIndex+1
            val fragments=positionedPages[pageIndex]
            if(!completePages[pageIndex] || fragments.isEmpty() || fragments.size>30_000 ||
                fragments.any {!valid(it)})
                return unsupported(pageNo,MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY)

            data class RowAnchor(val fragment:PdfTextFragment)
            val anchors=fragments.filter {anchor.matches(clean(it.text))}
                .map {RowAnchor(it)}.sortedBy {it.fragment.top}
            val statuses=fragments.filter {statusStart.containsMatchIn(it.text.trimStart())}
                .sortedBy {it.top}
            if(anchors.isEmpty()) {
                if(completed.containsMatchIn(original) || sourceStart.containsMatchIn(original))
                    return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
                reconstructed.add(original)
                continue
            }
            if(anchors.size!=statuses.size || anchors.size>5_000 ||
                anchors.zipWithNext().any { (a,b)->b.fragment.top-a.fragment.top<4f })
                return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            val statusLeft=statuses.minOf {it.left}
            if(anchors.any {it.fragment.left>=statusLeft-2f || it.fragment.right>=statusLeft+3f})
                return unsupported(pageNo,MpesaPositionedRows.Reason.UNRECOGNIZED_TABLE)
            if(sourceStart.findAll(original).count()!=anchors.size ||
                completed.findAll(original).count()!=anchors.size)
                return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val firstStart=sourceStart.find(original)?.range?.first
                ?: return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val prefix=original.substring(0,firstStart)
            val disclaimerY=fragments.filter {footer.containsMatchIn(clean(it.text))}
                .minOfOrNull {it.top} ?: Float.POSITIVE_INFINITY
            val pageRows=mutableListOf<String>()
            for(i in anchors.indices) {
                val origin=anchors[i].fragment
                val stop=anchors.getOrNull(i+1)?.fragment?.top ?: disclaimerY
                val status=statuses[i]
                if(abs(status.top-origin.top)>4.5f || status.top>=stop ||
                    origin.bottom>stop+2f)
                    return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val within=fragments.filter {f->
                    f.top>=origin.top-2f && f.top<stop-1f &&
                        f.bottom<=stop+2f &&
                        !header.containsMatchIn(clean(f.text)) &&
                        !footer.containsMatchIn(clean(f.text))
                }
                if(origin !in within || status !in within)
                    return unsupported(pageNo,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                val left=within.filter {it.left<statusLeft-2f}
                    .sortedWith(compareBy<PdfTextFragment> {it.top}.thenBy {it.left})
                val right=within.filter {it.left>=statusLeft-2f}
                    .sortedWith(compareBy<PdfTextFragment> {it.top}.thenBy {it.left})
                if(left.isEmpty() || right.isEmpty() ||
                    left.any {it.right>=statusLeft+3f} ||
                    right.count {completed.containsMatchIn(it.text)}!=1)
                    return unsupported(pageNo,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                val found=anchor.matchEntire(clean(left.joinToString(" ") {it.text}))
                    ?: return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val details=found.groupValues[4].trim()
                if(details.isBlank() || details.length>200 || completed.containsMatchIn(details))
                    return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val monetary=clean(right.joinToString(" ") {it.text})
                val money=rightSide.matchEntire(monetary)?.groupValues?.get(1)
                    ?.split(Regex("\\s+")) ?: return unsupported(
                        pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                if(money.size !in 2..3 || money.any {!moneyCell.matches(it)})
                    return unsupported(pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                pageRows.add(
                    "${found.groupValues[1].uppercase(Locale.ROOT)} ${found.groupValues[2]} " +
                        "${found.groupValues[3]} $details COMPLETED ${money.joinToString(" ")}"
                )
            }
            count+=pageRows.size
            if(count>5_000)return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            reconstructed.add(prefix+"\n"+pageRows.joinToString("\n"))
        }
        return if(count>0)MpesaPositionedRows.Ready(reconstructed) else
            unsupported(1,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
    }
}
