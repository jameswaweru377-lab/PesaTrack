package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayrollJournalPolicyTest {
    private fun line(type: PayrollLineType, amount: Long, label: String = type.name,
        role: PayrollLineRole = PayrollLineRole.EMPLOYEE_DEDUCTION) = PayrollLine(label, type, role, amount)
    private fun draft(lines: List<PayrollLine> = listOf(line(PayrollLineType.TAX, 1800000)),
        gross: Long = 7000000, net: Long = 5200000, employer: String = "TEST SCHOOL LTD", period: String = "2026-09") =
        PayrollDraft(employer, period, gross, net, PayrollSource.MANUAL, lines)
    @Test fun grossNetWithholdingAreSeparateFromCash() {
        val summary = PayrollJournalPolicy.validate(draft())
        assertEquals(1800000L, summary.employeeDeductionMinor)
        assertEquals(0L, summary.employerContributionMinor)
        assertEquals(5200000L, summary.netPayableMinor)
    }
    @Test fun employerContributionDoesNotReduceNet() {
        val data = draft(listOf(line(PayrollLineType.TAX,1800000),line(PayrollLineType.PENSION,500000,
            role=PayrollLineRole.EMPLOYER_CONTRIBUTION)))
        assertEquals(500000L, PayrollJournalPolicy.validate(data).employerContributionMinor)
    }
    @Test fun principalAndInterestStayExplicitAndDoNotInventLoanBalance() {
        val summary = PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.LOAN_PRINCIPAL,260000),
            line(PayrollLineType.LOAN_INTEREST,40000),line(PayrollLineType.TAX,1500000))))
        assertEquals(260000L, summary.employeeByType[PayrollLineType.LOAN_PRINCIPAL])
        assertEquals(40000L, summary.employeeByType[PayrollLineType.LOAN_INTEREST])
    }
    @Test fun historicHealthLabelSurvives() {
        val data = draft(listOf(line(PayrollLineType.HEALTH,1800000,"NHIF")),period="2020-03")
        PayrollJournalPolicy.validate(data)
        assertEquals("NHIF",data.lines.single().label)
    }
    @Test fun saccoSharesDepositsAndPensionStaySeparate() {
        val summary=PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.SACCO_SHARES,500000),
            line(PayrollLineType.SACCO_DEPOSIT,550000),line(PayrollLineType.PENSION,750000))))
        assertEquals(550000L,summary.employeeByType[PayrollLineType.SACCO_DEPOSIT])
        assertEquals(500000L,summary.employeeByType[PayrollLineType.SACCO_SHARES])
    }
    @Test fun incompletePayrollCannotSave() { assertTrue(runCatching { PayrollJournalPolicy.validate(draft(emptyList())) }.isFailure) }
    @Test fun deductionMismatchCannotSave() { assertTrue(runCatching { PayrollJournalPolicy.validate(draft(net=5100000)) }.isFailure) }
    @Test fun invalidAmountsCannotSave() {
        for (value in listOf(-1L,0L,Long.MIN_VALUE)) assertTrue(runCatching { PayrollJournalPolicy.validate(draft(gross=value)) }.isFailure)
        for (value in listOf(-1L,Long.MIN_VALUE,7000001)) assertTrue(runCatching { PayrollJournalPolicy.validate(draft(net=value)) }.isFailure)
        for (value in listOf(-1L,0L,Long.MIN_VALUE)) assertTrue(runCatching { PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.TAX,value)))) }.isFailure)
    }
    @Test fun noDeductionsIsValidWithEqualGrossAndNet() { PayrollJournalPolicy.validate(draft(emptyList(),net=7000000)) }
    @Test fun zeroNetIsPayableNotMissingCash() { PayrollJournalPolicy.validate(draft(net=0,lines=listOf(line(PayrollLineType.TAX,7000000)))) }
    @Test fun employerContributionOverflowCannotSave() {
        assertTrue(runCatching { PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.TAX,1800000),
            line(PayrollLineType.PENSION,Long.MAX_VALUE,role=PayrollLineRole.EMPLOYER_CONTRIBUTION),
            line(PayrollLineType.PENSION,1,role=PayrollLineRole.EMPLOYER_CONTRIBUTION)))) }.isFailure)
    }
    @Test fun tooManyOrLongLinesCannotSave() {
        assertTrue(runCatching { PayrollJournalPolicy.validate(draft(List(101) {line(PayrollLineType.OTHER,1)})) }.isFailure)
        assertTrue(runCatching { PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.TAX,1800000,"a".repeat(161))))) }.isFailure)
    }
    @Test fun invalidPeriodsAndEmployersCannotSave() {
        for(p in listOf("", "2026-13", "2026-00", "26-09", "2026-09-01")) assertTrue(runCatching {PayrollJournalPolicy.validate(draft(period=p))}.isFailure)
        for(e in listOf("", " ","a".repeat(161))) assertTrue(runCatching {PayrollJournalPolicy.validate(draft(employer=e))}.isFailure)
    }
    @Test fun strictMinorMoneyRejectsMalformedGroupingRoundingAndOverflow() {
        assertEquals(5200000L,PayrollJournalPolicy.parseAmount("52,000.00"))
        assertEquals(0L,PayrollJournalPolicy.parseAmount("0"))
        for(s in listOf("1,00", "52,00.00", "1.001", "1e3", "-1", "+1", "NaN", "92233720368547758.08")) assertNull(s,PayrollJournalPolicy.parseAmount(s))
    }
    @Test fun normalizedEmployerPeriodIsReplayIdentityNotAmount() {
        assertEquals(PayrollJournalPolicy.periodKey(draft()),PayrollJournalPolicy.periodKey(draft(employer="  test   school ltd  ",net=5100000)))
        assertNotEquals(PayrollJournalPolicy.periodKey(draft()),PayrollJournalPolicy.periodKey(draft(period="2026-10")))
        assertNotEquals(PayrollJournalPolicy.periodKey(draft()),PayrollJournalPolicy.periodKey(draft(employer="OTHER EMPLOYER LTD")))
    }
    @Test fun userCorrectionsChangeContentAndRemainSeparateFromReplayKey() {
        assertNotEquals(PayrollJournalPolicy.contentKey(draft()),PayrollJournalPolicy.contentKey(draft(listOf(line(PayrollLineType.HEALTH,1800000,"SHIF")))))
    }
    @Test fun unicodeSpacingAndCanonicalLettersDoNotCreateReplayTwins() {
        assertEquals(PayrollJournalPolicy.periodKey(draft(employer="TEST SCHOOL LTD")),
            PayrollJournalPolicy.periodKey(draft(employer="TEST\u00a0SCHOOL LTD")))
        assertEquals(PayrollJournalPolicy.periodKey(draft(employer="CAFÉ LTD")),
            PayrollJournalPolicy.periodKey(draft(employer="CAFE\u0301 LTD")))
    }
    @Test fun employeeDeductionOverflowCannotSave() {
        assertTrue(runCatching {PayrollJournalPolicy.validate(draft(listOf(line(PayrollLineType.TAX,Long.MAX_VALUE),
            line(PayrollLineType.OTHER,1))))}.isFailure)
    }
    @Test fun restoredSameRevisionWithDifferentFactsCannotOverwriteOrDelete() {
        val snapshot=PayrollRecord("synthetic-payroll",1,draft(),1000,1000)
        PayrollJournalPolicy.requireUnchanged(snapshot,snapshot)
        for(current in listOf(null,snapshot.copy(revision=2),snapshot.copy(draft=draft(net=5100000)),
            snapshot.copy(updatedAtEpochMs=2000))) {
            assertTrue(runCatching {PayrollJournalPolicy.requireUnchanged(current,snapshot)}.isFailure)
        }
    }
}
