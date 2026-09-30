package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentAction
import com.kedevjimmy.pesatrack.domain.rules.DebtRepaymentBalancePolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtRepaymentChainPolicy
import com.kedevjimmy.pesatrack.domain.rules.RepaymentChainConfidence
import com.kedevjimmy.pesatrack.domain.rules.InvestmentSettlementReviewPolicy
import kotlin.math.abs
import kotlin.math.max

enum class RelationshipReviewKind {
    REPAYMENT_FUNDING,
    REPAYMENT_BALANCE,
    LIABILITY_PAYMENT,
    INVESTMENT_SETTLEMENT
}

data class RelationshipReviewCandidate(
    val targetId: Long? = null,
    val targetKey: String? = null,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val label: String,
    val reason: String
)

data class RelationshipReviewItem(
    val key: String,
    val kind: RelationshipReviewKind,
    val debtEventId: Long? = null,
    val transactionId: Long? = null,
    val investmentEventId: Long? = null,
    val debtName: String,
    val fundingAccountName: String,
    val repaymentAmountMinor: Long,
    val occurredAtEpochMs: Long,
    val candidates: List<RelationshipReviewCandidate>
)

/**
 * Manual safety valve for relationships the automatic engine
 * intentionally leaves unresolved.
 *
 * This store never invents candidates. It only exposes records that
 * already satisfy the provider-neutral account/time constraints used
 * by the deterministic debt-chain engine.
 */
class RelationshipReviewStore private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)

    @Synchronized
    fun pendingReviews(): List<RelationshipReviewItem> =
        (
            pendingFundingReviews() +
                pendingBalanceReviews() +
                pendingLiabilityPaymentReviews() +
                pendingInvestmentSettlementReviews()
            )
            .sortedByDescending {
                it.occurredAtEpochMs
            }

    @Synchronized
    fun resolveFunding(
        debtEventId: Long,
        transactionId: Long?
    ): Boolean {
        val item =
            pendingFundingReviews()
                .firstOrNull {
                    it.debtEventId == debtEventId
                }
                ?: return false

        if (
            transactionId != null &&
            item.candidates.none {
                it.targetId == transactionId
            }
        ) {
            return false
        }

        val values = ContentValues().apply {
            if (transactionId == null) {
                put(
                    "funding_link_user_decision",
                    DECISION_KEEP_UNLINKED
                )
                putNull("linked_transaction_id")
                putNull("funding_link_confidence")
                put(
                    "funding_link_reason",
                    "User confirmed that the nearby incoming transactions are not the funding source for this repayment."
                )
            } else {
                put(
                    "funding_link_user_decision",
                    DECISION_LINKED
                )
                put(
                    "linked_transaction_id",
                    transactionId
                )
                put(
                    "funding_link_confidence",
                    RepaymentChainConfidence
                        .USER_CONFIRMED
                        .name
                )
                put(
                    "funding_link_reason",
                    "User confirmed this incoming transaction funded the debt repayment."
                )
            }
        }

        val changed =
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ? AND event_type = ?",
                arrayOf(
                    debtEventId.toString(),
                    DebtEventType.REPAYMENT.name
                )
            ) > 0

        if (changed) {
            if (transactionId != null) {
                DebtStore.get(appContext)
                    .reconcileBalanceAfterManualFunding(
                        debtEventId
                    )
            }
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }

    @Synchronized
    fun resolveBalance(
        debtEventId: Long,
        observationId: Long?
    ): Boolean {
        val item =
            pendingBalanceReviews()
                .firstOrNull {
                    it.debtEventId == debtEventId
                }
                ?: return false

        if (
            observationId != null &&
            item.candidates.none {
                it.targetId == observationId
            }
        ) {
            return false
        }

        val values = ContentValues().apply {
            if (observationId == null) {
                put(
                    "balance_link_user_decision",
                    DECISION_KEEP_UNLINKED
                )
                putNull(
                    "resulting_balance_observation_id"
                )
                putNull("balance_link_confidence")
                put(
                    "balance_link_reason",
                    "User confirmed that none of the nearby balance observations is the resulting balance for this repayment."
                )
            } else {
                put(
                    "balance_link_user_decision",
                    DECISION_LINKED
                )
                put(
                    "resulting_balance_observation_id",
                    observationId
                )
                put(
                    "balance_link_confidence",
                    RepaymentChainConfidence
                        .USER_CONFIRMED
                        .name
                )
                put(
                    "balance_link_reason",
                    "User confirmed this balance observation is the resulting balance after the debt repayment."
                )
            }
        }

        val changed =
            database.writableDatabase.update(
                "debt_events",
                values,
                "id = ? AND event_type = ?",
                arrayOf(
                    debtEventId.toString(),
                    DebtEventType.REPAYMENT.name
                )
            ) > 0

        if (changed) {
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }

    @Synchronized
    fun resolveLiabilityPayment(
        transactionId: Long,
        debtAccountId: String?
    ): Boolean {
        val item =
            pendingLiabilityPaymentReviews()
                .firstOrNull {
                    it.transactionId ==
                        transactionId
                } ?: return false

        if (
            debtAccountId != null &&
            item.candidates.none {
                it.targetKey == debtAccountId
            }
        ) {
            return false
        }

        if (debtAccountId != null) {
            val linked =
                DebtStore.get(appContext)
                    .recordPaymentAsRepayment(
                        transactionId =
                            transactionId,
                        debtAccountId =
                            debtAccountId,
                        userConfirmed = true
                    )
            if (!linked) {
                return false
            }
        }

        val values = ContentValues().apply {
            put(
                "transaction_id",
                transactionId
            )
            put(
                "decision",
                if (debtAccountId == null) {
                    DECISION_KEEP_UNLINKED
                } else {
                    DECISION_LINKED
                }
            )
            if (debtAccountId == null) {
                putNull("debt_account_id")
            } else {
                put(
                    "debt_account_id",
                    debtAccountId
                )
            }
            put(
                "created_at_epoch_ms",
                System.currentTimeMillis()
            )
        }

        val stored =
            database.writableDatabase
                .insertWithOnConflict(
                    "liability_payment_decisions",
                    null,
                    values,
                    android.database.sqlite
                        .SQLiteDatabase
                        .CONFLICT_REPLACE
                ) != -1L

        if (stored) {
            LedgerUpdateBus.notifyChanged()
        }
        return stored
    }

    @Synchronized
    fun resolveInvestmentSettlement(
        investmentEventId: Long,
        transactionId: Long?
    ): Boolean {
        val item =
            pendingInvestmentSettlementReviews()
                .firstOrNull {
                    it.investmentEventId ==
                        investmentEventId
                }
                ?: return false

        if (
            transactionId != null &&
            item.candidates.none {
                it.targetId == transactionId
            }
        ) {
            return false
        }

        return InvestmentStore.get(appContext)
            .resolveManualSettlement(
                eventId = investmentEventId,
                transactionId = transactionId
            )
    }

    private fun pendingInvestmentSettlementReviews():
        List<RelationshipReviewItem> =
        database.readableDatabase.rawQuery(
            """
            SELECT id,
                   event_type,
                   instrument_label,
                   value_minor,
                   occurred_at_epoch_ms
            FROM investment_events
            WHERE linked_transaction_id IS NULL
              AND settlement_user_decision IS NULL
              AND event_type IN (?, ?)
              AND value_minor IS NOT NULL
              AND value_minor > 0
            ORDER BY occurred_at_epoch_ms DESC,
                     id DESC
            LIMIT 200
            """.trimIndent(),
            arrayOf(
                InvestmentEventType
                    .BUY_EXECUTED.name,
                InvestmentEventType
                    .SELL_EXECUTED.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val event =
                        InvestmentSettlementForReview(
                            id = cursor.getLong(0),
                            eventType =
                                InvestmentEventType
                                    .valueOf(
                                        cursor.getString(1)
                                    ),
                            instrumentLabel =
                                cursor.getString(2),
                            valueMinor =
                                cursor.getLong(3),
                            occurredAtEpochMs =
                                cursor.getLong(4)
                        )
                    val candidates =
                        investmentSettlementCandidates(
                            event
                        )
                    if (candidates.isEmpty()) {
                        continue
                    }

                    add(
                        RelationshipReviewItem(
                            key =
                                "investment-settlement-" +
                                    event.id,
                            kind =
                                RelationshipReviewKind
                                    .INVESTMENT_SETTLEMENT,
                            investmentEventId =
                                event.id,
                            debtName =
                                event.instrumentLabel,
                            fundingAccountName =
                                when (
                                    event.eventType
                                ) {
                                    InvestmentEventType
                                        .BUY_EXECUTED ->
                                        "Executed buy"
                                    InvestmentEventType
                                        .SELL_EXECUTED ->
                                        "Executed sale"
                                    InvestmentEventType
                                        .ORDER_SUBMITTED ->
                                        "Investment order"
                                },
                            repaymentAmountMinor =
                                event.valueMinor,
                            occurredAtEpochMs =
                                event.occurredAtEpochMs,
                            candidates = candidates
                        )
                    )
                }
            }
        }

    private fun investmentSettlementCandidates(
        event: InvestmentSettlementForReview
    ): List<RelationshipReviewCandidate> {
        val expectedDirection =
            when (event.eventType) {
                InvestmentEventType.BUY_EXECUTED ->
                    MoneyDirection.EXPENSE
                InvestmentEventType.SELL_EXECUTED ->
                    MoneyDirection.INCOME
                InvestmentEventType.ORDER_SUBMITTED ->
                    return emptyList()
            }

        return database.readableDatabase.rawQuery(
            """
            SELECT
                t.id,
                t.amount_minor,
                t.occurred_at_epoch_ms,
                COALESCE(
                    NULLIF(TRIM(t.counterparty), ''),
                    NULLIF(TRIM(t.transaction_code), ''),
                    NULLIF(TRIM(a.name), ''),
                    t.source_kind
                ),
                EXISTS(
                    SELECT 1
                    FROM investment_event_references ir
                    JOIN transaction_external_references tr
                      ON tr.reference_value =
                            ir.reference_value
                    WHERE ir.investment_event_id = ?
                      AND tr.transaction_id = t.id
                )
            FROM transactions t
            LEFT JOIN money_accounts a
              ON a.id = t.account_id
            WHERE t.amount_minor = ?
              AND t.direction = ?
              AND t.rule_id IS NULL
              AND t.relation_type IS NULL
              AND t.review_state != ?
              AND t.review_state != ?
              AND t.id NOT IN (
                    SELECT linked_transaction_id
                    FROM investment_events
                    WHERE linked_transaction_id
                        IS NOT NULL
              )
              AND t.id NOT IN (
                    SELECT linked_transaction_id
                    FROM debt_events
                    WHERE linked_transaction_id
                        IS NOT NULL
              )
              AND (
                    EXISTS(
                        SELECT 1
                        FROM investment_event_references ir
                        JOIN transaction_external_references tr
                          ON tr.reference_value =
                                ir.reference_value
                        WHERE ir.investment_event_id = ?
                          AND tr.transaction_id = t.id
                    )
                    OR t.occurred_at_epoch_ms
                        BETWEEN ? AND ?
                  )
            ORDER BY
                EXISTS(
                    SELECT 1
                    FROM investment_event_references ir
                    JOIN transaction_external_references tr
                      ON tr.reference_value =
                            ir.reference_value
                    WHERE ir.investment_event_id = ?
                      AND tr.transaction_id = t.id
                ) DESC,
                ABS(
                    t.occurred_at_epoch_ms -
                        ?
                ) ASC,
                t.id DESC
            LIMIT 8
            """.trimIndent(),
            arrayOf(
                event.id.toString(),
                event.valueMinor.toString(),
                expectedDirection.name,
                ReviewState.USER_CLASSIFIED.name,
                ReviewState.EXCLUDED.name,
                event.id.toString(),
                (
                    event.occurredAtEpochMs -
                        InvestmentSettlementReviewPolicy
                            .MAX_TIME_DELTA_MS
                    ).toString(),
                (
                    event.occurredAtEpochMs +
                        InvestmentSettlementReviewPolicy
                            .MAX_TIME_DELTA_MS
                    ).toString(),
                event.id.toString(),
                event.occurredAtEpochMs
                    .toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val transactionId =
                        cursor.getLong(0)
                    val amountMinor =
                        cursor.getLong(1)
                    val occurredAt =
                        cursor.getLong(2)
                    val label =
                        cursor.getString(3)
                    val sharesReference =
                        cursor.getInt(4) != 0
                    val action =
                        if (
                            event.eventType ==
                                InvestmentEventType
                                    .BUY_EXECUTED
                        ) {
                            SemanticInvestmentAction.BUY
                        } else {
                            SemanticInvestmentAction.SELL
                        }
                    val assessment =
                        InvestmentSettlementReviewPolicy
                            .assess(
                                action = action,
                                investmentValueMinor =
                                    event.valueMinor,
                                investmentOccurredAtEpochMs =
                                    event.occurredAtEpochMs,
                                transactionAmountMinor =
                                    amountMinor,
                                transactionDirection =
                                    expectedDirection.name,
                                transactionOccurredAtEpochMs =
                                    occurredAt,
                                sharesExactReference =
                                    sharesReference,
                                protected = false
                            )
                    if (!assessment.eligible) {
                        continue
                    }

                    add(
                        RelationshipReviewCandidate(
                            targetId =
                                transactionId,
                            amountMinor =
                                amountMinor,
                            occurredAtEpochMs =
                                occurredAt,
                            label = label,
                            reason =
                                assessment.reason
                                    ?: "Plausible investment settlement candidate."
                        )
                    )
                }
            }
        }
    }

    private fun pendingLiabilityPaymentReviews():
        List<RelationshipReviewItem> {
        val debtStore =
            DebtStore.get(appContext)
        val debts =
            debtStore.accounts()
                .associateBy { it.id }

        return database.readableDatabase.rawQuery(
            """
            SELECT
                t.id,
                t.amount_minor,
                t.occurred_at_epoch_ms,
                t.counterparty,
                COALESCE(a.name, 'Payment account')
            FROM transactions t
            LEFT JOIN money_accounts a
              ON a.id = t.account_id
            WHERE t.direction = ?
              AND t.counterparty IS NOT NULL
              AND TRIM(t.counterparty) != ''
              AND t.rule_id IS NULL
              AND t.relation_type IS NULL
              AND t.review_state != ?
              AND t.review_state != ?
              AND t.id NOT IN (
                    SELECT linked_transaction_id
                    FROM debt_events
                    WHERE linked_transaction_id
                        IS NOT NULL
              )
              AND t.id NOT IN (
                    SELECT transaction_id
                    FROM liability_payment_decisions
              )
            ORDER BY t.occurred_at_epoch_ms DESC,
                     t.id DESC
            LIMIT 200
            """.trimIndent(),
            arrayOf(
                MoneyDirection.EXPENSE.name,
                ReviewState.USER_CLASSIFIED.name,
                ReviewState.EXCLUDED.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val transactionId =
                        cursor.getLong(0)
                    val amountMinor =
                        cursor.getLong(1)
                    val occurredAt =
                        cursor.getLong(2)
                    val counterparty =
                        cursor.getString(3)
                    val fundingAccountName =
                        cursor.getString(4)

                    val matches =
                        debtStore.liabilityPaymentMatches(
                            counterparty =
                                counterparty,
                            amountMinor =
                                amountMinor
                        )
                    if (matches.isEmpty()) {
                        continue
                    }

                    val candidates =
                        matches.mapNotNull { match ->
                            val debt =
                                debts[
                                    match.debtAccountId
                                ] ?: return@mapNotNull null
                            RelationshipReviewCandidate(
                                targetKey =
                                    debt.id,
                                amountMinor =
                                    amountMinor,
                                occurredAtEpochMs =
                                    occurredAt,
                                label = debt.name,
                                reason =
                                    match.reason
                            )
                        }

                    if (candidates.isEmpty()) {
                        continue
                    }

                    add(
                        RelationshipReviewItem(
                            key =
                                "liability-payment:" +
                                    transactionId,
                            kind =
                                RelationshipReviewKind
                                    .LIABILITY_PAYMENT,
                            transactionId =
                                transactionId,
                            debtName =
                                counterparty,
                            fundingAccountName =
                                fundingAccountName,
                            repaymentAmountMinor =
                                amountMinor,
                            occurredAtEpochMs =
                                occurredAt,
                            candidates = candidates
                        )
                    )
                }
            }
        }
    }

    private fun pendingFundingReviews():
        List<RelationshipReviewItem> =
        unresolvedRepayments(
            requireFundingUnresolved = true
        ).mapNotNull { repayment ->
            val candidates =
                fundingCandidates(repayment)

            if (candidates.isEmpty()) {
                return@mapNotNull null
            }

            RelationshipReviewItem(
                key =
                    "repayment-funding:" +
                        repayment.id,
                kind =
                    RelationshipReviewKind
                        .REPAYMENT_FUNDING,
                debtEventId = repayment.id,
                debtName = repayment.debtName,
                fundingAccountName =
                    repayment.fundingAccountName,
                repaymentAmountMinor =
                    repayment.amountMinor,
                occurredAtEpochMs =
                    repayment.occurredAtEpochMs,
                candidates = candidates
            )
        }

    private fun pendingBalanceReviews():
        List<RelationshipReviewItem> =
        unresolvedRepayments(
            requireFundingUnresolved = false
        ).mapNotNull { repayment ->
            if (
                repayment.linkedTransactionId == null ||
                repayment
                    .resultingBalanceObservationId !=
                    null ||
                repayment
                    .balanceLinkUserDecision != null
            ) {
                return@mapNotNull null
            }

            val target =
                repayment.explicitBalanceAfterMinor
                    ?: computedBalanceTarget(
                        repayment
                    )
                    ?: return@mapNotNull null

            val candidates =
                balanceCandidates(
                    repayment = repayment,
                    targetMinor = target
                )

            /*
             * A single valid balance should normally have been linked
             * automatically. Surface only cases that remain unresolved
             * so the user can choose among competing observations or
             * explicitly reject the remaining candidate.
             */
            if (candidates.isEmpty()) {
                return@mapNotNull null
            }

            RelationshipReviewItem(
                key =
                    "repayment-balance:" +
                        repayment.id,
                kind =
                    RelationshipReviewKind
                        .REPAYMENT_BALANCE,
                debtEventId = repayment.id,
                debtName = repayment.debtName,
                fundingAccountName =
                    repayment.fundingAccountName,
                repaymentAmountMinor =
                    repayment.amountMinor,
                occurredAtEpochMs =
                    repayment.occurredAtEpochMs,
                candidates = candidates
            )
        }

    private fun unresolvedRepayments(
        requireFundingUnresolved: Boolean
    ): List<RepaymentForReview> {
        val condition =
            if (requireFundingUnresolved) {
                """
                AND e.linked_transaction_id IS NULL
                AND e.funding_link_user_decision IS NULL
                """.trimIndent()
            } else {
                """
                AND e.linked_transaction_id IS NOT NULL
                AND e.resulting_balance_observation_id IS NULL
                AND e.balance_link_user_decision IS NULL
                """.trimIndent()
            }

        return database.readableDatabase.rawQuery(
            """
            SELECT
                e.id,
                e.amount_minor,
                e.wallet_balance_after_minor,
                e.funding_account_key,
                e.funding_account_id,
                e.linked_transaction_id,
                e.resulting_balance_observation_id,
                e.balance_link_user_decision,
                e.occurred_at_epoch_ms,
                d.name,
                COALESCE(
                    a.name,
                    f.display_name,
                    'Funding account'
                )
            FROM debt_events e
            JOIN debt_accounts d
              ON d.id = e.debt_account_id
            LEFT JOIN money_accounts a
              ON a.id = e.funding_account_id
            LEFT JOIN financial_entities f
              ON f.entity_key =
                    e.funding_account_key
            WHERE e.event_type = ?
              AND e.funding_account_id IS NOT NULL
              $condition
            ORDER BY e.occurred_at_epoch_ms DESC,
                     e.id DESC
            LIMIT 100
            """.trimIndent(),
            arrayOf(DebtEventType.REPAYMENT.name)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RepaymentForReview(
                            id = cursor.getLong(0),
                            amountMinor =
                                cursor.getLong(1),
                            explicitBalanceAfterMinor =
                                if (cursor.isNull(2)) {
                                    null
                                } else {
                                    cursor.getLong(2)
                                },
                            fundingAccountKey =
                                if (cursor.isNull(3)) {
                                    null
                                } else {
                                    cursor.getString(3)
                                },
                            fundingAccountId =
                                cursor.getString(4),
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
                                },
                            balanceLinkUserDecision =
                                if (cursor.isNull(7)) {
                                    null
                                } else {
                                    cursor.getString(7)
                                },
                            occurredAtEpochMs =
                                cursor.getLong(8),
                            debtName =
                                cursor.getString(9),
                            fundingAccountName =
                                cursor.getString(10)
                        )
                    )
                }
            }
        }
    }

    private fun fundingCandidates(
        repayment: RepaymentForReview
    ): List<RelationshipReviewCandidate> =
        database.readableDatabase.rawQuery(
            """
            SELECT
                id,
                amount_minor,
                occurred_at_epoch_ms,
                counterparty,
                source_kind,
                transaction_code
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
                    WHERE linked_transaction_id
                        IS NOT NULL
              )
            ORDER BY ABS(
                occurred_at_epoch_ms - ?
            ) ASC, id DESC
            LIMIT 8
            """.trimIndent(),
            arrayOf(
                MoneyDirection.INCOME.name,
                repayment.fundingAccountId,
                MoneyDirection.TRANSFER.name,
                repayment.fundingAccountId,
                (
                    repayment.occurredAtEpochMs -
                        DebtRepaymentChainPolicy
                            .MAX_LINK_DISTANCE_MS
                    ).toString(),
                (
                    repayment.occurredAtEpochMs +
                        DebtRepaymentChainPolicy
                            .MAX_LINK_DISTANCE_MS
                    ).toString(),
                ReviewState.EXCLUDED.name,
                repayment.occurredAtEpochMs
                    .toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val amount = cursor.getLong(1)
                    val occurredAt =
                        cursor.getLong(2)
                    val counterparty =
                        if (cursor.isNull(3)) {
                            null
                        } else {
                            cursor.getString(3)
                        }
                    val sourceKind =
                        cursor.getString(4)
                    val code =
                        if (cursor.isNull(5)) {
                            null
                        } else {
                            cursor.getString(5)
                        }
                    val distance =
                        abs(
                            repayment.occurredAtEpochMs -
                                occurredAt
                        )

                    add(
                        RelationshipReviewCandidate(
                            targetId = id,
                            amountMinor = amount,
                            occurredAtEpochMs =
                                occurredAt,
                            label =
                                counterparty
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: code
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                    ?: sourceKind,
                            reason =
                                when {
                                    amount ==
                                        repayment
                                            .amountMinor ->
                                        "Same amount as the debt repayment."
                                    distance <=
                                        2L * 60L *
                                            1000L ->
                                        "Incoming funds occurred within two minutes of the repayment."
                                    else ->
                                        "Incoming funds reached the same funding account inside the ten-minute review window."
                                }
                        )
                    )
                }
            }
        }

    private fun computedBalanceTarget(
        repayment: RepaymentForReview
    ): Long? {
        val transactionId =
            repayment.linkedTransactionId
                ?: return null

        val funding =
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
                    repayment.fundingAccountId,
                    repayment.fundingAccountId
                )
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    cursor.getLong(0) to
                        cursor.getLong(1)
                }
            } ?: return null

        val prior =
            latestBalanceBefore(
                accountId =
                    repayment.fundingAccountId,
                entityKey =
                    repayment.fundingAccountKey,
                epochMs = funding.second
            ) ?: return null

        return max(
            0L,
            prior +
                funding.first -
                repayment.amountMinor
        )
    }

    private fun balanceCandidates(
        repayment: RepaymentForReview,
        targetMinor: Long
    ): List<RelationshipReviewCandidate> {
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
                add(repayment.fundingAccountId)
                if (
                    repayment.fundingAccountKey != null
                ) {
                    add(
                        repayment.fundingAccountKey
                    )
                }
                add(targetMinor.toString())
                add(
                    (
                        repayment.occurredAtEpochMs -
                            DebtRepaymentBalancePolicy
                                .MAX_LINK_DISTANCE_MS
                        ).toString()
                )
                add(
                    (
                        repayment.occurredAtEpochMs +
                            DebtRepaymentBalancePolicy
                                .MAX_LINK_DISTANCE_MS
                        ).toString()
                )
                add(
                    repayment.occurredAtEpochMs
                        .toString()
                )
            }.toTypedArray()

        return database.readableDatabase.rawQuery(
            """
            SELECT id, balance_minor,
                   observed_at_epoch_ms
            FROM account_balance_observations
            WHERE (
                    account_id = ?
                    OR $entityClause
                  )
              AND balance_minor = ?
              AND observed_at_epoch_ms BETWEEN ? AND ?
            ORDER BY ABS(
                observed_at_epoch_ms - ?
            ) ASC, id DESC
            LIMIT 8
            """.trimIndent(),
            args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val balance = cursor.getLong(1)
                    val occurredAt =
                        cursor.getLong(2)
                    add(
                        RelationshipReviewCandidate(
                            targetId = id,
                            amountMinor = balance,
                            occurredAtEpochMs =
                                occurredAt,
                            label =
                                repayment
                                    .fundingAccountName +
                                    " balance",
                            reason =
                                "This observed balance matches the expected balance after the repayment."
                        )
                    )
                }
            }
        }
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
                if (entityKey != null) {
                    add(entityKey)
                }
                add(epochMs.toString())
            }.toTypedArray()

        return database.readableDatabase.rawQuery(
            """
            SELECT balance_minor
            FROM account_balance_observations
            WHERE (
                    account_id = ?
                    OR $entityClause
                  )
              AND observed_at_epoch_ms <= ?
            ORDER BY observed_at_epoch_ms DESC,
                     id DESC
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

    private data class InvestmentSettlementForReview(
        val id: Long,
        val eventType: InvestmentEventType,
        val instrumentLabel: String,
        val valueMinor: Long,
        val occurredAtEpochMs: Long
    )

    private data class RepaymentForReview(
        val id: Long,
        val amountMinor: Long,
        val explicitBalanceAfterMinor: Long?,
        val fundingAccountKey: String?,
        val fundingAccountId: String,
        val linkedTransactionId: Long?,
        val resultingBalanceObservationId: Long?,
        val balanceLinkUserDecision: String?,
        val occurredAtEpochMs: Long,
        val debtName: String,
        val fundingAccountName: String
    )

    companion object {
        private const val DECISION_LINKED =
            "USER_LINKED"
        private const val DECISION_KEEP_UNLINKED =
            "USER_KEEP_UNLINKED"

        @Volatile
        private var instance:
            RelationshipReviewStore? = null

        fun get(
            context: Context
        ): RelationshipReviewStore =
            instance
                ?: synchronized(this) {
                    instance
                        ?: RelationshipReviewStore(
                            context
                        ).also {
                            instance = it
                        }
                }
    }
}
