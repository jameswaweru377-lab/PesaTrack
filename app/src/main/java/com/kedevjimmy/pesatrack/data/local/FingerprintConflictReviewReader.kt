package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import java.util.Calendar

/** Non-posting evidence for the existing Statements review screen. */
internal data class FingerprintConflictReviewItem(
    val existingTransactionId: Long,
    val transactionCode: String,
    val originalAmountMinor: Long,
    val incomingAmountMinor: Long,
    val sourceKind: String
)

internal class FingerprintConflictReviewReader(context: Context) {
    private val app = context.applicationContext

    fun read(year: Int): List<FingerprintConflictReviewItem> {
        val conflicts = FingerprintConflictStore.listPending(PesaTrackDatabase.get(app).readableDatabase)
            .filter { row ->
                Calendar.getInstance().apply { timeInMillis = row.occurredAtEpochMs }
                    .get(Calendar.YEAR) == year
            }
        if (conflicts.isEmpty()) return emptyList()
        val store = TransactionStore.get(app)
        val originals = conflicts.map { it.existingTransactionId }.distinct()
            .chunked(50).flatMap { ids -> store.getByIds(ids).values }
            .associateBy { it.id }
        return conflicts.mapNotNull { conflict ->
            originals[conflict.existingTransactionId]?.let { original ->
                FingerprintConflictReviewItem(
                    existingTransactionId = original.id,
                    transactionCode = conflict.transactionCode,
                    originalAmountMinor = original.amountMinor,
                    incomingAmountMinor = conflict.amountMinor,
                    sourceKind = conflict.sourceKind
                )
            }
        }
    }
}
