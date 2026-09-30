package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtFundingBehavior
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.interpretation.InterpretationConfidence
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.rules.DebtFundingCandidate
import com.kedevjimmy.pesatrack.domain.rules.DebtFundingPairingPolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtProceedsEventCandidate
import com.kedevjimmy.pesatrack.domain.rules.DebtProceedsRepaymentLinkPolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtRepaymentBalancePolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtRepaymentChainPolicy
import com.kedevjimmy.pesatrack.domain.rules.LiabilityPaymentCandidate
import com.kedevjimmy.pesatrack.domain.rules.LiabilityPaymentLinkPolicy
import com.kedevjimmy.pesatrack.domain.rules.LiabilityPaymentMatch
import com.kedevjimmy.pesatrack.domain.rules.RepaymentBalanceCandidate
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
    val relatedDebtEventId: Long?,
    val walletBalanceAfterMinor: Long?,
    val fundingAccountKey: String?,
    val fundingAccountId: String?,
    val resultingBalanceObservationId: Long?,
    val balanceLinkConfidence: RepaymentChainConfidence?,
    val balanceLinkReason: String?,
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
    fun recordObservedOutstanding(
        reference: FinancialAccountReference,
        outstandingMinor: Long,
        observedAtEpochMs: Long
    ) {
        if (
            reference.nature !=
                FinancialAccountNature.LIABILITY
        ) {
            return
        }

        val identity = debtAccountIdentity(reference)
        ensureAccount(
            id = identity.id,
            providerKey = identity.providerKey,
            name = identity.name
        )
        updateBalance(
            accountId = identity.id,
            outstandingMinor =
                max(0L, outstandingMinor),
            quality = DebtBalanceQuality.OBSERVED,
            observedAtEpochMs = observedAtEpochMs
        )
    }

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
        val fundingAccountId =
            event.proceedsAccount?.let {
                FinancialUnderstandingStore
                    .get(appContext)
                    .accountIdFor(it)
            }
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
                    debtEventPrefix(
                        reference = event.liability,
                        suffix = "drawdown"
                    ),
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
            fundingAccountKey =
                event.proceedsAccount?.entityKey,
            fundingAccountId =
                fundingAccountId,
            availableCreditAfterMinor =
                event.availableCreditAfterMinor,
            repaymentCompletion = null,
            fundingLinkConfidence =
                fundingLinkConfidenceOverride,
            fundingLinkReason =
                fundingLinkReasonOverride,
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

        if (event.outstandingAfterMinor != null) {
            updateBalance(
                accountId = identity.id,
                outstandingMinor =
                    event.outstandingAfterMinor,
                quality =
                    DebtBalanceQuality.OBSERVED,
                observedAtEpochMs =
                    occurredAtEpochMs
            )
        } else if (
            insert.inserted &&
            !event.amountIsNetProceeds
        ) {
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

        event.proceedsAccount?.entityKey?.let {
            reconcileRepaymentsFromDebtProceedsAround(
                fundingAccountKey = it,
                occurredAtEpochMs = occurredAtEpochMs
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
        occurredAtEpochMs: Long,
        linkedTransactionIdOverride: Long? = null,
        fundingAccountIdOverride: String? = null,
        fundingLinkConfidenceOverride:
            RepaymentChainConfidence? = null,
        fundingLinkReasonOverride: String? = null
    ): DebtWriteResult {
        val identity =
            debtAccountIdentity(event.liability)
        ensureAccount(
            id = identity.id,
            providerKey = identity.providerKey,
            name = identity.name
        )

        val fundingAccountId =
            fundingAccountIdOverride
                ?: event.fundingAccount?.let {
                    FinancialUnderstandingStore
                        .get(appContext)
                        .accountIdFor(it)
                }

        val insert = insertEvent(
            eventKey = debtEventKey(
                prefix =
                    debtEventPrefix(
                        reference = event.liability,
                        suffix = "repayment"
                    ),
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
            linkedTransactionId =
                linkedTransactionIdOverride,
            walletBalanceAfterMinor =
                event.walletBalanceAfterMinor,
            fundingAccountKey =
                event.fundingAccount?.entityKey,
            fundingAccountId =
                fundingAccountId,
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

        reconcileRepaymentFromDebtProceeds(
            eventId = insert.id
        )

        if (fundingAccountId != null) {
            if (linkedTransactionIdOverride == null) {
                reconcileRepaymentEvent(insert.id)
            }
            reconcileRepaymentBalance(insert.id)
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
    fun liabilityPaymentMatches(
        counterparty: String?,
        amountMinor: Long
    ): List<LiabilityPaymentMatch> =
        LiabilityPaymentLinkPolicy.candidates(
            counterparty = counterparty,
            amountMinor = amountMinor,
            candidates =
                accounts().map { account ->
                    LiabilityPaymentCandidate(
                        debtAccountId = account.id,
                        displayName = account.name,
                        providerKey =
                            account.providerKey,
                        outstandingMinor =
                            account.outstandingMinor,
                        hasObservedOutstanding =
                            account.balanceQuality ==
                                DebtBalanceQuality
                                    .OBSERVED &&
                                account.observedAtEpochMs !=
                                null
                    )
                }
        )

    @Synchronized
    fun tryAutoLinkOutgoingPayment(
        transactionId: Long
    ): Boolean {
        if (
            hasLiabilityPaymentDecision(
                transactionId
            ) ||
            hasRepaymentLinkedToTransaction(
                transactionId
            )
        ) {
            return false
        }

        val payment =
            paymentForLiabilityLink(
                transactionId
            ) ?: return false

        val match =
            LiabilityPaymentLinkPolicy
                .chooseAutomatic(
                    counterparty =
                        payment.counterparty,
                    amountMinor =
                        payment.amountMinor,
                    candidates =
                        accounts().map { account ->
                            LiabilityPaymentCandidate(
                                debtAccountId =
                                    account.id,
                                displayName =
                                    account.name,
                                providerKey =
                                    account.providerKey,
                                outstandingMinor =
                                    account.outstandingMinor,
                                hasObservedOutstanding =
                                    account.balanceQuality ==
                                        DebtBalanceQuality
                                            .OBSERVED &&
                                        account
                                            .observedAtEpochMs !=
                                            null
                            )
                        }
                ) ?: return false

        return recordPaymentAsRepayment(
            transactionId = transactionId,
            debtAccountId =
                match.debtAccountId,
            userConfirmed = false
        )
    }

    @Synchronized
    fun recordPaymentAsRepayment(
        transactionId: Long,
        debtAccountId: String,
        userConfirmed: Boolean
    ): Boolean {
        if (
            hasRepaymentLinkedToTransaction(
                transactionId
            )
        ) {
            return true
        }

        val payment =
            paymentForLiabilityLink(
                transactionId
            ) ?: return false
        val debt =
            accounts().firstOrNull {
                it.id == debtAccountId
            } ?: return false
        val currentMatches =
            liabilityPaymentMatches(
                counterparty =
                    payment.counterparty,
                amountMinor =
                    payment.amountMinor
            )
        val selected =
            currentMatches.firstOrNull {
                it.debtAccountId ==
                    debtAccountId
            } ?: return false

        if (
            !userConfirmed &&
            selected.confidence !=
                RepaymentChainConfidence.HIGH
        ) {
            return false
        }

        val liability =
            debtAccountReferenceForProvider(
                debt.providerKey
            ).copy(
                displayName = debt.name,
                nature =
                    FinancialAccountNature.LIABILITY
            )
        val confidence =
            if (userConfirmed) {
                RepaymentChainConfidence
                    .USER_CONFIRMED
            } else {
                RepaymentChainConfidence.HIGH
            }
        val reason =
            if (userConfirmed) {
                "User confirmed this outgoing payment repaid the selected liability."
            } else {
                selected.reason
            }

        val result =
            recordRepayment(
                event =
                    DebtRepaymentEventFact(
                        amountMinor =
                            payment.amountMinor,
                        liability = liability,
                        fundingAccount = null,
                        completion =
                            DebtCompletion.UNKNOWN,
                        outstandingAfterMinor = null,
                        availableCreditAfterMinor =
                            null,
                        walletBalanceAfterMinor =
                            payment.balanceMinor,
                        confidence =
                            InterpretationConfidence.HIGH
                    ),
                transactionCode =
                    payment.transactionCode,
                messageFingerprint =
                    "linked-payment:" +
                        transactionId +
                        ":" +
                        debtAccountId,
                sourcePackage =
                    payment.sourcePackage,
                sourceKind =
                    "liability_payment_bridge:" +
                        payment.sourceKind,
                occurredAtEpochMs =
                    payment.occurredAtEpochMs,
                linkedTransactionIdOverride =
                    transactionId,
                fundingAccountIdOverride =
                    payment.accountId,
                fundingLinkConfidenceOverride =
                    confidence,
                fundingLinkReasonOverride =
                    reason
            )

        val classified =
            TransactionStore.get(appContext)
                .markDebtRepayment(
                    transactionId =
                        transactionId,
                    debtEventId = result.eventId,
                    debtName = debt.name,
                    userConfirmed =
                        userConfirmed
                )

        return result.inserted || classified
    }

    private fun paymentForLiabilityLink(
        transactionId: Long
    ): PaymentForLiabilityLink? =
        database.readableDatabase.rawQuery(
            """
            SELECT amount_minor,
                   counterparty,
                   account_id,
                   balance_minor,
                   occurred_at_epoch_ms,
                   transaction_code,
                   source_package,
                   source_kind,
                   direction,
                   review_state,
                   rule_id,
                   relation_type
            FROM transactions
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(transactionId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val direction =
                    cursor.getString(8)
                val reviewState =
                    cursor.getString(9)
                val ruleId =
                    if (cursor.isNull(10)) {
                        null
                    } else {
                        cursor.getString(10)
                    }
                val relationType =
                    if (cursor.isNull(11)) {
                        null
                    } else {
                        cursor.getString(11)
                    }

                if (
                    direction !=
                        com.kedevjimmy.pesatrack
                            .domain.model
                            .MoneyDirection.EXPENSE.name ||
                    reviewState ==
                        com.kedevjimmy.pesatrack
                            .domain.model
                            .ReviewState.USER_CLASSIFIED
                            .name ||
                    reviewState ==
                        com.kedevjimmy.pesatrack
                            .domain.model
                            .ReviewState.EXCLUDED.name ||
                    ruleId != null ||
                    relationType != null ||
                    cursor.isNull(1) ||
                    cursor.isNull(2)
                ) {
                    null
                } else {
                    PaymentForLiabilityLink(
                        amountMinor =
                            cursor.getLong(0),
                        counterparty =
                            cursor.getString(1),
                        accountId =
                            cursor.getString(2),
                        balanceMinor =
                            if (cursor.isNull(3)) {
                                null
                            } else {
                                cursor.getLong(3)
                            },
                        occurredAtEpochMs =
                            cursor.getLong(4),
                        transactionCode =
                            if (cursor.isNull(5)) {
                                null
                            } else {
                                cursor.getString(5)
                            },
                        sourcePackage =
                            if (cursor.isNull(6)) {
                                null
                            } else {
                                cursor.getString(6)
                            },
                        sourceKind =
                            cursor.getString(7)
                    )
                }
            }
        }

    private fun hasRepaymentLinkedToTransaction(
        transactionId: Long
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT 1
            FROM debt_events
            WHERE event_type = ?
              AND linked_transaction_id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                DebtEventType.REPAYMENT.name,
                transactionId.toString()
            )
        ).use { it.moveToFirst() }

    private fun hasLiabilityPaymentDecision(
        transactionId: Long
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT 1
            FROM liability_payment_decisions
            WHERE transaction_id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(transactionId.toString())
        ).use { it.moveToFirst() }

    @Synchronized
    fun reconcilePendingRepaymentsAround(
        transactionId: Long,
        occurredAtEpochMs: Long
    ) {
        val transactionAccounts =
            database.readableDatabase.rawQuery(
                """
                SELECT account_id, linked_account_id
                FROM transactions
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(transactionId.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    emptySet()
                } else {
                    buildSet {
                        if (!cursor.isNull(0)) {
                            add(cursor.getString(0))
                        }
                        if (!cursor.isNull(1)) {
                            add(cursor.getString(1))
                        }
                    }
                }
            }

        if (transactionAccounts.isEmpty()) {
            return
        }

        val placeholders =
            transactionAccounts.joinToString(",") { "?" }
        val args =
            buildList {
                add(DebtEventType.REPAYMENT.name)
                addAll(transactionAccounts)
                add(
                    (
                        occurredAtEpochMs -
                            REPAYMENT_LINK_WINDOW_MS
                        ).toString()
                )
                add(
                    (
                        occurredAtEpochMs +
                            REPAYMENT_LINK_WINDOW_MS
                        ).toString()
                )
            }.toTypedArray()

        val eventIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                  AND linked_transaction_id IS NULL
                  AND funding_link_user_decision IS NULL
                  AND funding_account_id IN ($placeholders)
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        eventIds.forEach { eventId ->
            reconcileRepaymentEvent(eventId)
            reconcileRepaymentBalance(eventId)
        }
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
                       funding_account_id,
                       occurred_at_epoch_ms
                FROM debt_events
                WHERE id = ?
                  AND event_type = ?
                  AND linked_transaction_id IS NULL
                  AND funding_link_user_decision IS NULL
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
                        fundingAccountId =
                            if (cursor.isNull(3)) {
                                null
                            } else {
                                cursor.getString(3)
                            },
                        occurredAtEpochMs =
                            cursor.getLong(4)
                    )
                }
            } ?: return

        val fundingAccountId =
            repayment.fundingAccountId ?: return

        val candidates =
            database.readableDatabase.rawQuery(
                """
                SELECT id, amount_minor,
                       occurred_at_epoch_ms,
                       balance_minor
                FROM transactions
                WHERE (
                        (
                            direction = ?
                            AND account_id = ?
                        )
                        OR (
                            direction = ?
                            AND linked_account_id = ?
                        )
                      )
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
                        .MoneyDirection.INCOME.name,
                    fundingAccountId,
                    com.kedevjimmy.pesatrack.domain.model
                        .MoneyDirection.TRANSFER.name,
                    fundingAccountId,
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
                                    latestBalanceBefore(
                                        accountId =
                                            fundingAccountId,
                                        entityKey =
                                            repayment
                                                .fundingAccountKey,
                                        epochMs =
                                            candidateAt
                                    )
                            )
                        )
                    }
                }
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
        reconcileRepaymentBalance(eventId)
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun reconcilePendingRepaymentBalancesAround(
        accountId: String,
        entityKey: String,
        observedAtEpochMs: Long
    ) {
        val window =
            DebtRepaymentBalancePolicy
                .MAX_LINK_DISTANCE_MS
        val eventIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                  AND resulting_balance_observation_id
                      IS NULL
                  AND balance_link_user_decision IS NULL
                  AND (
                        funding_account_id = ?
                        OR (
                            funding_account_id IS NULL
                            AND funding_account_key = ?
                        )
                      )
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.REPAYMENT.name,
                    accountId,
                    entityKey,
                    (
                        observedAtEpochMs -
                            window
                        ).toString(),
                    (
                        observedAtEpochMs +
                            window
                        ).toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        eventIds.forEach(::reconcileRepaymentBalance)
    }

    private fun reconcileRepaymentBalance(
        eventId: Long
    ) {
        val repayment =
            database.readableDatabase.rawQuery(
                """
                SELECT amount_minor,
                       wallet_balance_after_minor,
                       funding_account_key,
                       funding_account_id,
                       occurred_at_epoch_ms,
                       linked_transaction_id,
                       resulting_balance_observation_id
                FROM debt_events
                WHERE id = ?
                  AND event_type = ?
                  AND balance_link_user_decision IS NULL
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
                    RepaymentBalanceContext(
                        amountMinor = cursor.getLong(0),
                        explicitBalanceAfterMinor =
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
                        fundingAccountId =
                            if (cursor.isNull(3)) {
                                null
                            } else {
                                cursor.getString(3)
                            },
                        occurredAtEpochMs =
                            cursor.getLong(4),
                        linkedTransactionId =
                            if (cursor.isNull(5)) {
                                null
                            } else {
                                cursor.getLong(5)
                            },
                        resultingBalanceObservationId =
                            if (cursor.isNull(6)) {
                                null
                            } else {
                                cursor.getLong(6)
                            }
                    )
                }
            } ?: return

        if (
            repayment.resultingBalanceObservationId !=
                null
        ) {
            return
        }

        val fundingAccountId =
            repayment.fundingAccountId ?: return

        val linkedFunding =
            repayment.linkedTransactionId?.let {
                transactionId ->
                database.readableDatabase.rawQuery(
                    """
                    SELECT amount_minor,
                           occurred_at_epoch_ms
                    FROM transactions
                    WHERE id = ?
                      AND (
                            account_id = ?
                            OR linked_account_id = ?
                          )
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        transactionId.toString(),
                        fundingAccountId,
                        fundingAccountId
                    )
                ).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        val transactionAt =
                            cursor.getLong(1)
                        RepaymentFundingForBalance(
                            amountMinor =
                                cursor.getLong(0),
                            occurredAtEpochMs =
                                transactionAt,
                            priorBalanceMinor =
                                latestBalanceBefore(
                                    accountId =
                                        fundingAccountId,
                                    entityKey =
                                        repayment
                                            .fundingAccountKey,
                                    epochMs =
                                        transactionAt
                                )
                        )
                    }
                }
            }

        val computedBalanceAfterMinor =
            linkedFunding
                ?.priorBalanceMinor
                ?.let { prior ->
                    max(
                        0L,
                        prior +
                            linkedFunding.amountMinor -
                            repayment.amountMinor
                    )
                }

        val window =
            DebtRepaymentBalancePolicy
                .MAX_LINK_DISTANCE_MS
        val entityClause =
            if (
                repayment.fundingAccountKey == null
            ) {
                "0"
            } else {
                "entity_key = ?"
            }
        val args =
            buildList {
                add(fundingAccountId)
                if (
                    repayment.fundingAccountKey != null
                ) {
                    add(
                        repayment.fundingAccountKey
                    )
                }
                add(
                    (
                        repayment.occurredAtEpochMs -
                            window
                        ).toString()
                )
                add(
                    (
                        repayment.occurredAtEpochMs +
                            window
                        ).toString()
                )
            }.toTypedArray()

        val candidates =
            database.readableDatabase.rawQuery(
                """
                SELECT id, balance_minor,
                       observed_at_epoch_ms
                FROM account_balance_observations
                WHERE (
                        account_id = ?
                        OR $entityClause
                      )
                  AND observed_at_epoch_ms BETWEEN ? AND ?
                ORDER BY ABS(
                    observed_at_epoch_ms - ?
                ) ASC, id DESC
                """.trimIndent(),
                args +
                    repayment
                        .occurredAtEpochMs
                        .toString()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            RepaymentBalanceCandidate(
                                observationId =
                                    cursor.getLong(0),
                                balanceMinor =
                                    cursor.getLong(1),
                                observedAtEpochMs =
                                    cursor.getLong(2)
                            )
                        )
                    }
                }
            }

        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor =
                    repayment
                        .explicitBalanceAfterMinor,
                computedBalanceAfterMinor =
                    computedBalanceAfterMinor,
                repaymentAtEpochMs =
                    repayment.occurredAtEpochMs,
                candidates = candidates
            ) ?: return

        ContentValues().apply {
            put(
                "resulting_balance_observation_id",
                match.observationId
            )
            put(
                "balance_link_confidence",
                match.confidence.name
            )
            put(
                "balance_link_reason",
                match.reason
            )
        }.also { values ->
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ?",
                arrayOf(eventId.toString())
            )
        }
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun reconcileBalanceAfterManualFunding(
        eventId: Long
    ) {
        reconcileRepaymentBalance(eventId)
    }

    private fun reconcileRepaymentFromDebtProceeds(
        eventId: Long
    ) {
        val repayment =
            database.readableDatabase.rawQuery(
                """
                SELECT amount_minor,
                       funding_account_key,
                       debt_account_id,
                       occurred_at_epoch_ms
                FROM debt_events
                WHERE id = ?
                  AND event_type = ?
                  AND related_debt_event_id IS NULL
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
                    DebtRepaymentForProceedsLink(
                        amountMinor =
                            cursor.getLong(0),
                        fundingAccountKey =
                            if (cursor.isNull(1)) {
                                null
                            } else {
                                cursor.getString(1)
                            },
                        debtAccountId =
                            cursor.getString(2),
                        occurredAtEpochMs =
                            cursor.getLong(3)
                    )
                }
            } ?: return

        val fundingAccountKey =
            repayment.fundingAccountKey
                ?: return

        val window =
            DebtProceedsRepaymentLinkPolicy
                .MAX_LINK_DISTANCE_MS
        val candidates =
            database.readableDatabase.rawQuery(
                """
                SELECT id, amount_minor,
                       occurred_at_epoch_ms
                FROM debt_events
                WHERE event_type = ?
                  AND funding_account_key = ?
                  AND debt_account_id != ?
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                ORDER BY occurred_at_epoch_ms DESC, id DESC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.DRAWDOWN.name,
                    fundingAccountKey,
                    repayment.debtAccountId,
                    (
                        repayment.occurredAtEpochMs -
                            window
                    ).toString(),
                    (
                        repayment.occurredAtEpochMs +
                            window
                    ).toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DebtProceedsEventCandidate(
                                debtEventId =
                                    cursor.getLong(0),
                                amountMinor =
                                    cursor.getLong(1),
                                occurredAtEpochMs =
                                    cursor.getLong(2)
                            )
                        )
                    }
                }
            }

        val match =
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor =
                    repayment.amountMinor,
                repaymentAtEpochMs =
                    repayment.occurredAtEpochMs,
                candidates = candidates
            ) ?: return

        ContentValues().apply {
            put(
                "related_debt_event_id",
                match.debtEventId
            )
            put(
                "funding_link_confidence",
                match.confidence.name
            )
            put(
                "funding_link_reason",
                match.reason
            )
        }.also { values ->
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ?",
                arrayOf(eventId.toString())
            )
        }
        LedgerUpdateBus.notifyChanged()
    }

    private fun reconcileRepaymentsFromDebtProceedsAround(
        fundingAccountKey: String,
        occurredAtEpochMs: Long
    ) {
        val window =
            DebtProceedsRepaymentLinkPolicy
                .MAX_LINK_DISTANCE_MS
        val repaymentIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                  AND funding_account_key = ?
                  AND related_debt_event_id IS NULL
                  AND occurred_at_epoch_ms BETWEEN ? AND ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.REPAYMENT.name,
                    fundingAccountKey,
                    (
                        occurredAtEpochMs -
                            window
                    ).toString(),
                    (
                        occurredAtEpochMs +
                            window
                    ).toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        repaymentIds.forEach(
            ::reconcileRepaymentFromDebtProceeds
        )
    }

    @Synchronized
    fun reconcilePendingChainsForFundingAccount(
        accountId: String,
        entityKey: String
    ) {
        ContentValues().apply {
            put("funding_account_id", accountId)
        }.also { values ->
            database.writableDatabase.update(
                "debt_events",
                values,
                "funding_account_key = ?" +
                    " AND funding_account_id IS NULL",
                arrayOf(entityKey)
            )
        }

        val eventIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                  AND funding_account_id = ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.REPAYMENT.name,
                    accountId
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        eventIds.forEach { eventId ->
            reconcileRepaymentFromDebtProceeds(eventId)
            reconcileRepaymentEvent(eventId)
            reconcileRepaymentBalance(eventId)
        }
    }

    private fun ensureEventChainsReconciled() {
        val preferences =
            appContext.getSharedPreferences(
                "pesatrack_preferences",
                Context.MODE_PRIVATE
            )
        if (
            preferences.getBoolean(
                EVENT_CHAIN_RECONCILIATION_KEY,
                false
            )
        ) {
            return
        }

        val unresolvedFunding =
            database.readableDatabase.rawQuery(
                """
                SELECT id, funding_account_key
                FROM debt_events
                WHERE event_type = ?
                  AND funding_account_id IS NULL
                  AND funding_account_key IS NOT NULL
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(DebtEventType.REPAYMENT.name)
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            cursor.getLong(0) to
                                cursor.getString(1)
                        )
                    }
                }
            }

        unresolvedFunding.forEach {
            (eventId, entityKey) ->
            val accountId =
                database.readableDatabase.rawQuery(
                    """
                    SELECT confirmed_account_id
                    FROM financial_entities
                    WHERE entity_key = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(entityKey)
                ).use { cursor ->
                    if (
                        cursor.moveToFirst() &&
                        !cursor.isNull(0)
                    ) {
                        cursor.getString(0)
                    } else {
                        null
                    }
                }
            if (accountId != null) {
                ContentValues().apply {
                    put(
                        "funding_account_id",
                        accountId
                    )
                }.also { values ->
                    database.writableDatabase.update(
                        "debt_events",
                        values,
                        "id = ?",
                        arrayOf(eventId.toString())
                    )
                }
            }
        }

        val repaymentIds =
            database.readableDatabase.rawQuery(
                """
                SELECT id
                FROM debt_events
                WHERE event_type = ?
                ORDER BY occurred_at_epoch_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(DebtEventType.REPAYMENT.name)
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        repaymentIds.forEach { eventId ->
            reconcileRepaymentFromDebtProceeds(eventId)
            reconcileRepaymentEvent(eventId)
            reconcileRepaymentBalance(eventId)
        }

        preferences.edit()
            .putBoolean(
                EVENT_CHAIN_RECONCILIATION_KEY,
                true
            )
            .apply()
    }

    @Synchronized
    fun accounts(): List<DebtAccountSnapshot> {
        ensureEventChainsReconciled()
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
    ): List<DebtEventRecord> {
        ensureEventChainsReconciled()
        return database.readableDatabase.rawQuery(
            """
            SELECT id, debt_account_id, event_type, amount_minor,
                   fee_minor, outstanding_after_minor,
                   linked_transaction_id,
                   related_debt_event_id,
                   wallet_balance_after_minor,
                   funding_account_key,
                   funding_account_id,
                   resulting_balance_observation_id,
                   balance_link_confidence,
                   balance_link_reason,
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
                            relatedDebtEventId =
                                if (cursor.isNull(7)) {
                                    null
                                } else {
                                    cursor.getLong(7)
                                },
                            walletBalanceAfterMinor =
                                if (cursor.isNull(8)) {
                                    null
                                } else {
                                    cursor.getLong(8)
                                },
                            fundingAccountKey =
                                if (cursor.isNull(9)) {
                                    null
                                } else {
                                    cursor.getString(9)
                                },
                            fundingAccountId =
                                if (cursor.isNull(10)) {
                                    null
                                } else {
                                    cursor.getString(10)
                                },
                            resultingBalanceObservationId =
                                if (cursor.isNull(11)) {
                                    null
                                } else {
                                    cursor.getLong(11)
                                },
                            balanceLinkConfidence =
                                if (cursor.isNull(12)) {
                                    null
                                } else {
                                    runCatching {
                                        RepaymentChainConfidence
                                            .valueOf(
                                                cursor.getString(12)
                                            )
                                    }.getOrNull()
                                },
                            balanceLinkReason =
                                if (cursor.isNull(13)) {
                                    null
                                } else {
                                    cursor.getString(13)
                                },
                            availableCreditAfterMinor =
                                if (cursor.isNull(14)) {
                                    null
                                } else {
                                    cursor.getLong(14)
                                },
                            repaymentCompletion =
                                if (cursor.isNull(15)) {
                                    null
                                } else {
                                    runCatching {
                                        DebtCompletion
                                            .valueOf(
                                                cursor.getString(15)
                                            )
                                    }.getOrNull()
                                },
                            fundingLinkConfidence =
                                if (cursor.isNull(16)) {
                                    null
                                } else {
                                    runCatching {
                                        RepaymentChainConfidence
                                            .valueOf(
                                                cursor.getString(16)
                                            )
                                    }.getOrNull()
                                },
                            fundingLinkReason =
                                if (cursor.isNull(17)) {
                                    null
                                } else {
                                    cursor.getString(17)
                                },
                            occurredAtEpochMs =
                                cursor.getLong(18)
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun sumFeesBetween(
        startEpochMs: Long,
        endEpochMsExclusive: Long
    ): Long =
        database.readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM(fee_minor), 0)
            FROM debt_events
            WHERE occurred_at_epoch_ms >= ?
              AND occurred_at_epoch_ms < ?
            """.trimIndent(),
            arrayOf(
                startEpochMs.toString(),
                endEpochMsExclusive.toString()
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                0L
            }
        }

    @Synchronized
    fun reconcilePendingDrawdownsForTransaction(
        transactionId: Long,
        parsed:
            com.kedevjimmy.pesatrack.domain.model
                .ParsedMpesaTransaction,
        occurredAtEpochMs: Long
    ) {
        if (
            parsed.type !in setOf(
                MpesaTransactionType.SENT_MONEY,
                MpesaTransactionType.PAY_BILL,
                MpesaTransactionType.BUY_GOODS,
                MpesaTransactionType.AIRTIME
            )
        ) {
            return
        }

        val pending =
            database.readableDatabase.rawQuery(
                """
                SELECT e.id, e.amount_minor,
                       e.occurred_at_epoch_ms,
                       a.provider_key
                FROM debt_events e
                JOIN debt_accounts a
                  ON a.id = e.debt_account_id
                WHERE e.event_type = ?
                  AND e.linked_transaction_id IS NULL
                  AND e.occurred_at_epoch_ms
                      BETWEEN ? AND ?
                ORDER BY ABS(
                    e.occurred_at_epoch_ms - ?
                ) ASC
                """.trimIndent(),
                arrayOf(
                    DebtEventType.DRAWDOWN.name,
                    (
                        occurredAtEpochMs -
                            DRAW_DOWN_LINK_WINDOW_MS
                    ).toString(),
                    (
                        occurredAtEpochMs +
                            DRAW_DOWN_LINK_WINDOW_MS
                    ).toString(),
                    occurredAtEpochMs.toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            PendingDebtDrawdown(
                                eventId =
                                    cursor.getLong(0),
                                financedMinor =
                                    cursor.getLong(1),
                                occurredAtEpochMs =
                                    cursor.getLong(2),
                                providerKey =
                                    cursor.getString(3)
                            )
                        )
                    }
                }
            }

        val candidate = DebtFundingCandidate(
            transactionId = transactionId,
            occurredAtEpochMs = occurredAtEpochMs,
            amountMinor = parsed.amountMinor,
            transactionFeeMinor = parsed.feeMinor
        )

        val eligible = pending.filter { item ->
            val reference =
                debtAccountReferenceForProvider(
                    item.providerKey
                )
            val behavior =
                FinancialProviderRegistry
                    .debtBehaviorFor(reference)

            behavior?.fundingBehavior ==
                DebtFundingBehavior
                    .TRANSACTION_SHORTFALL &&
                DebtFundingPairingPolicy
                    .chooseTransactionShortfallCandidate(
                        financedMinor =
                            item.financedMinor,
                        financedAtEpochMs =
                            item.occurredAtEpochMs,
                        candidates =
                            listOf(candidate)
                    ) == transactionId
        }

        val totalRequired =
            parsed.amountMinor +
                parsed.feeMinor
        val exact = eligible.filter {
            it.financedMinor == totalRequired
        }
        val selected = when {
            exact.size == 1 -> exact.single()
            exact.isEmpty() &&
                eligible.size == 1 ->
                eligible.single()
            else -> null
        } ?: return

        attachDrawdownToTransaction(
            eventId = selected.eventId,
            transactionId = transactionId
        )
    }

    private fun findDrawdownFundingTransaction(
        financedAmountMinor: Long,
        occurredAtEpochMs: Long
    ): Long? {
        val candidates =
            database.readableDatabase.rawQuery(
                """
                SELECT id, occurred_at_epoch_ms,
                       amount_minor, fee_minor
                FROM transactions
                WHERE type IN (?, ?, ?, ?)
                  AND occurred_at_epoch_ms
                      BETWEEN ? AND ?
                  AND review_state != ?
                ORDER BY occurred_at_epoch_ms DESC
                """.trimIndent(),
                arrayOf(
                    MpesaTransactionType.SENT_MONEY.name,
                    MpesaTransactionType.PAY_BILL.name,
                    MpesaTransactionType.BUY_GOODS.name,
                    MpesaTransactionType.AIRTIME.name,
                    (
                        occurredAtEpochMs -
                            DRAW_DOWN_LINK_WINDOW_MS
                    ).toString(),
                    (
                        occurredAtEpochMs +
                            DRAW_DOWN_LINK_WINDOW_MS
                    ).toString(),
                    com.kedevjimmy.pesatrack.domain.model
                        .ReviewState.EXCLUDED.name
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DebtFundingCandidate(
                                transactionId =
                                    cursor.getLong(0),
                                occurredAtEpochMs =
                                    cursor.getLong(1),
                                amountMinor =
                                    cursor.getLong(2),
                                transactionFeeMinor =
                                    cursor.getLong(3)
                            )
                        )
                    }
                }
            }

        return DebtFundingPairingPolicy
            .chooseTransactionShortfallCandidate(
                financedMinor =
                    financedAmountMinor,
                financedAtEpochMs =
                    occurredAtEpochMs,
                candidates = candidates
            )
    }

    private fun attachDrawdownToTransaction(
        eventId: Long,
        transactionId: Long
    ) {
        ContentValues().apply {
            put(
                "linked_transaction_id",
                transactionId
            )
        }.also { values ->
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ?",
                arrayOf(eventId.toString())
            )
        }

        val metadata =
            database.readableDatabase.rawQuery(
                """
                SELECT e.amount_minor, e.fee_minor,
                       e.outstanding_after_minor,
                       a.provider_key
                FROM debt_events e
                JOIN debt_accounts a
                  ON a.id = e.debt_account_id
                WHERE e.id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(eventId.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    LegacyFundingMetadata(
                        financedMinor =
                            cursor.getLong(0),
                        feeMinor =
                            cursor.getLong(1),
                        outstandingMinor =
                            if (cursor.isNull(2)) {
                                null
                            } else {
                                cursor.getLong(2)
                            },
                        providerKey =
                            cursor.getString(3)
                    )
                }
            } ?: return

        val reference =
            debtAccountReferenceForProvider(
                metadata.providerKey
            )
        val behavior =
            FinancialProviderRegistry
                .debtBehaviorFor(reference)

        if (
            behavior
                ?.preserveLegacyTransactionMetadata ==
                true
        ) {
            ContentValues().apply {
                put(
                    "fuliza_financed_minor",
                    metadata.financedMinor
                )
                put(
                    "fuliza_access_fee_minor",
                    metadata.feeMinor
                )
                if (
                    metadata.outstandingMinor != null
                ) {
                    put(
                        "fuliza_outstanding_minor",
                        metadata.outstandingMinor
                    )
                } else {
                    putNull(
                        "fuliza_outstanding_minor"
                    )
                }
            }.also { values ->
                database.writableDatabase.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(
                        transactionId.toString()
                    )
                )
            }
        }

        LedgerUpdateBus.notifyChanged()
    }

    private fun applyLegacyTransactionFundingMetadata(
        transactionId: Long,
        event: DebtDrawdownEventFact,
        transactionCode: String?
    ) {
        ContentValues().apply {
            put(
                "fuliza_financed_minor",
                event.amountMinor
            )
            put(
                "fuliza_access_fee_minor",
                event.feeMinor
            )
            if (
                event.outstandingAfterMinor != null
            ) {
                put(
                    "fuliza_outstanding_minor",
                    event.outstandingAfterMinor
                )
            } else {
                putNull(
                    "fuliza_outstanding_minor"
                )
            }
            if (
                !transactionCode.isNullOrBlank()
            ) {
                put(
                    "fuliza_event_code",
                    transactionCode
                )
            }
        }.also { values ->
            database.writableDatabase.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(transactionId.toString())
            )
        }
    }

    private fun linkedTransactionId(
        eventId: Long
    ): Long? =
        database.readableDatabase.rawQuery(
            """
            SELECT linked_transaction_id
            FROM debt_events
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(eventId.toString())
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

    private fun debtAccountIdentity(
        reference: FinancialAccountReference
    ): DebtAccountIdentity {
        val providerKey =
            reference.providerKey
                ?: reference.entityKey

        val id =
            FinancialProviderRegistry
                .debtBehaviorFor(reference)
                ?.stableDebtAccountId
                ?: (
                    "debt-" +
                        sha256(providerKey)
                            .take(20)
                    )

        return DebtAccountIdentity(
            id = id,
            providerKey = providerKey,
            name = reference.displayName
        )
    }

    private fun debtAccountReferenceForProvider(
        providerKey: String
    ): FinancialAccountReference =
        FinancialProviderRegistry
            .referenceForProviderKey(providerKey)
            ?: FinancialProviderRegistry
                .resolveAccount(
                    providerKey
                        .removePrefix(
                            "discovered:"
                        )
                        .replace('-', ' ')
                )
                .copy(
                    nature =
                        com.kedevjimmy.pesatrack.domain
                            .interpretation
                            .FinancialAccountNature.LIABILITY
                )

    private fun sha256(
        value: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") { byte ->
                "%02x".format(byte)
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
        fundingAccountId: String?,
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
                if (fundingAccountId != null) {
                    put(
                        "funding_account_id",
                        fundingAccountId
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
            if (fundingAccountId != null) {
                put(
                    "funding_account_id",
                    fundingAccountId
                )
            } else {
                putNull("funding_account_id")
            }
            putNull(
                "resulting_balance_observation_id"
            )
            putNull("balance_link_confidence")
            putNull("balance_link_reason")
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

    private fun latestBalanceBefore(
        accountId: String,
        entityKey: String?,
        epochMs: Long
    ): Long? {
        val entityClause =
            if (entityKey == null) {
                "0"
            } else {
                "entity_key = ?"
            }
        val args =
            buildList {
                add(accountId)
                add(epochMs.toString())
                add(accountId)
                add(epochMs.toString())
                if (entityKey != null) {
                    add(entityKey)
                }
                add(epochMs.toString())
            }.toTypedArray()

        return database.readableDatabase.rawQuery(
            """
            SELECT balance_minor
            FROM (
                SELECT balance_minor,
                       occurred_at_epoch_ms AS observed_at,
                       0 AS priority
                FROM transactions
                WHERE balance_minor IS NOT NULL
                  AND account_id = ?
                  AND occurred_at_epoch_ms < ?

                UNION ALL

                SELECT balance_minor,
                       observed_at_epoch_ms AS observed_at,
                       1 AS priority
                FROM account_balance_observations
                WHERE account_id = ?
                  AND observed_at_epoch_ms < ?

                UNION ALL

                SELECT balance_minor,
                       observed_at_epoch_ms AS observed_at,
                       2 AS priority
                FROM account_balance_observations
                WHERE $entityClause
                  AND observed_at_epoch_ms < ?
            )
            ORDER BY observed_at DESC, priority DESC
            LIMIT 1
            """.trimIndent(),
            args
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    private fun debtEventPrefix(
        reference: FinancialAccountReference,
        suffix: String
    ): String {
        val behavior =
            FinancialProviderRegistry
                .debtBehaviorFor(reference)
        val namespace =
            behavior?.eventNamespace
                ?: (
                    "debt-" +
                        (
                            reference.providerKey
                                ?: reference.entityKey
                            )
                )
        return namespace + "-" + suffix
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

    private data class PaymentForLiabilityLink(
        val amountMinor: Long,
        val counterparty: String,
        val accountId: String,
        val balanceMinor: Long?,
        val occurredAtEpochMs: Long,
        val transactionCode: String?,
        val sourcePackage: String?,
        val sourceKind: String
    )

    private data class DebtAccountIdentity(
        val id: String,
        val providerKey: String,
        val name: String
    )

    private data class PendingDebtDrawdown(
        val eventId: Long,
        val financedMinor: Long,
        val occurredAtEpochMs: Long,
        val providerKey: String
    )

    private data class LegacyFundingMetadata(
        val financedMinor: Long,
        val feeMinor: Long,
        val outstandingMinor: Long?,
        val providerKey: String
    )

    private data class DebtRepaymentForProceedsLink(
        val amountMinor: Long,
        val fundingAccountKey: String?,
        val debtAccountId: String,
        val occurredAtEpochMs: Long
    )

    private data class RepaymentEventForLinking(
        val amountMinor: Long,
        val walletBalanceAfterMinor: Long?,
        val fundingAccountKey: String?,
        val fundingAccountId: String?,
        val occurredAtEpochMs: Long
    )

    private data class RepaymentBalanceContext(
        val amountMinor: Long,
        val explicitBalanceAfterMinor: Long?,
        val fundingAccountKey: String?,
        val fundingAccountId: String?,
        val occurredAtEpochMs: Long,
        val linkedTransactionId: Long?,
        val resultingBalanceObservationId: Long?
    )

    private data class RepaymentFundingForBalance(
        val amountMinor: Long,
        val occurredAtEpochMs: Long,
        val priorBalanceMinor: Long?
    )

    private data class DebtEventInsertResult(
        val id: Long,
        val inserted: Boolean
    )

    companion object {
        private const val REPAYMENT_LINK_WINDOW_MS =
            10L * 60L * 1000L
        private const val DRAW_DOWN_LINK_WINDOW_MS =
            3L * 60L * 1000L
        private const val EVENT_CHAIN_RECONCILIATION_KEY =
            "financial_event_chain_reconciliation_v1"
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
