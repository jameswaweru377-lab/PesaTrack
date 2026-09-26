package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.parser.ParsedFulizaEvent
import com.kedevjimmy.pesatrack.domain.rules.FulizaPairingPolicy
import com.kedevjimmy.pesatrack.domain.rules.FulizaPaymentCandidate
import java.security.MessageDigest
import kotlin.math.abs

data class FulizaStoreResult(
    val inserted: Boolean,
    val linkedTransactionId: Long?
)

class FulizaStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context.applicationContext)

    @Synchronized
    fun insertOrUpdate(
        parsed: ParsedFulizaEvent,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): FulizaStoreResult {
        val db = database.writableDatabase
        val key = eventKey(parsed)
        val existing = db.rawQuery(
            """
            SELECT id, linked_transaction_id
            FROM fuliza_events
            WHERE event_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(key)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0) to if (cursor.isNull(1)) null else cursor.getLong(1)
            } else {
                null
            }
        }

        val values = ContentValues().apply {
            put("event_key", key)
            if (parsed.transactionCode != null) put("transaction_code", parsed.transactionCode) else putNull("transaction_code")
            put("financed_amount_minor", parsed.financedAmountMinor)
            put("access_fee_minor", parsed.accessFeeMinor)
            if (parsed.outstandingAmountMinor != null) put("outstanding_amount_minor", parsed.outstandingAmountMinor) else putNull("outstanding_amount_minor")
            if (parsed.availableLimitMinor != null) put("available_limit_minor", parsed.availableLimitMinor) else putNull("available_limit_minor")
            put("occurred_at_epoch_ms", occurredAtEpochMs)
            if (sourcePackage != null) put("source_package", sourcePackage) else putNull("source_package")
            put("source_kind", sourceKind)
            put("created_at_epoch_ms", System.currentTimeMillis())
            if (existing?.second != null) put("linked_transaction_id", existing.second) else putNull("linked_transaction_id")
        }

        val eventId: Long
        val inserted: Boolean
        if (existing == null) {
            eventId = db.insertOrThrow("fuliza_events", null, values)
            inserted = true
        } else {
            eventId = existing.first
            db.update(
                "fuliza_events",
                values,
                "id = ?",
                arrayOf(eventId.toString())
            )
            inserted = false
        }

        parsed.transactionCode?.let { code ->
            // Older builds could ingest a Fuliza confirmation as a generic OTHER
            // transaction. Re-importing the SMS safely removes that duplicate row.
            db.delete(
                "transactions",
                "transaction_code = ? AND type = ?",
                arrayOf(code, MpesaTransactionType.OTHER.name)
            )
        }

        val linked = existing?.second ?: findAndAttachCandidate(
            eventId = eventId,
            financedAmountMinor = parsed.financedAmountMinor,
            occurredAtEpochMs = occurredAtEpochMs
        )

        return FulizaStoreResult(
            inserted = inserted,
            linkedTransactionId = linked
        )
    }

    @Synchronized
    fun linkPendingForTransaction(
        transactionId: Long,
        parsed: ParsedMpesaTransaction,
        occurredAtEpochMs: Long
    ) {
        if (!parsed.type.canUseFuliza()) return

        val pending = database.readableDatabase.rawQuery(
            """
            SELECT id, financed_amount_minor, occurred_at_epoch_ms
            FROM fuliza_events
            WHERE linked_transaction_id IS NULL
              AND occurred_at_epoch_ms BETWEEN ? AND ?
            ORDER BY ABS(occurred_at_epoch_ms - ?) ASC
            """.trimIndent(),
            arrayOf(
                (occurredAtEpochMs - PAIRING_WINDOW_MS).toString(),
                (occurredAtEpochMs + PAIRING_WINDOW_MS).toString(),
                occurredAtEpochMs.toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PendingFuliza(
                            id = cursor.getLong(0),
                            financedAmountMinor = cursor.getLong(1),
                            occurredAtEpochMs = cursor.getLong(2)
                        )
                    )
                }
            }
        }

        val candidate = FulizaPaymentCandidate(
            transactionId = transactionId,
            occurredAtEpochMs = occurredAtEpochMs,
            amountMinor = parsed.amountMinor,
            mpesaFeeMinor = parsed.feeMinor
        )

        val match = pending.firstOrNull { event ->
            FulizaPairingPolicy.chooseCandidate(
                fulizaFinancedMinor = event.financedAmountMinor,
                fulizaOccurredAtEpochMs = event.occurredAtEpochMs,
                candidates = listOf(candidate)
            ) == transactionId
        } ?: return

        attach(match.id, transactionId)
    }

    private fun findAndAttachCandidate(
        eventId: Long,
        financedAmountMinor: Long,
        occurredAtEpochMs: Long
    ): Long? {
        val candidates = database.readableDatabase.rawQuery(
            """
            SELECT id, occurred_at_epoch_ms, amount_minor, fee_minor
            FROM transactions
            WHERE type IN (?, ?, ?, ?)
              AND occurred_at_epoch_ms BETWEEN ? AND ?
            ORDER BY occurred_at_epoch_ms DESC
            """.trimIndent(),
            arrayOf(
                MpesaTransactionType.SENT_MONEY.name,
                MpesaTransactionType.PAY_BILL.name,
                MpesaTransactionType.BUY_GOODS.name,
                MpesaTransactionType.AIRTIME.name,
                (occurredAtEpochMs - PAIRING_WINDOW_MS).toString(),
                (occurredAtEpochMs + PAIRING_WINDOW_MS).toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FulizaPaymentCandidate(
                            transactionId = cursor.getLong(0),
                            occurredAtEpochMs = cursor.getLong(1),
                            amountMinor = cursor.getLong(2),
                            mpesaFeeMinor = cursor.getLong(3)
                        )
                    )
                }
            }
        }

        val transactionId = FulizaPairingPolicy.chooseCandidate(
            fulizaFinancedMinor = financedAmountMinor,
            fulizaOccurredAtEpochMs = occurredAtEpochMs,
            candidates = candidates
        ) ?: return null

        attach(eventId, transactionId)
        return transactionId
    }

    private fun attach(
        eventId: Long,
        transactionId: Long
    ) {
        val db = database.writableDatabase
        val event = db.rawQuery(
            """
            SELECT transaction_code, financed_amount_minor, access_fee_minor,
                   outstanding_amount_minor
            FROM fuliza_events
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(eventId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            AttachedFuliza(
                transactionCode = if (cursor.isNull(0)) null else cursor.getString(0),
                financedAmountMinor = cursor.getLong(1),
                accessFeeMinor = cursor.getLong(2),
                outstandingAmountMinor = if (cursor.isNull(3)) null else cursor.getLong(3)
            )
        }

        ContentValues().apply {
            put("linked_transaction_id", transactionId)
        }.also { values ->
            db.update(
                "fuliza_events",
                values,
                "id = ?",
                arrayOf(eventId.toString())
            )
        }

        ContentValues().apply {
            put("fuliza_financed_minor", event.financedAmountMinor)
            put("fuliza_access_fee_minor", event.accessFeeMinor)
            if (event.outstandingAmountMinor != null) {
                put("fuliza_outstanding_minor", event.outstandingAmountMinor)
            } else {
                putNull("fuliza_outstanding_minor")
            }
            if (event.transactionCode != null) {
                put("fuliza_event_code", event.transactionCode)
            } else {
                putNull("fuliza_event_code")
            }
        }.also { values ->
            db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(transactionId.toString())
            )
        }
    }

    private fun eventKey(parsed: ParsedFulizaEvent): String {
        parsed.transactionCode?.takeIf { it.isNotBlank() }?.let {
            return "code:" + it.uppercase()
        }

        val canonical = parsed.rawMessage
            .replace(Regex("""\s+"""), " ")
            .trim()
            .uppercase()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return "sha256:" + digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun MpesaTransactionType.canUseFuliza(): Boolean =
        this == MpesaTransactionType.SENT_MONEY ||
            this == MpesaTransactionType.PAY_BILL ||
            this == MpesaTransactionType.BUY_GOODS ||
            this == MpesaTransactionType.AIRTIME

    private data class PendingFuliza(
        val id: Long,
        val financedAmountMinor: Long,
        val occurredAtEpochMs: Long
    )

    private data class AttachedFuliza(
        val transactionCode: String?,
        val financedAmountMinor: Long,
        val accessFeeMinor: Long,
        val outstandingAmountMinor: Long?
    )

    companion object {
        private const val PAIRING_WINDOW_MS = 3 * 60 * 1000L

        @Volatile
        private var instance: FulizaStore? = null

        fun get(context: Context): FulizaStore =
            instance ?: synchronized(this) {
                instance ?: FulizaStore(context.applicationContext).also { instance = it }
            }
    }
}
