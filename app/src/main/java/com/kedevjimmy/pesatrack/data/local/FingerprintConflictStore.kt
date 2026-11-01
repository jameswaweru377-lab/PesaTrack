package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.ingestion.FingerprintConflictPolicy
import com.kedevjimmy.pesatrack.domain.ingestion.FingerprintFacts
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import java.security.MessageDigest

/** Sidecar evidence only: these rows never participate in the canonical money ledger. */
internal object FingerprintConflictStore {
    fun capture(
        db: SQLiteDatabase,
        stored: StoredTransaction,
        parsed: ParsedMpesaTransaction,
        occurredAtEpochMs: Long,
        sourceKind: String
    ): Boolean {
        val original = FingerprintFacts(
            stored.transactionCode, stored.type, stored.amountMinor,
            stored.feeMinor, stored.occurredAtEpochMs
        )
        val incoming = FingerprintFacts(
            parsed.transactionCode, parsed.type.name, parsed.amountMinor,
            parsed.feeMinor, occurredAtEpochMs
        )
        if (!FingerprintConflictPolicy.isConflicting(original, incoming)) return false
        recordIfConflicting(db, stored.id, original, incoming, sourceKind)
        return true
    }

    fun recordIfConflicting(
        db: SQLiteDatabase,
        existingTransactionId: Long,
        stored: FingerprintFacts,
        incoming: FingerprintFacts,
        sourceKind: String
    ): Boolean {
        if (!FingerprintConflictPolicy.isConflicting(stored, incoming)) return false
        val digestInput = listOf(
            existingTransactionId, incoming.code?.trim()?.uppercase(), incoming.type,
            incoming.amountMinor, incoming.feeMinor, incoming.occurredAtEpochMs, sourceKind
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val values = ContentValues().apply {
            put("existing_transaction_id", existingTransactionId)
            put("evidence_digest", digest)
            put("source_kind", sourceKind)
            put("transaction_code", incoming.code?.trim()?.uppercase())
            put("type", incoming.type)
            put("amount_minor", incoming.amountMinor)
            put("fee_minor", incoming.feeMinor)
            put("occurred_at_epoch_ms", incoming.occurredAtEpochMs)
            put("review_state", "PENDING")
        }
        return db.insertWithOnConflict(
            "fingerprint_conflicts", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }
}
