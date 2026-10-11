package com.kedevjimmy.pesatrack.domain.payroll

import java.util.Currency
import java.util.Locale

data class PayslipEarning(val label: String, val amountMinor: Long)
data class PayslipSuggestions(
    val grossMinor: Long?, val netMinor: Long?, val lines: List<PayrollLine>, val warnings: List<String>,
    val employer: String? = null, val period: String? = null,
    val earnings: List<PayslipEarning> = emptyList(),
    val reportedDeductionsMinor: Long? = null, val grossSource: String? = null
)

/** Local, bounded payroll grammar, independent of individual employers or tax rates. */
object PayslipFieldReview {
    private data class Field(val key: String, val type: PayrollLineType? = null, val employer: Boolean = false)
    private fun normalized(text: String) = text.uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")
    private val aliases = buildMap<String,Field> {
        fun simple(key:String,vararg names:String) = names.forEach {put(normalized(it),Field(key))}
        fun deduction(type:PayrollLineType,vararg names:String) = names.forEach {
            put(normalized(it),Field("DEDUCT:"+type.name,type))
        }
        simple("GROSS","GROSS PAY","GROSS EARNINGS","GROSS SALARY","TOTAL GROSS EARNINGS")
        simple("NET","NET PAY","NET PAYABLE","NET SALARY","TAKE HOME PAY","NET AMOUNT")
        simple("CASH","TOTAL CASH EARNINGS","TOTAL CASH PAY","TOTAL CASH EMOLUMENTS")
        simple("TAXABLE","GROSS TAX PAY","GROSS TAXABLE PAY","TAXABLE PAY","TAXABLE INCOME")
        simple("BASIC","BASIC PAY","BASIC SALARY","BASE SALARY")
        simple("HOUSING","HOUSE ALLOWANCE","HOUSING ALLOWANCE")
        simple("TOTAL_DEDUCTIONS","TOTAL DEDUCTIONS","TOTAL EMPLOYEE DEDUCTIONS","DEDUCTIONS TOTAL")
        deduction(PayrollLineType.TAX,"PAYE","PAYE TAX","P A Y E","P A Y E TAX","INCOME TAX","PAY AS YOU EARN")
        deduction(PayrollLineType.HEALTH,"NHIF","SHIF","SHA","SOCIAL HEALTH INSURANCE",
            "SOCIAL HEALTH INSURANCE FUND","HEALTH CONTRIBUTION","MEDICAL CONTRIBUTION",
            "S H I F","N H I F","S H A")
        deduction(PayrollLineType.NSSF,"NSSF","NSSF EMPLOYEE","NSSF TIER I","NSSF TIER II",
            "NSSF TIER 1","NSSF TIER 2","NSSF EMPLOYEE TIER 1","NSSF EMPLOYEE TIER 2",
            "N S S F TIER I","N S S F TIER II","N S S F TIER 1","N S S F TIER 2","N S S F")
        deduction(PayrollLineType.PENSION,"PENSION","EMPLOYEE PENSION","RETIREMENT CONTRIBUTION")
        deduction(PayrollLineType.SACCO_SHARES,"SACCO SHARES","SACCO SHARE CAPITAL")
        deduction(PayrollLineType.SACCO_DEPOSIT,"SACCO DEPOSIT","SACCO CONTRIBUTION","SACCO SAVINGS")
        deduction(PayrollLineType.INSURANCE,"INSURANCE","INSURANCE PREMIUM")
        deduction(PayrollLineType.LOAN_PRINCIPAL,"LOAN PRINCIPAL","LOAN REPAYMENT PRINCIPAL")
        deduction(PayrollLineType.LOAN_INTEREST,"LOAN INTEREST")
        deduction(PayrollLineType.OTHER,"AFFORDABLE HOUSING LEVY","HOUSING LEVY",
            "AHL","A H L","ROUNDING","ROUNDING CFWD","ROUNDING CFWD KES",
            "ROUNDING C FWD","UNION DUES","H E L B","HELB",
            "HIGHER EDUCATION LOANS BOARD")
        put("EMPLOYER PENSION",Field("EMPLOYER:PENSION",PayrollLineType.PENSION,true))
        put("EMPLOYER NSSF",Field("EMPLOYER:NSSF",PayrollLineType.NSSF,true))
    }
    private val money = Regex("^(?:(?:KES|KSH|KSHS)\\s*)?([0-9][0-9,.]*)$",RegexOption.IGNORE_CASE)
    private val rowValue = Regex("^(.{2,120}?)\\s*[:=]?\\s+((?:(?:KES|KSH|KSHS)\\s*)?[0-9][0-9,.]*)$",RegexOption.IGNORE_CASE)
    private val twoValues = Regex("^(.{2,120}?)\\s+([0-9][0-9,.]*)\\s+([0-9][0-9,.]*)$",RegexOption.IGNORE_CASE)
    private val kes = Regex("\\b(?:KES|KSH|KSHS|KENYAN SHILLINGS?)\\b",RegexOption.IGNORE_CASE)
    private val declaration = Regex("^\\s*(?:PAY\\s+)?CURRENCY(?:\\s+CODE)?\\s*[:=]?\\s*(.*?)\\s*$",RegexOption.IGNORE_CASE)
    private val foreign = Regex("(?:^\\s*(?:"+
        Currency.getAvailableCurrencies().map{it.currencyCode}.filter{it!="KES"}.sorted().joinToString("|")+
        ")\\s*$)|(?:\\b(?:"+
        Currency.getAvailableCurrencies().map{it.currencyCode}.filter{it!="KES"}.sorted().joinToString("|")+
        "|DOLLARS)\\b\\s*[0-9])|[$€£]",
        setOf(RegexOption.MULTILINE))
    private val employerTag = Regex("^(?:EMPLOYER(?: NAME)?|COMPANY(?: NAME)?|ORGANISATION(?: NAME)?|ORGANIZATION(?: NAME)?)\\s*[:=]\\s*(.+)$",RegexOption.IGNORE_CASE)
    private val periodTag = Regex("^(?:PAYROLL MONTH|SALARY MONTH|PAYSLIP MONTH|PAY PERIOD|PAYROLL PERIOD|PERIOD ENDING|MONTH YEAR|MONTH / YEAR|PAYSLIP FOR)\\s*[:=]?\\s+(.+)$",RegexOption.IGNORE_CASE)
    private val periodHeader = Regex("^(?:PAYSLIP|PAY SLIP|SALARY SLIP|PAY ADVICE)\\s+(?:FOR\\s+)?(.+)$",RegexOption.IGNORE_CASE)
    private val namedMonths = listOf("JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
        "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER")
    private val foreignIdentity = Regex("\\b(?:STAFF\\s+(?:NO|NUMBER|NAME|ID)|EMPLOYEE\\s+(?:NO|NUMBER|NAME|ID)|EMAIL|E\\s*MAIL|ADDRESS|P\\s*O\\s*BOX|POSTAL|ACCOUNT\\s*(?:NO|NUMBER)|PHONE|MOBILE|NATIONAL\\s+ID|KRA\\s+PIN|GRADE|DEPARTMENT|DESIGNATION)\\b",RegexOption.IGNORE_CASE)
    private val organizationSuffix = Regex("\\b(?:LTD|LIMITED|SCHOOL|SCHOOLS|UNIVERSITY|COLLEGE|HOSPITAL|COUNTY|MINISTRY|FOUNDATION|ORGANIZATION|ORGANISATION|COMPANY|GROUP|INC)\\b",RegexOption.IGNORE_CASE)
    private val unknownSafe = Regex("[A-Za-z][A-Za-z0-9 &/().+'-]{1,89}")
    private enum class Section { NONE, EARNINGS, DEDUCTIONS, EMPLOYER, CALCULATIONS }
    private fun heading(s:String):Section? = when(normalized(s)) {
        "EARNINGS","EARNINGS AND ALLOWANCES","ALLOWANCES","PAY ELEMENTS" -> Section.EARNINGS
        "DEDUCTIONS","OUTFLOWS","OUTFLOWS AMOUNT","STATUTORY DEDUCTIONS",
        "EMPLOYEE DEDUCTIONS","OTHER DEDUCTIONS",
        "DEDUCTIONS AND CONTRIBUTIONS" -> Section.DEDUCTIONS
        "EMPLOYER CONTRIBUTIONS","EMPLOYER DEDUCTIONS" -> Section.EMPLOYER
        "CALCULATIONS","CALCULATIONS AMOUNT","TAX CALCULATIONS","PAYROLL CALCULATIONS",
        "TAX COMPUTATION","TAX COMPUTATIONS","TAX RELIEFS" -> Section.CALCULATIONS
        "EMPLOYEE DETAILS","STAFF DETAILS","PERSONAL DETAILS","PAYROLL DETAILS","BANK DETAILS",
        "PAYSLIP SUMMARY","SUMMARY","PAY INFORMATION" -> Section.NONE
        else -> null
    }
    private fun safeDeductionLabel(label:String):Boolean =
        unknownSafe.matches(label) && !Regex("[0-9]{3,}").containsMatchIn(label) &&
        !Regex("\\b(?:EMPLOYEE|STAFF)\\b",RegexOption.IGNORE_CASE).containsMatchIn(label) &&
        !foreignIdentity.containsMatchIn(label) &&
        !Regex("\\b(?:YTD|YEAR TO DATE|BALANCE|NET|GROSS|TOTAL|TAXABLE|RATE|PERCENT|REFERENCE|CODE|DATE)\\b",RegexOption.IGNORE_CASE)
            .containsMatchIn(label)
    private fun amountMinor(text:String):Long? = money.matchEntire(text.trim())?.groupValues?.get(1)
        ?.let(PayrollJournalPolicy::parseAmount)
    private fun month(source:String):String? {
        val raw=source.trim().uppercase(Locale.ROOT).replace(Regex("\\s+")," ")
        Regex("(?:19|20|21)[0-9]{2}-(?:0[1-9]|1[0-2])").find(raw)?.let{return it.value}
        Regex("(0?[1-9]|1[0-2])[/.-]((?:19|20|21)[0-9]{2})").find(raw)?.let{
            return "${it.groupValues[2]}-${it.groupValues[1].padStart(2,'0')}"
        }
        Regex("\\b([A-Z]+)\\.?\\s+((?:19|20|21)[0-9]{2})\\b").find(raw)?.let {found ->
            val name=found.groupValues[1];val index=namedMonths.indexOfFirst {
                it==name || (name.length>=3 && it.startsWith(name))
            }
            if(index>=0)return "${found.groupValues[2]}-${(index+1).toString().padStart(2,'0')}"
        }
        Regex("((?:19|20|21)[0-9]{2})-(0[1-9]|1[0-2])-[0-3][0-9]").find(raw)?.let{
            return "${it.groupValues[1]}-${it.groupValues[2]}"
        }
        Regex("[0-3][0-9][/.-](0?[1-9]|1[0-2])[/.-]((?:19|20|21)[0-9]{2})").find(raw)?.let{
            return "${it.groupValues[2]}-${it.groupValues[1].padStart(2,'0')}"
        }
        return null
    }
    fun read(pages:List<String>):PayslipSuggestions {
        require(pages.size in 1..40 && pages.sumOf {it.length.toLong()}<=100000) {"Choose a shorter payslip."}
        if(pages.any{it.isBlank()})return PayslipSuggestions(null,null,emptyList(),
            listOf("Some pages have no selectable text. Enter payroll fields manually."))
        if(pages.any{page->foreign.containsMatchIn(page) || page.lines().any{line->
            declaration.matchEntire(line.trim())?.let{!kes.matches(it.groupValues[1].trim())}==true
        }})return PayslipSuggestions(null,null,emptyList(),
            listOf("A currency other than KES was found. KES fields were not prefilled."))
        val warnings=mutableListOf("Check the payroll month, currency, every amount and deduction before saving.")
        if(pages.none {kes.containsMatchIn(it)})warnings+=
            "Currency is not printed clearly. Confirm the amounts are KES before saving; no currency was assumed."
        data class Seen(val key:String,val label:String,val amount:Long,val type:PayrollLineType?,val employer:Boolean)
        val all=mutableListOf<Seen>();val occurrences=mutableMapOf<String,Int>()
        val companies=mutableListOf<String>();val periods=mutableListOf<String?>()
        var malformed=false;var unknown=0
        for(page in pages) {
            val lines=page.lines().map{it.trim()}.filter{it.isNotEmpty()}
            var section=Section.NONE;var currentYtd=0;var i=0
            while(i<lines.size) {
                val line=lines[i]
                val header=heading(line)
                if(header!=null){section=header;currentYtd=0;i++;continue}
                val normalizedLine=normalized(line)
                // The calculations column repeats some tax values and relief figures.
                // It is explanatory, never a second source of employee deductions.
                if(section==Section.CALCULATIONS){i++;continue}
                if(normalizedLine in setOf("CURRENT YTD","DESCRIPTION CURRENT YTD",
                    "CURRENT MONTH YEAR TO DATE","PARTICULARS CURRENT YTD")){
                    currentYtd=1;i++;continue
                }
                if(normalizedLine in setOf("YTD CURRENT","DESCRIPTION YTD CURRENT")){
                    currentYtd=-1;i++;continue
                }
                employerTag.matchEntire(line)?.let{companies+=it.groupValues[1].trim()}
                periodTag.matchEntire(line)?.let{periods+=month(it.groupValues[1])}
                if(i+1<lines.size && normalizedLine in setOf("EMPLOYER","EMPLOYER NAME",
                    "COMPANY","COMPANY NAME","ORGANIZATION NAME","ORGANISATION NAME")) {
                    val value=lines[i+1]
                    if(value.length in 3..160 && !value.contains("@") &&
                        !foreignIdentity.containsMatchIn(value) && value.none(Char::isISOControl))
                        companies+=value
                }
                if(i+1<lines.size && normalizedLine in setOf("PAY PERIOD","PAYROLL MONTH",
                    "PAYROLL PERIOD","PAYSLIP MONTH","SALARY MONTH","PERIOD ENDING"))
                    periods+=month(lines[i+1])
                periodHeader.matchEntire(line)?.let{headerMatch->
                    month(headerMatch.groupValues[1])?.let{periods+=it}
                }
                var label:String?=null;var value:String?=null
                // "NSSF Tier 1" is a label, not an amount of KES 1.
                if(i+1<lines.size && amountMinor(lines[i+1])!=null &&
                    (aliases.containsKey(normalized(line.trimEnd('=',':'))) ||
                        (section in setOf(Section.DEDUCTIONS,Section.EMPLOYER) && safeDeductionLabel(line)))) {
                    label=line.trimEnd('=',':').trim();value=lines[i+1];i++
                }
                val multi=twoValues.matchEntire(line)
                if(label==null && multi!=null && currentYtd!=0){
                    label=multi.groupValues[1].trimEnd('=',':',' ').trim()
                    value=multi.groupValues[if(currentYtd==1)2 else 3]
                } else if(label==null && multi!=null) {
                    val firstLabel=normalized(multi.groupValues[1].trimEnd('=',':',' '))
                    val key=aliases[firstLabel]?.key
                    if(key!=null) {
                        occurrences[key]=(occurrences[key]?:0)+1;malformed=true
                    } else {
                        // A numbered *label* (e.g. NSSF Tier 1) followed by one
                        // monetary value is not a CURRENT/YTD two-money row.
                        val paired=rowValue.matchEntire(line)
                        val candidate=paired?.groupValues?.get(1)?.trimEnd('=',':',' ')
                        if(candidate!=null && aliases.containsKey(normalized(candidate))) {
                            label=candidate;value=paired.groupValues[2]
                        }
                    }
                } else if(label==null) {
                    val pair=rowValue.matchEntire(line)
                    if(pair!=null) {
                        label=pair.groupValues[1].trimEnd('=',':',' ').trim()
                        value=pair.groupValues[2]
                    } else if(i+1<lines.size && amountMinor(lines[i+1])!=null &&
                        (aliases.containsKey(normalized(line.trimEnd('=',':'))) ||
                            (section in setOf(Section.DEDUCTIONS,Section.EMPLOYER) && safeDeductionLabel(line)))) {
                        label=line.trimEnd('=',':').trim();value=lines[i+1];i++
                    }
                }
                if(label==null && value==null) {
                    // Invalid marked values must invalidate an earlier apparently valid occurrence.
                    val marked=Regex("^(.{2,120}?)\\s*[:=]\\s*(.+)$").matchEntire(line)
                    val badKey=marked?.let{aliases[normalized(it.groupValues[1])]?.key}
                    if(badKey!=null){occurrences[badKey]=(occurrences[badKey]?:0)+1;malformed=true}
                }
                if(label!=null && value!=null){
                    val norm=normalized(label)
                    val field=aliases[norm]
                    val isUnknown=field==null && section in setOf(Section.DEDUCTIONS,Section.EMPLOYER)
                        && safeDeductionLabel(label)
                    if(field!=null || isUnknown) {
                        val key=if(field!=null) {
                            if(norm.contains("TIER"))"DEDUCT:NSSF:"+norm else field.key
                        } else "OTHER:"+norm
                        occurrences[key]=(occurrences[key]?:0)+1
                        val parsed=amountMinor(value)
                        if(parsed==null)malformed=true else {
                            all+=Seen(key,label,parsed,field?.type ?:
                                if(isUnknown)PayrollLineType.OTHER else null,
                                field?.employer==true || section==Section.EMPLOYER)
                            if(isUnknown)unknown++
                        }
                    }
                }
                i++
            }
            val first=lines.take(12)
            val payslip=first.indexOfFirst {normalized(it) in setOf("PAYSLIP","PAY SLIP","EMPLOYER PAYSLIP","SALARY SLIP","PAY ADVICE")}
            if(payslip>0)first.take(payslip).filter{
                it.length in 3..160 && organizationSuffix.containsMatchIn(it) &&
                !foreignIdentity.containsMatchIn(it) && !it.contains("@") &&
                !it.contains(":") && it.none(Char::isDigit)
            }.singleOrNull()?.let{companies+=it}
        }
        val unique=all.groupBy{it.key}.filter{(key,rows)->rows.size==1 && occurrences[key]==1}
            .values.map{it.single()}
        if(occurrences.values.any{it>1})warnings+="Repeated or conflicting labels were left blank for correction."
        if(malformed)warnings+="Some payroll rows have invalid or multiple amounts; check them manually."
        if(unknown>0)warnings+="Unrecognized deduction labels were retained as Other for your review."
        val employers=companies.filter{it.length in 3..160 && !it.contains("@") &&
            !foreignIdentity.containsMatchIn(it) && it.none(Char::isISOControl)}
            .distinctBy{normalized(it)}
        val months=periods.filterNotNull().distinct()
        if(employers.size!=1)warnings+="Employer was not reliably identified; select or enter it manually."
        if(months.size!=1)warnings+="Payroll month was not reliably identified; confirm the month."
        val explicitGross=unique.firstOrNull{it.key=="GROSS"}?.amount
        val cash=unique.firstOrNull{it.key=="CASH"}?.amount
        val taxable=unique.firstOrNull{it.key=="TAXABLE"}?.amount
        val gross=explicitGross?:cash
        if(explicitGross==null && cash!=null)warnings+=
            "Gross pay suggested from Total Cash Earnings; check before saving."
        if(explicitGross!=null && cash!=null && explicitGross!=cash)
            warnings+="Gross pay and Total Cash Earnings differ; review their meanings."
        if(gross==null && taxable!=null)warnings+=
            "Taxable pay does not necessarily equal gross cash pay; confirm gross manually."
        val net=unique.firstOrNull{it.key=="NET"}?.amount
        val statedDeductions=unique.firstOrNull{it.key=="TOTAL_DEDUCTIONS"}?.amount
        val hasTiers=unique.any{it.key.startsWith("DEDUCT:NSSF:")}
        val hasAggregate=unique.any{it.key=="DEDUCT:NSSF"}
        if(hasTiers && hasAggregate)
            warnings+="NSSF total and tier entries are both present. Review the values to prevent double counting."
        val deductions=unique.filter{it.type!=null && it.amount>0 &&
            !(hasTiers && hasAggregate && (it.key=="DEDUCT:NSSF" ||
                it.key.startsWith("DEDUCT:NSSF:")))}
            .take(PayrollJournalPolicy.MAX_LINES).map{
            PayrollLine(it.label,it.type!!,
                if(it.employer)PayrollLineRole.EMPLOYER_CONTRIBUTION else PayrollLineRole.EMPLOYEE_DEDUCTION,
                it.amount)
        }
        val sum=runCatching{deductions.filter{it.role==PayrollLineRole.EMPLOYEE_DEDUCTION}
            .fold(0L){acc,line->Math.addExact(acc,line.amountMinor)}}.getOrNull()
        if(gross!=null && net!=null && gross>=net && sum!=null && sum!=gross-net)
            warnings+="Deduction breakdown is incomplete or differs from gross minus net. Check the source; no deductions were invented."
        if(statedDeductions!=null && sum!=null && statedDeductions!=sum) {
            val roundingLines=deductions.filter {line->
                normalized(line.label).startsWith("ROUNDING")
            }
            val adjustment=runCatching {roundingLines.fold(0L){acc,line->Math.addExact(acc,line.amountMinor)}}.getOrNull()
            if(adjustment!=null && adjustment>0 && sum-statedDeductions==adjustment)
                warnings+="Rounding adjustment is separate from the payslip's Total Deductions; it also affects net payable."
            else warnings+="Total deductions differs from the identified lines; review missing entries."
        }
        return PayslipSuggestions(gross,net,deductions,warnings,employers.singleOrNull(),
            months.singleOrNull(),
            unique.filter{it.key in setOf("BASIC","HOUSING","CASH","TAXABLE") && it.amount>0}
                .map{PayslipEarning(it.label,it.amount)},
            statedDeductions,
            if(explicitGross!=null)"Gross pay" else if(cash!=null)"Total Cash Earnings" else null)
    }
}
