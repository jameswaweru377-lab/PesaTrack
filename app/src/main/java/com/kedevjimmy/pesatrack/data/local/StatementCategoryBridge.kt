package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/**
 * Only reads approved categorization preferences from the canonical Activity DB.
 * Activity and the debt ledger are NEVER updated while browsing Statements.
 *
 * Statement-side categorization and its own remembered merchant rules live in
 * StatementActivityStore, so deleting a PDF does not remove any SMS records.
 */
class StatementCategoryBridge(context: Context) {
    private val app = context.applicationContext

    data class CategoryMemory(val name: String?, val phone: String?, val categoryId: String)

    fun activityMemories(): List<CategoryMemory> {
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
                        add(CategoryMemory(cursor.getString(0), cursor.getString(1), category))
                    }
                }
            }
        }
    }

    /**
     * Require the same strict counterparty policy as Activity. A statement
     * without a phone cannot silently match a phone-specific preference.
     */
    fun learnedFor(description: String, rules: List<CategoryMemory>): String? {
        val name = StatementFinancialPolicy.merchantKey(description) ?: return null
        val matches = rules.filter {
            RememberedCategoryMatchPolicy.matches(it.name, it.phone, name, null)
        }.map { it.categoryId }.distinct()
        return matches.singleOrNull()
    }

    fun categoryKinds(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.kind.name }

    fun categoryLabels(): Map<String, String> =
        CategoryStore.get(app).all().associate { it.id to it.label }
}
