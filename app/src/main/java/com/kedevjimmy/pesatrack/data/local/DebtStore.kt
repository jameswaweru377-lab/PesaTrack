package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtFundingBehavior
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.interpretation.InterpretationConfidence
import com.kedevjimmy.pesatrack.domain.parser.FulizaRepaymentCompletion
import com.kedevjimmy.pesatrack.domain.parser.ParsedFulizaEvent
import com.kedevjimmy.pesatrack.domain.parser.ParsedFulizaRepayment
import com.kedevjimmy.pesatrack.domain.rules.DebtFundingCandidate
import com.kedevjimmy.pesatrack.domain.rules.DebtFundingPairingPolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtRepaymentChainPolicy
import com.kedevjimmy.pesatrack.domain.rules.RepaymentChainConfidence
import com.kedevjimmy.pesatrack.domain.rules.RepaymentFundingCandidate
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
    val observedAtEpochMs: Long?,
    val availableCreditMinor: Long?,
    val availableCreditObservedAtEpochMs: Long?
)

data class DebtEventRecord(
    val id: Long,
    val debtAccountId: String,
    val type: DebtEventType,
    val amountMinor: Long,
    val feeMinor: Long,
    val outstandingAfterMinor: Long?,
    val linkedTransactionId: Long?,
    val walletBalanceAfterMinor: Long?,
    val fundingAccountKey: String?,
    val availableCreditAfterMinor: Long?,
    val repaymentCompletion: DebtCompletion?,
    val fundingLinkConfidence: RepaymentChainConfidence?,
    val fundingLinkReason: String?,
    val occurredAtEpochMs: Long
)

data class DebtWriteResult(
    val eventId: Long,
    val inserted: Boolean,
    val linkedTransactionId: Long?
)


class DebtStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)

    @Synchronized
    fun recordDrawdown(
        event: DebtDrawdownEventFact,
        transactionCode: String?,
        messageFingerprint: String,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long,
        linkedTransactionIdOverride: Long? = null
    ): DebtWriteResult {
        val identity =
            debtAccountIdentity(event.liability)
        ensureAccount(
            id = identity.id,
            providerKey = identity.providerKey,
            name = identity.name
        )

        val behavior =
            FinancialProviderRegistry.debtBehaviorFor(
                event.liability
            )
        val linkedTransactionId =
            linkedTransactionIdOverride
                ?: when (
                    behavior?.fundingBehavior
                        ?: DebtFundingBehavior.UNKNOWN
                ) {
                    DebtFundingBehavior
                        .TRANSACTION_SHORTFALL ->
                        findDrawdownFundingTransaction(
                            financedAmountMinor =
                                event.amountMinor,
                            occurredAtEpochMs =
                                occurredAtEpochMs
                        )
                    DebtFundingBehavior.CASH_DISBURSEMENT,
                    DebtFundingBehavior.UNKNOWN ->
                        null
                }

        val insert = insertEvent(
            eventKey = debtEventKey(
                prefix =
                    "debt-drawdown:" +
                        identity.providerKey,
                transactionCode =
                    transactionCode,
                rawMessage =
                    messageFingerprint
            ),
            debtAccountId = identity.id,
            type = DebtEventType.DRAWDOWN,
            amountMinor = event.amountMinor,
            feeMinor = event.feeMinor,
            outstandingAfterMinor =
                event.outstandingAfterMinor,
            linkedTransactionId =
                linkedTransactionId,
            walletBalanceAfterMinor = null,
            fundingAccountKey = null,
            availableCreditAfterMinor =
                event.availableCreditAfterMinor,
            repaymentCompletion = null,
            fundingLinkConfidence = null,
            fundingLinkReason = null,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs = occurredAtEpochMs
        )

        event.availableCreditAfterMinor?.let {
            updateAvailableCredit(
                accountId = identity.id,
                availableCreditMinor = it,
                observedAtEpochMs = occurredAtEpochMs
            )
        }

        event.outstandingAfterMinor?.let {
            updateBalance(
                accountId = identity.id,
                outstandingMinor = it,
                quality =
                    DebtBalanceQuality.OBSERVED,
                observedAtEpochMs =
                    occurredAtEpochMs
            )
        } ?: if (insert.inserted) {
            applyEstimatedDelta(
                accountId = identity.id,
                deltaMinor =
                    event.amountMinor +
                        event.feeMinor,
                occurredAtEpochMs =
                    occurredAtEpochMs
            )
        }

        if (
            linkedTransactionId != null &&
            behavior
                ?.preserveLegacyTransactionMetadata ==
                true
        ) {
            applyLegacyTransactionFundingMetadata(
                transactionId =
                    linkedTransactionId,
                event = event,
                transactionCode =
                    transactionCode
            )
        }

        LedgerUpdateBus.notifyChanged()
        return DebtWriteResult(
            eventId = insert.id,
            inserted = insert.inserted,
            linkedTransactionId =
                linkedTransactionId
        )
    }

    @Synchronized
    fun recordRepayment(
        event: DebtRepaymentEventFact,
        transactionCode: String?,
        messageFingerprint: String,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): DebtWriteResult {
        val identity =
            debtAccountIdentity(event.liability)
        ensureAccount(
            id = identity.id,
            providerKey = identity.providerKey,
            name = identity.name
        )

        val insert = insertEvent(
            eventKey = debtEventKey(
                prefix =
                    "debt-repayment:" +
                        identity.providerKey,
                transactionCode =
                    transactionCode,
                rawMessage =
                    messageFingerprint
            ),
            debtAccountId = identity.id,
            type = DebtEventType.REPAYMENT,
            amountMinor = event.amountMinor,
            feeMinor = 0L,
            outstandingAfterMinor =
                event.outstandingAfterMinor,
            linkedTransactionId = null,
            walletBalanceAfterMinor =
                event.walletBalanceAfterMinor,
            fundingAccountKey =
                event.fundingAccount?.entityKey,
            availableCreditAfterMinor =
                event.availableCreditAfterMinor,
            repaymentCompletion =
                event.completion,
            fundingLinkConfidence = null,
            fundingLinkReason = null,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs = occurredAtEpochMs
        )

        event.availableCreditAfterMinor?.let {
            updateAvailableCredit(
                accountId = identity.id,
                availableCreditMinor = it,
                observedAtEpochMs = occurredAtEpochMs
            )
        }

        when (event.completion) {
            DebtCompletion.FULL ->
                updateBalance(
                    accountId = identity.id,
                    outstandingMinor = 0L,
                    quality =
                        DebtBalanceQuality.OBSERVED,
                    observedAtEpochMs =
                        occurredAtEpochMs
                )
            DebtCompletion.PARTIAL,
            DebtCompletion.UNKNOWN -> {
                if (insert.inserted) {
                    applyEstimatedDelta(
                        accountId = identity.id,
                        deltaMinor =
                            -event.amountMinor,
                        occurredAtEpochMs =
                            occurredAtEpochMs
                    )
                }
            }
        }

        if (
            event.fundingAccount?.providerKey ==
                MPESA_PROVIDER_KEY
        ) {
            reconcileRepaymentEvent(insert.id)
        }

        val linked = linkedTransactionId(insert.id)
        LedgerUpdateBus.notifyChanged()
        return DebtWriteResult(
            eventId = insert.id,
            inserted = insert.inserted,
            linkedTransactionId = linked
        )
    }

    @Synchronized
    fun recordFulizaDrawdown(
        parsed: ParsedFulizaEvent,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long,
        linkedTransactionId: Long?
    ) {
        recordDrawdown(
            event = DebtDrawdownEventFact(
                amountMinor =
                    parsed.financedAmountMinor,
                liability =
                    FinancialProviderRegistry
                        .resolveAccount(
                            "Fuliza M-PESA"
                        ),
                feeMinor =
                    parsed.accessFeeMinor,
                outstandingAfterMinor =
                    parsed.outstandingAmountMinor,
                availableCreditAfterMinor =
                    parsed.availableLimitMinor,
                confidence =
                    InterpretationConfidence.HIGH
            ),
            transactionCode =
                parsed.transactionCode,
            messageFingerprint =
                parsed.rawMessage,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs =
                occurredAtEpochMs,
            linkedTransactionIdOverride =
                linkedTransactionId
        )
    }

    @Synchronized
    fun recordFulizaRepayment(
        parsed: ParsedFulizaRepayment,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): Long {
        val result = recordRepayment(
            event = DebtRepaymentEventFact(
                amountMinor =
                    parsed.repaidAmountMinor,
                liability =
                    FinancialProviderRegistry
                        .resolveAccount(
                            "Fuliza M-PESA"
                        ),
                fundingAccount =
                    FinancialProviderRegistry
                        .mpesaAccount(),
                completion = when (
                    parsed.completion
                ) {
                    FulizaRepaymentCompletion.PARTIAL ->
                        DebtCompletion.PARTIAL
                    FulizaRepaymentCompletion.FULL ->
                        DebtCompletion.FULL
                    FulizaRepaymentCompletion.UNKNOWN ->
                        DebtCompletion.UNKNOWN
                },
                outstandingAfterMinor =
                    parsed.outstandingAfterMinor,
                availableCreditAfterMinor =
                    parsed.availableLimitMinor,
                walletBalanceAfterMinor =
                    parsed.mpesaBalanceMinor,
                confidence =
                    InterpretationConfidence.HIGH
            ),
            transactionCode =
                parsed.transactionCode,
            messageFingerprint =
                parsed.rawMessage,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            occurredAtEpochMs =
                occurredAtEpochMs
        )
        return result.eventId
    }

    @Synchronized
    fun reconcilePendingRepaymentsAround(
        occurredAtEpochMs: Long
    ) {
        val eventIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                  AND linked_transaction_id IS NULL
                  AND (
                        funding_account_key IS NULL
                        OR funding_account_key = ?
                      )
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.REPAYMENT.name,
                    FinancialProviderRegistry
                        .mpesaAccount()
                        .entityKey,
                    (
                        occurredAtEpochMs -
                            REPAYMENT_LINK_WINDOW_MS
                    ).toString(),
                    (
                        occurredAtEpochMs +
                            REPAYMENT_LINK_WINDOW_MS
                    ).toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        eventIds.forEach(::reconcileRepaymentEvent)
    }

    private fun reconcileRepaymentEvent(
        eventId: Long
    ) {
        val repayment =
            database.readableDatabase.rawQuery(
                """
                SELECT amount_minor,
                       wallet_balance_after_minor,
                       funding_account_key,
                       occurred_at_epoch_ms
                FROM debt_events
                WHERE id = ?
                  AND event_type = ?
                  AND linked_transaction_id IS NULL
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    eventId.toString(),
                    DebtEventType.REPAYMENT.name
                )
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    RepaymentEventForLinking(
                        amountMinor = cursor.getLong(0),
                        walletBalanceAfterMinor =
                            if (cursor.isNull(1)) {
                                null
                            } else {
                                cursor.getLong(1)
                            },
                        fundingAccountKey =
                            if (cursor.isNull(2)) {
                                null
                            } else {
                                cursor.getString(2)
                            },
                        occurredAtEpochMs =
                            cursor.getLong(3)
                    )
                }
            } ?: return

        val candidates =
            database.readableDatabase.rawQuery(
                """
                SELECT id, amount_minor,
                       occurred_at_epoch_ms,
                       balance_minor
                FROM transactions
                WHERE type IN (?, ?)
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                  AND review_state != ?
                  AND id NOT IN (
                        SELECT linked_transaction_id
                        FROM debt_events
                        WHERE linked_transaction_id IS NOT NULL
                  )
                ORDER BY occurred_at_epoch_ms DESC, id DESC
                """.trimIndent(),
                arrayOf(
                    com.kedevjimmy.pesatrack.domain.model
                        .MpesaTransactionType.RECEIVED.name,
                    com.kedevjimmy.pesatrack.domain.model
                        .MpesaTransactionType.DEPOSIT.name,
                    (
                        repayment.occurredAtEpochMs -
                            REPAYMENT_LINK_WINDOW_MS
                    ).toString(),
                    (
                        repayment.occurredAtEpochMs +
                            REPAYMENT_LINK_WINDOW_MS
                    ).toString(),
                    com.kedevjimmy.pesatrack.domain.model
                        .ReviewState.EXCLUDED.name
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val candidateAt =
                            cursor.getLong(2)
                        add(
                            RepaymentFundingCandidate(
                                transactionId =
                                    cursor.getLong(0),
                                amountMinor =
                                    cursor.getLong(1),
                                occurredAtEpochMs =
                                    candidateAt,
                                balanceAfterMinor =
                                    if (cursor.isNull(3)) {
                                        null
                                    } else {
                                        cursor.getLong(3)
                                    },
                                priorBalanceMinor =
                                    latestMpesaBalanceBefore(
                                        candidateAt
                                    )
                            )
                        )
                    }
                }
            }

        if (
            repayment.fundingAccountKey != null &&
            repayment.fundingAccountKey !=
                FinancialProviderRegistry
                    .mpesaAccount()
                    .entityKey
        ) {
            return
        }

        val match =
            DebtRepaymentChainPolicy
                .chooseFundingTransaction(
                    repaymentAmountMinor =
                        repayment.amountMinor,
                    walletBalanceAfterMinor =
                        repayment
                            .walletBalanceAfterMinor,
                    repaymentAtEpochMs =
                        repayment.occurredAtEpochMs,
                    candidates = candidates
                ) ?: return

        val values = ContentValues().apply {
            put(
                "linked_transaction_id",
                match.transactionId
            )
            put(
                "funding_link_confidence",
                match.confidence.name
            )
            put(
                "funding_link_reason",
                match.reason
            )
        }
        database.writableDatabase.update(
            "debt_events",
            values,
            "id = ?",
            arrayOf(eventId.toString())
        )
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun accounts(): List<DebtAccountSnapshot> {
        ensureLegacyFulizaRepaymentsReconciled()
        return database.readableDatabase.rawQuery(
            """
            SELECT id, provider_key, name, outstanding_minor,
                   balance_quality, last_observed_at_epoch_ms,
                   available_credit_minor,
                   available_credit_observed_at_epoch_ms
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
                                },
                            availableCreditMinor =
                                if (cursor.isNull(6)) {
                                    null
                                } else {
                                    cursor.getLong(6)
                                },
                            availableCreditObservedAtEpochMs =
                                if (cursor.isNull(7)) {
                                    null
                                } else {
                                    cursor.getLong(7)
                                }
                        )
                    )
                }
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
                   linked_transaction_id,
                   wallet_balance_after_minor,
                   funding_account_key,
                   available_credit_after_minor,
                   repayment_completion,
                   funding_link_confidence,
                   funding_link_reason,
                   occurred_at_epoch_ms
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
                            walletBalanceAfterMinor =
                                if (cursor.isNull(7)) {
                                    null
                                } else {
                                    cursor.getLong(7)
                                },
                            fundingAccountKey =
                                if (cursor.isNull(8)) {
                                    null
                                } else {
                                    cursor.getString(8)
                                },
                            availableCreditAfterMinor =
                                if (cursor.isNull(9)) {
                                    null
                                } else {
                                    cursor.getLong(9)
                                },
                            repaymentCompletion =
                                if (cursor.isNull(10)) {
                                    null
                                } else {
                                    runCatching {
                                        DebtCompletion
                                            .valueOf(
                                                cursor.getString(10)
                                            )
                                    }.getOrNull()
                                },
                            fundingLinkConfidence =
                                if (cursor.isNull(11)) {
                                    null
                                } else {
                                    runCatching {
                                        RepaymentChainConfidence
                                            .valueOf(
                                                cursor.getString(11)
                                            )
                                    }.getOrNull()
                                },
                            fundingLinkReason =
                                if (cursor.isNull(12)) {
                                    null
                                } else {
                                    cursor.getString(12)
                                },
                            occurredAtEpochMs =
                                cursor.getLong(13)
                        )
                    )
                }
            }
        }

    private fun ensureLegacyFulizaRepaymentsReconciled() {
        val preferences =
            appContext.getSharedPreferences(
                "pesatrack_preferences",
                Context.MODE_PRIVATE
            )
        if (
            preferences.getBoolean(
                LEGACY_FULIZA_REPAYMENT_RECONCILIATION_KEY,
                false
            )
        ) {
            return
        }

        val legacyRows =
            database.readableDatabase.rawQuery(
                """
                SELECT id, raw_message,
                       occurred_at_epoch_ms,
                       source_package
                FROM transactions
                WHERE review_state != ?
                  AND UPPER(raw_message)
                      LIKE '%FROM YOUR M-PESA HAS BEEN USED TO%PAY YOUR OUTSTANDING FULIZA M-PESA%'
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(
                    com.kedevjimmy.pesatrack.domain.model
                        .ReviewState.EXCLUDED.name
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            LegacyFulizaRepaymentRow(
                                transactionId =
                                    cursor.getLong(0),
                                rawMessage =
                                    cursor.getString(1),
                                occurredAtEpochMs =
                                    cursor.getLong(2),
                                sourcePackage =
                                    if (cursor.isNull(3)) {
                                        null
                                    } else {
                                        cursor.getString(3)
                                    }
                            )
                        )
                    }
                }
            }

        var changed = false
        legacyRows.forEach { row ->
            val parsed =
                com.kedevjimmy.pesatrack.domain.parser
                    .FulizaParser
                    .parseRepayment(row.rawMessage)
                    ?: return@forEach

            val excluded = ContentValues().apply {
                put(
                    "review_state",
                    com.kedevjimmy.pesatrack.domain.model
                        .ReviewState.EXCLUDED.name
                )
                put(
                    "direction",
                    com.kedevjimmy.pesatrack.domain.model
                        .MoneyDirection.NEUTRAL.name
                )
                put(
                    "category",
                    com.kedevjimmy.pesatrack.domain.model
                        .TransactionCategory.DEBT_LOAN.name
                )
                put(
                    "evidence_label",
                    "Fuliza repayment moved to debt ledger"
                )
                put(
                    "evidence_source",
                    "fuliza_debt_reconciliation"
                )
            }
            database.writableDatabase.update(
                "transactions",
                excluded,
                "id = ?",
                arrayOf(
                    row.transactionId.toString()
                )
            )

            recordFulizaRepayment(
                parsed = parsed,
                sourcePackage = row.sourcePackage,
                sourceKind =
                    "legacy_fuliza_repayment_reconciliation",
                occurredAtEpochMs =
                    row.occurredAtEpochMs
            )
            changed = true
        }

        preferences.edit()
            .putBoolean(
                LEGACY_FULIZA_REPAYMENT_RECONCILIATION_KEY,
                true
            )
            .apply()

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
            LedgerUpdateBus.notifyChanged()
        }
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
        walletBalanceAfterMinor: Long?,
        fundingAccountKey: String?,
        availableCreditAfterMinor: Long?,
        repaymentCompletion:
            DebtCompletion?,
        fundingLinkConfidence:
            RepaymentChainConfidence?,
        fundingLinkReason: String?,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): DebtEventInsertResult {
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
            ContentValues().apply {
                if (linkedTransactionId != null) {
                    put(
                        "linked_transaction_id",
                        linkedTransactionId
                    )
                }
                if (walletBalanceAfterMinor != null) {
                    put(
                        "wallet_balance_after_minor",
                        walletBalanceAfterMinor
                    )
                }
                if (fundingAccountKey != null) {
                    put(
                        "funding_account_key",
                        fundingAccountKey
                    )
                }
                if (availableCreditAfterMinor != null) {
                    put(
                        "available_credit_after_minor",
                        availableCreditAfterMinor
                    )
                }
                if (repaymentCompletion != null) {
                    put(
                        "repayment_completion",
                        repaymentCompletion.name
                    )
                }
                if (fundingLinkConfidence != null) {
                    put(
                        "funding_link_confidence",
                        fundingLinkConfidence.name
                    )
                }
                if (fundingLinkReason != null) {
                    put(
                        "funding_link_reason",
                        fundingLinkReason
                    )
                }
            }.also { values ->
                if (values.size() > 0) {
                    database.writableDatabase.update(
                        "debt_events",
                        values,
                        "id = ?",
                        arrayOf(existing.toString())
                    )
                }
            }
            return DebtEventInsertResult(
                id = existing,
                inserted = false
            )
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
            if (walletBalanceAfterMinor != null) {
                put(
                    "wallet_balance_after_minor",
                    walletBalanceAfterMinor
                )
            } else {
                putNull("wallet_balance_after_minor")
            }
            if (fundingAccountKey != null) {
                put(
                    "funding_account_key",
                    fundingAccountKey
                )
            } else {
                putNull("funding_account_key")
            }
            if (availableCreditAfterMinor != null) {
                put(
                    "available_credit_after_minor",
                    availableCreditAfterMinor
                )
            } else {
                putNull(
                    "available_credit_after_minor"
                )
            }
            if (repaymentCompletion != null) {
                put(
                    "repayment_completion",
                    repaymentCompletion.name
                )
            } else {
                putNull("repayment_completion")
            }
            if (fundingLinkConfidence != null) {
                put(
                    "funding_link_confidence",
                    fundingLinkConfidence.name
                )
            } else {
                putNull("funding_link_confidence")
            }
            if (fundingLinkReason != null) {
                put(
                    "funding_link_reason",
                    fundingLinkReason
                )
            } else {
                putNull("funding_link_reason")
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

        return DebtEventInsertResult(
            id = database.writableDatabase.insertOrThrow(
                "debt_events",
                null,
                values
            ),
            inserted = true
        )
    }

    private fun updateBalance(
        accountId: String,
        outstandingMinor: Long,
        quality: DebtBalanceQuality,
        observedAtEpochMs: Long
    ) {
        val latestObservedAt =
            database.readableDatabase.rawQuery(
                """
                SELECT last_observed_at_epoch_ms
                FROM debt_accounts
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(accountId)
            ).use { cursor ->
                if (
                    cursor.moveToFirst() &&
                    !cursor.isNull(0)
                ) {
                    cursor.getLong(0)
                } else {
                    null
                }
            }

        if (
            latestObservedAt != null &&
            observedAtEpochMs < latestObservedAt
        ) {
            return
        }

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
            SELECT outstanding_minor, last_observed_at_epoch_ms
            FROM debt_accounts
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                0L to null
            } else {
                cursor.getLong(0) to
                    if (cursor.isNull(1)) {
                        null
                    } else {
                        cursor.getLong(1)
                    }
            }
        }

        if (
            current.second != null &&
            occurredAtEpochMs < current.second!!
        ) {
            return
        }

        updateBalance(
            accountId = accountId,
            outstandingMinor =
                max(0L, current.first + deltaMinor),
            quality = DebtBalanceQuality.ESTIMATED,
            observedAtEpochMs = occurredAtEpochMs
        )
    }

    private fun updateAvailableCredit(
        accountId: String,
        availableCreditMinor: Long,
        observedAtEpochMs: Long
    ) {
        val currentObservedAt =
            database.readableDatabase.rawQuery(
                """
                SELECT available_credit_observed_at_epoch_ms
                FROM debt_accounts
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(accountId)
            ).use { cursor ->
                if (
                    cursor.moveToFirst() &&
                    !cursor.isNull(0)
                ) {
                    cursor.getLong(0)
                } else {
                    null
                }
            }

        if (
            currentObservedAt != null &&
            observedAtEpochMs <
                currentObservedAt
        ) {
            return
        }

        ContentValues().apply {
            put(
                "available_credit_minor",
                max(0L, availableCreditMinor)
            )
            put(
                "available_credit_observed_at_epoch_ms",
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

    private fun latestMpesaBalanceBefore(
        epochMs: Long
    ): Long? =
        database.readableDatabase.rawQuery(
            """
            SELECT balance_minor
            FROM (
                SELECT balance_minor,
                       occurred_at_epoch_ms AS observed_at,
                       0 AS priority
                FROM transactions
                WHERE balance_minor IS NOT NULL
                  AND occurred_at_epoch_ms < ?

                UNION ALL

                SELECT wallet_balance_after_minor AS balance_minor,
                       occurred_at_epoch_ms AS observed_at,
                       1 AS priority
                FROM debt_events
                WHERE wallet_balance_after_minor IS NOT NULL
                  AND occurred_at_epoch_ms < ?

                UNION ALL

                SELECT balance_minor,
                       observed_at_epoch_ms AS observed_at,
                       2 AS priority
                FROM account_balance_observations
                WHERE account_id = ?
                  AND observed_at_epoch_ms < ?
            )
            ORDER BY observed_at DESC, priority DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                epochMs.toString(),
                epochMs.toString(),
                AccountStore.DEFAULT_MPESA_ID,
                epochMs.toString()
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                null
            }
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

    private data class LegacyFulizaRepaymentRow(
        val transactionId: Long,
        val rawMessage: String,
        val occurredAtEpochMs: Long,
        val sourcePackage: String?
    )

    private data class RepaymentEventForLinking(
        val amountMinor: Long,
        val walletBalanceAfterMinor: Long?,
        val fundingAccountKey: String?,
        val occurredAtEpochMs: Long
    )

    private data class DebtEventInsertResult(
        val id: Long,
        val inserted: Boolean
    )

    companion object {
        const val FULIZA_ACCOUNT_ID = "debt-fuliza-mpesa"
        const val FULIZA_PROVIDER_KEY = "SAFARICOM_FULIZA"

        private const val REPAYMENT_LINK_WINDOW_MS =
            10L * 60L * 1000L
        private const val LEGACY_FULIZA_REPAYMENT_RECONCILIATION_KEY =
            "legacy_fuliza_repayment_reconciliation_v1"

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
