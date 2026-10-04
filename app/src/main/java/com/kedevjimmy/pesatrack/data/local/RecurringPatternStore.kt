package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternCandidate
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternObservation
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternPolicy
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine

class RecurringPatternStore private constructor(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val database =
        PesaTrackDatabase.get(appContext)
    private val transactionStore =
        TransactionStore.get(appContext)
    private val ruleStore =
        RuleStore.get(appContext)
    private val identityGraphStore =
        IdentityGraphStore.get(appContext)

    @Synchronized
    fun suggestions(
        limit: Int = 5,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): List<RecurringPatternCandidate> {
        val rules =
            ruleStore.listRules(
                enabledOnly = true
            )

        val observations =
            transactionStore
                .recent(500)
                .mapNotNull { transaction ->
                    observation(
                        transaction =
                            transaction,
                        existingRules =
                            rules
                    )
                }

        return RecurringPatternPolicy
            .detect(
                observations =
                    observations,
                nowEpochMs =
                    nowEpochMs
            )
            .filter(
                ::shouldPresent
            )
            .take(
                limit.coerceIn(
                    1,
                    20
                )
            )
    }

    @Synchronized
    fun accept(
        candidate:
            RecurringPatternCandidate
    ): SmartRule? {
        val transaction =
            transactionStore.getById(
                candidate
                    .representativeTransactionId
            ) ?: return null

        val category =
            runCatching {
                TransactionCategory
                    .valueOf(
                        candidate.categoryId
                    )
            }.getOrNull()
                ?: return null

        if (
            category in
            STRUCTURAL_OR_UNHELPFUL_CATEGORIES
        ) {
            return null
        }

        val exactName =
            transaction.counterparty
                .orEmpty()
                .trim()
        val phone =
            transaction.counterpartyPhone
                .orEmpty()
                .trim()

        if (
            exactName.isBlank() &&
            phone.isBlank()
        ) {
            return null
        }

        val saved =
            ruleStore.save(
                name =
                    candidate.label +
                        " recurring",
                counterpartyContains = "",
                counterpartyExact =
                    exactName,
                counterpartyPhoneEquals =
                    phone,
                category = category,
                direction =
                    MoneyDirection.EXPENSE,
                displayLabel =
                    candidate.label,
                preset =
                    RulePreset.OTHER_RECURRING,
                plannedAmountMinor =
                    candidate
                        .typicalAmountMinor
            )

        transactionStore.reapplyRule(
            rule = saved,
            rebuildPayCycles = false
        )

        database.writableDatabase
            .delete(
                "recurring_pattern_decisions",
                "pattern_key = ?",
                arrayOf(
                    candidate.patternKey
                )
            )

        PayCycleStore.get(appContext)
            .rebuild()
        LedgerUpdateBus.notifyChanged()

        return saved
    }

    @Synchronized
    fun dismiss(
        candidate:
            RecurringPatternCandidate
    ) {
        val values =
            ContentValues().apply {
                put(
                    "pattern_key",
                    candidate.patternKey
                )
                put(
                    "decision",
                    DECISION_NOT_RECURRING
                )
                put(
                    "evidence_count",
                    candidate
                        .occurrenceCount
                )
                put(
                    "updated_at_epoch_ms",
                    System
                        .currentTimeMillis()
                )
            }

        database.writableDatabase
            .insertWithOnConflict(
                "recurring_pattern_decisions",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )

        LedgerUpdateBus.notifyChanged()
    }

    private fun observation(
        transaction: StoredTransaction,
        existingRules: List<SmartRule>
    ): RecurringPatternObservation? {
        if (
            transaction.direction !=
                MoneyDirection.EXPENSE.name ||
            transaction.amountMinor <= 0L ||
            transaction.ruleId != null ||
            transaction.relationType != null ||
            transaction.reviewState ==
                ReviewState.NEEDS_REVIEW.name ||
            transaction.reviewState ==
                ReviewState.EXCLUDED.name ||
            transaction.type ==
                MpesaTransactionType
                    .REVERSAL.name
        ) {
            return null
        }

        val category =
            runCatching {
                TransactionCategory
                    .valueOf(
                        transaction.category
                    )
            }.getOrNull()
                ?: return null

        if (
            category in
            STRUCTURAL_OR_UNHELPFUL_CATEGORIES
        ) {
            return null
        }

        if (
            transaction.counterparty
                .isNullOrBlank() &&
            transaction.counterpartyPhone
                .isNullOrBlank()
        ) {
            return null
        }

        if (
            SmartRuleEngine
                .matchTransaction(
                    counterparty =
                        transaction
                            .counterparty,
                    counterpartyPhone =
                        transaction
                            .counterpartyPhone,
                    amountMinor =
                        transaction
                            .amountMinor,
                    rules = existingRules
                ) != null
        ) {
            return null
        }

        val resolution =
            identityGraphStore.resolve(
                name =
                    transaction.counterparty,
                phone =
                    transaction
                        .counterpartyPhone
            )

        if (
            resolution?.node?.kind ==
                IdentityNodeKind.SELF ||
            resolution?.node?.kind ==
                IdentityNodeKind
                    .FINANCIAL_ACCOUNT
        ) {
            return null
        }

        val identityKey =
            resolution?.node?.identityId
                ?: IdentityGraphPolicy
                    .counterpartyIdentityId(
                        name =
                            transaction
                                .counterparty,
                        phone =
                            transaction
                                .counterpartyPhone
                    )

        val label =
            resolution?.node
                ?.canonicalName
                ?.takeIf(
                    String::isNotBlank
                )
                ?: transaction
                    .counterparty
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
                ?: transaction
                    .counterpartyPhone
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
                ?: return null

        return RecurringPatternObservation(
            transactionId =
                transaction.id,
            identityKey =
                identityKey,
            label = label,
            categoryId =
                category.name,
            amountMinor =
                transaction.amountMinor,
            occurredAtEpochMs =
                transaction
                    .occurredAtEpochMs,
            counterparty =
                transaction.counterparty,
            counterpartyPhone =
                transaction
                    .counterpartyPhone,
            accountId =
                transaction.accountId
        )
    }

    private fun shouldPresent(
        candidate:
            RecurringPatternCandidate
    ): Boolean {
        val decision =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT decision,
                           evidence_count
                    FROM recurring_pattern_decisions
                    WHERE pattern_key = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        candidate.patternKey
                    )
                ).use { cursor ->
                    if (
                        cursor.moveToFirst()
                    ) {
                        cursor.getString(0) to
                            cursor.getInt(1)
                    } else {
                        null
                    }
                }
                ?: return true

        if (
            decision.first !=
                DECISION_NOT_RECURRING
        ) {
            return true
        }

        // A dismissal is respected until materially
        // more evidence arrives. Two additional
        // occurrences are required before asking again.
        return candidate.occurrenceCount >=
            decision.second + 2
    }

    companion object {
        private const val
            DECISION_NOT_RECURRING =
            "NOT_RECURRING"

        private val
            STRUCTURAL_OR_UNHELPFUL_CATEGORIES =
            setOf(
                TransactionCategory
                    .DEBT_LOAN,
                TransactionCategory.INCOME,
                TransactionCategory.TRANSFER,
                TransactionCategory.CASH,
                TransactionCategory.SAVINGS,
                TransactionCategory.INVESTMENT,
                TransactionCategory.OTHER,
                TransactionCategory
                    .UNCATEGORIZED
            )

        @Volatile
        private var instance:
            RecurringPatternStore? = null

        fun get(
            context: Context
        ): RecurringPatternStore =
            instance ?: synchronized(this) {
                instance
                    ?: RecurringPatternStore(
                        context
                            .applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
