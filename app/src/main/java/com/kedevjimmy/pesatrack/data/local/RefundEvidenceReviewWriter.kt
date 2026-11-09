package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.RefundReviewReason

/** Preserve the incoming credit while withholding an unproven economic category. */
internal object RefundEvidenceReviewWriter {
    fun markForReview(db: SQLiteDatabase, id: Long, reason: RefundReviewReason): Boolean {
        val label = when (reason) {
            RefundReviewReason.PARTIAL -> "Possible partial refund — check original payment"
            RefundReviewReason.AMBIGUOUS -> "Possible refund — multiple matching payments"
        }
        val values = ContentValues().apply {
            put("review_state", ReviewState.NEEDS_REVIEW.name)
            put("evidence_label", label)
            put("evidence_source", "refund_review_required")
            put("evidence_confidence", "LOW")
            putNull("evidence_time_delta_ms")
        }
        return db.update("transactions", values, "id = ? AND relation_type IS NULL",
            arrayOf(id.toString())) > 0
    }
}
