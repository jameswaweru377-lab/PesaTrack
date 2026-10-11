package com.kedevjimmy.pesatrack.domain.payroll

import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Locale
import java.text.Normalizer

enum class PayrollLineType(val title: String) {
    TAX("Tax / PAYE"), HEALTH("Health contribution"), NSSF("NSSF"), PENSION("Pension"),
    SACCO_SHARES("SACCO shares"), SACCO_DEPOSIT("SACCO deposit"), INSURANCE("Insurance"),
    LOAN_PRINCIPAL("Loan principal"), LOAN_INTEREST("Loan interest"), OTHER("Other / unclassified")
}
enum class PayrollLineRole(val title: String) { EMPLOYEE_DEDUCTION("Employee deduction"), EMPLOYER_CONTRIBUTION("Employer contribution") }
enum class PayrollSource { MANUAL, PDF_TEXT }
data class PayrollLine(val label: String, val type: PayrollLineType, val role: PayrollLineRole, val amountMinor: Long)
data class PayrollDraft(val employer: String, val period: String, val grossMinor: Long,
    val netMinor: Long, val source: PayrollSource, val lines: List<PayrollLine>)
data class PayrollSummary(val employeeDeductionMinor: Long, val employerContributionMinor: Long,
    val netPayableMinor: Long, val employeeByType: Map<PayrollLineType, Long>)
data class PayrollRecord(val id: String, val revision: Long, val draft: PayrollDraft,
    val createdAtEpochMs: Long, val updatedAtEpochMs: Long)

/** Reported payroll facts only. No cash, settlement, remittance or balance authority. */
object PayrollJournalPolicy {
    const val MAX_LINES = 100
    /** Compare the complete loaded snapshot, including after a restore reuses an earlier revision. */
    fun requireUnchanged(current: PayrollRecord?, expected: PayrollRecord) {
        require(current == expected) { "This payroll record changed. Reopen it before editing or deleting." }
    }
    private val amountPattern = Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?")
    fun parseAmount(text: String): Long? = runCatching {
        val value=text.trim()
        require(value.length <= 40 && amountPattern.matches(value))
        BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact()
    }.getOrNull()
    fun money(minor: Long): String = BigDecimal.valueOf(minor,2).toPlainString()
    fun normalizeEmployer(employer: String): String = Normalizer.normalize(employer,Normalizer.Form.NFC)
        .trim().replace(Regex("[\\s\\p{Z}]+")," ").uppercase(Locale.ROOT)
    fun validate(draft: PayrollDraft): PayrollSummary {
        require(draft.employer.trim().length in 1..160 && draft.employer.none { it.isISOControl() }) { "Enter an employer name up to 160 characters." }
        require(Regex("(?:19|20|21)[0-9]{2}-(?:0[1-9]|1[0-2])").matches(draft.period)) { "Enter the payroll month as yyyy-MM." }
        require(draft.grossMinor > 0 && draft.netMinor in 0..draft.grossMinor) { "Check gross earnings and net payable." }
        require(draft.lines.size <= MAX_LINES) { "Use up to 100 payroll lines." }
        var employee=0L; var employer=0L
        val byType=mutableMapOf<PayrollLineType,Long>()
        for(line in draft.lines) {
            require(line.label.trim().length in 1..160 && line.label.none { it.isISOControl() }) { "Give each line its original label, up to 160 characters." }
            require(line.amountMinor > 0) { "Each payroll line needs a positive amount." }
            try {
                if(line.role == PayrollLineRole.EMPLOYEE_DEDUCTION) {
                    employee=Math.addExact(employee,line.amountMinor)
                    byType[line.type]=Math.addExact(byType[line.type] ?: 0,line.amountMinor)
                } else employer=Math.addExact(employer,line.amountMinor)
            } catch(_: ArithmeticException) { throw IllegalArgumentException("Payroll totals are too large.") }
        }
        require(employee == draft.grossMinor-draft.netMinor) { "Employee deductions must equal gross earnings minus net payable. Check missing or repeated lines." }
        return PayrollSummary(employee,employer,draft.netMinor,byType.toMap())
    }
    /** One reviewed record per employer/month; conflicting replay must request editing, never overwrite. */
    fun periodKey(draft: PayrollDraft): String = digest(listOf(normalizeEmployer(draft.employer),draft.period))
    fun contentKey(draft: PayrollDraft): String = digest(listOf(normalizeEmployer(draft.employer),draft.period,
        draft.grossMinor.toString(),draft.netMinor.toString(),draft.source.name) + draft.lines.flatMap {
        listOf(it.label.trim(),it.type.name,it.role.name,it.amountMinor.toString())
    })
    private fun digest(parts: List<String>): String = MessageDigest.getInstance("SHA-256")
        .digest(parts.joinToString("") { "${it.length}:$it" }.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
