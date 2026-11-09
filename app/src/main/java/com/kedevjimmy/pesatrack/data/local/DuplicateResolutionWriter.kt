package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicateObservation
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicatePolicy

/**
 * The only write boundary for possible-duplicate review decisions.
 * Keep weak evidence review-only until the user's current card is verified.
 */
internal class DuplicateResolutionWriter(
    context: Context,
    private val database: PesaTrackDatabase,
    private val loadTransaction: (Long) -> StoredTransaction?
) {
    private val appContext = context.applicationContext

    fun resolve(
        pairKey: String,
        sameTransaction: Boolean,
        expectedFirst: StoredTransaction?,
        expectedSecond: StoredTransaction?
    ): Boolean = synchronized(CanonicalStoreLock.monitor) {
        val ids = PossibleDuplicatePolicy.parsePairIds(pairKey)
            ?: return@synchronized false
        if (listOf(ids.first, ids.second).any(
                OwnedTransferReviewStore(appContext)::isConfirmedPair
            )) return@synchronized false

        val db = database.writableDatabase
        var applied = false
        db.beginTransaction()
        try {
            val first = loadTransaction(ids.first) ?: return@synchronized false
            val second = loadTransaction(ids.second) ?: return@synchronized false

            if (!matchesCurrentReview(
                    db, pairKey, sameTransaction,
                    first, second, expectedFirst, expectedSecond
                )) return@synchronized false
            if (hasExistingDecision(db, ids.first, ids.second)) {
                return@synchronized false
            }

            if (sameTransaction) {
                val canonical = chooseDuplicateCanonical(first, second)
                val duplicate = if (canonical.id == first.id) second else first
                check(db.update("transactions", ContentValues().apply {
                    put("review_state", ReviewState.EXCLUDED.name)
                    putNull("cycle_candidate_reason")
                }, "id = ?", arrayOf(duplicate.id.toString())) == 1) {
                    "Duplicate source row changed during confirmation."
                }
            }
            val now = System.currentTimeMillis()
            val canonicalKey = minOf(ids.first, ids.second).toString() +
                ":" + maxOf(ids.first, ids.second)
            check(db.insertWithOnConflict(
                "duplicate_resolutions", null, ContentValues().apply {
                    put("pair_key", canonicalKey)
                    put("first_transaction_id", ids.first)
                    put("second_transaction_id", ids.second)
                    put("decision", if (sameTransaction) "SAME_TRANSACTION" else "KEEP_BOTH")
                    put("created_at_epoch_ms", now)
                    put("updated_at_epoch_ms", now)
                }, SQLiteDatabase.CONFLICT_ABORT
            ) != -1L) { "Could not persist duplicate decision." }

            db.setTransactionSuccessful()
            applied = true
        } finally {
            db.endTransaction()
        }

        if (applied) {
            try { PayCycleStore.get(appContext).rebuild() }
            finally { LedgerUpdateBus.notifyChanged() }
        }
        applied
    }

    private fun matchesCurrentReview(
        db: SQLiteDatabase,
        pairKey: String,
        sameTransaction: Boolean,
        first: StoredTransaction,
        second: StoredTransaction,
        shownFirst: StoredTransaction?,
        shownSecond: StoredTransaction?
    ): Boolean {
        // The card is time-sorted; the persisted pair key sorts numeric IDs.
        // Compare each complete row by identity, not by its displayed position.
        if (shownFirst != null || shownSecond != null) {
            val matches = (first == shownFirst && second == shownSecond) ||
                (first == shownSecond && second == shownFirst)
            if (!matches) return false
        }
        if (!sameTransaction) return true

        val expectedFirst = shownFirst ?: return false
        val expectedSecond = shownSecond ?: return false
        if (first.reversalOfCode != null || second.reversalOfCode != null ||
            hasIncomingReversal(db, first, second) ||
            DuplicateCashRoleSafety.hasDependents(db, first.id, second.id)) return false
        return PossibleDuplicatePolicy.canConfirm(
            pairKey,
            expectedFirst.toPossibleDuplicateObservation(),
            expectedSecond.toPossibleDuplicateObservation(),
            first.toPossibleDuplicateObservation(),
            second.toPossibleDuplicateObservation()
        )
    }

    /** Reversal evidence can arrive as a third row without editing the card's rows. */
    private fun hasIncomingReversal(
        db: SQLiteDatabase,
        first: StoredTransaction,
        second: StoredTransaction
    ): Boolean {
        val codes = listOfNotNull(first.transactionCode, second.transactionCode)
            .filter(String::isNotBlank).distinct()
        if (codes.isEmpty()) return false
        val marks = List(codes.size) { "?" }.joinToString(",")
        return db.rawQuery(
            "SELECT 1 FROM transactions WHERE reversal_of_code IN ($marks) LIMIT 1",
            codes.toTypedArray()
        ).use { it.moveToFirst() }
    }

    private fun hasExistingDecision(db: SQLiteDatabase, first: Long, second: Long): Boolean =
        db.rawQuery(
            """SELECT 1 FROM duplicate_resolutions
               WHERE (first_transaction_id=? AND second_transaction_id=?)
                  OR (first_transaction_id=? AND second_transaction_id=?)
               LIMIT 1""",
            arrayOf(first.toString(), second.toString(), second.toString(), first.toString())
        ).use { it.moveToFirst() }

    private fun chooseDuplicateCanonical(
        first: StoredTransaction,
        second: StoredTransaction
    ): StoredTransaction {
        fun score(item: StoredTransaction): Int {
            var value = 0
            if (!item.transactionCode.isNullOrBlank()) value += 8
            if (!item.counterparty.isNullOrBlank()) value += 4
            if (!item.counterpartyPhone.isNullOrBlank()) value += 3
            if (!item.evidenceLabel.isNullOrBlank()) value += 2
            if (item.type != MpesaTransactionType.OTHER.name) value += 2
            if (item.direction != MoneyDirection.NEUTRAL.name) value += 1
            return value
        }
        val firstScore = score(first)
        val secondScore = score(second)
        return when {
            firstScore > secondScore -> first
            secondScore > firstScore -> second
            else -> if (first.id <= second.id) first else second
        }
    }
}

/** A cash leg already used by another financial workflow must never be excluded
 * merely because two messages share an amount and timestamp. All checks are
 * read-only and run inside the duplicate writer's existing SQLite transaction.
 */
internal object DuplicateCashRoleSafety {
    fun hasDependents(db: SQLiteDatabase, firstId: Long, secondId: Long): Boolean {
        require(firstId > 0L && secondId > 0L && firstId != secondId)
        val ids = arrayOf(firstId.toString(), secondId.toString())
        val sql = """SELECT 1 WHERE
            EXISTS(SELECT 1 FROM debt_events WHERE linked_transaction_id IN (?,?)) OR
            EXISTS(SELECT 1 FROM investment_events WHERE linked_transaction_id IN (?,?)) OR
            EXISTS(SELECT 1 FROM personal_receivable_events WHERE linked_transaction_id IN (?,?)) OR
            EXISTS(SELECT 1 FROM transaction_allocations WHERE transaction_id IN (?,?)) OR
            EXISTS(SELECT 1 FROM payday_snapshots WHERE salary_transaction_id IN (?,?)) OR
            EXISTS(SELECT 1 FROM transactions WHERE related_transaction_id IN (?,?))
            LIMIT 1""".trimIndent()
        return db.rawQuery(sql, Array(6) { ids }.flatMap { it.toList() }.toTypedArray())
            .use { it.moveToFirst() }
    }
}

internal fun StoredTransaction.toPossibleDuplicateObservation(): PossibleDuplicateObservation =
    PossibleDuplicateObservation(
        id = id,
        accountId = accountId,
        amountMinor = amountMinor,
        receivedAtEpochMs = receivedAtEpochMs,
        transactionCode = transactionCode,
        sourcePackage = sourcePackage,
        sourceKind = sourceKind,
        direction = direction,
        reviewState = reviewState,
        category = category,
        hasProtectedCashRole = linkedAccountId != null || relationType != null ||
            relatedTransactionId != null || reversalOfCode != null || feeMinor != 0L ||
            fulizaFinancedMinor != 0L || fulizaAccessFeeMinor != 0L ||
            fulizaOutstandingMinor != null || fulizaEventCode != null,
        counterparty = counterparty,
        evidenceLabel = evidenceLabel,
        type = type
    )
