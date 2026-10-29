package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementSourceCoveragePolicyTest {
    private val at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }.parse("2026-09-15 12:00:00")!!.time

    private fun row(
        id: String, document: String, account: String = "wallet",
        minor: Long = -1_000L, reference: String? = "QAB1234569",
        time: Long = at, currency: String = "KES", confirmed: Boolean = true,
        totalsVerified: Boolean = true,
        source: StatementEvidenceCorrelationPolicy.Source =
            StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT
    ) = StatementEvidenceCorrelationPolicy.Observation(
        id, source, document, account, currency, time, minor, reference, confirmed,
        sourceTotalsVerified = totalsVerified
    )

    @Test fun threeDocumentsRepeatingOneVerifiedLegCountItOnce() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("one", "file1"), row("two", "file2"), row("three", "file3")
        )).single()
        assertEquals("2026-09", coverage.monthKey)
        assertEquals(3, coverage.sourceCount)
        assertEquals(3, coverage.observedLegCount)
        assertEquals(1, coverage.remainingObservedLegCount)
        assertEquals(2, coverage.confirmedRepeatCopies)
        assertEquals(0, coverage.unresolvedCandidateCount)
        assertEquals(StatementSourceCoveragePolicy.Status.VERIFIED_REPEAT, coverage.status)
        assertEquals(1_000L, coverage.safeMoneyOutMinor)
        assertEquals(0L, coverage.safeMoneyInMinor)
    }

    @Test fun complementaryDocumentsRetainSeparatelyReferencedLegs() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("one", "file1", minor = -2_000L),
            row("two", "file2", minor = 5_000L, reference = "QAB9876543", time = at + 86_400_000L)
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.COMPLEMENTARY, coverage.status)
        assertEquals(2, coverage.remainingObservedLegCount)
        assertEquals(0, coverage.confirmedRepeatCopies)
        assertEquals(2_000L, coverage.safeMoneyOutMinor)
        assertEquals(5_000L, coverage.safeMoneyInMinor)
    }

    @Test fun bankDocumentsWithoutReferencesCannotSilentlyDeduplicate() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("first", "equity-a", account = "equity", reference = null,
                source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            row("second", "equity-b", account = "equity", reference = null,
                time = at + 60_000L, source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertEquals(0, coverage.confirmedRepeatCopies)
        assertEquals(2, coverage.unresolvedCandidateCount)
        assertNull(coverage.safeMoneyOutMinor)
        assertNull(coverage.safeMoneyInMinor)
    }

    @Test fun unreferencedBankRowsRemainUncertainEvenWhenNoNearTimeCollisionExists() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("first", "equity-a", account = "equity", reference = null,
                source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            row("second", "equity-b", account = "equity", reference = null,
                time = at + 6 * 86_400_000L,
                source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertEquals(2, coverage.unresolvedCandidateCount)
        assertNull(coverage.safeMoneyOutMinor)
    }

    @Test fun conflictingAmountUnderSameReferenceIsReviewNotASuppressedDuplicate() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("first", "file-a"), row("changed", "file-b", minor = -990L)
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertEquals(2, coverage.unresolvedCandidateCount)
        assertEquals(0, coverage.confirmedRepeatCopies)
        assertNull(coverage.safeMoneyOutMinor)
    }

    @Test fun feeAndPrincipalRemainSeparateEvenWhenEachAppearsTwice() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("principal-a", "file-a", minor = -1_000L),
            row("fee-a", "file-a", minor = -50L),
            row("principal-b", "file-b", minor = -1_000L),
            row("fee-b", "file-b", minor = -50L)
        )).single()
        assertEquals(2, coverage.confirmedRepeatCopies)
        assertEquals(2, coverage.remainingObservedLegCount)
        assertEquals(0, coverage.unresolvedCandidateCount)
        assertEquals(1_050L, coverage.safeMoneyOutMinor)
    }

    @Test fun ownedBankToWalletTransferProducesIndependentAccountCoverage() {
        val months = StatementSourceCoveragePolicy.analyze(listOf(
            row("bank", "equity-pdf", account = "equity", minor = -4_000L,
                source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT),
            row("wallet", "mpesa-pdf", account = "wallet", minor = 4_000L)
        ))
        assertEquals(2, months.size)
        assertEquals(4_000L, months.single { it.accountId == "equity" }.safeMoneyOutMinor)
        assertEquals(4_000L, months.single { it.accountId == "wallet" }.safeMoneyInMinor)
        assertTrue(months.all { it.confirmedRepeatCopies == 0 })
    }

    @Test fun differentCurrenciesAreNotCombinedIntoOneMoneyTotal() {
        val months = StatementSourceCoveragePolicy.analyze(listOf(
            row("kes", "doc-a", account = "bank", currency = "KES"),
            row("usd", "doc-b", account = "bank", currency = "USD")
        ))
        assertEquals(2, months.size)
        assertEquals(setOf("KES", "USD"), months.map { it.currency }.toSet())
        assertTrue(months.all { it.sourceCount == 1 && it.confirmedRepeatCopies == 0 })
    }

    @Test fun duplicatedLegInsideOneDocumentIsNotDeleted() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("one", "file-a"), row("two", "file-a")
        )).single()
        assertEquals(1, coverage.sourceCount)
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertEquals(0, coverage.confirmedRepeatCopies)
        assertNull(coverage.safeMoneyOutMinor)
    }

    @Test fun independentExactReferencesStayDistinctEvenAtSameTimeAndAmount() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("first", "file-a"), row("second", "file-b", reference = "XYZ1234567")
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.COMPLEMENTARY, coverage.status)
        assertEquals(2_000L, coverage.safeMoneyOutMinor)
    }

    @Test fun unresolvedCandidatePreventsTotalsEvenIfOtherRowsAreProvenRepeated() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("a", "file-a"), row("b", "file-b"),
            row("unreferenced", "file-b", minor = -2_000L, reference = null),
            row("also-unreferenced", "file-c", minor = -2_000L,
                reference = null, time = at + 30_000L)
        )).single()
        assertEquals(1, coverage.confirmedRepeatCopies)
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertNull(coverage.safeMoneyOutMinor)
    }

    @Test fun sourceOrderNeverChangesResult() {
        val documents = listOf(
            row("a", "file-a"), row("b", "file-b"),
            row("c", "file-c", minor = -800L, reference = "ZZZ1234567")
        )
        assertEquals(StatementSourceCoveragePolicy.analyze(documents),
            StatementSourceCoveragePolicy.analyze(documents.reversed()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun activityIsNotASecondStatementDocument() {
        StatementSourceCoveragePolicy.analyze(listOf(row(
            "activity", "sms", source = StatementEvidenceCorrelationPolicy.Source.ACTIVITY
        )))
    }

    @Test fun unconfirmedOwnershipBlocksCombinedTotals() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("one", "file-a", confirmed = false), row("two", "file-b")
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertNull(coverage.safeMoneyOutMinor)
    }

    @Test fun independentlyUnverifiedSourceTotalsBlockCombinedCashFigures() {
        val coverage = StatementSourceCoveragePolicy.analyze(listOf(
            row("unverified", "file-a", totalsVerified = false)
        )).single()
        assertEquals(StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, coverage.status)
        assertEquals(1, coverage.unresolvedCandidateCount)
        assertNull(coverage.safeMoneyInMinor)
        assertNull(coverage.safeMoneyOutMinor)
    }
}
