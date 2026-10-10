package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context

data class MonthlyBudget(
    val categoryId: String,
    val limitMinor: Long,
    val baseLimitMinor: Long = limitMinor,
    val periodLabel: String = "This month",
    val periodStart: Long = 0
)

class BudgetStore private constructor(private val context: Context) {
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
                    val status = FlexibleBudgetRepository(context).status(cursor.getString(0), cursor.getLong(1))
                    add(
                        MonthlyBudget(
                            categoryId = cursor.getString(0),
                            limitMinor = status.limit,
                            baseLimitMinor = cursor.getLong(1),
                            periodLabel = when (status.options.cadence) {
                                com.kedevjimmy.pesatrack.domain.everyday.BudgetCadence.MONTHLY -> "This month"
                                com.kedevjimmy.pesatrack.domain.everyday.BudgetCadence.WEEKLY -> "This week"
                                else -> "This pay period"
                            },
                            periodStart = status.window.start
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

        val oldLimit = database.readableDatabase.rawQuery("SELECT limit_minor FROM category_budgets WHERE category = ?", arrayOf(category.id)).use { if (it.moveToFirst()) it.getLong(0) else null }
        if (oldLimit != null && oldLimit != limitMinor) {
            database.writableDatabase.update("flexible_budget_options", ContentValues().apply { put("starts_at_epoch_ms", System.currentTimeMillis()) }, "category = ?", arrayOf(category.id))
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
        FlexibleBudgetRepository(context).delete(categoryId)
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
