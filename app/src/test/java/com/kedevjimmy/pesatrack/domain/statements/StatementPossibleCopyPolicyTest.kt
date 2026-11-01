package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class StatementPossibleCopyPolicyTest {
    private val hour = 3_600_000L
    private val base = 1_780_000_000_000L
    private fun row(id: String, source: StatementEvidenceCorrelationPolicy.Source, account: String = "wallet",
                    amount: Long = -500, offset: Long = 0, sourceId: String = id,
                    totals: Boolean = true, protected: Boolean = false) =
        StatementEvidenceCorrelationPolicy.Observation(id, source, sourceId, account, "KES", base + offset * hour,
            amount, null, true, roleProtected = protected, sourceTotalsVerified = totals)
    private val statement = row("statement", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
    private val activity = row("activity", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, offset = 1, totals = false)
    private fun suggest(rows: List<StatementEvidenceCorrelationPolicy.Observation>, excluded: Set<String> = emptySet()) =
        StatementPossibleCopyPolicy.suggest(rows, excluded, base, base + 24 * hour)

    @Test fun differentSourcesForSameAccountAmountAndDirectionAreOnlyAClue() {
        val result = suggest(listOf(statement, activity))
        assertEquals(1, result.size)
        assertEquals(setOf("statement", "activity"), result.single().rows.map { it.id }.toSet())
    }

    @Test fun separateActivityRowsCanBeReviewedWithoutAssumingTheyAreCopies() {
        val first = row("one", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, sourceId = "activity-ledger")
        val second = row("two", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, offset = 1, sourceId = "activity-ledger")
        assertEquals(1, suggest(listOf(first, second)).size)
    }

    @Test fun competingRowsProtectedEvidenceAndUnverifiedDocumentsAreNotGuessed() {
        val rival = row("rival", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, offset = 2)
        assertTrue(suggest(listOf(statement, activity, rival)).isEmpty())
        assertTrue(suggest(listOf(statement.copy(roleProtected = true), activity)).isEmpty())
        assertTrue(suggest(listOf(statement.copy(sourceTotalsVerified = false), activity)).isEmpty())
        assertTrue(suggest(listOf(statement, activity), setOf("statement")).isEmpty())
    }

    @Test fun sameSourceDifferentAccountsOrDirectionsAreNotCopies() {
        assertTrue(suggest(listOf(statement, activity.copy(sourceId = statement.sourceId))).isEmpty())
        assertTrue(suggest(listOf(statement, activity.copy(accountId = "bank"))).isEmpty())
        assertTrue(suggest(listOf(statement, activity.copy(movementMinor = 500))).isEmpty())
        assertTrue(suggest(listOf(statement, activity.copy(occurredAt = base + 49 * hour))).isEmpty())
    }

    @Test fun busyAmountsDoNotHideUniquePairsElsewhere() {
        val distant = (1..300).map { row("extra-$it", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, offset = 100L + it * 3L) }
        assertEquals(1, suggest(listOf(statement, activity) + distant).size)
    }

    @Test fun competitorNearSecondRecordPreventsAnApparentlyUniquePair() {
        val later = activity.copy(occurredAt = base + 48 * hour)
        val competitor = row("later", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, offset = 96)
        assertTrue(suggest(listOf(statement, later, competitor)).isEmpty())
    }

    @Test fun suggestionDiagnosticsDoNotExposeAmountsOrReferences() {
        val rows = listOf(statement.copy(reference = "0712345678", movementMinor = -123456), activity)
        val copy = StatementPossibleCopyPolicy.Suggestion(rows)
        val transfer = StatementPossibleTransferPolicy.Suggestion(rows[0], rows[1])
        assertFalse(copy.toString().contains("0712345678"))
        assertFalse(copy.toString().contains("123456"))
        assertFalse(transfer.toString().contains("0712345678"))
        assertFalse(transfer.toString().contains("123456"))
    }
}
