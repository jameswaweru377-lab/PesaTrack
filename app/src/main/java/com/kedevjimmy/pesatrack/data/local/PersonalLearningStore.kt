package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPreference
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningSuggestion
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

class PersonalLearningStore private constructor(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val database =
        PesaTrackDatabase(appContext)
    private val identityGraphStore =
        IdentityGraphStore.get(appContext)

    @Synchronized
    fun remember(
        transaction: StoredTransaction,
        categoryId: String,
        direction: MoneyDirection
    ): PersonalLearningPreference? {
        if (
            !PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId = categoryId,
                    direction = direction
                )
        ) {
            return null
        }

        runCatching {
            identityGraphStore.synchronize(
                transactionLimit = 100
            )
        }

        val resolution =
            identityGraphStore.resolve(
                name = transaction.counterparty,
                phone = transaction.counterpartyPhone
            ) ?: return null

        if (
            resolution.node.kind ==
                IdentityNodeKind.SELF ||
            resolution.node.kind ==
                IdentityNodeKind.FINANCIAL_ACCOUNT
        ) {
            return null
        }

        val existing =
            preferenceForIdentity(
                resolution.node.identityId
            )
        val now =
            System.currentTimeMillis()
        val sameDecision =
            existing?.categoryId == categoryId &&
                existing.direction == direction
        val confirmationCount =
            if (sameDecision) {
                existing!!.confirmationCount + 1
            } else {
                1
            }
        val createdAt =
            existing?.createdAtEpochMs ?: now

        val values =
            ContentValues().apply {
                put(
                    "identity_id",
                    resolution.node.identityId
                )
                put("category_id", categoryId)
                put("direction", direction.name)
                put(
                    "confirmation_count",
                    confirmationCount
                )
                put(
                    "source_transaction_id",
                    transaction.id
                )
                put(
                    "created_at_epoch_ms",
                    createdAt
                )
                put(
                    "updated_at_epoch_ms",
                    now
                )
            }

        val saved =
            database.writableDatabase
                .insertWithOnConflict(
                    "identity_category_preferences",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                ) != -1L
        if (!saved) {
            return null
        }

        return PersonalLearningPreference(
            identityId =
                resolution.node.identityId,
            categoryId = categoryId,
            direction = direction,
            confirmationCount =
                confirmationCount,
            sourceTransactionId =
                transaction.id,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = now
        )
    }

    @Synchronized
    fun suggestionFor(
        transaction: StoredTransaction
    ): PersonalLearningSuggestion? {
        if (!isEligibleForLearning(transaction)) {
            return null
        }

        val resolution =
            identityGraphStore.resolve(
                name = transaction.counterparty,
                phone = transaction.counterpartyPhone
            ) ?: return null

        if (
            resolution.node.kind ==
                IdentityNodeKind.SELF ||
            resolution.node.kind ==
                IdentityNodeKind.FINANCIAL_ACCOUNT
        ) {
            return null
        }

        val preference =
            preferenceForIdentity(
                resolution.node.identityId
            ) ?: return null

        if (
            !PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        preference.categoryId,
                    direction =
                        preference.direction
                )
        ) {
            return null
        }

        val confidence =
            PersonalLearningPolicy
                .confidenceFor(
                    identityConfidence =
                        resolution.confidence,
                    identityUserConfirmed =
                        resolution.node
                            .userConfirmed
                )
        val action =
            PersonalLearningPolicy
                .actionFor(confidence)

        return PersonalLearningSuggestion(
            identityId =
                resolution.node.identityId,
            categoryId =
                preference.categoryId,
            direction =
                preference.direction,
            confidence = confidence,
            action = action,
            confirmationCount =
                preference.confirmationCount
        )
    }

    @Synchronized
    fun applyHighConfidenceTo(
        transactions: List<StoredTransaction>
    ): Int {
        val db =
            database.writableDatabase
        val now =
            System.currentTimeMillis()
        var changed = 0

        transactions.forEach { transaction ->
            val suggestion =
                suggestionFor(transaction)
                    ?: return@forEach

            if (
                suggestion.action !=
                PersonalLearningAction.AUTO_APPLY
            ) {
                return@forEach
            }

            val values =
                ContentValues().apply {
                    put(
                        "category",
                        suggestion.categoryId
                    )
                    put(
                        "direction",
                        suggestion.direction.name
                    )
                    put(
                        "review_state",
                        ReviewState
                            .AUTO_CLASSIFIED
                            .name
                    )
                }

            val updated =
                db.update(
                    "transactions",
                    values,
                    """
                    id = ?
                    AND rule_id IS NULL
                    AND relation_type IS NULL
                    AND direction = ?
                    AND (
                        review_state = ?
                        OR (
                            review_state = ?
                            AND category = ?
                        )
                    )
                    """.trimIndent(),
                    arrayOf(
                        transaction.id.toString(),
                        MoneyDirection.EXPENSE.name,
                        ReviewState.NEEDS_REVIEW.name,
                        ReviewState.AUTO_CLASSIFIED.name,
                        TransactionCategory.OTHER.name
                    )
                )

            if (updated > 0) {
                recordApplication(
                    db = db,
                    transactionId =
                        transaction.id,
                    suggestion = suggestion,
                    appliedAtEpochMs = now
                )
                changed += updated
            }
        }

        if (changed > 0) {
            PayCycleStore.get(appContext)
                .rebuild()
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }

    @Synchronized
    fun clearApplication(
        transactionId: Long
    ) {
        database.writableDatabase
            .delete(
                "personal_learning_applications",
                "transaction_id = ?",
                arrayOf(
                    transactionId.toString()
                )
            )
    }

    @Synchronized
    fun preferenceCount(): Int =
        scalarCount(
            "SELECT COUNT(*) FROM identity_category_preferences"
        )

    @Synchronized
    fun applicationCount(): Int =
        scalarCount(
            "SELECT COUNT(*) FROM personal_learning_applications"
        )

    private fun preferenceForIdentity(
        identityId: String
    ): PersonalLearningPreference? =
        database.readableDatabase
            .rawQuery(
                """
                SELECT identity_id,
                       category_id,
                       direction,
                       confirmation_count,
                       source_transaction_id,
                       created_at_epoch_ms,
                       updated_at_epoch_ms
                FROM identity_category_preferences
                WHERE identity_id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(identityId)
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val direction =
                    runCatching {
                        MoneyDirection.valueOf(
                            cursor.getString(2)
                        )
                    }.getOrNull()
                        ?: return@use null

                PersonalLearningPreference(
                    identityId =
                        cursor.getString(0),
                    categoryId =
                        cursor.getString(1),
                    direction = direction,
                    confirmationCount =
                        cursor.getInt(3),
                    sourceTransactionId =
                        cursor.getLong(4),
                    createdAtEpochMs =
                        cursor.getLong(5),
                    updatedAtEpochMs =
                        cursor.getLong(6)
                )
            }

    private fun isEligibleForLearning(
        transaction: StoredTransaction
    ): Boolean {
        if (
            transaction.ruleId != null ||
            transaction.relationType != null ||
            transaction.direction !=
                MoneyDirection.EXPENSE.name
        ) {
            return false
        }

        return transaction.reviewState ==
            ReviewState.NEEDS_REVIEW.name ||
            (
                transaction.reviewState ==
                    ReviewState
                        .AUTO_CLASSIFIED
                        .name &&
                    transaction.category ==
                    TransactionCategory.OTHER.name
            )
    }

    private fun recordApplication(
        db: SQLiteDatabase,
        transactionId: Long,
        suggestion:
            PersonalLearningSuggestion,
        appliedAtEpochMs: Long
    ) {
        val values =
            ContentValues().apply {
                put(
                    "transaction_id",
                    transactionId
                )
                put(
                    "identity_id",
                    suggestion.identityId
                )
                put(
                    "category_id",
                    suggestion.categoryId
                )
                put(
                    "direction",
                    suggestion.direction.name
                )
                put(
                    "confidence",
                    suggestion.confidence.name
                )
                put(
                    "confirmation_count",
                    suggestion.confirmationCount
                )
                put(
                    "applied_at_epoch_ms",
                    appliedAtEpochMs
                )
            }

        db.insertWithOnConflict(
            "personal_learning_applications",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun scalarCount(
        sql: String
    ): Int =
        database.readableDatabase
            .rawQuery(
                sql,
                null
            ).use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            }

    companion object {
        @Volatile
        private var instance:
            PersonalLearningStore? = null

        fun get(
            context: Context
        ): PersonalLearningStore =
            instance ?: synchronized(this) {
                instance
                    ?: PersonalLearningStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
