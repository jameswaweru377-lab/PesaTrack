package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayslipFieldReviewTest {
    @Test fun onlyAnchoredSingleAmountsSuggestKnownLabels() {
        val review=PayslipFieldReview.read(listOf("Gross Pay: KES 70,000.00\nPAYE: 12,000.00\nNHIF: 1,500.00\nSACCO Deposit: 4,500.00\nNet Pay: 52,000.00"))
        assertEquals(7000000L,review.grossMinor); assertEquals(5200000L,review.netMinor)
        assertEquals(3,review.lines.size); assertEquals("NHIF",review.lines[1].label)
        assertEquals(PayrollLineType.HEALTH,review.lines[1].type)
    }
    @Test fun employerAmountsNeverBecomeEmployeeDeductions() {
        val review=PayslipFieldReview.read(listOf("Employer Pension: 5000.00\nEmployee Pension: 3000.00"))
        assertEquals(PayrollLineRole.EMPLOYER_CONTRIBUTION,review.lines[0].role)
        assertEquals(PayrollLineRole.EMPLOYEE_DEDUCTION,review.lines[1].role)
    }
    @Test fun repeatedOrConflictingGrossIsNotChosenSilently() {
        for (other in listOf("70000","71000")) {
            val review=PayslipFieldReview.read(listOf("Gross Pay: 70000\nGross Pay: $other\nNet Pay: 52000"))
            assertNull(review.grossMinor); assertTrue(review.warnings.isNotEmpty())
        }
    }
    @Test fun repeatedDeductionRequiresManualReviewRatherThanSum() {
        val review=PayslipFieldReview.read(listOf("NSSF: 1000\nNSSF: 2000"))
        assertTrue(review.lines.isEmpty()); assertTrue(review.warnings.isNotEmpty())
    }
    @Test fun unknownAndAmbiguousLabelsAreNotClassifiedAsAssetsOrLoanPrincipal() {
        val review=PayslipFieldReview.read(listOf("SACCO: 5500\nLoan: 3000\nHousing Allowance: 12000"))
        assertTrue(review.lines.isEmpty()); assertTrue(review.warnings.isNotEmpty())
    }
    @Test fun explicitLoanSplitAndHistoricalLabelsSurvive() {
        val review=PayslipFieldReview.read(listOf("Loan Principal: 2600\nLoan Interest: 400\nSHIF: 1500"))
        assertEquals(listOf(PayrollLineType.LOAN_PRINCIPAL,PayrollLineType.LOAN_INTEREST,PayrollLineType.HEALTH),review.lines.map { it.type })
        assertEquals("SHIF",review.lines.last().label)
    }
    @Test fun multicolumnYearToDateAndMalformedMoneyAreNotGuessed() {
        for (text in listOf("Gross Pay: 70000 840000", "Gross Pay YTD: 840000", "PAYE: 1000 NSSF: 2000", "Gross Pay: 7,00", "Gross Pay: 70,000.001", "Gross Pay: -100")) {
            val review=PayslipFieldReview.read(listOf(text)); assertNull(text,review.grossMinor); assertTrue(text,review.lines.isEmpty())
        }
    }
    @Test fun foreignCurrencyAndMissingPagesCannotPrefillKes() {
        for(pages in listOf(listOf("USD\nGross Pay: 70000"),listOf("Gross Pay: 70000",""))) {
            val review=PayslipFieldReview.read(pages); assertNull(review.grossMinor); assertTrue(review.lines.isEmpty()); assertTrue(review.warnings.isNotEmpty())
        }
    }
    @Test fun boundedTextFailsBeforeParsing() { assertTrue(runCatching {PayslipFieldReview.read(listOf("a".repeat(100001)))}.isFailure) }
    @Test fun zeroDeductionIsNotInventedAsPositive() {
        val review=PayslipFieldReview.read(listOf("PAYE: 0\nGross Pay: 70000\nNet Pay: 70000"))
        assertTrue(review.lines.isEmpty()); assertEquals(7000000L,review.netMinor)
    }
    @Test fun malformedOrMultivalueDuplicateInvalidatesKnownField() {
        for(value in listOf("7,00","71000 852000","USD 71000","-70000")) {
            val review=PayslipFieldReview.read(listOf("Gross Pay: 70000\nGross Pay: $value"))
            assertNull(value,review.grossMinor)
        }
    }
    @Test fun synonymousDeductionLabelsCannotPrefillTwice() {
        for(text in listOf("PAYE: 12000\nIncome Tax: 12000","Pension: 3000\nEmployee Pension: 3500","NHIF: 1500\nHealth Contribution: 1500")) {
            val review=PayslipFieldReview.read(listOf(text));assertTrue(text,review.lines.isEmpty())
        }
    }
}
