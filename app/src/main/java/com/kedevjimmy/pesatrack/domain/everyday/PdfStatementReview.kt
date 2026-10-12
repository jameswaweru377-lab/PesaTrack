package com.kedevjimmy.pesatrack.domain.everyday

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy

data class PdfStatementCandidates(
    val rows: List<StatementRow>,
    val problems: List<String>,
    /** Statement layout identifies the Kenya currency, but authenticity is not verified. */
    val currencyReview: String? = null,
    /** Zero-value informational receipts are verified, but never posted as cash. */
    val informationalRows: Int = 0
)

/** Bounded M-Pesa text-layout adapter. Unsupported/partial evidence offers no import candidates. */
object PdfStatementReview {
    fun requireUniqueReferences(rows: List<StatementRow>) {
        val references=rows.map {it.reference.orEmpty().trim().uppercase(Locale.ROOT)}
        require(references.all {Regex("[A-Z0-9]{10}").matches(it)}) {"Each entry needs the 10-letter/digit M-Pesa receipt reference from the statement."}
        require(references.distinct().size==references.size) {"Two entries use the same receipt reference. Correct the reference or cancel this edit."}
    }
    fun correct(date: String,description: String,amount: String,reference: String,zone: TimeZone,now: Long=System.currentTimeMillis()): StatementRow {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}").matches(date)){"Use yyyy-MM-dd HH:mm:ss for the date and time."}
        val position=ParsePosition(0)
        val epoch=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply {isLenient=false;timeZone=zone}.parse(date,position)?.time
        require(epoch!=null && position.index==date.length){"Enter a valid date and time."}
        val money=requireNotNull(EverydayMoneyPolicy.money(amount)){"Enter a valid signed KES amount."}
        val canonicalReference=reference.trim().uppercase(Locale.ROOT)
        require(Regex("[A-Z0-9]{10}").matches(canonicalReference)){ "Enter the 10-letter/digit M-Pesa receipt reference from the statement." }
        return StatementRow(epoch,description.trim(),money,canonicalReference).also {StatementCsv.validateRows(listOf(it),now)}
    }
    private val start=Regex("(?m)^[ \\t]*(\\S+)\\s+(?=[0-9]{4}[-/]|[0-9]{2}[/.-])")
    private val status=Regex("\\b(?:COMPLETED|FAILED|REVERSED|PENDING)\\b",RegexOption.IGNORE_CASE)
    private val time=Regex("[0-9]{2}:[0-9]{2}:[0-9]{2}")
    private val transaction=Regex("^([A-Z0-9]{10})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})\\s+([0-9]{2}:[0-9]{2}:[0-9]{2})\\s+(.+?)\\s+COMPLETED\\s+(.+)$",RegexOption.IGNORE_CASE)
    private val moneyCell=Regex("(?:-|(?:-?[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?))")
    private val explicitZero=Regex("0+(?:\\.0{1,2})?")
    private val footer=Regex("(?im)^[ \\t]*(?:DISCLAIMER\\b|STATEMENT VERIFICATION CODE\\b|FOR SELF[- ]HELP\\b)")
    private val officialLayout=Regex("\\bM[ -]?PESA STATEMENT\\b")
    private val explicitForeign=Regex("\\b(?:USD|EUR|GBP|TZS|UGX|ZAR|NGN)\\b")
    private data class DecodedRow(val row:StatementRow,val balance:Long,val compact:Boolean,val signedDebit:Boolean)
    /**
     * Android PDF text extraction can emit money cells on separate lines after
     * the Completed status. Recover only when exactly two or three unambiguous
     * money tokens remain. Description fragments may wrap below the money cells,
     * but unknown numeric/receipt/status fragments are never silently ignored.
     */
    private fun normalizeReceiptBlock(block:String):String? {
        val raw=block.lines().map {it.trim()}.filter {it.isNotEmpty()}
        if(raw.isEmpty())return null
        val lines=raw.filterNot {line->
            val header=line.uppercase(Locale.ROOT).replace(Regex("\\s+")," ")
            // Safaricom can repeat the column header after the final row.
            Regex("^RECEIPT NO\\.? COMPLETION TIME DETAILS TRANSACTION STATUS PAID IN(?: \\(KES\\))? WITHDRAWN(?: \\(KES\\))? BALANCE(?: \\(KES\\))?$")
                .matches(header)
        }
        val completion=Regex("\\bCOMPLETED\\b",RegexOption.IGNORE_CASE)
        val statusLine=lines.indexOfFirst {completion.containsMatchIn(it)}
        if(statusLine<0 || lines.count {completion.containsMatchIn(it)}!=1)return null
        val prior=lines.take(statusLine+1).joinToString(" ").replace(Regex("\\s+")," ")
        val completed=completion.find(prior)?:return null
        val before=prior.substring(0,completed.range.first).trim()
        val after=prior.substring(completed.range.last+1).trim()
        val continuation=lines.drop(statusLine+1)
        // Two or more money cells already attached to the Completed line
        // followed by a separate numeric-only line are ambiguous: the latter
        // could be stray text or an additional table column. Without PDF
        // column coordinates, never reinterpret it as a valid receipt.
        val inlineMoney=after.split(Regex("\\s+")).count {moneyCell.matches(it)}
        if(inlineMoney>=2 && continuation.any {line ->
                val cells=line.trim().split(Regex("\\s+"))
                cells.isNotEmpty() && cells.all {moneyCell.matches(it)}
            }) return null
        val suffix=(listOf(after)+continuation).filter {it.isNotBlank()}
            .joinToString(" ")
        if(before.isBlank() || suffix.isBlank())return null
        if(status.containsMatchIn(suffix) || time.containsMatchIn(suffix) ||
            start.containsMatchIn(suffix) || suffix.length>280)return null
        val money=mutableListOf<String>()
        val wrapped=mutableListOf<String>()
        var wordsAfterAmount=false
        val tokens=suffix.split(Regex("\\s+"))
        for(token in tokens) {
            if(moneyCell.matches(token)) {
                // A numeric amount after wrapped description is not reliably
                // aligned with either the Paid In or Withdrawn PDF column.
                if(wordsAfterAmount)return null
                money.add(token)
            } else {
                // Merchant/reference words can contain digits; standalone numeric
                // tokens are never silently inserted into a wrapped description.
                if(!token.any(Char::isLetter))return null
                if(money.isNotEmpty())wordsAfterAmount=true
                wrapped.add(token)
            }
        }
        if(money.size !in 2..3)return null
        val description=if(wrapped.isEmpty())before else before+" "+wrapped.joinToString(" ")
        return "$description COMPLETED "+money.joinToString(" ")
    }

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
        val normalized=metadata.replace(Regex("\\s+")," ").uppercase(Locale.ROOT)
        val columnUnits=Regex("\\b(?:PAID IN|WITHDRAWN|BALANCE)\\s*\\(\\s*([A-Z]{3,8})\\s*\\)")
            .findAll(normalized).map {it.groupValues[1]}.toList()
        val titleUnits=Regex("\\bM-PESA\\s+STATEMENT\\s*\\(\\s*([A-Z]{3,8})\\s*\\)")
            .findAll(normalized).map {it.groupValues[1]}.toList()
        val currencies=declared+columnUnits+titleUnits
        val declaredKes=currencies.isNotEmpty() &&
            currencies.all {it in setOf("KES","KSH","KSHS")}
        if(currencies.any {it !in setOf("KES","KSH","KSHS")}) return unsupported(
            "This statement declares a currency other than KES, or contains conflicting currency declarations. Nothing was imported."
        )
        // Safaricom's official M-Pesa statement layout may not print 'Currency: KES'.
        // Require the full document/table structure, not a transaction description or logo alone.
        val structuredKenyanStatement=officialLayout.containsMatchIn(normalized) &&
            normalized.contains("STATEMENT PERIOD") && normalized.contains("DETAILED STATEMENT") &&
            normalized.contains("RECEIPT NO") && normalized.contains("COMPLETION TIME") &&
            normalized.contains("TRANSACTION STATUS") && normalized.contains("PAID IN") &&
            normalized.contains("WITHDRAWN") && normalized.contains("BALANCE") &&
            !explicitForeign.containsMatchIn(normalized)
        if(!declaredKes && !structuredKenyanStatement) return unsupported(
            "KES could not be verified from a currency declaration or the full M-Pesa Kenya statement structure. No transactions were selected."
        )
        if(!officialLayout.containsMatchIn(normalized)) return unsupported(
            "This PDF does not show the required M-Pesa statement header."
        )
        val currencyReview=if(declaredKes) null else
            "KES is inferred from the M-Pesa Kenya statement structure, not an explicit currency declaration. Confirm the original statement uses KES before importing."
        val normalizedHeader=normalized.replace(
            Regex("\\s*\\(\\s*(?:KES|KSH|KSHS)\\s*\\)"),"")
        val headers=Regex("(?:PAID IN WITHDRAWN|WITHDRAWN PAID IN) BALANCE")
            .findAll(normalizedHeader).map {it.value}.toSet()
        if(headers.size!=1)return unsupported("The paid-in and withdrawn columns could not be identified consistently. No transactions were selected.")
        val paidFirst=headers.single().startsWith("PAID IN")
        val decoded=mutableListOf<DecodedRow>()
        for((pageIndex,page) in pages.withIndex()) {
            // The original statement prints a repeated verification/disclaimer footer.
            // It must not be interpreted as continuation of the final receipt row.
            val cutoff=footer.find(page)?.range?.first ?: page.length
            val transactionText=page.substring(0,cutoff)
            val matches=start.findAll(transactionText).toList()
            val prefix=transactionText.substring(0,matches.firstOrNull()?.range?.first ?: transactionText.length)
            if(status.containsMatchIn(prefix) || time.containsMatchIn(prefix))
                return unsupported("Transaction-like text outside a receipt row needs review. No partial import was prepared.")
            for((index,match) in matches.withIndex()) {
                val until=matches.getOrNull(index+1)?.range?.first ?: transactionText.length
                val text=normalizeReceiptBlock(transactionText.substring(match.range.first,until))
                    ?: return unsupported("Page ${pageIndex+1}, receipt row ${index+1}: description or amount columns could not be reconstructed safely. Nothing was imported.")
                if(status.findAll(text).count()!=1 || time.findAll(text).count()!=1)
                    return unsupported("A row has ambiguous transaction boundaries. No partial import was prepared.")
                val result=transaction.matchEntire(text) ?: return unsupported(
                    "The M-Pesa PDF table could not be read reliably in text order. No transactions were imported; use CSV or a different PDF."
                )
                val dateText=result.groupValues[2]+" "+result.groupValues[3]
                val pattern=if(result.groupValues[2].contains('/'))"dd/MM/yyyy HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
                val position=ParsePosition(0)
                val date=SimpleDateFormat(pattern,Locale.ROOT).apply {isLenient=false;timeZone=zone}
                    .parse(dateText,position)?.time
                val cells=result.groupValues[5].split(Regex("\\s+"))
                if(cells.size !in 2..3 || cells.any {!moneyCell.matches(it)})
                    return unsupported("Paid In, Withdrawn and Balance values could not be aligned. No transactions were imported.")
                // A two-cell row containing only a blank ('-') amount is not proof
                // of zero movement: the missing cell may contain an unread debit.
                if(cells.size==2 && cells[0]=="-")
                    return unsupported("A receipt has no readable amount. Nothing was imported.")
                fun value(cell:String):Long? =
                    if(cell=="-")0L else EverydayMoneyPolicy.money(cell)
                val balance=value(cells.last())
                val compact=cells.size==2
                val first=value(cells[0])
                val second=if(compact)0L else value(cells[1])
                if(date==null || date<=0 || date>now || position.index!=dateText.length ||
                    balance==null || balance<0 || first==null || second==null)
                    return unsupported("A transaction date or amount is invalid; no partial import was prepared.")
                val signed=if(compact) {
                    // Official signed-withdrawn table may omit an empty Paid In cell.
                    // A positive compact amount cannot be assigned to a column from text alone;
                    // balance reconciliation below must prove the ordering/sign.
                    first
                } else {
                    val paid=if(paidFirst)first else second
                    val debit=if(paidFirst)second else first
                    if(paid<0 || debit==Long.MIN_VALUE || (paid>0 && debit!=0L))
                        return unsupported("A transaction has ambiguous paid-in and withdrawn amounts. No transactions were selected.")
                    runCatching {
                        if(debit<0) Math.addExact(paid,debit) else Math.subtractExact(paid,debit)
                    }.getOrNull() ?: return unsupported("An amount is outside the supported range. Nothing was imported.")
                }
                if(signed==Long.MIN_VALUE)
                    return unsupported("A transaction amount is outside the supported range. Nothing was imported.")
                // Some M-Pesa statements contain completed zero-value informational
                // receipts. Keep them as balance anchors; never import them as money.
                // Blank cells alone are not enough evidence of a zero-value entry.
                if(signed==0L && !(if(compact) explicitZero.matches(cells[0])
                    else explicitZero.matches(cells[0]) && explicitZero.matches(cells[1])))
                    return unsupported("A zero-value row has missing or ambiguous money cells. Nothing was imported.")
                val description=result.groupValues[4].trim()
                if(description.length !in 1..200)
                    return unsupported("A transaction description is longer than supported. No partial import was prepared.")
                decoded+=DecodedRow(StatementRow(date,description,signed,result.groupValues[1]),
                    balance,compact,compact && first<0)
                if(decoded.size>StatementCsv.MAX_ROWS)
                    return unsupported("This statement exceeds 5,000 transactions. Choose a shorter statement.")
            }
            if(matches.isEmpty() && time.containsMatchIn(transactionText))
                return unsupported("A page has transaction timestamps without readable receipt rows. No partial import was prepared.")
        }
        if(decoded.isEmpty())return unsupported("No supported M-Pesa transaction rows were found. No partial import was prepared.")
        val informational=decoded.count {it.row.signedMinor==0L}
        // Check *all* statement receipts, including any zero-value records that
        // will be excluded from the import review, for duplicate references.
        if(decoded.map {it.row.reference}.distinct().size!=decoded.size)
            return unsupported("Repeated receipt references need review. No transactions were selected.")
        // The omitted money column in a compact row requires corroborating
        // evidence of direction. A zero receipt additionally requires an intact
        // running balance across adjacent rows before it can be left out.
        if(decoded.any {it.compact} || informational>0) {
            if(decoded.size<2)
                return unsupported("An informational receipt has no adjacent balance evidence. Nothing was imported.")
            if(decoded.any {it.compact} &&
                (!structuredKenyanStatement || !paidFirst || decoded.none {it.signedDebit}))
                return unsupported("The PDF omits empty money columns and their direction could not be proved. No transactions were selected.")
            val descending=decoded.first().row.date>=decoded.last().row.date
            // A compact oldest zero record has no preceding balance to prove
            // its zero movement. Refuse to drop a potentially missing debit.
            val oldest=if(descending) decoded.last() else decoded.first()
            if(oldest.compact && oldest.row.signedMinor==0L)
                return unsupported("The oldest zero-value receipt has no earlier balance to verify its amount. Nothing was imported.")
            for(i in 0 until decoded.size-1) {
                val newer=if(descending)decoded[i] else decoded[i+1]
                val older=if(descending)decoded[i+1] else decoded[i]
                if(newer.row.date<older.row.date || runCatching {
                        Math.addExact(older.balance,newer.row.signedMinor)
                    }.getOrNull()!=newer.balance)
                    return unsupported("The PDF's Paid In/Withdrawn columns could not be reconciled with successive balances. No transactions were selected.")
            }
        }
        val rows=decoded.map {it.row}.filter {it.signedMinor!=0L}
        if(rows.isEmpty())return unsupported(
            "This statement has only verified zero-value informational receipts; there are no monetary transactions to import."
        )
        val validation=runCatching {StatementCsv.validateRows(rows,now)}
        if(validation.isFailure)return unsupported(
            "A statement amount, date or description failed validation. Nothing was imported."
        )
        return PdfStatementCandidates(rows,emptyList(),currencyReview,informational)
    }
}
