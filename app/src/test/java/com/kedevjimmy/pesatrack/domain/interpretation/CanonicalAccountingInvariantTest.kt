package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 / A1: independent financial-safety assertions over the existing,
 * sanitized interpreter fixture corpus. No production parsing rules change.
 *
 * Financial messages can yield multiple records (e.g. a transfer and two
 * balance observations). Observations are evidence, never new cash flow.
 */
class CanonicalAccountingInvariantTest {
    private val nonEarningKinds = setOf(
        CanonicalFinancialRecordKind.TRANSFER,
        CanonicalFinancialRecordKind.CASH_WITHDRAWAL,
        CanonicalFinancialRecordKind.CASH_DEPOSIT,
        CanonicalFinancialRecordKind.DEBT_DRAWDOWN,
        CanonicalFinancialRecordKind.DEBT_REPAYMENT,
        CanonicalFinancialRecordKind.REFUND,
        CanonicalFinancialRecordKind.REVERSAL,
        CanonicalFinancialRecordKind.FEE,
        CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
        CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION,
        CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT
    )

    @Test
    fun fixturesHaveUniqueIdentifiersAndExpectedCanonicalContracts() {
        val fixtures = FinancialMessageFixtureCorpus.all
        assertTrue("Fixture corpus must not be empty", fixtures.isNotEmpty())
        assertEquals(
            "Duplicate fixture IDs conceal coverage regressions",
            fixtures.size,
            fixtures.map { it.id }.toSet().size
        )
        val codes = fixtures.mapNotNull { it.transactionCode }
        assertEquals(
            "Duplicate synthetic transaction codes create false replay evidence",
            codes.size,
            codes.toSet().size
        )
        fixtures.forEach { fixture ->
            assertTrue("Blank fixture ID", fixture.id.isNotBlank())
            assertTrue("${fixture.id}: blank message", fixture.body.isNotBlank())
            val draft = FinancialMessageInterpreter
                .interpret(title = fixture.title, body = fixture.body)
                .toCanonicalDraft()
            assertEquals("${fixture.id}: transaction ID", fixture.transactionCode, draft.transactionCode)
            assertEquals(
                "${fixture.id}: expected canonical kinds",
                fixture.expectedKinds,
                draft.records.map { it.kind }
            )
            if (fixture.coverage == FixtureCoverage.NON_FINANCIAL) {
                assertFalse("${fixture.id}: unrelated text recognized as financial", draft.recognized)
                assertTrue("${fixture.id}: unrelated text created records", draft.records.isEmpty())
            }
        }
    }

    @Test
    fun observationsAndInternalMovementsNeverBecomeEarnings() {
        val fixtures = FinancialMessageFixtureCorpus.all
        val candidates = fixtures.filter { fixture ->
            fixture.expectedKinds.isNotEmpty() &&
                fixture.expectedKinds.all { it in nonEarningKinds }
        }
        assertTrue("Need at least one non-earnings fixture", candidates.isNotEmpty())

        candidates.forEach { fixture ->
            val draft = FinancialMessageInterpreter
                .interpret(title = fixture.title, body = fixture.body)
                .toCanonicalDraft()
            assertFalse(
                "${fixture.id}: an internal/non-earning event became a receipt",
                draft.records.any { it.kind == CanonicalFinancialRecordKind.RECEIPT }
            )
            assertFalse(
                "${fixture.id}: an internal/non-earning event became a payment",
                draft.records.any { it.kind == CanonicalFinancialRecordKind.PAYMENT }
            )
        }
    }

    @Test
    fun allCanonicalMovementAmountsAreNonnegativeAndObservationsAreNotMovements() {
        FinancialMessageFixtureCorpus.all.forEach { fixture ->
            val records = FinancialMessageInterpreter
                .interpret(title = fixture.title, body = fixture.body)
                .toCanonicalDraft().records
            records.forEach { record ->
                when (record) {
                    is MoneyMovementDraft ->
                        assertTrue("${fixture.id}: negative movement", record.amountMinor >= 0L)
                    is DebtMovementDraft ->
                        assertTrue("${fixture.id}: negative debt movement", record.amountMinor >= 0L)
                    is UnresolvedMovementDraft ->
                        assertTrue("${fixture.id}: negative unknown movement", record.amountMinor >= 0L)
                    is ReversalDraft ->
                        assertTrue("${fixture.id}: negative reversal", record.amountMinor >= 0L)
                    is FeeDraft ->
                        assertTrue("${fixture.id}: negative fee", record.amountMinor >= 0L)
                    is BalanceObservationDraft ->
                        assertEquals(
                            "${fixture.id}: balance observation kind",
                            CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                            record.kind
                        )
                    is AvailableCreditObservationDraft ->
                        assertEquals(
                            "${fixture.id}: credit observation kind",
                            CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION,
                            record.kind
                        )
                }
            }
        }
    }
}
