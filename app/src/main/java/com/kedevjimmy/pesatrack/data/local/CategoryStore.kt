package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.UUID

enum class CategoryKind {
    EXPENSE,
    ALLOCATION,
    INCOME,
    TRANSFER,
    SYSTEM
}

data class PesaCategory(
    val id: String,
    val label: String,
    val kind: CategoryKind,
    val builtIn: Boolean,
    val budgetable: Boolean,
    val sortOrder: Int
)

class CategoryStore private constructor(context: Context) {
    private val database = PesaTrackDatabase.get(context.applicationContext)

    fun all(): List<PesaCategory> =
        (BUILT_INS + customCategories())
            .sortedWith(
                compareBy<PesaCategory> { it.sortOrder }
                    .thenBy { it.label.lowercase() }
            )

    fun activityCategories(): List<PesaCategory> =
        all().filter {
            it.kind == CategoryKind.EXPENSE ||
                it.kind == CategoryKind.ALLOCATION ||
                it.kind == CategoryKind.INCOME
        }

    fun budgetCategories(): List<PesaCategory> =
        all().filter(PesaCategory::budgetable)

    fun find(id: String): PesaCategory? =
        all().firstOrNull { it.id == canonicalId(id) }

    fun labelFor(id: String): String =
        find(id)?.label ?: humanize(id)

    fun directionFor(
        categoryId: String,
        existingDirection: MoneyDirection = MoneyDirection.NEUTRAL
    ): MoneyDirection =
        when (find(categoryId)?.kind) {
            CategoryKind.INCOME -> MoneyDirection.INCOME
            CategoryKind.ALLOCATION,
            CategoryKind.TRANSFER -> MoneyDirection.TRANSFER
            CategoryKind.EXPENSE -> MoneyDirection.EXPENSE
            CategoryKind.SYSTEM, null ->
                if (existingDirection == MoneyDirection.INCOME) {
                    MoneyDirection.INCOME
                } else {
                    MoneyDirection.EXPENSE
                }
        }

    @Synchronized
    fun addCustom(label: String): PesaCategory {
        val clean = label.trim().replace(Regex("""\s+"""), " ")
        require(clean.length in 2..32) {
            "Category name must be 2 to 32 characters."
        }

        all().firstOrNull {
            it.label.equals(clean, ignoreCase = true)
        }?.let { return it }

        val id = "CUSTOM_" + UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(12)
            .uppercase()
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", id)
            put("label", clean)
            put("kind", CategoryKind.EXPENSE.name)
            put("created_at_epoch_ms", now)
            put("updated_at_epoch_ms", now)
        }
        database.writableDatabase.insertOrThrow(
            "custom_categories",
            null,
            values
        )
        LedgerUpdateBus.notifyChanged()
        return PesaCategory(
            id = id,
            label = clean,
            kind = CategoryKind.EXPENSE,
            builtIn = false,
            budgetable = true,
            sortOrder = 900
        )
    }

    fun canonicalId(id: String): String =
        when (id) {
            "LUNCH" -> TransactionCategory.EATING_OUT.name
            else -> id
        }

    private fun customCategories(): List<PesaCategory> =
        database.readableDatabase.rawQuery(
            """
            SELECT id, label, kind
            FROM custom_categories
            ORDER BY created_at_epoch_ms ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                var index = 0
                while (cursor.moveToNext()) {
                    val kind = runCatching {
                        CategoryKind.valueOf(cursor.getString(2))
                    }.getOrDefault(CategoryKind.EXPENSE)
                    add(
                        PesaCategory(
                            id = cursor.getString(0),
                            label = cursor.getString(1),
                            kind = kind,
                            builtIn = false,
                            budgetable =
                                kind == CategoryKind.EXPENSE ||
                                    kind == CategoryKind.ALLOCATION,
                            sortOrder = 900 + index++
                        )
                    )
                }
            }
        }

    companion object {
        private val BUILT_INS = listOf(
            PesaCategory(TransactionCategory.GROCERIES.name, "Groceries", CategoryKind.EXPENSE, true, true, 10),
            PesaCategory(TransactionCategory.EATING_OUT.name, "Eating out", CategoryKind.EXPENSE, true, true, 20),
            PesaCategory(TransactionCategory.TRANSPORT.name, "Transport", CategoryKind.EXPENSE, true, true, 30),
            PesaCategory(TransactionCategory.RENT.name, "Rent", CategoryKind.EXPENSE, true, true, 40),
            PesaCategory(TransactionCategory.ELECTRICITY.name, "Electricity", CategoryKind.EXPENSE, true, true, 50),
            PesaCategory(TransactionCategory.WATER.name, "Water", CategoryKind.EXPENSE, true, true, 60),
            PesaCategory(TransactionCategory.INTERNET.name, "Internet", CategoryKind.EXPENSE, true, true, 70),
            PesaCategory(TransactionCategory.PHONE_AND_DATA.name, "Phone & data", CategoryKind.EXPENSE, true, true, 80),
            PesaCategory(TransactionCategory.HOME_HOUSEHOLD.name, "Home & household", CategoryKind.EXPENSE, true, true, 90),
            PesaCategory(TransactionCategory.SHOPPING.name, "Shopping", CategoryKind.EXPENSE, true, true, 100),
            PesaCategory(TransactionCategory.HEALTH.name, "Health", CategoryKind.EXPENSE, true, true, 110),
            PesaCategory(TransactionCategory.EDUCATION.name, "Education", CategoryKind.EXPENSE, true, true, 120),
            PesaCategory(TransactionCategory.ENTERTAINMENT.name, "Entertainment", CategoryKind.EXPENSE, true, true, 130),
            PesaCategory(TransactionCategory.PERSONAL_CARE.name, "Personal care", CategoryKind.EXPENSE, true, true, 140),
            PesaCategory(TransactionCategory.FAMILY.name, "Family support", CategoryKind.EXPENSE, true, true, 150),
            PesaCategory(TransactionCategory.FEES.name, "Fees", CategoryKind.EXPENSE, true, true, 160),
            PesaCategory(TransactionCategory.DEBT_LOAN.name, "Debt repayment", CategoryKind.ALLOCATION, true, true, 170),
            PesaCategory(TransactionCategory.OTHER.name, "Other", CategoryKind.EXPENSE, true, true, 180),
            PesaCategory(TransactionCategory.SAVINGS.name, "Savings", CategoryKind.ALLOCATION, true, true, 200),
            PesaCategory(TransactionCategory.INVESTMENT.name, "Investments", CategoryKind.ALLOCATION, true, true, 210),
            PesaCategory(TransactionCategory.INCOME.name, "Income", CategoryKind.INCOME, true, false, 300),
            PesaCategory(TransactionCategory.TRANSFER.name, "Transfer", CategoryKind.TRANSFER, true, false, 400),
            PesaCategory(TransactionCategory.CASH.name, "Cash", CategoryKind.TRANSFER, true, false, 410),
            PesaCategory(TransactionCategory.UNCATEGORIZED.name, "Uncategorized", CategoryKind.SYSTEM, true, false, 999)
        )

        @Volatile
        private var instance: CategoryStore? = null

        fun get(context: Context): CategoryStore =
            instance ?: synchronized(this) {
                instance ?: CategoryStore(context.applicationContext).also {
                    instance = it
                }
            }

        fun builtIn(id: String): PesaCategory? =
            BUILT_INS.firstOrNull {
                it.id == if (id == "LUNCH") {
                    TransactionCategory.EATING_OUT.name
                } else {
                    id
                }
            }

        private fun humanize(value: String): String =
            value.lowercase()
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
    }
}
