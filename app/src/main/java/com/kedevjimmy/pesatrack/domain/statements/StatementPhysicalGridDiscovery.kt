package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfGridSegment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import kotlin.math.abs

/**
 * A discovered physical, seven-column financial table. Coordinates are in the
 * same top-left PDFBox text space as PdfTextFragment. Edge 0..1 is the *actual*
 * printed header cell, subsequent adjacent pairs are body rows.
 */
data class StatementPhysicalGridTable(
    val pageNumber: Int,
    val columns: StatementPdfColumns,
    val xEdges: List<Float>,
    val yEdges: List<Float>
) {
    init {
        require(pageNumber > 0 && columns.columns.size == xEdges.size - 1)
        require(xEdges.size in 6..21 && yEdges.size in 3..5002)
        require(xEdges.zipWithNext().all { (a,b) -> b-a >= 4f })
        require(yEdges.zipWithNext().all { (a,b) -> b-a >= 3f })
    }

    /** Header is region 0; receipt records occupy regions 1 onward. */
    fun rowOf(y: Float): Int? {
        if (!y.isFinite()) return null
        return (0 until yEdges.lastIndex).singleOrNull {
            y >= yEdges[it] - .65f && y < yEdges[it+1] + .65f
        }
    }

    /** A word may overhang a drawn line slightly but not into another real cell. */
    fun owner(word: PdfTextFragment, expectedRow: Int? = null): StatementField? {
        val y = (word.top + word.bottom) / 2f
        val row = rowOf(y) ?: return null
        if (row == 0 || (expectedRow != null && row != expectedRow)) return null
        if (word.top < yEdges[row] - 3f || word.bottom > yEdges[row+1] + 3f) return null
        val x = (word.left + word.right) / 2f
        val index = (0 until xEdges.lastIndex).singleOrNull {
            x >= xEdges[it] && x < xEdges[it+1]
        } ?: return null
        if (word.left < xEdges[index] - 3f ||
            word.right > xEdges[index+1] + 3f) return null
        return columns.columns[index].field
    }
}

/**
 * Tabula-inspired vector-intersection extraction. This is a separate physical
 * table finder, never a replacement for M-Pesa receipt/balance verification.
 *
 * No invented edges, no arbitrary midpoint-derived financial column widths.
 * Missing/ambiguous grid evidence produces no grid, not a guessed table.
 */
object StatementPhysicalGridDiscovery {
    private data class Rule(val axis: Float, val from: Float, val to: Float)
    private val needed = setOf(StatementField.REFERENCE,StatementField.DATE,
        StatementField.DESCRIPTION,StatementField.STATUS,StatementField.CREDIT,
        StatementField.DEBIT,StatementField.BALANCE)

    private fun rules(segments: List<PdfGridSegment>, vertical: Boolean): List<Rule> {
        val raw = segments.mapNotNull { s ->
            val axisDelta = if(vertical)abs(s.x2-s.x1) else abs(s.y2-s.y1)
            if(axisDelta>1.25f) null
            else {
                val axis=if(vertical)(s.x1+s.x2)/2f else (s.y1+s.y2)/2f
                val from=if(vertical)minOf(s.y1,s.y2) else minOf(s.x1,s.x2)
                val to=if(vertical)maxOf(s.y1,s.y2) else maxOf(s.x1,s.x2)
                if(to-from<2f)null else Rule(axis,from,to)
            }
        }.sortedWith(compareBy<Rule>{it.axis}.thenBy{it.from})
        val merged = mutableListOf<Rule>()
        for(p in raw) {
            val previous=merged.lastOrNull()
            if(previous!=null && abs(previous.axis-p.axis)<=1.25f &&
                p.from<=previous.to+3f) {
                merged[merged.lastIndex]=Rule(previous.axis,minOf(previous.from,p.from),
                    maxOf(previous.to,p.to))
            } else merged.add(p)
        }
        return merged
    }

    private fun unique(values:List<Float>):List<Float> {
        val sorted=values.sorted()
        val result=mutableListOf<Float>()
        for(x in sorted) if(result.none { abs(x-it)<=1.5f })result.add(x)
        return result
    }

    fun discover(
        page: Int,
        width: Float,
        height: Float,
        words: List<PdfTextFragment>,
        vectorLines: List<PdfGridSegment>
    ): List<StatementPhysicalGridTable> {
        if(page !in 1..160 || !width.isFinite() || !height.isFinite() ||
            width<=0f || height<=0f || vectorLines.size>10000 ||
            words.size>32000 || vectorLines.any {
                !it.x1.isFinite() || !it.x2.isFinite() ||
                !it.y1.isFinite() || !it.y2.isFinite()
            }) return emptyList()
        val hs=rules(vectorLines,false)
        val vs=rules(vectorLines,true)
        val xs=unique(vs.filter {it.to-it.from>=height*.08f}.map{it.axis})
        if(xs.size !in 8..80 || hs.size>5000 || vs.size>5000)return emptyList()
        val found=mutableListOf<StatementPhysicalGridTable>()
        // Eight observed vertical boundaries = seven physical money/metadata
        // columns. Other bank schemas can be added separately with tests.
        for(first in 0..xs.size-8) {
            val edges=xs.subList(first,first+8)
            if(edges.zipWithNext().any {(a,b)->b-a<4f})continue
            // Keep only horizontal rules that genuinely span the candidate.
            // Separate segments are first joined conservatively by rules().
            val ys=unique(hs.filter {
                it.from<=edges.first()+2f && it.to>=edges.last()-2f
            }.map{it.axis})
            if(ys.size !in 3..5002)continue
            for(header in 0 until ys.size-2) {
                val top=ys[header]
                val next=ys[header+1]
                if(next-top !in 6f..70f)continue
                val headerWords=words.filter {
                    val cx=(it.left+it.right)/2f
                    val cy=(it.top+it.bottom)/2f
                    cx>=edges.first() && cx<edges.last() &&
                        cy>top+.4f && cy<next-.4f
                }
                if(headerWords.size !in 7..40)continue
                // Unlike the legacy header-midpoint detector, the observed
                // physical cell boundaries themselves partition header text.
                // Every cell must independently have a known semantic label.
                val headings=(0 until 7).map {column->
                    headerWords.filter {word->
                        val x=(word.left+word.right)/2f
                        x>=edges[column] && x<edges[column+1]
                    }.sortedWith(compareBy<PdfTextFragment>{it.top}.thenBy{it.left})
                }
                if(headings.any {it.isEmpty()})continue
                val observed=headings.map {cell->
                    UniversalStatementTableInterpreter.fieldForHeader(
                        cell.joinToString(" ") {it.text}
                    )
                }
                if(observed.toSet()!=needed || observed.distinct().size!=7)continue

                // Every real border must span the detected transaction region.
                // Pick the longest connected continuation of complete rows.
                var end=header+2
                while(end<ys.size && edges.all {x->
                    vs.any {abs(it.axis-x)<=1.5f &&
                        it.from<=top+3f && it.to>=ys[end]-3f}
                })end++
                val bottom=ys[end-1]
                if(bottom-next < 4f)continue
                val physical=StatementPdfColumns(headings.mapIndexed{i,cell->
                    StatementPdfColumn(observed[i],cell,edges[i],edges[i+1])
                })
                val table=StatementPhysicalGridTable(page,physical,
                    edges.toList(),ys.subList(header,end))
                // Never accept two competing grids covering the same cells.
                if(found.any {abs(it.yEdges.first()-table.yEdges.first())<2f &&
                    abs(it.xEdges.first()-table.xEdges.first())<2f})continue
                found.add(table)
            }
        }
        if(found.size>8)return emptyList()
        return found.sortedBy {it.yEdges.first()}
    }
}
