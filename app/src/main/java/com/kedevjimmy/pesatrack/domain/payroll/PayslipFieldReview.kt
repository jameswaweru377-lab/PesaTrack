package com.kedevjimmy.pesatrack.domain.payroll

import java.util.Locale

data class PayslipSuggestions(val grossMinor: Long?, val netMinor: Long?, val lines: List<PayrollLine>, val warnings: List<String>)

/** Bounded one-column label suggestions. User correction/review remains mandatory; no cash authority. */
object PayslipFieldReview {
    private data class Field(val key: String, val type: PayrollLineType? = null,
        val role: PayrollLineRole = PayrollLineRole.EMPLOYEE_DEDUCTION)
    private val labels = buildMap<String, Field> {
        for(label in listOf("GROSS PAY","GROSS EARNINGS","TOTAL EARNINGS")) put(label,Field("GROSS"))
        for(label in listOf("NET PAY","NET PAYABLE","NET SALARY")) put(label,Field("NET"))
        fun deduction(type: PayrollLineType,vararg names: String) { for(name in names) put(name,Field("EMPLOYEE:$type",type)) }
        deduction(PayrollLineType.TAX,"PAYE","INCOME TAX")
        deduction(PayrollLineType.HEALTH,"NHIF","SHIF","SHA","HEALTH CONTRIBUTION")
        deduction(PayrollLineType.NSSF,"NSSF")
        deduction(PayrollLineType.PENSION,"PENSION","EMPLOYEE PENSION")
        deduction(PayrollLineType.SACCO_SHARES,"SACCO SHARES","SACCO SHARE CAPITAL")
        deduction(PayrollLineType.SACCO_DEPOSIT,"SACCO DEPOSIT")
        deduction(PayrollLineType.INSURANCE,"INSURANCE","INSURANCE PREMIUM")
        deduction(PayrollLineType.LOAN_PRINCIPAL,"LOAN PRINCIPAL")
        deduction(PayrollLineType.LOAN_INTEREST,"LOAN INTEREST")
        for((name,type) in listOf("EMPLOYER PENSION" to PayrollLineType.PENSION,"EMPLOYER NSSF" to PayrollLineType.NSSF))
            put(name,Field(name,type,PayrollLineRole.EMPLOYER_CONTRIBUTION))
    }
    private val row=Regex("^([A-Za-z][A-Za-z /()-]{0,79}?)(?:\\s*[:=]\\s*|\\t+| {2,})(.*)$",RegexOption.IGNORE_CASE)
    private val value=Regex("^(?:KES\\s+|KSH\\s+)?([0-9][0-9,.]*)$",RegexOption.IGNORE_CASE)
    fun read(pages: List<String>): PayslipSuggestions {
        require(pages.size in 1..40 && pages.sumOf { it.length.toLong() } <= 100000) { "Choose a shorter payslip." }
        if(pages.any { it.isBlank() }) return PayslipSuggestions(null,null,emptyList(),listOf("Some pages have no selectable text. Enter payroll fields manually."))
        if(pages.any { Regex("\\b(?:USD|EUR|GBP|TZS|UGX|ZAR|DOLLARS)\\b|[$€£]",RegexOption.IGNORE_CASE).containsMatchIn(it) })
            return PayslipSuggestions(null,null,emptyList(),listOf("A currency other than KES was found. KES fields were not prefilled."))
        data class Observed(val field: Field,val label: String,val amount: Long)
        val observed=mutableListOf<Observed>();val occurrences=mutableMapOf<String,Int>();var unsupported=false
        pages.forEach { page->page.lineSequence().forEach { raw->
            val text=raw.trim();val match=row.matchEntire(text)
            if(match != null) {
                val label=match.groupValues[1].trim(); val field=labels[label.uppercase(Locale.ROOT).replace(Regex("\\s+")," ")]
                if(field!=null) occurrences[field.key]=(occurrences[field.key] ?: 0)+1
                val amount=value.matchEntire(match.groupValues[2])?.let {PayrollJournalPolicy.parseAmount(it.groupValues[1])}
                if(field!=null && amount!=null) observed+=Observed(field,label,amount) else unsupported=true
            } else if(text.any { it.isDigit() }) unsupported=true
        } }
        val warnings=mutableListOf("Suggestions are not verified payroll. Check every amount, missing line, payroll month and KES currency before saving.")
        if(unsupported) warnings+="Some labels or table rows were not understood. Enter missing fields manually."
        val groups=observed.groupBy { it.field.key }
        val unique=groups.filter { (key,items)->items.size==1 && occurrences[key]==1 }.values.map { it.single() }
        if(occurrences.values.any { it>1 }) warnings+="Repeated or conflicting labels were left blank. Check them manually; no values were summed."
        return PayslipSuggestions(unique.firstOrNull {it.field.key=="GROSS"}?.amount,
            unique.firstOrNull {it.field.key=="NET"}?.amount,
            unique.filter {it.field.type!=null && it.amount>0}.take(100).map {PayrollLine(it.label,it.field.type!!,it.field.role,it.amount)},warnings)
    }
}
