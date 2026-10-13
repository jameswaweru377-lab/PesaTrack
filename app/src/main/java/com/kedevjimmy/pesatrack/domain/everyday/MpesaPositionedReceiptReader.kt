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
    data class Ready(
        val pages: List<String>,
        /** Populated only by the PDFBox adapter; never persisted or logged. */
        val pdfBoxProof: PdfBoxStatementProof? = null
    ) : MpesaPositionedRows()
    data class Unsupported(val page: Int, val reason: Reason) : MpesaPositionedRows()
    enum class Reason {
        INCOMPLETE_GEOMETRY, UNRECOGNIZED_TABLE, AMBIGUOUS_RECEIPT,
        MISSING_STATUS_OR_MONEY, OUT_OF_ROW_FRAGMENT, UNVERIFIED_ROW_COUNT,
        UNVERIFIED_FINANCIAL_EVIDENCE
    }
}

object MpesaPositionedReceiptReader {
    private val anchor=Regex(
        "^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})(?:\\s+(.*))?$",
        setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL)
    )
    /** Some native extractors emit a full receipt + status + cells as one box. */
    private val wholeRow=Regex(
        "^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})" +
            "\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})\\s+(.+?)\\s+COMPLETED\\s+(.+)$",
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
    private fun moneyShapeValid(tokens:List<String>):Boolean {
        if(tokens.size !in 2..3 || tokens.any {!moneyCell.matches(it)} ||
            tokens.last()=="-" || (tokens.size==2 && tokens[0]=="-"))return false
        if(tokens.size==3) {
            val first=if(tokens[0]=="-")0L else EverydayMoneyPolicy.money(tokens[0]) ?: return false
            val second=if(tokens[1]=="-")0L else EverydayMoneyPolicy.money(tokens[1]) ?: return false
            if(first!=0L && second!=0L)return false
        }
        return true
    }
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

            // Native text may expose either (a) a whole receipt row inside
            // one bounded box or (b) separate left/details and right/status
            // boxes. Both layouts must have exactly one Completed and receipt
            // anchor per financial row; an unseen box aborts reconstruction.
            data class RowAnchor(val fragment:PdfTextFragment,val whole:Boolean)
            val whole=fragments.filter { wholeRow.matches(clean(it.text)) }
            val split=fragments.filter {
                anchor.matches(clean(it.text)) && !completed.containsMatchIn(it.text)
            }
            val anchors=(whole.map {RowAnchor(it,true)}+split.map {RowAnchor(it,false)})
                .sortedBy {it.fragment.top}
            val statuses=fragments.filter {statusStart.containsMatchIn(it.text.trimStart())}
            if(anchors.isEmpty()) {
                if(completed.containsMatchIn(original) || sourceStart.containsMatchIn(original))
                    return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
                reconstructed.add(original)
                continue
            }
            if(anchors.size>5_000 ||
                statuses.size+whole.size!=anchors.size ||
                fragments.sumOf {completed.findAll(it.text).count()}!=anchors.size ||
                anchors.zipWithNext().any { (a,b)->b.fragment.top-a.fragment.top<4f })
                return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            // The service also sends the original page text. It independently
            // establishes Completed receipt count even if its reading order is
            // unsuitable for per-cell parsing.
            if(completed.findAll(original).count()!=anchors.size)
                return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val firstStart=sourceStart.find(original)?.range?.first
                ?: return unsupported(pageNo,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
            val prefix=original.substring(0,firstStart)
            val disclaimerY=fragments.filter {footer.containsMatchIn(clean(it.text))}
                .minOfOrNull {it.top} ?: Float.POSITIVE_INFINITY
            val pageRows=mutableListOf<String>()
            for(i in anchors.indices) {
                val marker=anchors[i]
                val origin=marker.fragment
                val stop=anchors.getOrNull(i+1)?.fragment?.top ?: disclaimerY
                if(origin.bottom>stop+2f)
                    return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                val within=fragments.filter {f->
                    f.top>=origin.top-2f && f.top<stop-1f &&
                        f.bottom<=stop+2f &&
                        !footer.containsMatchIn(clean(f.text))
                }
                if(origin !in within)
                    return unsupported(pageNo,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                if(marker.whole) {
                    // A single native block owns the full physical receipt
                    // row. No additional unassigned text may be discarded.
                    if(within.size!=1)
                        return unsupported(pageNo,MpesaPositionedRows.Reason.OUT_OF_ROW_FRAGMENT)
                    val match=wholeRow.matchEntire(clean(origin.text))
                        ?: return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                    val detail=match.groupValues[4].trim()
                    val amounts=match.groupValues[5].trim().split(Regex("\\s+"))
                    if(detail.isBlank() || detail.length>200 ||
                        completed.containsMatchIn(detail) ||
                        // A whole-row native rectangle carries no distinct
                        // x coordinate per monetary cell. The two-cell signed
                        // shape is proven; three numbers could mean a genuine
                        // empty column or an orphan amount. Abstain.
                        amounts.size!=2 || amounts.any {!moneyCell.matches(it)})
                        return unsupported(pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                    pageRows.add(
                        "${match.groupValues[1].uppercase(Locale.ROOT)} ${match.groupValues[2]} " +
                            "${match.groupValues[3]} $detail COMPLETED ${amounts.joinToString(" ")}"
                    )
                } else {
                    val foundStatus=statuses.filter {abs(it.top-origin.top)<=4.5f}
                    if(foundStatus.size!=1)
                        return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
                    val status=foundStatus.single()
                    val statusLeft=status.left
                    if(status.top>=stop || origin.right>=statusLeft+3f ||
                        status !in within)
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
                    // A complete Completed/amount/balance native fragment
                    // followed by any extra monetary fragment is not proof
                    // of a fourth financial column; fail rather than guess.
                    val statusTokens=rightSide.matchEntire(clean(status.text))
                        ?.groupValues?.get(1)?.split(Regex("\\s+")).orEmpty()
                    if(right.size>1 && statusTokens.size>=2 &&
                        statusTokens.all {moneyCell.matches(it)})
                        return unsupported(pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                    val monetary=clean(right.joinToString(" ") {it.text})
                    val money=rightSide.matchEntire(monetary)?.groupValues?.get(1)
                        ?.split(Regex("\\s+")) ?: return unsupported(
                            pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                    if(!moneyShapeValid(money))
                        return unsupported(pageNo,MpesaPositionedRows.Reason.MISSING_STATUS_OR_MONEY)
                    pageRows.add(
                        "${found.groupValues[1].uppercase(Locale.ROOT)} ${found.groupValues[2]} " +
                            "${found.groupValues[3]} $details COMPLETED ${money.joinToString(" ")}"
                    )
                }
            }
            count+=pageRows.size
            if(count>5_000)return unsupported(pageNo,MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT)
            reconstructed.add(prefix+"\n"+pageRows.joinToString("\n"))
        }
        return if(count>0)MpesaPositionedRows.Ready(reconstructed) else
            unsupported(1,MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT)
    }
}
