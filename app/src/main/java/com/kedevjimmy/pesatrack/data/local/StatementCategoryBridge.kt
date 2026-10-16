package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementCategoryMemoryPolicy
import com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
import com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport
import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/**
 * Only reads approved Activity category memories. Statements remain their own
 * immutable source journal; learning never imports/updates SMS or debt entries.
 */
class StatementCategoryBridge(context: Context) {
    private val app = context.applicationContext

    fun activityMemories(): List<StatementCategoryMemoryPolicy.Memory> {
        val db = PesaTrackDatabase.get(app).readableDatabase
        return db.rawQuery(
            """
            SELECT t.counterparty, t.counterparty_phone, p.category_id
            FROM identity_category_preferences p
            JOIN transactions t ON t.id = p.source_transaction_id
            WHERE p.direction = 'EXPENSE'
            """.trimIndent(), null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    // Historical 'LUNCH' labels use EATING_OUT in current
                    // Activity, budgets and Statement report groupings.
                    val category = CategoryStore.get(app).canonicalId(cursor.getString(2))
                    if (PersonalLearningPolicy.isLearnableCategory(category, MoneyDirection.EXPENSE)) {
                        add(StatementCategoryMemoryPolicy.Memory(
                            cursor.getString(0), cursor.getString(1), category
                        ))
                    }
                }
            }
        }
    }

    fun learnedFor(
        description: String,
        memories: List<StatementCategoryMemoryPolicy.Memory>
    ): String? = StatementCategoryMemoryPolicy.learnedFor(description, memories)

    /**
     * Scans EVERY saved PDF, not merely a single chosen source per month.
     * Reports remain deduplicated by their existing account/month policy,
     * so counts are disclosed separately for all PDFs and report-visible PDFs.
     */
    fun matchingStatementPreview(
        source: StoredTransaction, newCategory: String
    ): com.kedevjimmy.pesatrack.domain.everyday.StatementMatchScanPolicy.Result {
        val store = StatementActivityStore(app)
        val batches = store.batches()
        val kinds = categoryKinds()
        val memories = activityMemories()
        val sourceMonths = store.sourceMonths()
        val visibleMonths = sourceMonths.groupBy { it.accountId }.values
            .flatMap { monthSources ->
                StatementHistoryReport.selectForAccount(
                    monthSources, monthSources.first().accountId
                )
            }.groupBy { it.source.batchId }
            .mapValues { (_, entries) -> entries.map { it.key }.toSet() }
        val allRows = buildList<com.kedevjimmy.pesatrack.domain.everyday.StatementMatchScanPolicy.Row> {
            for (batch in batches) {
                val corrections = store.categoriesFor(batch.id)
                val visibleKeys = visibleMonths[batch.id].orEmpty()
                for (row in store.lines(batch.id)) {
                    add(com.kedevjimmy.pesatrack.domain.everyday.StatementMatchScanPolicy.Row(
                        batchId = batch.id,
                        accountId = batch.accountId,
                        entry = row,
                        visibleInReport = StatementMonthlyUsage.monthKey(row.occurredAt) in visibleKeys,
                        manuallyCorrected = (row.page to row.row) in corrections
                    ))
                }
            }
        }
        if (source.counterparty.isNullOrBlank() ||
            (kinds[newCategory] != "EXPENSE" &&
                newCategory !in setOf("INVESTMENT", "TRANSFER"))) {
            return com.kedevjimmy.pesatrack.domain.everyday.StatementMatchScanPolicy.Result(
                allRows.size, batches.size, 0, 0, 0, 0, 0
            )
        }
        return com.kedevjimmy.pesatrack.domain.everyday.StatementMatchScanPolicy.scan(
            allRows, source.counterparty, source.counterpartyPhone,
            newCategory, memories, kinds, store.categoryRules(),
            StatementReceiptEvidenceReader(app).read(allRows.map { it.entry })
        )
    }

    fun categoryKinds(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.kind.name }

    fun categoryLabels(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.label }
}
