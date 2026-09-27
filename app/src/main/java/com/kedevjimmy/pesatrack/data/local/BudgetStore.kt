package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

data class MonthlyBudget(
    val category: TransactionCategory,
    val limitMinor: Long
)

class BudgetStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context.applicationContext)

    @Synchronized
    fun list(): List<MonthlyBudget> =
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
                    val category = runCatching {
                        TransactionCategory.valueOf(cursor.getString(0))
                    }.getOrNull() ?: continue
                    add(
                        MonthlyBudget(
                            category = category,
                            limitMinor = cursor.getLong(1)
                        )
                    )
                }
            }
        }

    @Synchronized
    fun save(
        category: TransactionCategory,
        limitMinor: Long
    ) {
        require(limitMinor > 0L) {
            "Budget amount must be greater than zero."
        }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("category", category.name)
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

    @Synchronized
    fun delete(category: TransactionCategory) {
        database.writableDatabase.delete(
            "category_budgets",
            "category = ?",
            arrayOf(category.name)
        )
        LedgerUpdateBus.notifyChanged()
    }

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
