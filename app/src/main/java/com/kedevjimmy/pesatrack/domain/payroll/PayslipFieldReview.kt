package com.kedevjimmy.pesatrack.domain.payroll

import java.util.Locale
import java.util.Currency

data class PayslipSuggestions(val grossMinor: Long?, val netMinor: Long?, val lines: List<PayrollLine>, val warnings: List<String>,
    val employer: String?=null,val period: String?=null)

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
    private val row=Regex("^("+labels.keys.sortedByDescending {it.length}.joinToString("|"){Regex.escape(it)}+")(?:\\s*[:=]\\s*|\\s+)(.*)$",RegexOption.IGNORE_CASE)
    private val value=Regex("^(?:KES\\s+|KSH\\s+)?([0-9][0-9,.]*)$",RegexOption.IGNORE_CASE)
    private val kesCurrency=Regex("\\b(?:KES|KSH|KSHS|KENYAN SHILLINGS?)\\b",RegexOption.IGNORE_CASE)
    private val declaredCurrency=Regex("^\\s*(?:PAY\\s+)?CURRENCY(?:\\s+CODE)?(?:\\s*[:=]\\s*|\\s+)(.*?)\\s*$",setOf(RegexOption.IGNORE_CASE,RegexOption.MULTILINE))
    private val foreignCurrency=Regex("\\b(?:"+Currency.getAvailableCurrencies().map {it.currencyCode}.filter {it!="KES"}.sorted().joinToString("|")+"|DOLLARS)\\b|[$€£]",RegexOption.IGNORE_CASE)
    private val metadata=Regex("^(EMPLOYER(?: NAME)?|COMPANY(?: NAME)?|PAYROLL MONTH|PAY PERIOD)\\s*[:=]\\s*(.*)$",RegexOption.IGNORE_CASE)
    private val months=listOf("January","February","March","April","May","June","July","August","September","October","November","December")
    private fun period(raw: String): String? {
        if(Regex("(?:19|20|21)[0-9]{2}-(?:0[1-9]|1[0-2])").matches(raw))return raw
        val m=Regex("([A-Za-z]+) ((?:19|20|21)[0-9]{2})").matchEntire(raw) ?: return null
        val month=months.indexOfFirst {it.equals(m.groupValues[1],true)}+1
        return if(month>0) "${m.groupValues[2]}-${month.toString().padStart(2,'0')}" else null
    }
    private fun cells(page: String): List<String> {
        val input=page.lines().map {it.trim()}.filter {it.isNotEmpty()};val result=mutableListOf<String>();var i=0
        while(i<input.size) {
            val label=input[i].trimEnd(':','=')
            if(label.uppercase(Locale.ROOT) in labels && i+1<input.size && value.matches(input[i+1])) {
                result+="$label: ${input[i+1]}";i+=2
            } else {result+=input[i];i++}
        }
        return result
    }
    fun read(pages: List<String>): PayslipSuggestions {
        require(pages.size in 1..40 && pages.sumOf { it.length.toLong() } <= 100000) { "Choose a shorter payslip." }
        if(pages.any { it.isBlank() }) return PayslipSuggestions(null,null,emptyList(),listOf("Some pages have no selectable text. Enter payroll fields manually."))
        if(pages.any { page -> foreignCurrency.containsMatchIn(page) || declaredCurrency.findAll(page).any { !kesCurrency.matches(it.groupValues[1]) } })
            return PayslipSuggestions(null,null,emptyList(),listOf("A currency other than KES was found. KES fields were not prefilled."))
        if(pages.none { kesCurrency.containsMatchIn(it) })
            return PayslipSuggestions(null,null,emptyList(),listOf("KES currency was not identified. Enter payroll fields manually and check their currency."))
        data class Observed(val field: Field,val label: String,val amount: Long)
        val observed=mutableListOf<Observed>();val occurrences=mutableMapOf<String,Int>();var unsupported=false
        val employers=mutableListOf<String>();val periods=mutableListOf<String?>()
        pages.forEach { page->cells(page).forEach { raw->
            metadata.matchEntire(raw)?.let {m->
                if(m.groupValues[1].startsWith("EMPLOYER",true)||m.groupValues[1].startsWith("COMPANY",true)) employers+=m.groupValues[2].trim()
                else periods+=period(m.groupValues[2].trim())
            }
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
        if(employers.size!=1 || periods.size!=1 || periods.singleOrNull()==null) warnings+="Employer or payroll month was not uniquely identified. Check these fields manually."
        return PayslipSuggestions(unique.firstOrNull {it.field.key=="GROSS"}?.amount,
            unique.firstOrNull {it.field.key=="NET"}?.amount,
            unique.filter {it.field.type!=null && it.amount>0}.take(100).map {PayrollLine(it.label,it.field.type!!,it.field.role,it.amount)},warnings,
            employers.singleOrNull()?.takeIf {it.length in 1..160 && it.none {c->c.isISOControl()}},periods.singleOrNull())
    }
}
