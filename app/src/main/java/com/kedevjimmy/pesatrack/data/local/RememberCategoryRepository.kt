package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/** Saves consent and applies it across imported history as one canonical transaction. */
class RememberCategoryRepository(context: Context) {
    private val context = context.applicationContext
    private val store = TransactionStore.get(this.context)
    private val learning = PersonalLearningStore.get(this.context)

    fun matchingCount(source: StoredTransaction): Int = synchronized(CanonicalStoreLock.monitor) {
        store.categoryLearningCandidates().count {
            it.id != source.id && RememberedCategoryMatchPolicy.matches(
                source.counterparty, source.counterpartyPhone, it.counterparty, it.counterpartyPhone)
        }
    }

    fun remember(transactionId: Long, categoryId: String): Int = synchronized(CanonicalStoreLock.monitor) {
        val source = requireNotNull(store.getById(transactionId)) { "This transaction no longer exists." }
        require(store.categoryLearningCandidates().any { it.id == source.id }) {
            "This entry has changed. Open it again before remembering a category."
        }
        val direction = CategoryStore.get(context).directionFor(categoryId, MoneyDirection.EXPENSE)
        require(PersonalLearningPolicy.isLearnableCategory(categoryId, direction))
        val db = PesaTrackDatabase.get(context).writableDatabase
        var changed = 0
        db.beginTransaction()
        try {
            requireNotNull(learning.remember(source, categoryId, direction)) {
                "This identity cannot be remembered safely. Choose Only this one instead."
            }
            // Keep the original name/phone on the source row for future exact matching.
            store.classifyManually(source.id, categoryId, direction)
            val matching = store.categoryLearningCandidates().filter {
                RememberedCategoryMatchPolicy.matches(source.counterparty, source.counterpartyPhone,
                    it.counterparty, it.counterpartyPhone)
            }
            changed = learning.applyHighConfidenceTo(matching, rebuildPayCycles = false)
            PayCycleStore.get(context).rebuild()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        LedgerUpdateBus.notifyChanged()
        changed
    }
}
