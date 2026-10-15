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

    data class MatchingPreview(val activity: Int, val statements: Int) {
        val total: Int get() = activity + statements
    }

    /** Preview Activity and independent report rows before saving consent. */
    fun matchingPreview(source: StoredTransaction, categoryId: String): MatchingPreview =
        synchronized(CanonicalStoreLock.monitor) {
            val activity = matchingCount(source)
            val statements = StatementCategoryBridge(context)
                .matchingStatementCount(source, categoryId)
            MatchingPreview(activity, statements)
        }

    fun matchingCount(source: StoredTransaction): Int = synchronized(CanonicalStoreLock.monitor) {
        store.categoryLearningCandidates().count {
            it.id != source.id && RememberedCategoryMatchPolicy.matches(
                source.counterparty, source.counterpartyPhone, it.counterparty, it.counterpartyPhone)
        }
    }

    fun remember(transactionId: Long, categoryId: String): Int = synchronized(CanonicalStoreLock.monitor) {
        val source = requireNotNull(store.getById(transactionId)) { "This transaction no longer exists." }
        val eligibleUnreviewed = store.categoryLearningCandidates().any { it.id == source.id }
        // "Only this one" is a valid prior choice. Later explicit consent in
        // the Classification card must be able to teach the existing category
        // without rewriting unrelated Activity history or inventing a new type.
        val eligibleManual = source.reviewState == "USER_CLASSIFIED" &&
            source.direction == MoneyDirection.EXPENSE.name &&
            source.category == categoryId &&
            source.ruleId == null && source.relationType == null &&
            source.type != "REVERSAL" && source.reversalOfCode == null &&
            !PesaTrackDatabase.get(context).readableDatabase.rawQuery(
                "SELECT 1 FROM debt_events WHERE linked_transaction_id=? " +
                    "AND event_type='REPAYMENT' LIMIT 1",
                arrayOf(source.id.toString())
            ).use { it.moveToFirst() }
        require(eligibleUnreviewed || eligibleManual) {
            "This entry cannot be safely remembered. Review its financial type."
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
