package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment

data class StatementProbePage(val pageNumber: Int, val words: List<PdfTextFragment>)

enum class StatementProbeReason {
    INVALID_PAGE_SEQUENCE, UNRECOGNIZED_TABLE, UNVERIFIED_MOVEMENTS
}

/** Provisional evidence only. This is never a verified document or an import admission. */
sealed class StatementProbeResult {
    data class Provisional(
        val tables: List<StatementTablePage>,
        val legs: List<StatementLegEvidence>,
        val sourceRows: Int,
        val oldestLegUnanchored: Boolean
    ) : StatementProbeResult()

    /** Stable metadata only: no statement text, names, account numbers or amounts. */
    data class Blocked(val pageNumber: Int?, val reason: StatementProbeReason) : StatementProbeResult()
}

/**
 * Bounded, read-only inspection of all supplied physical PDF pages.
 * It cannot prove that a PDF contains only these pages or that it belongs to an issuer/account.
 * The production statement importer must not treat Provisional as permission to save.
 */
object UniversalStatementReadOnlyProbe {
    private const val MAX_PAGES = 160
    private const val MAX_FRAGMENTS = 32_000L
    private const val MAX_ROWS = 5_000

    fun inspect(
        pages: List<StatementProbePage>,
        openingBalanceMinor: Long? = null,
        profile: StatementFormatProfile = StatementFormatProfile(),
        now: Long = System.currentTimeMillis()
    ): StatementProbeResult {
        fun blocked(page: Int?, reason: StatementProbeReason) =
            StatementProbeResult.Blocked(page, reason)
        if (pages.isEmpty() || pages.size > MAX_PAGES ||
            pages.withIndex().any { (index, page) -> page.pageNumber != index + 1 } ||
            pages.sumOf { it.words.size.toLong() } > MAX_FRAGMENTS
        ) return blocked(null, StatementProbeReason.INVALID_PAGE_SEQUENCE)

        val completeTables = mutableListOf<StatementTablePage>()
        var completeRows = 0
        for (page in pages) {
            if (page.words.isEmpty())
                return blocked(page.pageNumber, StatementProbeReason.UNRECOGNIZED_TABLE)
            when (val result = StatementTableDiscovery.discover(page.pageNumber, page.words, profile)) {
                is StatementTableDiscoveryResult.Blocked ->
                    return blocked(page.pageNumber, StatementProbeReason.UNRECOGNIZED_TABLE)
                is StatementTableDiscoveryResult.Found -> {
                    completeRows += result.tables.sumOf { it.rows.size }
                    if (completeRows > MAX_ROWS)
                        return blocked(page.pageNumber, StatementProbeReason.INVALID_PAGE_SEQUENCE)
                    completeTables.addAll(result.tables)
                }
            }
        }

        return when (val result = UniversalStatementTableInterpreter.interpret(
            completeTables, openingBalanceMinor = openingBalanceMinor, profile = profile, now = now
        )) {
            is StatementTableResult.Blocked ->
                blocked(result.pageNumber, StatementProbeReason.UNVERIFIED_MOVEMENTS)
            is StatementTableResult.Accepted -> StatementProbeResult.Provisional(
                completeTables.toList(), result.legs, result.sourceRows, result.oldestLegUnanchored
            )
        }
    }
}
