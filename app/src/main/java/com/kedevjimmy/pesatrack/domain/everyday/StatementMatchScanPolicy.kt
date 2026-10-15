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
        val accountId: String,
        val entry: StatementActivityLine,
        val visibleInReport: Boolean,
        val manuallyCorrected: Boolean
    )

    data class Result(
        val scannedRows: Int,
        val documents: Int,
        val namedMatches: Int,
        val receiptMatches: Int,
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
        statementRules: Map<String, String>,
        receiptEvidence: Map<String, List<StatementReceiptEvidence.ActivityReceipt>> = emptyMap()
    ): Result {
        var named = 0
        var receiptMatched = 0
        var eligible = 0
        var report = 0
        var protected = 0
        for (row in rows) {
            val extracted = StatementFinancialPolicy.merchantKey(row.entry.description)
            // Diagnostic count proves the source scan ran without exposing names,
            // and distinguishes a matching name from an eligible category edit.
            val namedIdentity = extracted != null && RememberedCategoryMatchPolicy.matches(
                name, null, extracted, null
            )
            val safeName = namedIdentity && StatementCategoryMemoryPolicy.proposedSourceMatches(
                name, phone, row.entry.description, existingMemories
            )
            val receipt = StatementReceiptEvidence.corroborated(
                row.entry, row.accountId,
                receiptEvidence[row.entry.reference].orEmpty()
            )
            val receiptIdentity = receipt != null &&
                StatementReceiptEvidence.sameIdentity(name, phone, receipt)
            if (!namedIdentity && !receiptIdentity) continue
            if (namedIdentity) named++
            if (receiptIdentity && !safeName) receiptMatched++
            if (!safeName && !receiptIdentity) {
                protected++
                continue
            }
            // Receipt-matched identity may classify descriptions that omit
            // the name, but never overrules structural financial evidence.
            val current = StatementCategoryMemoryPolicy.learnedFor(
                row.entry.description, existingMemories
            ) ?: StatementReceiptEvidence.categoryFor(
                row.entry, row.accountId, receiptEvidence, existingMemories
            ) ?: extracted?.let(statementRules::get)
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
            receiptMatches = receiptMatched,
            eligibleAllSources = eligible,
            eligibleInReports = report,
            protectedOrAlreadyCorrect = protected
        )
    }
}
