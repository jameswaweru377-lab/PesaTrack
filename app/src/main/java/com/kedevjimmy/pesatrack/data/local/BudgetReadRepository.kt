package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/** Category spending derived only from the ledger's canonical eligible rows. */
object BudgetSpendingProjection {
    fun calculate(
        trustedRows: List<StoredTransaction>,
        trustedOriginal: (Long) -> StoredTransaction?,
        canonicalCategoryId: (String) -> String,
        allocations: (Long) -> List<com.kedevjimmy.pesatrack.domain.everyday.MoneyPart> = { emptyList() }
    ): Map<String, Long> {
        val totals = linkedMapOf<String, Long>()
        fun add(category: String, amount: Long) {
            val id = canonicalCategoryId(category)
            totals[id] = Math.addExact(totals[id] ?: 0L, amount)
        }
        trustedRows.forEach { row ->
            if (row.relationType == TransactionStore.RELATION_REFUND) {
                val original = row.relatedTransactionId?.let(trustedOriginal)
                if (original != null) {
                    val parts = allocations(original.id)
                    if (parts.isEmpty()) add(original.category, Math.negateExact(row.amountMinor))
                    else com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy.apportion(row.amountMinor, parts).forEach { add(it.category, Math.negateExact(it.minor)) }
                }
            } else if (row.direction == MoneyDirection.EXPENSE.name) {
                val parts = allocations(row.id)
                if (parts.isEmpty()) add(row.category, row.amountMinor) else parts.forEach { add(it.category, it.minor) }
            }
            // Transfers do not consume a budget's principal, but their fees do.
            if (row.feeMinor != 0L) add(row.category, row.feeMinor)
        }
        return totals.mapValues { (_, amount) -> amount.coerceAtLeast(0L) }
    }
}

class BudgetReadRepository(private val context: Context) {
    private val transactions = TransactionStore.get(context.applicationContext)
    private val categories = CategoryStore.get(context.applicationContext)

    fun spendingByCategory(): Map<String, Long> = synchronized(CanonicalStoreLock.monitor) {
        val start = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH,1); set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0) }
        val end = (start.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH,1) }
        val allocations = EverydayMoneyStore(context).allAllocations()
        val out = BudgetSpendingProjection.calculate(
            trustedRows = transactions.activitySnapshotBetween(start.timeInMillis, end.timeInMillis, System.currentTimeMillis(), performMaintenance = false).trustedTransactions,
            trustedOriginal = transactions::trustedRefundOriginal,
            canonicalCategoryId = categories::canonicalId,
            allocations = { allocations[it].orEmpty() }
        ).toMutableMap()
        val repository = FlexibleBudgetRepository(context)
        val db = PesaTrackDatabase.get(context).readableDatabase
        db.rawQuery("SELECT category,limit_minor FROM category_budgets", null).use { c ->
            while (c.moveToNext()) out[c.getString(0)] = repository.status(c.getString(0), c.getLong(1)).spent
        }
        out
    }
}
