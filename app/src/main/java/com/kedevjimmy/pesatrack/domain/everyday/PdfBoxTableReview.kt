package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxVerificationReport
import com.kedevjimmy.pesatrack.domain.statements.StatementField
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import com.kedevjimmy.pesatrack.domain.statements.UniversalStatementTableInterpreter
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Shared-table M-Pesa adapter. No formatted transaction text is reparsed. */
object PdfBoxTableReview {
    private val required=setOf(StatementField.REFERENCE,StatementField.DATE,StatementField.DESCRIPTION,
        StatementField.STATUS,StatementField.CREDIT,StatementField.DEBIT,StatementField.BALANCE)

    fun read(tables:List<StatementTablePage>,report:PdfBoxVerificationReport,
        currencyReview:String?,zone:TimeZone,now:Long=System.currentTimeMillis(),
        declarations:PdfBoxStatementDeclarations=PdfBoxStatementDeclarations()
    ):PdfStatementCandidates {
        fun blocked(page:Int?=null,row:Int?=null)=PdfStatementCandidates(emptyList(),listOf(
            "Structured statement evidence could not be verified"+
                (page?.let {" on page $it"} ?: "")+(row?.let {", row $it"} ?: "")+". Nothing was imported."))
        if(tables.isEmpty() || tables.sumOf {it.rows.size}!=report.receiptRows)return blocked()
        val evidence=mutableListOf<PdfBoxReceiptEvidence>()
        val rows=mutableListOf<StatementRow>()
        val statementActivityLines=mutableListOf<StatementActivityLine>()
        for(table in tables) {
            val fields=table.headers.map {UniversalStatementTableInterpreter.fieldForHeader(it)}
            if(table.pageNumber !in 1..160 || fields.size!=required.size || fields.toSet()!=required)
                return blocked(table.pageNumber)
            for((index,cells) in table.rows.withIndex()) {
                if(cells.size!=fields.size)return blocked(table.pageNumber,index+1)
                fun value(field:StatementField)=cells[fields.indexOf(field)].trim()
                val reference=value(StatementField.REFERENCE).uppercase(Locale.ROOT)
                val description=value(StatementField.DESCRIPTION)
                val date=value(StatementField.DATE)
                if(!Regex("[A-Z0-9]{10}").matches(reference) || description.length !in 1..200 ||
                    !value(StatementField.STATUS).equals("Completed",true))return blocked(table.pageNumber,index+1)
                val format=if(date.contains('/'))"dd/MM/yyyy HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
                val pos=ParsePosition(0)
                val epoch=SimpleDateFormat(format,Locale.ROOT).apply {isLenient=false;timeZone=zone}
                    .parse(date,pos)?.time
                if(epoch==null || pos.index!=date.length || epoch<=0 || epoch>now)return blocked(table.pageNumber,index+1)
                fun amount(field:StatementField,emptyAllowed:Boolean):Long? {
                    val text=value(field)
                    if(text.isEmpty() || text=="-")return if(emptyAllowed)0L else null
                    return EverydayMoneyPolicy.money(text)
                }
                val paid=amount(StatementField.CREDIT,true)
                val debit=amount(StatementField.DEBIT,true)
                val balance=amount(StatementField.BALANCE,false)
                if(paid==null || paid<0 || debit==null || debit==Long.MIN_VALUE || balance==null || balance<0 ||
                    paid>0 && debit!=0L)return blocked(table.pageNumber,index+1)
                if(paid==0L && debit==0L && listOf(StatementField.CREDIT,StatementField.DEBIT).none {
                    EverydayMoneyPolicy.money(value(it))==0L
                })return blocked(table.pageNumber,index+1)
                val e=PdfBoxReceiptEvidence(table.pageNumber,index+1,reference,date,description,emptyMap(),paid,abs(debit),balance)
                evidence.add(e)
                statementActivityLines.add(StatementActivityLine(table.pageNumber,index+1,reference,
                    epoch,description,paid,abs(debit),balance))
                if(e.movementMinor!=0L)rows.add(StatementRow(epoch,description,e.movementMinor,reference))
            }
        }
        // Recheck cell arithmetic and the complete receipt-group sequence
        // after transport. Summary/boundary declarations were verified before
        // transport; their privacy-safe proof flags remain in the report.
        val proof=PdfBoxStatementFinancialAudit.verify(evidence,emptyList(),declarations) ?:return blocked()
        if(rows.size!=report.cashRows || proof.verifiedTransitions!=report.verifiedTransitions ||
            proof.openingVerified!=report.openingVerified || proof.closingVerified!=report.closingVerified ||
            proof.totalsVerified!=report.totalsVerified)return blocked()
        if(runCatching {StatementCsv.validateRows(rows,now);PdfStatementReview.requireDistinctStatementLegs(rows)}.isFailure)
            return blocked()
        return PdfStatementCandidates(rows,emptyList(),currencyReview,
            informationalRows=evidence.size-rows.size,verifiedReceiptTransitions=proof.verifiedTransitions,
            totalReceiptTransitions=proof.verifiedTransitions,
            statementActivityLines=statementActivityLines.toList())
    }
}
