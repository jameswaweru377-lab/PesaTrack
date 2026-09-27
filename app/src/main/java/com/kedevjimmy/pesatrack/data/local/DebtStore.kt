package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.parser.ParsedFulizaEvent
import com.kedevjimmy.pesatrack.domain.parser.ParsedFulizaRepayment
import java.security.MessageDigest
import kotlin.math.max

enum class DebtEventType {
    DRAWDOWN,
    REPAYMENT,
    FEE,
    ADJUSTMENT
}

enum class DebtBalanceQuality {
    OBSERVED,
    ESTIMATED
}

data class DebtAccountSnapshot(
    val id: String,
    val providerKey: String,
    val name: String,
    val outstandingMinor: Long,
    val balanceQuality: DebtBalanceQuality,
    val observedAtEpochMs: Long?
)

data class DebtEventRecord(
    val id: Long,
    val debtAccountId: String,
    val type: DebtEventType,
    val amountMinor: Long,
    val feeMinor: Long,
    val outstandingAfterMinor: Long?,
    val linkedTransactionId: Long?,
    val occurredAtEpochMs: Long
)

class DebtStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context.applicationContext)

    @Synchronized
    fun recordFulizaDrawdown(
        parsed: ParsedFulizaEvent,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long,
        linkedTransactionId: Long?
    ) {
        ensureAccount(
            id = FULIZA_ACCOUNT_ID,
            providerKey = FULIZA_PROVIDER_KEY,
            name = "Fuliza M-PESA"
        )

        val outstanding = parsed.outstandingAmountMinor
        insertEvent(
            eventKey = debtEventKey(
                prefix = "fuliza-drawdown",
                transactionCode = parsed.transactionCode,
                rawMessage = parsed.rawMessage
            ),
            debtAccountId = FULIZA_ACCOUNT_ID,
            type = DebtEventType.DRAWDOWN,
            amountMinor = parsed.financedAmountMinor,
            feeMinor = parsed.accessFeeMinor,
            outstandingAfterMinor = outstanding,
            linkedTransactionId = linkedTransactionId,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs = occurredAtEpochMs
        )

        if (outstanding != null) {
            updateBalance(
                accountId = FULIZA_ACCOUNT_ID,
                outstandingMinor = outstanding,
                quality = DebtBalanceQuality.OBSERVED,
                observedAtEpochMs = occurredAtEpochMs
            )
        } else {
            applyEstimatedDelta(
                accountId = FULIZA_ACCOUNT_ID,
                deltaMinor =
                    parsed.financedAmountMinor +
                        parsed.accessFeeMinor,
                occurredAtEpochMs = occurredAtEpochMs
            )
        }
    }

    @Synchronized
    fun recordFulizaRepayment(
        parsed: ParsedFulizaRepayment,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): Long {
        ensureAccount(
            id = FULIZA_ACCOUNT_ID,
            providerKey = FULIZA_PROVIDER_KEY,
            name = "Fuliza M-PESA"
        )

        val linkedTransactionId =
            findRepaymentFundingTransaction(
                amountMinor = parsed.repaidAmountMinor,
                occurredAtEpochMs = occurredAtEpochMs
            )

        val eventId = insertEvent(
            eventKey = debtEventKey(
                prefix = "fuliza-repayment",
                transactionCode = parsed.transactionCode,
                rawMessage = parsed.rawMessage
            ),
            debtAccountId = FULIZA_ACCOUNT_ID,
            type = DebtEventType.REPAYMENT,
            amountMinor = parsed.repaidAmountMinor,
            feeMinor = 0L,
            outstandingAfterMinor = null,
            linkedTransactionId = linkedTransactionId,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs = occurredAtEpochMs
        )

        applyEstimatedDelta(
            accountId = FULIZA_ACCOUNT_ID,
            deltaMinor = -parsed.repaidAmountMinor,
            occurredAtEpochMs = occurredAtEpochMs
        )

        return eventId
    }

    @Synchronized
    fun linkPendingRepaymentForTransaction(
        transactionId: Long,
        amountMinor: Long,
        occurredAtEpochMs: Long
    ) {
        val candidate = database.readableDatabase.rawQuery(
            """
            SELECT id
            FROM debt_events
            WHERE debt_account_id = ?
              AND event_type = ?
              AND linked_transaction_id IS NULL
              AND amount_minor = ?
              AND occurred_at_epoch_ms BETWEEN ? AND ?
            ORDER BY ABS(occurred_at_epoch_ms - ?) ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                FULIZA_ACCOUNT_ID,
                DebtEventType.REPAYMENT.name,
                amountMinor.toString(),
                (occurredAtEpochMs - REPAYMENT_LINK_WINDOW_MS)
                    .toString(),
                (occurredAtEpochMs + REPAYMENT_LINK_WINDOW_MS)
                    .toString(),
                occurredAtEpochMs.toString()
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        } ?: return

        ContentValues().apply {
            put("linked_transaction_id", transactionId)
        }.also { values ->
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ?",
                arrayOf(candidate.toString())
            )
        }
    }

    @Synchronized
    fun accounts(): List<DebtAccountSnapshot> =
        database.readableDatabase.rawQuery(
            """
            SELECT id, provider_key, name, outstanding_minor,
                   balance_quality, last_observed_at_epoch_ms
            FROM debt_accounts
            ORDER BY name COLLATE NOCASE
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DebtAccountSnapshot(
                            id = cursor.getString(0),
                            providerKey = cursor.getString(1),
                            name = cursor.getString(2),
                            outstandingMinor = cursor.getLong(3),
                            balanceQuality = runCatching {
                                DebtBalanceQuality.valueOf(
                                    cursor.getString(4)
                                )
                            }.getOrDefault(
                                DebtBalanceQuality.ESTIMATED
                            ),
                            observedAtEpochMs =
                                if (cursor.isNull(5)) {
                                    null
                                } else {
                                    cursor.getLong(5)
                                }
                        )
                    )
                }
            }
        }

    @Synchronized
    fun recentEvents(
        debtAccountId: String,
        limit: Int = 50
    ): List<DebtEventRecord> =
        database.readableDatabase.rawQuery(
            """
            SELECT id, debt_account_id, event_type, amount_minor,
                   fee_minor, outstanding_after_minor,
                   linked_transaction_id, occurred_at_epoch_ms
            FROM debt_events
            WHERE debt_account_id = ?
            ORDER BY occurred_at_epoch_ms DESC, id DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                debtAccountId,
                limit.coerceIn(1, 200).toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DebtEventRecord(
                            id = cursor.getLong(0),
                            debtAccountId = cursor.getString(1),
                            type = DebtEventType.valueOf(
                                cursor.getString(2)
                            ),
                            amountMinor = cursor.getLong(3),
                            feeMinor = cursor.getLong(4),
                            outstandingAfterMinor =
                                if (cursor.isNull(5)) {
                                    null
                                } else {
                                    cursor.getLong(5)
                                },
                            linkedTransactionId =
                                if (cursor.isNull(6)) {
                                    null
                                } else {
                                    cursor.getLong(6)
                                },
                            occurredAtEpochMs = cursor.getLong(7)
                        )
                    )
                }
            }
        }

    private fun findRepaymentFundingTransaction(
        amountMinor: Long,
        occurredAtEpochMs: Long
    ): Long? =
        database.readableDatabase.rawQuery(
            """
            SELECT id
            FROM transactions
            WHERE amount_minor = ?
              AND type IN (?, ?)
              AND occurred_at_epoch_ms BETWEEN ? AND ?
              AND review_state != ?
            ORDER BY ABS(occurred_at_epoch_ms - ?) ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                amountMinor.toString(),
                com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
                    .RECEIVED.name,
                com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
                    .DEPOSIT.name,
                (occurredAtEpochMs - REPAYMENT_LINK_WINDOW_MS)
                    .toString(),
                (occurredAtEpochMs + REPAYMENT_LINK_WINDOW_MS)
                    .toString(),
                com.kedevjimmy.pesatrack.domain.model.ReviewState
                    .EXCLUDED.name,
                occurredAtEpochMs.toString()
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }

    private fun ensureAccount(
        id: String,
        providerKey: String,
        name: String
    ) {
        val now = System.currentTimeMillis()
        ContentValues().apply {
            put("id", id)
            put("provider_key", providerKey)
            put("name", name)
            put("outstanding_minor", 0L)
            put("balance_quality", DebtBalanceQuality.ESTIMATED.name)
            put("created_at_epoch_ms", now)
            put("updated_at_epoch_ms", now)
        }.also { values ->
            database.writableDatabase.insertWithOnConflict(
                "debt_accounts",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    private fun insertEvent(
        eventKey: String,
        debtAccountId: String,
        type: DebtEventType,
        amountMinor: Long,
        feeMinor: Long,
        outstandingAfterMinor: Long?,
        linkedTransactionId: Long?,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): Long {
        val existing = database.readableDatabase.rawQuery(
            """
            SELECT id
            FROM debt_events
            WHERE event_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(eventKey)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
        if (existing != null) {
            if (linkedTransactionId != null) {
                ContentValues().apply {
                    put("linked_transaction_id", linkedTransactionId)
                }.also { values ->
                    database.writableDatabase.update(
                        "debt_events",
                        values,
                        "id = ?",
                        arrayOf(existing.toString())
                    )
                }
            }
            return existing
        }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("event_key", eventKey)
            put("debt_account_id", debtAccountId)
            put("event_type", type.name)
            put("amount_minor", amountMinor)
            put("fee_minor", feeMinor)
            if (outstandingAfterMinor != null) {
                put(
                    "outstanding_after_minor",
                    outstandingAfterMinor
                )
            } else {
                putNull("outstanding_after_minor")
            }
            if (linkedTransactionId != null) {
                put(
                    "linked_transaction_id",
                    linkedTransactionId
                )
            } else {
                putNull("linked_transaction_id")
            }
            if (sourcePackage != null) {
                put("source_package", sourcePackage)
            } else {
                putNull("source_package")
            }
            put("source_kind", sourceKind)
            put("occurred_at_epoch_ms", occurredAtEpochMs)
            put("created_at_epoch_ms", now)
        }

        return database.writableDatabase.insertOrThrow(
            "debt_events",
            null,
            values
        )
    }

    private fun updateBalance(
        accountId: String,
        outstandingMinor: Long,
        quality: DebtBalanceQuality,
        observedAtEpochMs: Long
    ) {
        ContentValues().apply {
            put(
                "outstanding_minor",
                max(0L, outstandingMinor)
            )
            put("balance_quality", quality.name)
            put(
                "last_observed_at_epoch_ms",
                observedAtEpochMs
            )
            put(
                "updated_at_epoch_ms",
                System.currentTimeMillis()
            )
        }.also { values ->
            database.writableDatabase.update(
                "debt_accounts",
                values,
                "id = ?",
                arrayOf(accountId)
            )
        }
        LedgerUpdateBus.notifyChanged()
    }

    private fun applyEstimatedDelta(
        accountId: String,
        deltaMinor: Long,
        occurredAtEpochMs: Long
    ) {
        val current = database.readableDatabase.rawQuery(
            """
            SELECT outstanding_minor
            FROM debt_accounts
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

        updateBalance(
            accountId = accountId,
            outstandingMinor = max(0L, current + deltaMinor),
            quality = DebtBalanceQuality.ESTIMATED,
            observedAtEpochMs = occurredAtEpochMs
        )
    }

    private fun debtEventKey(
        prefix: String,
        transactionCode: String?,
        rawMessage: String
    ): String {
        transactionCode
            ?.takeIf { it.isNotBlank() }
            ?.let {
                return prefix + ":code:" + it.uppercase()
            }

        val canonical = rawMessage
            .replace(Regex("""\s+"""), " ")
            .trim()
            .uppercase()

        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))

        return prefix + ":sha256:" +
            digest.joinToString("") { byte ->
                "%02x".format(byte)
            }
    }

    companion object {
        const val FULIZA_ACCOUNT_ID = "debt-fuliza-mpesa"
        const val FULIZA_PROVIDER_KEY = "SAFARICOM_FULIZA"

        private const val REPAYMENT_LINK_WINDOW_MS =
            10L * 60L * 1000L

        @Volatile
        private var instance: DebtStore? = null

        fun get(context: Context): DebtStore =
            instance ?: synchronized(this) {
                instance ?: DebtStore(
                    context.applicationContext
                ).also { instance = it }
            }
    }
}
