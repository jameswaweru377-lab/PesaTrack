package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.learning.PersonalCategoryEvidence
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningSnapshot
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningSuggestion
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

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
    fun recordUserReview(
        transaction: StoredTransaction
    ): PersonalLearningSuggestion? {
        val candidate =
            transaction.toLearningCandidate()
        if (
            candidate.reviewState !=
                "USER_CLASSIFIED"
        ) {
            return null
        }

        val direction =
            candidate.moneyDirection()
                ?: return null
        if (
            !PersonalLearningPolicy
                .isEligibleReviewEvidence(
                    type = candidate.type,
                    direction = direction,
                    ruleId = candidate.ruleId,
                    relationType =
                        candidate.relationType,
                    categoryId =
                        candidate.categoryId
                )
        ) {
            deleteReview(
                transactionId =
                    candidate.id
            )
            return null
        }

        runCatching {
            identityGraphStore.synchronize(
                transactionLimit = 100
            )
        }

        val resolution =
            identityGraphStore.resolve(
                name = candidate.counterparty,
                phone =
                    candidate.counterpartyPhone
            ) ?: return null

        if (
            !PersonalLearningPolicy
                .isLearnableIdentity(
                    kind =
                        resolution.node.kind,
                    ownership =
                        resolution.node.ownership
                )
        ) {
            deleteReview(
                transactionId =
                    candidate.id
            )
            return null
        }

        upsertReview(
            transactionId =
                candidate.id,
            identityId =
                resolution.node.identityId,
            categoryId =
                candidate.categoryId,
            direction = direction,
            reviewedAtEpochMs =
                System.currentTimeMillis()
        )

        val suggestion =
            suggestionForIdentity(
                identityId =
                    resolution.node.identityId,
                direction = direction,
                trustedForAutoApply =
                    PersonalLearningPolicy
                        .isTrustedForAutoApply(
                            kind =
                                resolution.node.kind,
                            ownership =
                                resolution.node
                                    .ownership,
                            confidence =
                                resolution.confidence,
                            userConfirmed =
                                resolution.node
                                    .userConfirmed
                        )
            )

        if (
            suggestion?.action ==
            PersonalLearningAction.AUTO_APPLY
        ) {
            reconcilePending(
                limit = 200
            )
        }

        return suggestion
    }

    @Synchronized
    fun suggestionFor(
        transaction: StoredTransaction
    ): PersonalLearningSuggestion? =
        suggestionFor(
            transaction.toLearningCandidate()
        )

    @Synchronized
    fun suggestionForTransaction(
        transactionId: Long
    ): PersonalLearningSuggestion? =
        loadCandidate(
            transactionId
        )?.let(::suggestionFor)

    @Synchronized
    fun synchronizeFromUserReviews(
        limit: Int = 1_000
    ): Int {
        runCatching {
            identityGraphStore.synchronize(
                transactionLimit =
                    limit.coerceIn(
                        1,
                        5_000
                    )
            )
        }

        val candidates =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT id,
                           type,
                           counterparty,
                           counterparty_phone,
                           direction,
                           category,
                           review_state,
                           rule_id,
                           relation_type
                    FROM transactions
                    WHERE review_state = ?
                    ORDER BY occurred_at_epoch_ms DESC,
                             id DESC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(
                        "USER_CLASSIFIED",
                        limit.coerceIn(
                            1,
                            5_000
                        ).toString()
                    )
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                LearningCandidate(
                                    id =
                                        cursor.getLong(0),
                                    type =
                                        cursor.getString(1),
                                    counterparty =
                                        cursor.stringOrNull(
                                            2
                                        ),
                                    counterpartyPhone =
                                        cursor.stringOrNull(
                                            3
                                        ),
                                    direction =
                                        cursor.getString(4),
                                    categoryId =
                                        cursor.getString(5),
                                    reviewState =
                                        cursor.getString(6),
                                    ruleId =
                                        cursor.stringOrNull(
                                            7
                                        ),
                                    relationType =
                                        cursor.stringOrNull(
                                            8
                                        )
                                )
                            )
                        }
                    }
                }

        var changed = 0
        candidates.forEach { candidate ->
            val direction =
                candidate.moneyDirection()
                    ?: return@forEach
            if (
                !PersonalLearningPolicy
                    .isEligibleReviewEvidence(
                        type = candidate.type,
                        direction = direction,
                        ruleId = candidate.ruleId,
                        relationType =
                            candidate.relationType,
                        categoryId =
                            candidate.categoryId
                    )
            ) {
                changed +=
                    deleteReview(
                        transactionId =
                            candidate.id
                    )
                return@forEach
            }

            val resolution =
                identityGraphStore.resolve(
                    name = candidate.counterparty,
                    phone =
                        candidate.counterpartyPhone
                ) ?: return@forEach

            if (
                !PersonalLearningPolicy
                    .isLearnableIdentity(
                        kind =
                            resolution.node.kind,
                        ownership =
                            resolution.node.ownership
                    )
            ) {
                return@forEach
            }

            changed +=
                upsertReview(
                    transactionId =
                        candidate.id,
                    identityId =
                        resolution.node.identityId,
                    categoryId =
                        candidate.categoryId,
                    direction = direction,
                    reviewedAtEpochMs =
                        System.currentTimeMillis()
                )
        }

        return changed
    }

    @Synchronized
    fun ensureHistoricalBackfill(
        limit: Int = 1_000
    ) {
        val preferences =
            appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
        if (
            preferences.getBoolean(
                BACKFILL_KEY,
                false
            )
        ) {
            return
        }

        synchronizeFromUserReviews(
            limit = limit
        )
        reconcilePending(
            limit = 300
        )

        preferences.edit()
            .putBoolean(
                BACKFILL_KEY,
                true
            )
            .apply()
    }

    @Synchronized
    fun reconcilePending(
        limit: Int = 200
    ): Int {
        val candidates =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT id,
                           type,
                           counterparty,
                           counterparty_phone,
                           direction,
                           category,
                           review_state,
                           rule_id,
                           relation_type
                    FROM transactions
                    WHERE direction = ?
                      AND rule_id IS NULL
                      AND relation_type IS NULL
                      AND COALESCE(
                            evidence_source,
                            ''
                          ) != 'personal_learning'
                      AND review_state IN (?, ?)
                    ORDER BY occurred_at_epoch_ms DESC,
                             id DESC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(
                        MoneyDirection.EXPENSE.name,
                        "NEEDS_REVIEW",
                        "AUTO_CLASSIFIED",
                        limit.coerceIn(
                            1,
                            1_000
                        ).toString()
                    )
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                LearningCandidate(
                                    id =
                                        cursor.getLong(0),
                                    type =
                                        cursor.getString(1),
                                    counterparty =
                                        cursor.stringOrNull(
                                            2
                                        ),
                                    counterpartyPhone =
                                        cursor.stringOrNull(
                                            3
                                        ),
                                    direction =
                                        cursor.getString(4),
                                    categoryId =
                                        cursor.getString(5),
                                    reviewState =
                                        cursor.getString(6),
                                    ruleId =
                                        cursor.stringOrNull(
                                            7
                                        ),
                                    relationType =
                                        cursor.stringOrNull(
                                            8
                                        )
                                )
                            )
                        }
                    }
                }

        val db =
            database.writableDatabase
        var changed = 0

        candidates.forEach { candidate ->
            val suggestion =
                suggestionFor(candidate)
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
                        "review_state",
                        "AUTO_CLASSIFIED"
                    )
                    put(
                        "evidence_label",
                        "Learned from " +
                            suggestion.supportCount +
                            " of " +
                            suggestion.totalReviewCount +
                            " reviewed transactions"
                    )
                    put(
                        "evidence_source",
                        "personal_learning"
                    )
                    put(
                        "evidence_confidence",
                        suggestion.confidence
                            .name
                    )
                }

            val updated =
                db.update(
                    "transactions",
                    values,
                    """
                    id = ?
                    AND direction = ?
                    AND rule_id IS NULL
                    AND relation_type IS NULL
                    AND review_state IN (?, ?)
                    """.trimIndent(),
                    arrayOf(
                        candidate.id.toString(),
                        MoneyDirection.EXPENSE.name,
                        "NEEDS_REVIEW",
                        "AUTO_CLASSIFIED"
                    )
                )

            if (updated > 0) {
                recordApplication(
                    db = db,
                    transactionId =
                        candidate.id,
                    suggestion =
                        suggestion
                )
                changed += updated
            }
        }

        if (changed > 0) {
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }

    @Synchronized
    fun forgetReview(
        transactionId: Long
    ) {
        deleteReview(
            transactionId =
                transactionId
        )
        clearApplication(
            transactionId =
                transactionId
        )
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
    fun snapshot(): PersonalLearningSnapshot =
        PersonalLearningSnapshot(
            reviewedTransactionCount =
                scalarCount(
                    "SELECT COUNT(*) FROM personal_category_reviews"
                ),
            learnedIdentityCount =
                scalarCount(
                    "SELECT COUNT(DISTINCT identity_id) FROM personal_category_reviews"
                ),
            autoAppliedTransactionCount =
                scalarCount(
                    "SELECT COUNT(*) FROM personal_learning_applications"
                )
        )

    private fun suggestionFor(
        candidate: LearningCandidate
    ): PersonalLearningSuggestion? {
        if (
            !PersonalLearningPolicy
                .isEligiblePredictionTarget(
                    type = candidate.type,
                    direction =
                        candidate.direction,
                    reviewState =
                        candidate.reviewState,
                    ruleId =
                        candidate.ruleId,
                    relationType =
                        candidate.relationType,
                    currentCategoryId =
                        candidate.categoryId
                )
        ) {
            return null
        }

        val resolution =
            identityGraphStore.resolve(
                name = candidate.counterparty,
                phone =
                    candidate.counterpartyPhone
            ) ?: return null

        if (
            !PersonalLearningPolicy
                .isLearnableIdentity(
                    kind =
                        resolution.node.kind,
                    ownership =
                        resolution.node.ownership
                )
        ) {
            return null
        }

        return suggestionForIdentity(
            identityId =
                resolution.node.identityId,
            direction =
                MoneyDirection.EXPENSE,
            trustedForAutoApply =
                PersonalLearningPolicy
                    .isTrustedForAutoApply(
                        kind =
                            resolution.node.kind,
                        ownership =
                            resolution.node.ownership,
                        confidence =
                            resolution.confidence,
                        userConfirmed =
                            resolution.node
                                .userConfirmed
                    )
        )
    }

    private fun suggestionForIdentity(
        identityId: String,
        direction: MoneyDirection,
        trustedForAutoApply: Boolean
    ): PersonalLearningSuggestion? =
        PersonalLearningPolicy.recommend(
            identityId = identityId,
            direction = direction,
            evidence =
                evidenceFor(
                    identityId =
                        identityId,
                    direction =
                        direction
                ),
            trustedForAutoApply =
                trustedForAutoApply
        )

    private fun evidenceFor(
        identityId: String,
        direction: MoneyDirection
    ): List<PersonalCategoryEvidence> =
        database.readableDatabase
            .rawQuery(
                """
                SELECT category_id,
                       COUNT(*)
                FROM personal_category_reviews
                WHERE identity_id = ?
                  AND direction = ?
                GROUP BY category_id
                ORDER BY COUNT(*) DESC,
                         category_id ASC
                """.trimIndent(),
                arrayOf(
                    identityId,
                    direction.name
                )
            ).use { cursor ->
                buildList {
                    while (
                        cursor.moveToNext()
                    ) {
                        add(
                            PersonalCategoryEvidence(
                                categoryId =
                                    cursor.getString(0),
                                supportCount =
                                    cursor.getInt(1)
                            )
                        )
                    }
                }
            }

    private fun upsertReview(
        transactionId: Long,
        identityId: String,
        categoryId: String,
        direction: MoneyDirection,
        reviewedAtEpochMs: Long
    ): Int {
        val current =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT identity_id,
                           category_id,
                           direction
                    FROM personal_category_reviews
                    WHERE transaction_id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        transactionId.toString()
                    )
                ).use { cursor ->
                    if (
                        cursor.moveToFirst()
                    ) {
                        Triple(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2)
                        )
                    } else {
                        null
                    }
                }

        if (
            current ==
            Triple(
                identityId,
                categoryId,
                direction.name
            )
        ) {
            return 0
        }

        val values =
            ContentValues().apply {
                put(
                    "transaction_id",
                    transactionId
                )
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
                    "reviewed_at_epoch_ms",
                    reviewedAtEpochMs
                )
            }

        val saved =
            database.writableDatabase
                .insertWithOnConflict(
                    "personal_category_reviews",
                    null,
                    values,
                    SQLiteDatabase
                        .CONFLICT_REPLACE
                )

        return if (saved == -1L) {
            0
        } else {
            1
        }
    }

    private fun deleteReview(
        transactionId: Long
    ): Int =
        database.writableDatabase
            .delete(
                "personal_category_reviews",
                "transaction_id = ?",
                arrayOf(
                    transactionId.toString()
                )
            )

    private fun recordApplication(
        db: SQLiteDatabase,
        transactionId: Long,
        suggestion:
            PersonalLearningSuggestion
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
                    "support_count",
                    suggestion.supportCount
                )
                put(
                    "total_review_count",
                    suggestion.totalReviewCount
                )
                put(
                    "applied_at_epoch_ms",
                    System.currentTimeMillis()
                )
            }

        db.insertWithOnConflict(
            "personal_learning_applications",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun loadCandidate(
        transactionId: Long
    ): LearningCandidate? =
        database.readableDatabase
            .rawQuery(
                """
                SELECT id,
                       type,
                       counterparty,
                       counterparty_phone,
                       direction,
                       category,
                       review_state,
                       rule_id,
                       relation_type
                FROM transactions
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    transactionId.toString()
                )
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                LearningCandidate(
                    id =
                        cursor.getLong(0),
                    type =
                        cursor.getString(1),
                    counterparty =
                        cursor.stringOrNull(2),
                    counterpartyPhone =
                        cursor.stringOrNull(3),
                    direction =
                        cursor.getString(4),
                    categoryId =
                        cursor.getString(5),
                    reviewState =
                        cursor.getString(6),
                    ruleId =
                        cursor.stringOrNull(7),
                    relationType =
                        cursor.stringOrNull(8)
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

    private fun StoredTransaction
        .toLearningCandidate() =
        LearningCandidate(
            id = id,
            type = type,
            counterparty =
                counterparty,
            counterpartyPhone =
                counterpartyPhone,
            direction = direction,
            categoryId = category,
            reviewState =
                reviewState,
            ruleId = ruleId,
            relationType =
                relationType
        )

    private fun LearningCandidate
        .moneyDirection():
        MoneyDirection? =
        runCatching {
            MoneyDirection.valueOf(
                direction
            )
        }.getOrNull()

    private data class LearningCandidate(
        val id: Long,
        val type: String,
        val counterparty: String?,
        val counterpartyPhone: String?,
        val direction: String,
        val categoryId: String,
        val reviewState: String,
        val ruleId: String?,
        val relationType: String?
    )

    companion object {
        private const val PREFS_NAME =
            "personal_learning_v1"
        private const val BACKFILL_KEY =
            "historical_user_reviews_backfilled"

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

private fun android.database.Cursor
    .stringOrNull(
        index: Int
    ): String? =
    if (isNull(index)) {
        null
    } else {
        getString(index)
    }
