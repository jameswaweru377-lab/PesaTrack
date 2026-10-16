package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayslipLabelMappingTest {
    @Test fun statutoryLevyStudentLoanAndRoundingRemainSeparateFinancialFacts() {
        val payroll = PayslipFieldReview.read(listOf("""
            Currency: KES
            Payroll month: 2026-10
            Gross Pay 79,380.00
            Deductions
            P.A.Y.E. 13,756.34
            N.S.S.F. (Tier I) 1,000.00
            A.H.L. 1,190.70
            S.H.I.F. 2,182.95
            H.E.L.B. 4,500.00
            Rounding Cfwd (KES) 0.01
            Net Pay 56,750.00
        """.trimIndent()))
        assertEquals(PayrollLineType.HOUSING_LEVY,
            payroll.lines.first { it.label == "A.H.L." }.type)
        assertEquals(PayrollLineType.STUDENT_LOAN,
            payroll.lines.first { it.label == "H.E.L.B." }.type)
        assertEquals(PayrollLineType.ROUNDING_ADJUSTMENT,
            payroll.lines.first { it.label.contains("Rounding",true) }.type)
        assertEquals(1L, payroll.lines.first {
            it.type == PayrollLineType.ROUNDING_ADJUSTMENT
        }.amountMinor)
        assertFalse(payroll.lines.any { it.type == PayrollLineType.OTHER })
        // Extraction is payroll-only: no Activity income event or bank credit.
    }

    @Test fun otherUnrecognizedPayrollLabelsStillRequireManualClassification() {
        val payroll = PayslipFieldReview.read(listOf("""
            Currency: KES
            Gross Pay 70,000
            Deductions
            Association Charge 500
            Net Pay 69,500
        """.trimIndent()))
        assertEquals(PayrollLineType.OTHER,payroll.lines.single().type)
    }
}
