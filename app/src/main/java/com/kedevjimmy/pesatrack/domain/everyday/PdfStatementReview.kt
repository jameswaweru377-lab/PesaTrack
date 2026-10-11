package com.kedevjimmy.pesatrack.domain.everyday

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy

data class PdfStatementCandidates(val rows: List<StatementRow>,val problems: List<String>)

/** Bounded M-Pesa text-layout adapter. Unsupported/partial evidence offers no import candidates. */
object PdfStatementReview {
    fun correct(date: String,description: String,amount: String,reference: String,zone: TimeZone,now: Long=System.currentTimeMillis()): StatementRow {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}").matches(date)){"Use yyyy-MM-dd HH:mm:ss for the date and time."}
        val position=ParsePosition(0)
        val epoch=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply {isLenient=false;timeZone=zone}.parse(date,position)?.time
        require(epoch!=null && position.index==date.length){"Enter a valid date and time."}
        val money=requireNotNull(EverydayMoneyPolicy.money(amount)){"Enter a valid signed KES amount."}
        require(reference.trim().isNotEmpty()){ "Enter the receipt reference from the statement." }
        return StatementRow(epoch,description.trim(),money,reference.trim()).also {StatementCsv.validateRows(listOf(it),now)}
    }
    private val start=Regex("(?m)^[ \\t]*(\\S+)\\s+(?=[0-9]{4}[-/]|[0-9]{2}[/.-])")
    private val status=Regex("\\b(?:COMPLETED|FAILED|REVERSED|PENDING)\\b")
    private val time=Regex("[0-9]{2}:[0-9]{2}:[0-9]{2}")
    private val transaction=Regex("^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})\\s+(.+?)\\s+COMPLETED\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$")
    fun read(pages: List<String>,zone: TimeZone,now: Long=System.currentTimeMillis()): PdfStatementCandidates {
        fun unsupported(reason: String)=PdfStatementCandidates(emptyList(),listOf(reason))
        PdfTextReviewPolicy.preview(pages)
        if(pages.any {it.isBlank()})return unsupported("Some pages have no text. A complete transaction review is unavailable; use CSV or manual entry.")
        val metadata=pages.joinToString("\n") {page->page.substring(0,start.find(page)?.range?.first ?: page.length)}
        val lines=metadata.lines().map {it.trim().uppercase(Locale.ROOT)}.filter {it.isNotEmpty()}
        val declared=lines.mapIndexedNotNull {i,line->
            Regex("^CURRENCY(?: CODE)?(?:\\s*[:=]\\s*|\\s+)(.+)$").matchEntire(line)?.groupValues?.get(1)
                ?: if(Regex("CURRENCY(?: CODE)?\\s*[:=]?").matches(line))lines.getOrNull(i+1) ?: "" else null
        }
        if(declared.isEmpty() || declared.any {!Regex("KES|KSH|KSHS").matches(it)})
            return unsupported("KES currency could not be established from the statement metadata, or a conflicting currency was found. No transaction candidates were prepared.")
        val normalized=metadata.replace(Regex("\\s+")," ").uppercase(Locale.ROOT)
        if(!normalized.contains("M-PESA") || !Regex("\\b(?:KES|KSH|KSHS)\\b").containsMatchIn(normalized))
            return unsupported("This PDF layout or currency is not supported for transaction import. Use supported CSV or manual entry.")
        val headers=Regex("(?:PAID IN WITHDRAWN|WITHDRAWN PAID IN) BALANCE").findAll(normalized).map {it.value}.toSet()
        if(headers.size!=1)return unsupported("The paid-in and withdrawn columns could not be identified consistently. No transactions were selected.")
        val paidFirst=headers.single().startsWith("PAID IN")
        val rows=mutableListOf<StatementRow>()
        for(page in pages) {
            val matches=start.findAll(page).toList()
            val prefix=page.substring(0,matches.firstOrNull()?.range?.first ?: page.length)
            if(status.containsMatchIn(prefix) || Regex("[0-9]{2}:[0-9]{2}:[0-9]{2}").containsMatchIn(prefix))
                return unsupported("Some transaction-like text could not be assigned to a complete row. No partial import was prepared.")
            for((index,match) in matches.withIndex()) {
                val end=matches.getOrNull(index+1)?.range?.first ?: page.length
                val text=page.substring(match.range.first,end).trim().replace(Regex("\\s+")," ")
                if(status.findAll(text).count()!=1 || time.findAll(text).count()!=1)return unsupported("A row contains ambiguous transaction boundaries. No partial import was prepared.")
                val row=transaction.matchEntire(text) ?: return unsupported("A transaction row could not be read completely. No partial import was prepared; use CSV or manual entry.")
                val dateText=row.groupValues[2]+" "+row.groupValues[3]
                val pattern=if(row.groupValues[2].contains('/'))"dd/MM/yyyy HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
                val position=ParsePosition(0)
                val date=SimpleDateFormat(pattern,Locale.ROOT).apply {isLenient=false;timeZone=zone}.parse(dateText,position)?.time
                fun amount(text: String): Long? = if(text=="-")0 else EverydayMoneyPolicy.money(text)?.takeIf {it>=0}
                val first=amount(row.groupValues[5]);val second=amount(row.groupValues[6]);val balance=amount(row.groupValues[7])
                if(date==null || position.index!=dateText.length || date<=0 || date>now || first==null || second==null || balance==null || (first>0)==(second>0))
                    return unsupported("A transaction date or amount is ambiguous. No partial import was prepared; check the original statement.")
                val paid=if(paidFirst)first else second;val withdrawn=if(paidFirst)second else first
                val description=row.groupValues[4].trim()
                if(description.length !in 1..200)return unsupported("A transaction description exceeds the supported length. Use CSV or manual entry.")
                rows+=StatementRow(date,description,paid-withdrawn,row.groupValues[1])
                if(rows.size>StatementCsv.MAX_ROWS)return unsupported("This statement exceeds 2,000 transactions. Choose a shorter statement.")
            }
            // A page with transaction column headings but no decodable receipts is never silently ignored.
            if(matches.isEmpty() && Regex("[0-9]{2}:[0-9]{2}:[0-9]{2}").containsMatchIn(page))
                return unsupported("A page contains transaction times without readable receipt rows. No partial import was prepared.")
        }
        if(rows.isEmpty())return unsupported("No supported M-Pesa transaction rows were found. Read the extracted text below or use CSV/manual entry.")
        if(rows.map {it.reference}.distinct().size!=rows.size)return unsupported("Repeated receipt references need review. No transactions were selected.")
        StatementCsv.validateRows(rows,now)
        return PdfStatementCandidates(rows,emptyList())
    }
}
