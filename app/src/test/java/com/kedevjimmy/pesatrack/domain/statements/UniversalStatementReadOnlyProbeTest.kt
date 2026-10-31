package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import org.junit.Assert.*
import org.junit.Test

class UniversalStatementReadOnlyProbeTest {
    private fun box(text: String, x: Float, y: Float, width: Float = 50f) =
        PdfTextFragment(text, x, y, x + width, y + 8f)

    private fun signedHeader() = listOf(
        box("Date", 10f, 50f), box("Description", 120f, 50f, 95f),
        box("Signed Amount", 300f, 50f, 95f), box("Balance", 460f, 50f)
    )

    private fun signedRow(y: Float, date: String, movement: String, balance: String) = listOf(
        box(date, 10f, y, 75f), box("Invented shop", 120f, y, 95f),
        box(movement, 300f, y), box(balance, 460f, y)
    )

    private fun splitHeader() = listOf(
        box("Date", 10f, 50f), box("Narration", 120f, 50f, 95f),
        box("Debit", 300f, 50f), box("Credit", 380f, 50f),
        box("Balance", 460f, 50f)
    )

    private fun splitRow(y: Float, date: String, debit: String, credit: String, balance: String) = listOf(
        box(date, 10f, y, 75f), box("Invented salary", 120f, y, 95f),
        box(debit, 300f, y), box(credit, 380f, y), box(balance, 460f, y)
    )

    private fun directedHeader() = listOf(
        box("Date", 10f, 50f), box("Description", 120f, 50f, 95f),
        box("Amount", 300f, 50f), box("Direction", 380f, 50f, 70f),
        box("Balance", 480f, 50f)
    )

    private fun directedRow(y: Float, date: String, movement: String, direction: String, balance: String) = listOf(
        box(date, 10f, y, 75f), box("Invented ATM", 120f, y, 95f),
        box(movement, 300f, y), box(direction, 380f, y), box(balance, 480f, y)
    )

    private fun firstPage() = StatementProbePage(1, signedHeader() +
        signedRow(80f, "2026-01-01", "-10.00", "90.00") +
        signedRow(110f, "2026-01-02", "+20.00", "110.00"))

    private fun secondPage() = StatementProbePage(2, splitHeader() +
        splitRow(80f, "2026-01-03", "5.00", "-", "105.00") +
        splitRow(110f, "2026-01-04", "-", "10.00", "115.00"))

    private fun blocked(pages: List<StatementProbePage>): StatementProbeResult.Blocked =
        UniversalStatementReadOnlyProbe.inspect(pages, openingBalanceMinor = 10_000L)
            as StatementProbeResult.Blocked

    @Test fun unrelatedHeaderFamiliesReconcileAcrossBothPages() {
        val result = UniversalStatementReadOnlyProbe.inspect(
            listOf(firstPage(), secondPage()), openingBalanceMinor = 10_000L
        ) as StatementProbeResult.Provisional
        assertEquals(2, result.tables.size)
        assertEquals(4, result.sourceRows)
        assertEquals(listOf(-1_000L, 2_000L, -500L, 1_000L),
            result.legs.map { it.movementMinor })
        assertEquals(listOf(1, 1, 2, 2), result.legs.map { it.pageNumber })
        assertFalse(result.oldestLegUnanchored)
    }

    @Test fun firstPageMayBeUnanchoredButAllAdjacentLegsMustReconcile() {
        val result = UniversalStatementReadOnlyProbe.inspect(
            listOf(firstPage(), secondPage())
        ) as StatementProbeResult.Provisional
        assertEquals(4, result.sourceRows)
        assertTrue(result.oldestLegUnanchored)
    }

    @Test fun malformedSecondPageNeverExposesFirstPageCandidate() {
        val result = blocked(listOf(firstPage(), StatementProbePage(2, listOf(
            box("Unrelated", 10f, 50f), box("Not", 120f, 50f), box("Table", 300f, 50f)
        ))))
        assertEquals(StatementProbeReason.UNRECOGNIZED_TABLE, result.reason)
        assertEquals(2, result.pageNumber)
    }

    @Test fun missingUnreadableSecondPageBlocksEntireDocument() {
        val result = blocked(listOf(firstPage(), StatementProbePage(2, emptyList())))
        assertEquals(StatementProbeReason.UNRECOGNIZED_TABLE, result.reason)
        assertEquals(2, result.pageNumber)
    }

    @Test fun emptyGappedDuplicatedReorderedAndNonStartingPagesAreInvalid() {
        val first = firstPage()
        val second = secondPage()
        val invalid = listOf(
            emptyList(), listOf(first, second.copy(pageNumber = 3)),
            listOf(first, first), listOf(second, first), listOf(first.copy(pageNumber = 2)),
            listOf(first.copy(pageNumber = 0))
        )
        for (pages in invalid) {
            assertEquals(StatementProbeReason.INVALID_PAGE_SEQUENCE, blocked(pages).reason)
        }
    }

    @Test fun contradictoryCrossPageBalanceBlocksAllMovements() {
        val broken = secondPage().copy(words = splitHeader() +
            splitRow(80f, "2026-01-03", "5.00", "-", "100.00") +
            splitRow(110f, "2026-01-04", "-", "10.00", "110.00"))
        assertEquals(StatementProbeReason.UNVERIFIED_MOVEMENTS,
            blocked(listOf(firstPage(), broken)).reason)
    }

    @Test fun unknownNumericColumnCannotBeIgnored() {
        val broken = secondPage().copy(words = secondPage().words +
            box("Adjustment", 550f, 50f, 90f) + box("5.00", 550f, 80f))
        val result = blocked(listOf(firstPage(), broken))
        assertEquals(StatementProbeReason.UNRECOGNIZED_TABLE, result.reason)
        assertEquals(2, result.pageNumber)
    }

    @Test fun unrepresentableDirectedMagnitudeIsBlockedWithoutArithmeticCrash() {
        val page = StatementProbePage(1, directedHeader() +
            directedRow(80f, "2026-01-01", "-92233720368547758.08", "DR", "90.00") +
            directedRow(110f, "2026-01-02", "10.00", "CR", "100.00"))
        assertEquals(StatementProbeReason.UNVERIFIED_MOVEMENTS, blocked(listOf(page)).reason)
    }

    @Test fun minimumLongSignedMovementIsNeverProvisional() {
        val page = StatementProbePage(1, signedHeader() +
            signedRow(80f, "2026-01-01", "-92233720368547758.08", "-92233720368547758.08") +
            signedRow(110f, "2026-01-02", "+10.00", "-92233720368547748.08"))
        val result = UniversalStatementReadOnlyProbe.inspect(listOf(page), openingBalanceMinor = 0L)
            as StatementProbeResult.Blocked
        assertEquals(StatementProbeReason.UNVERIFIED_MOVEMENTS, result.reason)
    }

    @Test fun fragmentAndPageCapsRejectBeforeInterpretingPartials() {
        val tooManyFragments = List(32_001) { box("invented", 10f, it.toFloat()) }
        assertEquals(StatementProbeReason.INVALID_PAGE_SEQUENCE,
            blocked(listOf(StatementProbePage(1, tooManyFragments))).reason)
        val overPageCap = (1..161).map { firstPage().copy(pageNumber = it) }
        assertEquals(StatementProbeReason.INVALID_PAGE_SEQUENCE, blocked(overPageCap).reason)
    }

    @Test fun totalRowCapCannotBeBypassedBySplittingAcrossPages() {
        val pages = (1..101).map { page ->
            StatementProbePage(page, splitHeader() + (0 until 50).flatMap { row ->
                splitRow(80f + row * 18f, "2026-01-01", "1.00", "-", "99.00")
            })
        }
        assertEquals(StatementProbeReason.INVALID_PAGE_SEQUENCE, blocked(pages).reason)
    }
}
