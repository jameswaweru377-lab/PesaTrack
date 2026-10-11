package com.kedevjimmy.pesatrack.domain.evidence

import java.util.Locale

enum class FinancialPdfDocumentKind { PAYSLIP, STATEMENT, UNKNOWN }

/**
 * Routes documents without recording or displaying their raw text.
 * Never pass a payslip to a statement transaction importer.
 */
object FinancialPdfDocumentClassifier {
    fun classify(pages: List<String>): FinancialPdfDocumentKind {
        val excerpt = pages.take(6).joinToString("\n").take(80_000)
            .uppercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        val explicitPayslip = Regex("\\b(?:PAYSLIP|PAY SLIP|PAY ADVICE|SALARY SLIP)\\b").containsMatchIn(excerpt)
        val payrollFacts = listOf("BASIC PAY", "GROSS PAY", "GROSS EARNINGS", "NET PAY",
            "NET SALARY", "PAYE", "NSSF", "HOUSE ALLOWANCE", "TOTAL CASH EARNINGS")
            .count { Regex("\\b"+Regex.escape(it)+"\\b").containsMatchIn(excerpt) }
        if ((explicitPayslip && payrollFacts >= 1) || payrollFacts >= 4)
            return FinancialPdfDocumentKind.PAYSLIP
        if (Regex("\\bM[ -]?PESA\\b").containsMatchIn(excerpt) &&
            Regex("\\b(?:PAID IN|WITHDRAWN|TRANSACTION STATUS|RECEIPT)\\b").containsMatchIn(excerpt))
            return FinancialPdfDocumentKind.STATEMENT
        return FinancialPdfDocumentKind.UNKNOWN
    }
}
