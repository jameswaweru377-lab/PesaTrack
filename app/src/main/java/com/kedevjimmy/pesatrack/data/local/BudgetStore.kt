package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context

data class MonthlyBudget(
    val categoryId: String,
    val limitMinor: Long
)

class BudgetStore private constructor(context: Context) {
    private val database = PesaTrackDatabase.get(context.applicationContext)

    fun list(): List<MonthlyBudget> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            """
            SELECT category, limit_minor
            FROM category_budgets
            ORDER BY updated_at_epoch_ms DESC, category COLLATE NOCASE
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MonthlyBudget(
                            categoryId = cursor.getString(0),
                            limitMinor = cursor.getLong(1)
                        )
                    )
                }
            }
        }
    }

    fun save(
        categoryId: String,
        limitMinor: Long
    ) {
        synchronized(CanonicalStoreLock.monitor) {
        require(limitMinor > 0L) {
            "Budget amount must be greater than zero."
        }

        val category = CategoryStore.get(appContext).find(categoryId)
            ?: error("Unknown category.")
        require(category.budgetable) {
            "This category cannot have a monthly budget."
        }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("category", category.id)
            put("limit_minor", limitMinor)
            put("updated_at_epoch_ms", now)
            put("created_at_epoch_ms", now)
        }

        database.writableDatabase.insertWithOnConflict(
            "category_budgets",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        LedgerUpdateBus.notifyChanged()
    }
    }

    fun delete(categoryId: String) {
        synchronized(CanonicalStoreLock.monitor) {
        database.writableDatabase.delete(
            "category_budgets",
            "category = ?",
            arrayOf(categoryId)
        )
        LedgerUpdateBus.notifyChanged()
    }
    }

    private val appContext: Context = context.applicationContext

    companion object {
        @Volatile
        private var instance: BudgetStore? = null

        fun get(context: Context): BudgetStore =
            instance ?: synchronized(this) {
                instance ?: BudgetStore(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
