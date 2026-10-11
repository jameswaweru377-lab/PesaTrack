package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayslipSemanticReviewTest {
    @Test fun cashGrossAndCurrentDeductionsAreRecognizedWithoutMatchingEmployerNames() {
        val payroll=PayslipFieldReview.read(listOf("""
            Example Schools Ltd
            PAYSLIP
            Pay Period: September 2026
            Staff Number: 900123
            Staff Email: employee@example.invalid
            EARNINGS
            Basic Pay: 65,000.00
            House Allowance
            15,000.00
            Total Cash Earnings 80,000.00
            Gross Taxable Pay 80,000.00
            DEDUCTIONS
            PAYE Tax: 15,000.00
            NSSF: 2,000.00
            Social Health Insurance Fund: 2,000.00
            Sacco Deposit: 4,000.00
            TOTAL DEDUCTIONS: 23,000.00
            Net Pay: 57,000.00
        """.trimIndent()))
        assertEquals("Example Schools Ltd",payroll.employer)
        assertEquals("2026-09",payroll.period)
        assertEquals(8_000_000L,payroll.grossMinor)
        assertEquals("Total Cash Earnings",payroll.grossSource)
        assertEquals(5_700_000L,payroll.netMinor)
        assertEquals(2_300_000L,payroll.reportedDeductionsMinor)
        assertEquals(4,payroll.lines.size)
        assertEquals(listOf(PayrollLineType.TAX,PayrollLineType.NSSF,PayrollLineType.HEALTH,
            PayrollLineType.SACCO_DEPOSIT),payroll.lines.map{it.type})
        assertEquals(4,payroll.earnings.size)
        assertFalse(payroll.warnings.any{it.contains("breakdown is incomplete")})
        assertFalse(payroll.employer!!.contains("employee@"))
    }
    @Test fun taxableGrossAloneIsNotInventedAsCashGross() {
        val review=PayslipFieldReview.read(listOf("Gross Taxable Pay 80,000\nNet Pay 57,000"))
        assertNull(review.grossMinor)
        assertEquals(5_700_000L,review.netMinor)
        assertTrue(review.warnings.any{it.contains("Taxable pay does not necessarily")})
    }
    @Test fun unknownDeductionRequiresExplicitSectionAndPreservesSafeLabel() {
        val review=PayslipFieldReview.read(listOf("""
            Company: Example Ltd
            Payroll month: 09/2026
            Gross Pay 80000
            DEDUCTIONS
            Community Welfare Contribution 1200
            Staff Number 900123
            Staff Grade B3
            PAYE 10000
            Net Pay 68800
        """.trimIndent()))
        assertEquals("2026-09",review.period)
        assertEquals(2,review.lines.size)
        assertEquals(PayrollLineType.OTHER,review.lines[0].type)
        assertEquals("Community Welfare Contribution",review.lines[0].label)
        assertFalse(review.lines.any{it.label.contains("Staff")})
        assertTrue(review.warnings.any{it.contains("retained as Other")})
    }
    @Test fun unknownDeductionOutsideSectionCannotBecomePayrollLine() {
        val report=PayslipFieldReview.read(listOf("Unknown Department 4000\nStaff Number 900123\nGross Pay 80000\nNet Pay 76000"))
        assertTrue(report.lines.isEmpty())
    }
    @Test fun knownYtdColumnUsesCurrentOnlyWithExplicitHeader() {
        val report=PayslipFieldReview.read(listOf("""
            Currency: KES
            Payroll month: 2026-09
            DEDUCTIONS
            Description Current YTD
            PAYE 1000 12000
            SHIF 1200 14400
            Gross Pay 50000
            Net Pay 47800
        """.trimIndent()))
        assertEquals(listOf(100_000L,120_000L),report.lines.map{it.amountMinor})
        assertEquals(5_000_000L,report.grossMinor)
    }
    @Test fun unlabeledCurrentYtdDoesNotOverrideExactGross() {
        val report=PayslipFieldReview.read(listOf("Gross Pay 80000\nGross Pay 85000 900000\nNet Pay 57000"))
        assertNull(report.grossMinor)
        assertTrue(report.warnings.any{it.contains("Repeated or conflicting")})
    }
    @Test fun twoDifferentPayrollMonthLabelsRemainUnresolved() {
        val report=PayslipFieldReview.read(listOf("Payroll month: 2026-09\nPay period: 2026-10\nGross Pay: 80000\nNet Pay: 57000"))
        assertNull(report.period)
    }

    @Test fun splitTierLabelsUseFollowingAmountsRatherThanTierNumbers() {
        val result=PayslipFieldReview.read(listOf("""
            Employer: Example Ltd
            Pay Period
            September 2026
            DEDUCTIONS
            NSSF Tier 1
            500.00
            NSSF Tier 2
            1,500.00
            Gross Pay: 50000
            Net Pay: 48000
        """.trimIndent()))
        assertEquals("2026-09",result.period)
        assertEquals(listOf(50_000L,150_000L),result.lines.map{it.amountMinor})
    }
    @Test fun nssfCombinedTotalAndTiersCannotBothBecomeDeductions() {
        val result=PayslipFieldReview.read(listOf("""
            NSSF 2000
            NSSF Tier 1 500
            NSSF Tier 2 1500
            Gross Pay: 50000
            Net Pay: 48000
        """.trimIndent()))
        assertTrue(result.lines.isEmpty())
        assertTrue(result.warnings.any { it.contains("double counting") })
    }
}
