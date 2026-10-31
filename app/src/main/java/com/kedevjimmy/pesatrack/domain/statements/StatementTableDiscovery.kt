package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import kotlin.math.abs

data class StatementPhysicalLine(val y:Float,val words:List<PdfTextFragment>) {
    val text:String get()=words.joinToString(" ") {it.text}.trim()
}

/** Shared bounded physical reading order; never drops invalid fragments. */
object StatementPdfLines {
    fun group(words:List<PdfTextFragment>):List<StatementPhysicalLine>? {
        if(words.isEmpty() || words.size>32_000 || words.any {
            it.text.isBlank() || it.text.length>1200 || it.text.any(Char::isISOControl) ||
                !it.left.isFinite() || !it.top.isFinite() || !it.right.isFinite() ||
                !it.bottom.isFinite() || it.left<0 || it.top<0 ||
                it.right<=it.left || it.bottom<=it.top
        })return null
        val groups=mutableListOf<MutableList<PdfTextFragment>>()
        for(word in words.sortedWith(compareBy<PdfTextFragment> {(it.top+it.bottom)/2f}.thenBy {it.left})) {
            val previous=groups.lastOrNull()
            val old=previous?.map {(it.top+it.bottom)/2f}?.average()?.toFloat()
            val tolerance=maxOf(1.5f,minOf(3.4f,(word.bottom-word.top)*0.38f))
            if(old==null || abs((word.top+word.bottom)/2f-old)>tolerance)
                groups.add(mutableListOf(word))
            else previous.add(word)
            if(groups.size>20_000)return null
        }
        return groups.map {StatementPhysicalLine(
            it.map {box->(box.top+box.bottom)/2f}.average().toFloat(),it.sortedBy {box->box.left}
        )}
    }
}

data class StatementHeaderBand(val start:Int,val end:Int,val columns:StatementPdfColumns)

sealed class StatementTableDiscoveryResult {
    data class Found(val tables:List<StatementTablePage>):StatementTableDiscoveryResult()
    data class Blocked(val pageNumber:Int,val reason:StatementGeometryProblem):StatementTableDiscoveryResult()
}

/**
 * Provider-neutral discovery of selectable-text, borderless table regions.
 * Schemas come from semantic headings, not provider names or fixed positions.
 * No footer is guessed away: all text after the last header must own a cell.
 * Provider adapters can separately validate cover/summary/footer boundaries.
 */
object StatementTableDiscovery {
    private val date=Regex("^(?:[0-9]{4}[-/][0-9]{2}[-/][0-9]{2}|[0-9]{2}[/.-][0-9]{2}[/.-][0-9]{4})(?:[ T].*)?$")
    private val schemas=listOf(
        setOf(StatementField.DATE,StatementField.DESCRIPTION,StatementField.DEBIT,StatementField.CREDIT,StatementField.BALANCE),
        setOf(StatementField.DATE,StatementField.DESCRIPTION,StatementField.AMOUNT,StatementField.DIRECTION,StatementField.BALANCE)
    )
    private val explicitSignedSchema=setOf(
        StatementField.DATE,StatementField.DESCRIPTION,StatementField.AMOUNT,StatementField.BALANCE)
    private val optional=setOf(StatementField.REFERENCE,StatementField.STATUS,StatementField.VALUE_DATE,
        StatementField.FEE,StatementField.INFORMATION)

    fun headers(lines:List<StatementPhysicalLine>,required:Set<StatementField>,
        profile:StatementFormatProfile=StatementFormatProfile(),optionalFields:Set<StatementField> = emptySet()
    ):List<StatementHeaderBand> {
        val candidates=lines.indices.flatMap {start ->
            (start until minOf(lines.size,start+3)).mapNotNull {end ->
                if(lines[end].y-lines[start].y>24f)null else
                    StatementPdfColumnDetector.detect(lines.subList(start,end+1).flatMap {it.words},
                        required,profile,optionalFields)?.let {StatementHeaderBand(start,end,it)}
            }
        }
        // A stacked band can also contain a complete single-line subset.
        // Prefer the band owning all observed words, never reconstruct both.
        val selected=mutableListOf<StatementHeaderBand>()
        for(band in candidates.sortedWith(compareByDescending<StatementHeaderBand> {
            it.columns.columns.sumOf {column->column.heading.size}
        }.thenBy {it.start})) {
            if(selected.none {it.start<=band.end && band.start<=it.end})selected.add(band)
        }
        return selected.sortedBy {it.start}
    }

    fun discover(pageNumber:Int,words:List<PdfTextFragment>,
        profile:StatementFormatProfile=StatementFormatProfile()
    ):StatementTableDiscoveryResult {
        fun reject(reason:StatementGeometryProblem)=StatementTableDiscoveryResult.Blocked(pageNumber,reason)
        if(pageNumber !in 1..160)return reject(StatementGeometryProblem.INVALID_BOUNDS)
        val lines=StatementPdfLines.group(words) ?:return reject(StatementGeometryProblem.INVALID_BOUNDS)
        val signed=headers(lines,explicitSignedSchema,profile,optional).filter { band ->
            band.columns.columns.any { column ->
                column.field==StatementField.AMOUNT &&
                    column.heading.joinToString(" ") { it.text.trim() }
                        .replace(Regex("\\s+")," ").equals("Signed Amount",ignoreCase=true)
            }
        }
        val bands=(schemas.flatMap {headers(lines,it,profile,optional)}+signed)
            .distinctBy {it.start to it.end}.sortedBy {it.start}
        if(bands.isEmpty())return reject(StatementGeometryProblem.INVALID_HEADER)
        if(bands.zipWithNext().any {(a,b)->a.end>=b.start})return reject(StatementGeometryProblem.INVALID_HEADER)
        val allHeaderWords=bands.flatMap {it.columns.columns.flatMap {column->column.heading}}.toSet()
        val potentialDates=words.filter {date.matches(it.text.trim()) && it !in allHeaderWords}
        val assignedDates=mutableSetOf<PdfTextFragment>()
        val tables=mutableListOf<StatementTablePage>()
        for((index,band) in bands.withIndex()) {
            val body=lines.subList(band.end+1,bands.getOrNull(index+1)?.start ?:lines.size).flatMap {it.words}
            if(body.isEmpty())return reject(StatementGeometryProblem.INVALID_ROWS)
            val columns=band.columns.columns
            fun owner(word:PdfTextFragment)=columns.indexOfFirst {word.left>=it.left && word.right<=it.right}.takeIf {it>=0}
            val assigned=body.map {it to owner(it)}
            if(assigned.any {it.second==null})return reject(StatementGeometryProblem.UNASSIGNED_FRAGMENT)
            val dateIndex=columns.indexOfFirst {it.field==StatementField.DATE}
            val anchors=assigned.filter {(word,col)->col==dateIndex && date.matches(word.text.trim())}
                .map {it.first}.sortedBy {it.top}
            if(anchors.isEmpty() || anchors.size>5_000)return reject(StatementGeometryProblem.MISSING_DATE_ANCHORS)
            if(anchors.zipWithNext().any {(a,b)->b.top-a.top<=2.5f})return reject(StatementGeometryProblem.INVALID_ROWS)
            val cells=List(anchors.size){List(columns.size){mutableListOf<PdfTextFragment>()}}
            for((word,col) in assigned) {
                val y=(word.top+word.bottom)/2f
                val row=anchors.indices.lastOrNull {y>=anchors[it].top-2.5f}
                    ?:return reject(StatementGeometryProblem.UNASSIGNED_FRAGMENT)
                cells[row][col!!].add(word)
            }
            val ordered=cells.map {row->row.map {cell->cell.sortedWith(
                compareBy<PdfTextFragment> {it.top}.thenBy {it.left})}}
            val rows=ordered.map {row->row.map {cell->cell.joinToString(" "){it.text.trim()}}}
            tables.add(StatementTablePage(pageNumber,columns.map {column->column.heading.sortedWith(
                compareBy<PdfTextFragment> {it.top}.thenBy {it.left}).joinToString(" "){it.text}},rows,ordered,index+1))
            assignedDates.addAll(anchors)
        }
        // A transaction before a header, or a second date in an unexpected
        // column, is unresolved evidence rather than permission to lose it.
        if(potentialDates.toSet()!=assignedDates)return reject(StatementGeometryProblem.UNASSIGNED_FRAGMENT)
        if(tables.sumOf {it.rows.size}>5_000)return reject(StatementGeometryProblem.INVALID_ROWS)
        return StatementTableDiscoveryResult.Found(tables)
    }
}
