package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementClassified
import com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
import com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport
import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage

/**
 * Snapshot reader across selected statement months for one confirmed account.
 * Purely read-only. A source batch spanning multiple months is loaded once.
 */
class StatementReportReader(context: Context) {
    private val app = context.applicationContext
    private val store = StatementActivityStore(app)
    private val bridge = StatementCategoryBridge(app)

    fun read(selected: List<StatementHistoryReport.SelectedMonth>): List<StatementClassified> {
        if (selected.isEmpty()) return emptyList()
        require(selected.map { it.source.accountId }.distinct().size == 1) {
            "Never silently combine multiple statement accounts."
        }
        val memories = bridge.activityMemories()
        val kinds = bridge.categoryKinds()
        val statementRules = store.categoryRules()
        val byBatch = selected.groupBy { it.source.batchId }
        val sources = byBatch.mapValues { (_, entries) ->
            store.lines(entries.first().source.batchId)
        }
        val receiptEvidence = StatementReceiptEvidenceReader(
            context = app
        ).read(sources.values.flatten())
        return buildList {
            byBatch.forEach { (batchId, selectedMonths) ->
                val keys = selectedMonths.map { it.key }.toSet()
                val corrections = store.categoriesFor(batchId)
                sources[batchId].orEmpty().forEach { row ->
                    if (StatementMonthlyUsage.monthKey(row.occurredAt) !in keys) return@forEach
                    val merchant = StatementFinancialPolicy.merchantKey(row.description)
                    // Activity's latest confirmed learning applies to historical
                    // and future PDFs; explicit per-row statement edits still win.
                    val remembered = bridge.learnedFor(row.description, memories)
                        ?: com.kedevjimmy.pesatrack.domain.everyday.StatementReceiptEvidence.categoryFor(
                            row, selectedMonths.first().source.accountId,
                            receiptEvidence, memories
                        )
                        ?: merchant?.let { statementRules[it] }
                    add(StatementFinancialPolicy.classify(
                        batchId = batchId, line = row,
                        assignedCategory = corrections[row.page to row.row],
                        rememberedCategory = remembered, categoryKinds = kinds
                    ))
                }
            }
        }.sortedWith(
            compareByDescending<StatementClassified> { it.line.occurredAt }
                .thenBy { it.batchId }.thenBy { it.line.page }.thenBy { it.line.row }
        )
    }
}
