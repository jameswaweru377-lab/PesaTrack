package com.kedevjimmy.pesatrack.domain.evidence

import kotlin.math.abs

/** Coordinates of short PDF text fragments, expressed in PDF points.
 * All processing is local; raw fragments are discarded after import review.
 */
data class PdfTextFragment(val text:String, val left:Float, val top:Float, val right:Float, val bottom:Float)

/**
 * Reconstruct reading order from line-level fragment positions; no row amounts
 * are inferred here. If geometry is ambiguous, preserve source order so the
 * strict financial parser can reject it instead of assigning the wrong amount.
 */
object PdfTextPositionReflow {
    private const val MAX_FRAGMENTS=32_000
    private const val MAX_TEXT=500_000
    fun reflow(fragments:List<PdfTextFragment>, original:String):String {
        if(fragments.isEmpty() || fragments.size>MAX_FRAGMENTS || original.length>MAX_TEXT)
            return original
        if(fragments.any {
            !it.left.isFinite() || !it.top.isFinite() || !it.right.isFinite() ||
                !it.bottom.isFinite() || it.left>=it.right || it.top>=it.bottom ||
                it.text.isBlank() || it.text.contains('\n') || it.text.contains('\r')
        }) return original
        val total=fragments.sumOf { it.text.length.toLong() }
        // Never drop content whose coordinates are incomplete.
        if(total<original.length * 0.75 || total>MAX_TEXT) return original
        val byY=fragments.sortedWith(compareBy<PdfTextFragment> { (it.top+it.bottom)/2f }
            .thenBy{it.left})
        val rows=mutableListOf<MutableList<PdfTextFragment>>()
        var y=Float.NaN
        for(fragment in byY) {
            val mid=(fragment.top+fragment.bottom)/2f
            val tolerance= if(rows.isEmpty()) 2.4f else
                maxOf(1.5f,minOf(2.4f,(fragment.bottom-fragment.top)*0.38f))
            if(rows.isEmpty() || abs(mid-y)>tolerance) {
                rows.add(mutableListOf(fragment))
                y=mid
            } else {
                rows.last().add(fragment)
                y=rows.last().map{(it.top+it.bottom)/2f}.average().toFloat()
            }
        }
        val output=rows.joinToString("\n"){line->
            val sorted=line.sortedBy{it.left}
            // Overlapping non-identical text boxes can indicate duplicate render
            // layers / multi-column extraction. Fall back without deleting data.
            for(i in 1 until sorted.size) {
                val previous=sorted[i-1]
                if(sorted[i].left < previous.right-1f && sorted[i].text!=previous.text)
                    return original
            }
            sorted.joinToString(" "){it.text.trim()}
        }
        return if(output.length > MAX_TEXT || output.isBlank())original else output
    }
}
