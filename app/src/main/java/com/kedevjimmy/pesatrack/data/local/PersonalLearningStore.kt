package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningApplication
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningConfidence
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningCorrectionAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningExplanation
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
        PesaTrackDatabase.get(appContext)
    private val identityGraphStore =
        IdentityGraphStore.get(appContext)

    fun remember(
        transaction: StoredTransaction,
        categoryId: String,
        direction: MoneyDirection
    ): PersonalLearningPreference? {
        synchronized(CanonicalStoreLock.monitor) {
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
            identityGraphStore
                .synchronizeTransaction(
                    transaction.id
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

        return rememberForIdentity(
            identityId =
                resolution.node.identityId,
            transactionId =
                transaction.id,
            categoryId = categoryId,
            direction = direction
        )
    }
    }

    fun preferenceFor(
        transaction: StoredTransaction
    ): PersonalLearningPreference? {
        synchronized(CanonicalStoreLock.monitor) {
        val identityId =
            applicationFor(transaction.id)
                ?.identityId
                ?: identityGraphStore
                    .resolve(
                        name =
                            transaction.counterparty,
                        phone =
                            transaction
                                .counterpartyPhone
                    )
                    ?.node
                    ?.identityId
                ?: return null

        return preferenceForIdentity(
            identityId
        )
    }
    }

    fun forgetPreferenceFor(
        transaction: StoredTransaction
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val identityId =
            applicationFor(transaction.id)
                ?.identityId
                ?: identityGraphStore
                    .resolve(
                        name =
                            transaction.counterparty,
                        phone =
                            transaction
                                .counterpartyPhone
                    )
                    ?.node
                    ?.identityId
                ?: return false

        return forgetPreference(
            identityId
        )
    }
    }

    fun correctLearnedPreference(
        transaction: StoredTransaction,
        categoryId: String,
        direction: MoneyDirection
    ): PersonalLearningPreference? {
        synchronized(CanonicalStoreLock.monitor) {
        val application =
            applicationFor(
                transaction.id
            ) ?: return null

        return when (
            PersonalLearningPolicy
                .correctionAction(
                    categoryId = categoryId,
                    direction = direction
                )
        ) {
            PersonalLearningCorrectionAction
                .UPDATE_PREFERENCE ->
                rememberForIdentity(
                    identityId =
                        application.identityId,
                    transactionId =
                        transaction.id,
                    categoryId =
                        categoryId,
                    direction =
                        direction
                )

            PersonalLearningCorrectionAction
                .FORGET_PREFERENCE -> {
                forgetPreference(
                    application.identityId
                )
                null
            }
        }
    }
    }

    fun suggestionFor(
        transaction: StoredTransaction
    ): PersonalLearningSuggestion? {
        synchronized(CanonicalStoreLock.monitor) {
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

        val source = TransactionStore.get(appContext).getById(preference.sourceTransactionId)
            ?: return null
        if (!RememberedCategoryMatchPolicy.matches(source.counterparty, source.counterpartyPhone,
                transaction.counterparty, transaction.counterpartyPhone)) return null
        // The user explicitly approved this exact identity. Repetition is not consent.
        val confidence = PersonalLearningConfidence.HIGH
        val action =
            PersonalLearningPolicy
                .actionFor(confidence)

        return PersonalLearningSuggestion(
            identityId =
                resolution.node.identityId,
            identityLabel =
                resolution.node.canonicalName,
            categoryId =
                preference.categoryId,
            direction =
                preference.direction,
            confidence = confidence,
            action = action,
            confirmationCount =
                preference.confirmationCount,
            matchedAliasType =
                resolution.matchedAlias?.type,
            matchedAliasSource =
                resolution.matchedAlias?.source
        )
    }
    }

    fun applicationFor(
        transactionId: Long
    ): PersonalLearningApplication? = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase
            .rawQuery(
                """
                SELECT transaction_id,
                       identity_id,
                       category_id,
                       direction,
                       confidence,
                       confirmation_count,
                       applied_at_epoch_ms
                FROM personal_learning_applications
                WHERE transaction_id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    transactionId.toString()
                )
            )
            .use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val direction =
                    runCatching {
                        MoneyDirection.valueOf(
                            cursor.getString(3)
                        )
                    }.getOrNull()
                        ?: return@use null
                val confidence =
                    runCatching {
                        PersonalLearningConfidence
                            .valueOf(
                                cursor.getString(4)
                            )
                    }.getOrNull()
                        ?: return@use null

                PersonalLearningApplication(
                    transactionId =
                        cursor.getLong(0),
                    identityId =
                        cursor.getString(1),
                    categoryId =
                        cursor.getString(2),
                    direction = direction,
                    confidence =
                        confidence,
                    confirmationCount =
                        cursor.getInt(5),
                    appliedAtEpochMs =
                        cursor.getLong(6)
                )
            }
    }

    fun explanationFor(
        transaction: StoredTransaction
    ): PersonalLearningExplanation? {
        synchronized(CanonicalStoreLock.monitor) {
        val application =
            applicationFor(
                transaction.id
            ) ?: return null
        val resolution =
            identityGraphStore.resolve(
                name = transaction.counterparty,
                phone =
                    transaction.counterpartyPhone
            )

        return PersonalLearningExplanation(
            application =
                application,
            identityLabel =
                resolution
                    ?.takeIf {
                        it.node.identityId ==
                            application.identityId
                    }
                    ?.node
                    ?.canonicalName
                    ?: transaction.counterparty
                    ?: transaction.counterpartyPhone
                    ?: "this identity",
            matchedAliasType =
                resolution
                    ?.takeIf {
                        it.node.identityId ==
                            application.identityId
                    }
                    ?.matchedAlias
                    ?.type,
            matchedAliasSource =
                resolution
                    ?.takeIf {
                        it.node.identityId ==
                            application.identityId
                    }
                    ?.matchedAlias
                    ?.source
        )
    }
    }

    fun undoAndForget(
        transaction: StoredTransaction
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val application =
            applicationFor(
                transaction.id
            ) ?: return false

        val db =
            database.writableDatabase
        db.beginTransaction()
        val changed: Boolean
        try {
            db.delete(
                "identity_category_preferences",
                "identity_id = ?",
                arrayOf(
                    application.identityId
                )
            )

            val values =
                ContentValues().apply {
                    put(
                        "category",
                        TransactionCategory
                            .OTHER
                            .name
                    )
                    put(
                        "direction",
                        application.direction.name
                    )
                    put(
                        "review_state",
                        ReviewState
                            .NEEDS_REVIEW
                            .name
                    )
                    putNull("rule_id")
                }
            changed =
                db.update(
                    "transactions",
                    values,
                    """
                    id = ?
                    AND rule_id IS NULL
                    """.trimIndent(),
                    arrayOf(
                        transaction.id.toString()
                    )
                ) > 0

            db.delete(
                "personal_learning_applications",
                "transaction_id = ?",
                arrayOf(
                    transaction.id.toString()
                )
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        if (changed) {
            PayCycleStore.get(appContext)
                .rebuild()
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }
    }

    fun applyHighConfidenceTo(
        transactions: List<StoredTransaction>,
        rebuildPayCycles: Boolean = true
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val db =
            database.writableDatabase
        val now =
            System.currentTimeMillis()
        var changed = 0

        db.beginTransaction()
        try {
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
                    AND type != 'REVERSAL' AND reversal_of_code IS NULL
                    AND category IN ('OTHER', 'UNCATEGORIZED')
                    AND id NOT IN (SELECT linked_transaction_id FROM debt_events
                        WHERE linked_transaction_id IS NOT NULL AND event_type = 'REPAYMENT')
                    AND (
                        review_state = ?
                        OR (
                            review_state = ?
                            AND category IN (?, 'UNCATEGORIZED')
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

        db.setTransactionSuccessful()
        } finally { db.endTransaction() }

        if (changed > 0) {
            if (rebuildPayCycles) {
                PayCycleStore.get(appContext)
                    .rebuild()
            }
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }
    }

    fun clearApplication(
        transactionId: Long
    ) {
        synchronized(CanonicalStoreLock.monitor) {
        database.writableDatabase
            .delete(
                "personal_learning_applications",
                "transaction_id = ?",
                arrayOf(
                    transactionId.toString()
                )
            )
    }
    }

    fun preferenceCount(): Int = synchronized(CanonicalStoreLock.monitor) {
        scalarCount(
            "SELECT COUNT(*) FROM identity_category_preferences"
        )
    }

    fun applicationCount(): Int = synchronized(CanonicalStoreLock.monitor) {
        scalarCount(
            "SELECT COUNT(*) FROM personal_learning_applications"
        )
    }

    private fun rememberForIdentity(
        identityId: String,
        transactionId: Long,
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

        val existing =
            preferenceForIdentity(
                identityId
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
                    identityId
                )
                put(
                    "category_id",
                    categoryId
                )
                put(
                    "direction",
                    direction.name
                )
                put(
                    "confirmation_count",
                    confirmationCount
                )
                put(
                    "source_transaction_id",
                    transactionId
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
                identityId,
            categoryId =
                categoryId,
            direction =
                direction,
            confirmationCount =
                confirmationCount,
            sourceTransactionId =
                transactionId,
            createdAtEpochMs =
                createdAt,
            updatedAtEpochMs =
                now
        )
    }

    private fun forgetPreference(
        identityId: String
    ): Boolean =
        database.writableDatabase
            .delete(
                "identity_category_preferences",
                "identity_id = ?",
                arrayOf(identityId)
            ) > 0

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

    private fun isEligibleForLearning(transaction: StoredTransaction): Boolean =
        transaction.ruleId == null && transaction.relationType == null &&
            transaction.direction == MoneyDirection.EXPENSE.name &&
            transaction.type != "REVERSAL" && transaction.reversalOfCode == null &&
            transaction.category in setOf(TransactionCategory.OTHER.name, TransactionCategory.UNCATEGORIZED.name) &&
            transaction.reviewState in setOf(ReviewState.NEEDS_REVIEW.name, ReviewState.AUTO_CLASSIFIED.name)

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

