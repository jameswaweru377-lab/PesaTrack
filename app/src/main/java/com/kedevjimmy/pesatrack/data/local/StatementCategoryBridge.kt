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
                    val category = cursor.getString(2)
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
     * Preview only the statement rows that appear in the account-specific
     * reports: alternative overlapping PDFs are intentionally not counted.
     * This is a read-only scan done on Dispatchers.IO from the existing UI.
     */
    fun matchingStatementCount(source: StoredTransaction, newCategory: String): Int {
        val policy = StatementCategoryMemoryPolicy
        if (source.counterparty.isNullOrBlank()) return 0
        val store = StatementActivityStore(app)
        val memories = activityMemories()
        val kinds = categoryKinds()
        if (kinds[newCategory] != "EXPENSE") return 0
        val localRules = store.categoryRules()
        val selected = store.sourceMonths().groupBy { it.accountId }.values.flatMap { sources ->
            StatementHistoryReport.selectForAccount(sources, sources.first().accountId)
        }
        var count = 0
        selected.groupBy { it.source.batchId }.forEach { (batchId, months) ->
            val keys = months.map { it.key }.toSet()
            val overrides = store.categoriesFor(batchId)
            store.lines(batchId).forEach { row ->
                if (row.withdrawnMinor == 0L ||
                    StatementMonthlyUsage.monthKey(row.occurredAt) !in keys ||
                    !policy.proposedSourceMatches(
                        source.counterparty, source.counterpartyPhone, row.description, memories
                    )) return@forEach
                val key = StatementFinancialPolicy.merchantKey(row.description)
                val currentCategory = learnedFor(row.description, memories)
                    ?: key?.let(localRules::get)
                if (policy.wouldChange(
                    batchId, row, newCategory, currentCategory,
                    row.page to row.row in overrides, kinds
                )) count++
            }
        }
        return count
    }

    fun categoryKinds(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.kind.name }

    fun categoryLabels(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.label }
}
