package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** Invented positions and values. Never store a real financial document fixture. */
class UniversalStatementGeometryTest {
    private val headerLabels = listOf(
        "Receipt No.", "Completion Time", "Details", "Transaction Status",
        "Paid In", "Withdrawn", "Balance"
    )
    private val lefts = listOf(10f, 80f, 150f, 220f, 260f, 310f, 360f)
    private val rights = listOf(65f, 140f, 205f, 245f, 295f, 345f, 402f)
    private fun box(text: String, x: Float, y: Float, right: Float) =
        PdfTextFragment(text, x, y, right, y + 8f)
    private fun headers() = headerLabels.indices.map { i ->
        box(headerLabels[i], lefts[i], 5f, rights[i])
    }
    private fun body(): List<PdfTextFragment> = listOf(
        box("ABC1234569", 10f, 30f, 65f),
        box("2026-10-10 10:30:00", 80f, 30f, 140f),
        box("Shop payment", 150f, 30f, 200f),
        box("Completed", 220f, 30f, 245f),
        box("-20.00", 310f, 30f, 343f),
        box("490.00", 360f, 30f, 402f),

        box("ABC1234568", 10f, 60f, 65f),
        box("2026-10-09 10:30:00", 80f, 60f, 140f),
        box("Money reversal", 150f, 60f, 202f),
        box("via API from", 150f, 71f, 198f),
        box("Completed", 220f, 60f, 245f),
        box("100.00", 260f, 60f, 293f),
        box("510.00", 360f, 60f, 402f),

        box("ABC1234567", 10f, 90f, 65f),
        box("2026-10-08 10:30:00", 80f, 90f, 140f),
        box("Taxi", 150f, 90f, 200f),
        box("Completed", 220f, 90f, 245f),
        box("-50.00", 310f, 90f, 343f),
        box("410.00", 360f, 90f, 402f)
    )
    private fun input(
        body: List<PdfTextFragment> = body(),
        headers: List<PdfTextFragment> = headers()
    ) = StatementPositionedPage(3, 0f, 415f, headers, body)
    private fun table(page: StatementPositionedPage = input()): StatementTablePage {
        val result = UniversalStatementGeometry.reconstruct(page)
        assertTrue(result.toString(), result is StatementGeometryResult.Reconstructed)
        return (result as StatementGeometryResult.Reconstructed).table
    }

    @Test fun positionAwareExtractionReassemblesWrappedDetailsWithoutMovingAmounts() {
        val result = UniversalStatementTableInterpreter.interpret(
            listOf(table()),
            TimeZone.getTimeZone("Africa/Nairobi"),
            46_000L
        )
        assertTrue(result.toString(), result is StatementTableResult.Accepted)
        val rows = (result as StatementTableResult.Accepted).legs
        assertEquals(3,rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),rows.map { it.movementMinor })
        assertEquals("Money reversal via API from", rows[1].description)
        assertEquals(3,rows[1].pageNumber)
    }

    @Test fun ambiguousOverlappingColumnHeadingsRejectDocument() {
        val headers = headers().toMutableList()
        headers[5] = box("Withdrawn", 285f, 5f, 345f)
        val r = UniversalStatementGeometry.reconstruct(input(headers = headers))
        assertEquals(StatementGeometryProblem.AMBIGUOUS_COLUMNS,
            (r as StatementGeometryResult.Unsupported).problem)
    }

    @Test fun missingAnchoringDatesDoNotCreateGuessedRows() {
        val body = body().filterNot { it.text.startsWith("2026-") }
        val r = UniversalStatementGeometry.reconstruct(input(body = body))
        assertEquals(StatementGeometryProblem.MISSING_DATE_ANCHORS,
            (r as StatementGeometryResult.Unsupported).problem)
    }

    @Test fun fragmentCrossingColumnBoundaryCannotBeAssignedByGuess() {
        val body = body().map {
            if (it.text == "100.00") box(it.text, 270f, 60f, 320f) else it
        }
        val r = UniversalStatementGeometry.reconstruct(input(body = body))
        assertEquals(StatementGeometryProblem.UNASSIGNED_FRAGMENT,
            (r as StatementGeometryResult.Unsupported).problem)
    }

    @Test fun unknownMoneyColumnSurvivesReconstructionAndBlocksInterpreter() {
        val headers = headers() + box("Other Amount", 420f, 5f, 475f)
        val data = body() + box("KES 3.00", 420f, 60f, 475f)
        val geom = StatementPositionedPage(3, 0f, 485f, headers, data)
        val parsed = table(geom)
        val result = UniversalStatementTableInterpreter.interpret(listOf(parsed))
        assertEquals(StatementBlockReason.UNKNOWN_MONETARY_COLUMN,
            (result as StatementTableResult.Blocked).reason)
    }

    @Test fun orphanPrecedingFragmentCannotBeSilentlyDropped() {
        val data = body() + box("Unexplained", 150f, 20f, 205f)
        val r = UniversalStatementGeometry.reconstruct(input(body = data))
        assertEquals(StatementGeometryProblem.UNASSIGNED_FRAGMENT,
            (r as StatementGeometryResult.Unsupported).problem)
    }

    @Test fun invalidUnboundedBoxesMustNotFallbackToUnverifiedData() {
        val bad = body() + PdfTextFragment("Cash", Float.NaN, 100f, 210f, 108f)
        val r = UniversalStatementGeometry.reconstruct(input(body = bad))
        assertEquals(StatementGeometryProblem.INVALID_BOUNDS,
            (r as StatementGeometryResult.Unsupported).problem)
    }
}
