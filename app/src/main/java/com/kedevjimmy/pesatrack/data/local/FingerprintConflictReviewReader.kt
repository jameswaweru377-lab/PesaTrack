package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import java.util.Calendar

/** Non-posting evidence for the existing Statements review screen. */
internal data class FingerprintConflictReviewItem(
    val existingTransactionId: Long,
    val transactionCode: String,
    val originalAmountMinor: Long,
    val incomingAmountMinor: Long,
    val sourceKind: String,
    val conflictId: Long = 0
)

internal class FingerprintConflictReviewReader(context: Context) {
    private val app = context.applicationContext
    data class Page(val items: List<FingerprintConflictReviewItem>, val nextBeforeId: Long?)

    /** Compatibility helper for existing readers/tests; UI uses bounded readPage. */
    fun read(year: Int): List<FingerprintConflictReviewItem> {
        val result = mutableListOf<FingerprintConflictReviewItem>()
        var cursor: Long? = Long.MAX_VALUE
        while (cursor != null) {
            val page = readPage(year, cursor)
            result.addAll(page.items)
            cursor = page.nextBeforeId
        }
        return result
    }

    fun readPage(year: Int, beforeId: Long = Long.MAX_VALUE, limit: Int = 50): Page {
        require(year in 1970..2100)
        require(beforeId > 0 && limit in 1..100)
        val zone = java.util.TimeZone.getTimeZone("Africa/Nairobi")
        val start = Calendar.getInstance(zone).apply {
            clear(); set(year, Calendar.JANUARY, 1)
        }.timeInMillis
        val end = Calendar.getInstance(zone).apply {
            clear(); set(year + 1, Calendar.JANUARY, 1)
        }.timeInMillis
        val conflicts = FingerprintConflictStore.listPending(
            PesaTrackDatabase.get(app).readableDatabase, start, end, beforeId, limit + 1
        )
        if (conflicts.isEmpty()) return Page(emptyList(), null)
        val visible = conflicts.take(limit)
        val store = TransactionStore.get(app)
        val originals = visible.map { it.existingTransactionId }.distinct()
            .chunked(50).flatMap { ids -> store.getByIds(ids).values }
            .associateBy { it.id }
        val items = visible.mapNotNull { conflict ->
            originals[conflict.existingTransactionId]?.let { original ->
                FingerprintConflictReviewItem(
                    conflictId = conflict.id,
                    existingTransactionId = original.id,
                    transactionCode = conflict.transactionCode,
                    originalAmountMinor = original.amountMinor,
                    incomingAmountMinor = conflict.amountMinor,
                    sourceKind = conflict.sourceKind
                )
            }
        }
        return Page(items, if (conflicts.size > limit) visible.last().id else null)
    }
}
