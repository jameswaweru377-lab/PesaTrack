package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayslipOutflowsColumnsTest {
    @Test fun outflowsAreDeductionsAndCalculationsAreNotDoubleCounted() {
        // Entirely synthetic arithmetic; no real payslip or staff details retained.
        val review=PayslipFieldReview.read(listOf("""
            Example Schools Ltd
            PAYSLIP
            Payroll month: 2026-09
            Inflows
            Basic Pay 40,000.00
            House Allowance 10,000.00
            Total Cash Earnings 50,000.00
            Outflows
            P.A.Y.E. 10,000.00
            N.S.S.F. (Tier I) 500.00
            N.S.S.F. (Tier II) 2,500.00
            A.H.L. 750.00
            S.H.I.F. 1,250.00
            H.E.L.B. 5,000.00
            Rounding Cfwd (KES) 0.01
            Total Deductions 20,000.00
            Net Pay 29,999.99
            Calculations
            Gross Taxable Pay 50,000.00
            Less: Pension/N.S.S.F Relief -3,000.00
            Less: AHL/S.H.I.F Contrib -2,000.00
            Net Taxable Pay 45,000.00
            Gross P.A.Y.E Computed (M) 12,400.00
            Less: Personal Relief -2,400.00
            Net P.A.Y.E Computed 10,000.00
        """.trimIndent()))
        assertEquals(5_000_000L,review.grossMinor)
        assertEquals(2_999_999L,review.netMinor)
        assertEquals(2_000_000L,review.reportedDeductionsMinor)
        assertEquals(7,review.lines.size)
        assertEquals(2_000_001L,review.lines.sumOf { it.amountMinor })
        assertEquals(2,review.lines.count {it.type==PayrollLineType.NSSF})
        assertEquals(1,review.lines.count {it.type==PayrollLineType.HEALTH})
        assertEquals(1,review.lines.count {it.type==PayrollLineType.TAX})
        assertEquals(1,review.lines.count {it.type==PayrollLineType.HOUSING_LEVY})
        assertEquals(1,review.lines.count {it.type==PayrollLineType.STUDENT_LOAN})
        assertEquals(1,review.lines.count {it.type==PayrollLineType.ROUNDING_ADJUSTMENT})
        assertEquals(1L,review.lines.first {
            it.type==PayrollLineType.ROUNDING_ADJUSTMENT
        }.amountMinor)
        assertFalse(review.lines.any{it.label.contains("Computed",true) || it.label.contains("Relief",true)})
        assertTrue(review.warnings.any{it.contains("Rounding adjustment")})
        val draft=PayrollDraft("Example Schools Ltd","2026-09",review.grossMinor!!,review.netMinor!!,
            PayrollSource.PDF_TEXT,review.lines)
        assertEquals(2_000_001L,PayrollJournalPolicy.validate(draft).employeeDeductionMinor)
    }
    @Test fun calculationOnlyReliefIsNeverAReportedEmployeeDeduction() {
        val review=PayslipFieldReview.read(listOf("""
            Gross Pay 20,000
            Outflows
            SHIF 500
            Total Deductions 500
            Net Pay 19,500
            Calculations
            PAYE 1,400
            NSSF 2,500
            Housing Levy 400
            Personal Relief 2,400
        """.trimIndent()))
        assertEquals(1,review.lines.size)
        assertEquals(PayrollLineType.HEALTH,review.lines.single().type)
    }
}
