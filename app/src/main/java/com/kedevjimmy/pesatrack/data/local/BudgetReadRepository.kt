package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/** Category spending derived only from the ledger's canonical eligible rows. */
object BudgetSpendingProjection {
    fun calculate(
        trustedRows: List<StoredTransaction>,
        trustedOriginal: (Long) -> StoredTransaction?,
        canonicalCategoryId: (String) -> String
    ): Map<String, Long> {
        val totals = linkedMapOf<String, Long>()
        fun add(category: String, amount: Long) {
            val id = canonicalCategoryId(category)
            totals[id] = Math.addExact(totals[id] ?: 0L, amount)
        }
        trustedRows.forEach { row ->
            if (row.relationType == TransactionStore.RELATION_REFUND) {
                val original = row.relatedTransactionId?.let(trustedOriginal)
                if (original != null) add(original.category, Math.negateExact(row.amountMinor))
            } else if (row.direction == MoneyDirection.EXPENSE.name) {
                add(row.category, row.amountMinor)
            }
            // Transfers do not consume a budget's principal, but their fees do.
            if (row.feeMinor != 0L) add(row.category, row.feeMinor)
        }
        return totals.mapValues { (_, amount) -> amount.coerceAtLeast(0L) }
    }
}

class BudgetReadRepository(context: Context) {
    private val transactions = TransactionStore.get(context.applicationContext)
    private val categories = CategoryStore.get(context.applicationContext)

    fun spendingByCategory(): Map<String, Long> = synchronized(CanonicalStoreLock.monitor) {
        BudgetSpendingProjection.calculate(
            trustedRows = transactions.activitySnapshot().trustedTransactions,
            trustedOriginal = transactions::trustedRefundOriginal,
            canonicalCategoryId = categories::canonicalId
        )
    }
}
