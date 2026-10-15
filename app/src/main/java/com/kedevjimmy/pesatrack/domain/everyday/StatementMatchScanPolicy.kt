package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy

/**
 * Pure read-only scan over ALL immutable source rows. One row can be in a
 * saved PDF but excluded from the current one-source-per-month report due to
 * overlapping uploads. Never present excluded rows as report-visible spending.
 */
object StatementMatchScanPolicy {
    data class Row(
        val batchId: String,
        val entry: StatementActivityLine,
        val visibleInReport: Boolean,
        val manuallyCorrected: Boolean
    )

    data class Result(
        val scannedRows: Int,
        val documents: Int,
        val namedMatches: Int,
        val eligibleAllSources: Int,
        val eligibleInReports: Int,
        val protectedOrAlreadyCorrect: Int
    ) {
        val hiddenBySourceSelection: Int
            get() = eligibleAllSources - eligibleInReports
    }

    fun scan(
        rows: List<Row>, name: String?, phone: String?,
        categoryId: String,
        existingMemories: List<StatementCategoryMemoryPolicy.Memory>,
        categoryKinds: Map<String, String>,
        statementRules: Map<String, String>
    ): Result {
        var named = 0
        var eligible = 0
        var report = 0
        var protected = 0
        for (row in rows) {
            val extracted = StatementFinancialPolicy.merchantKey(row.entry.description)
            // Diagnostic count proves the source scan ran without exposing names,
            // and distinguishes a matching name from an eligible category edit.
            if (extracted == null || !RememberedCategoryMatchPolicy.matches(
                    name, null, extracted, null
                )) continue
            named++
            if (!StatementCategoryMemoryPolicy.proposedSourceMatches(
                    name, phone, row.entry.description, existingMemories
                )) {
                protected++
                continue
            }
            val current = StatementCategoryMemoryPolicy.learnedFor(
                row.entry.description, existingMemories
            ) ?: statementRules[extracted]
            if (!StatementCategoryMemoryPolicy.wouldChange(
                    row.batchId, row.entry, categoryId, current,
                    row.manuallyCorrected, categoryKinds
                )) {
                protected++
                continue
            }
            eligible++
            if (row.visibleInReport) report++
        }
        return Result(
            scannedRows = rows.size,
            documents = rows.map { it.batchId }.distinct().size,
            namedMatches = named,
            eligibleAllSources = eligible,
            eligibleInReports = report,
            protectedOrAlreadyCorrect = protected
        )
    }
}
