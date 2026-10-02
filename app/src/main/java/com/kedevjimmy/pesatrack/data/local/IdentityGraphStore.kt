package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.identity.IdentityAlias
import com.kedevjimmy.pesatrack.domain.identity.IdentityAliasType
import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.identity.IdentityEvidenceSource
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphSnapshot
import com.kedevjimmy.pesatrack.domain.identity.IdentityNode
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.identity.IdentityOwnership
import com.kedevjimmy.pesatrack.domain.identity.IdentityRelationType
import com.kedevjimmy.pesatrack.domain.identity.IdentityResolution
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewDecision
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewDecisionType
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewPrompt
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewPromptType
import com.kedevjimmy.pesatrack.domain.intelligence.EntityLookupCandidate
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.LocalSeedEntityEnrichmentProvider

class IdentityGraphStore private constructor(
    context: Context
) {
    private val database =
        PesaTrackDatabase(
            context.applicationContext
        )

    @Synchronized
    fun synchronize(
        transactionLimit: Int = 500
    ): Int {
        val db =
            database.writableDatabase
        val now =
            System.currentTimeMillis()
        var observed = 0

        db.beginTransaction()
        try {
            ensureSelfNode(
                db = db,
                now = now
            )
            synchronizeAccounts(
                db = db,
                now = now
            )
            synchronizeConfirmedFinancialEntities(
                db = db,
                now = now
            )
            observed =
                synchronizeTransactions(
                    db = db,
                    limit =
                        transactionLimit
                            .coerceIn(
                                1,
                                5_000
                            )
                )
            db.delete(
                "identity_review_decisions",
                "identity_id = ?",
                arrayOf(identityId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return observed
    }

    @Synchronized
    fun snapshot(): IdentityGraphSnapshot {
        val db =
            database.readableDatabase
        return IdentityGraphSnapshot(
            nodeCount =
                scalarCount(
                    db,
                    "SELECT COUNT(*) FROM identity_nodes"
                ),
            aliasCount =
                scalarCount(
                    db,
                    "SELECT COUNT(*) FROM identity_aliases"
                ),
            selfAliasCount =
                scalarCount(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM identity_aliases
                    WHERE identity_id = ?
                    """.trimIndent(),
                    arrayOf(
                        IdentityGraphPolicy
                            .SELF_ID
                    )
                ),
            pendingPromptCount =
                pendingReviewPrompts(
                    limit = 100
                ).size,
            unclassifiedIdentityCount =
                scalarCount(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM identity_nodes
                    WHERE kind = ?
                    """.trimIndent(),
                    arrayOf(
                        IdentityNodeKind
                            .UNKNOWN
                            .name
                    )
                )
        )
    }

    @Synchronized
    fun pendingReviewPrompts(
        limit: Int = 20
    ): List<IdentityReviewPrompt> {
        val db =
            database.readableDatabase
        val candidates =
            mutableListOf<
                IdentityReviewPrompt
            >()

        db.rawQuery(
            """
            SELECT a.identity_id,
                   a.display_value,
                   a.normalized_value,
                   COUNT(DISTINCT o.transaction_id)
            FROM identity_aliases a
            JOIN identity_observations o
              ON o.identity_id = a.identity_id
            WHERE a.alias_type = ?
              AND a.identity_id != ?
              AND o.own_transfer_candidate = 1
              AND NOT EXISTS (
                  SELECT 1
                  FROM identity_aliases self_alias
                  WHERE self_alias.identity_id = ?
                    AND self_alias.alias_type = ?
                    AND self_alias.normalized_value =
                        a.normalized_value
              )
            GROUP BY a.identity_id,
                     a.display_value,
                     a.normalized_value
            ORDER BY COUNT(DISTINCT o.transaction_id) DESC
            """.trimIndent(),
            arrayOf(
                IdentityAliasType.PHONE.name,
                IdentityGraphPolicy.SELF_ID,
                IdentityGraphPolicy.SELF_ID,
                IdentityAliasType.PHONE.name
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                candidates +=
                    IdentityReviewPrompt(
                        promptType =
                            IdentityReviewPromptType
                                .CONFIRM_SELF_PHONE,
                        identityId =
                            cursor.getString(0),
                        displayLabel =
                            cursor.getString(1),
                        aliasType =
                            IdentityAliasType.PHONE,
                        normalizedValue =
                            cursor.getString(2),
                        observationCount =
                            cursor.getInt(3),
                        reason =
                            "This phone appears in an own-account transfer. Confirm it only if it is yours."
                    )
            }
        }

        db.rawQuery(
            """
            SELECT a.identity_id,
                   a.display_value,
                   a.normalized_value,
                   COUNT(DISTINCT o.transaction_id)
            FROM identity_aliases a
            JOIN identity_observations o
              ON o.identity_id = a.identity_id
            JOIN identity_nodes n
              ON n.identity_id = a.identity_id
            WHERE a.alias_type = ?
              AND a.identity_id != ?
              AND o.own_transfer_candidate = 1
              AND n.kind IN (?, ?)
              AND NOT EXISTS (
                  SELECT 1
                  FROM identity_aliases self_alias
                  WHERE self_alias.identity_id = ?
                    AND self_alias.alias_type = ?
                    AND self_alias.normalized_value =
                        a.normalized_value
              )
            GROUP BY a.identity_id,
                     a.display_value,
                     a.normalized_value
            HAVING COUNT(DISTINCT o.transaction_id) >= ?
            ORDER BY COUNT(DISTINCT o.transaction_id) DESC
            """.trimIndent(),
            arrayOf(
                IdentityAliasType.NAME.name,
                IdentityGraphPolicy.SELF_ID,
                IdentityNodeKind.UNKNOWN.name,
                IdentityNodeKind.PERSON.name,
                IdentityGraphPolicy.SELF_ID,
                IdentityAliasType.NAME.name,
                IdentityGraphPolicy
                    .SELF_NAME_PROMPT_MIN_OBSERVATIONS
                    .toString()
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val display =
                    cursor.getString(1)
                if (
                    IdentityGraphPolicy
                        .isPlausiblePersonName(
                            display
                        )
                ) {
                    candidates +=
                        IdentityReviewPrompt(
                            promptType =
                                IdentityReviewPromptType
                                    .CONFIRM_SELF_NAME,
                            identityId =
                                cursor.getString(0),
                            displayLabel =
                                display,
                            aliasType =
                                IdentityAliasType.NAME,
                            normalizedValue =
                                cursor.getString(2),
                            observationCount =
                                cursor.getInt(3),
                            reason =
                                "This name repeatedly appears in transfers between accounts PesaTrack believes are yours."
                        )
                }
            }
        }

        db.rawQuery(
            """
            SELECT n.identity_id,
                   n.canonical_name,
                   a.normalized_value,
                   a.occurrence_count
            FROM identity_nodes n
            JOIN identity_aliases a
              ON a.identity_id = n.identity_id
             AND a.alias_type = ?
            WHERE n.kind = ?
              AND n.user_confirmed = 0
              AND a.occurrence_count >= ?
            ORDER BY a.occurrence_count DESC,
                     n.last_seen_epoch_ms DESC
            """.trimIndent(),
            arrayOf(
                IdentityAliasType.NAME.name,
                IdentityNodeKind.UNKNOWN.name,
                IdentityGraphPolicy
                    .CLASSIFY_PROMPT_MIN_OBSERVATIONS
                    .toString()
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                candidates +=
                    IdentityReviewPrompt(
                        promptType =
                            IdentityReviewPromptType
                                .CLASSIFY_COUNTERPARTY,
                        identityId =
                            cursor.getString(0),
                        displayLabel =
                            cursor.getString(1),
                        aliasType =
                            IdentityAliasType.NAME,
                        normalizedValue =
                            cursor.getString(2),
                        observationCount =
                            cursor.getInt(3),
                        reason =
                            "This counterparty appears repeatedly. Repetition alone does not prove whether it is a person or business."
                    )
            }
        }

        val reviewDecisions =
            reviewDecisions(db)

        return candidates
            .distinctBy {
                it.promptType to
                    it.identityId
            }
            .filter {
                IdentityGraphPolicy
                    .shouldPresentReviewPrompt(
                        prompt = it,
                        decisions =
                            reviewDecisions
                    )
            }
            .sortedWith(
                compareBy<
                    IdentityReviewPrompt
                > {
                    promptPriority(
                        it.promptType
                    )
                }.thenByDescending {
                    it.observationCount
                }
            )
            .take(
                limit.coerceIn(
                    1,
                    100
                )
            )
    }

    @Synchronized
    fun confirmIdentityAsSelf(
        identityId: String
    ): Boolean {
        if (
            identityId ==
            IdentityGraphPolicy.SELF_ID
        ) {
            return true
        }

        val db =
            database.writableDatabase
        val aliases =
            aliasesFor(
                db = db,
                identityId =
                    identityId
            )
        if (aliases.isEmpty()) {
            return false
        }

        val now =
            System.currentTimeMillis()
        db.beginTransaction()
        try {
            ensureSelfNode(
                db = db,
                now = now
            )
            aliases.forEach { alias ->
                upsertAlias(
                    db = db,
                    identityId =
                        IdentityGraphPolicy
                            .SELF_ID,
                    type = alias.type,
                    displayValue =
                        alias.displayValue,
                    normalizedValue =
                        alias.normalizedValue,
                    source =
                        IdentityEvidenceSource
                            .USER_CONFIRMATION,
                    confidence =
                        IdentityConfidence
                            .USER_CONFIRMED,
                    userConfirmed = true,
                    observedAtEpochMs =
                        now,
                    incrementOccurrence =
                        false
                )
            }
            upsertLink(
                db = db,
                sourceIdentityId =
                    identityId,
                targetIdentityId =
                    IdentityGraphPolicy
                        .SELF_ID,
                relationType =
                    IdentityRelationType
                        .SAME_AS,
                source =
                    IdentityEvidenceSource
                        .USER_CONFIRMATION,
                confidence =
                    IdentityConfidence
                        .USER_CONFIRMED,
                userConfirmed = true,
                observedAtEpochMs =
                    now,
                incrementOccurrence =
                    false
            )
            db.execSQL(
                """
                UPDATE identity_nodes
                SET ownership = ?,
                    confidence = ?,
                    user_confirmed = 1,
                    updated_at_epoch_ms = ?
                WHERE identity_id = ?
                """.trimIndent(),
                arrayOf<Any>(
                    IdentityOwnership.SELF.name,
                    IdentityConfidence
                        .USER_CONFIRMED
                        .name,
                    now,
                    identityId
                )
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return true
    }

    @Synchronized
    fun classifyIdentity(
        identityId: String,
        kind: IdentityNodeKind
    ): Boolean {
        require(
            kind in setOf(
                IdentityNodeKind.PERSON,
                IdentityNodeKind.BUSINESS,
                IdentityNodeKind.BILLER,
                IdentityNodeKind
                    .FINANCIAL_INSTITUTION
            )
        ) {
            "Only external identity kinds can be classified manually."
        }

        val values =
            ContentValues().apply {
                put(
                    "kind",
                    kind.name
                )
                put(
                    "ownership",
                    IdentityOwnership
                        .EXTERNAL
                        .name
                )
                put(
                    "confidence",
                    IdentityConfidence
                        .USER_CONFIRMED
                        .name
                )
                put(
                    "user_confirmed",
                    1
                )
                put(
                    "updated_at_epoch_ms",
                    System.currentTimeMillis()
                )
            }

        val db =
            database.writableDatabase
        val changed =
            db.update(
                "identity_nodes",
                values,
                "identity_id = ?",
                arrayOf(identityId)
            ) > 0

        if (changed) {
            db.delete(
                "identity_review_decisions",
                "identity_id = ?",
                arrayOf(identityId)
            )
        }

        return changed
    }

    @Synchronized
    fun rejectIdentityAsSelf(
        prompt: IdentityReviewPrompt
    ): Boolean {
        require(
            prompt.promptType ==
                IdentityReviewPromptType
                    .CONFIRM_SELF_PHONE ||
                prompt.promptType ==
                IdentityReviewPromptType
                    .CONFIRM_SELF_NAME
        ) {
            "Only self-identity prompts can be rejected as not mine."
        }

        return recordReviewDecision(
            prompt = prompt,
            decision =
                IdentityReviewDecisionType
                    .REJECTED_SELF
        )
    }

    @Synchronized
    fun deferReviewPrompt(
        prompt: IdentityReviewPrompt
    ): Boolean =
        recordReviewDecision(
            prompt = prompt,
            decision =
                IdentityReviewDecisionType
                    .DEFERRED
        )

    @Synchronized
    fun resolve(
        name: String?,
        phone: String?
    ): IdentityResolution? {
        val db =
            database.readableDatabase

        IdentityGraphPolicy
            .normalizeKenyanPhone(
                phone
            )
            ?.let { normalizedPhone ->
                resolveAlias(
                    db = db,
                    type =
                        IdentityAliasType.PHONE,
                    normalizedValue =
                        normalizedPhone
                )?.let {
                    return it
                }
            }

        val normalizedName =
            IdentityGraphPolicy
                .normalizeName(name)
                ?: return null

        val matches =
            resolveAliasCandidates(
                db = db,
                type =
                    IdentityAliasType.NAME,
                normalizedValue =
                    normalizedName
            )

        if (matches.isEmpty()) {
            return null
        }

        val preferred =
            matches.filter {
                it.node.userConfirmed ||
                    it.node.kind ==
                        IdentityNodeKind.SELF
            }

        return when {
            preferred.size == 1 ->
                preferred.single()

            preferred.size > 1 ->
                preferred.maxByOrNull {
                    confidenceRank(
                        it.confidence
                    )
                }

            matches.size == 1 ->
                matches.single()

            else -> null
        }
    }

    private fun synchronizeAccounts(
        db: SQLiteDatabase,
        now: Long
    ) {
        db.rawQuery(
            """
            SELECT id, name, type, identifier
            FROM money_accounts
            WHERE enabled = 1
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val accountId =
                    cursor.getString(0)
                val name =
                    cursor.getString(1)
                val identifier =
                    cursor.stringOrNull(3)
                val identityId =
                    IdentityGraphPolicy
                        .accountIdentityId(
                            accountId
                        )

                upsertNode(
                    db = db,
                    identityId =
                        identityId,
                    kind =
                        IdentityNodeKind
                            .FINANCIAL_ACCOUNT,
                    canonicalName = name,
                    ownership =
                        IdentityOwnership.SELF,
                    confidence =
                        IdentityConfidence.HIGH,
                    userConfirmed = false,
                    observedAtEpochMs = now
                )
                upsertAlias(
                    db = db,
                    identityId =
                        identityId,
                    type =
                        IdentityAliasType
                            .ACCOUNT_ID,
                    displayValue =
                        accountId,
                    normalizedValue =
                        accountId
                            .trim()
                            .uppercase(),
                    source =
                        IdentityEvidenceSource
                            .ACCOUNT_STORE,
                    confidence =
                        IdentityConfidence.HIGH,
                    userConfirmed = false,
                    observedAtEpochMs = now,
                    incrementOccurrence =
                        false
                )
                IdentityGraphPolicy
                    .normalizeName(name)
                    ?.let { normalizedName ->
                        upsertAlias(
                            db = db,
                            identityId =
                                identityId,
                            type =
                                IdentityAliasType.NAME,
                            displayValue = name,
                            normalizedValue =
                                normalizedName,
                            source =
                                IdentityEvidenceSource
                                    .ACCOUNT_STORE,
                            confidence =
                                IdentityConfidence.HIGH,
                            userConfirmed = false,
                            observedAtEpochMs =
                                now,
                            incrementOccurrence =
                                false
                        )
                    }

                val phone =
                    IdentityGraphPolicy
                        .normalizeKenyanPhone(
                            identifier
                        )
                if (phone != null) {
                    upsertAlias(
                        db = db,
                        identityId =
                            identityId,
                        type =
                            IdentityAliasType.PHONE,
                        displayValue = phone,
                        normalizedValue =
                            phone,
                        source =
                            IdentityEvidenceSource
                                .ACCOUNT_STORE,
                        confidence =
                            IdentityConfidence
                                .USER_CONFIRMED,
                        userConfirmed = true,
                        observedAtEpochMs =
                            now,
                        incrementOccurrence =
                            false
                    )
                    upsertAlias(
                        db = db,
                        identityId =
                            IdentityGraphPolicy
                                .SELF_ID,
                        type =
                            IdentityAliasType.PHONE,
                        displayValue = phone,
                        normalizedValue =
                            phone,
                        source =
                            IdentityEvidenceSource
                                .ACCOUNT_STORE,
                        confidence =
                            IdentityConfidence
                                .USER_CONFIRMED,
                        userConfirmed = true,
                        observedAtEpochMs =
                            now,
                        incrementOccurrence =
                            false
                    )
                } else if (
                    !identifier.isNullOrBlank()
                ) {
                    upsertAlias(
                        db = db,
                        identityId =
                            identityId,
                        type =
                            IdentityAliasType
                                .ACCOUNT_IDENTIFIER,
                        displayValue =
                            identifier.trim(),
                        normalizedValue =
                            identifier.trim()
                                .uppercase(),
                        source =
                            IdentityEvidenceSource
                                .ACCOUNT_STORE,
                        confidence =
                            IdentityConfidence.HIGH,
                        userConfirmed = false,
                        observedAtEpochMs =
                            now,
                        incrementOccurrence =
                            false
                    )
                }

                upsertLink(
                    db = db,
                    sourceIdentityId =
                        IdentityGraphPolicy
                            .SELF_ID,
                    targetIdentityId =
                        identityId,
                    relationType =
                        IdentityRelationType.OWNS,
                    source =
                        IdentityEvidenceSource
                            .ACCOUNT_STORE,
                    confidence =
                        IdentityConfidence.HIGH,
                    userConfirmed = false,
                    observedAtEpochMs = now,
                    incrementOccurrence =
                        false
                )
            }
        }
    }

    private fun synchronizeConfirmedFinancialEntities(
        db: SQLiteDatabase,
        now: Long
    ) {
        db.rawQuery(
            """
            SELECT confirmed_account_id,
                   display_name,
                   provider_key
            FROM financial_entities
            WHERE ownership_hint = 'USER_OWNED'
              AND confirmed_account_id IS NOT NULL
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val accountId =
                    cursor.getString(0)
                val identityId =
                    IdentityGraphPolicy
                        .accountIdentityId(
                            accountId
                        )
                val providerKey =
                    cursor.stringOrNull(2)
                if (
                    !providerKey.isNullOrBlank()
                ) {
                    upsertAlias(
                        db = db,
                        identityId =
                            identityId,
                        type =
                            IdentityAliasType
                                .PROVIDER_KEY,
                        displayValue =
                            providerKey,
                        normalizedValue =
                            providerKey
                                .trim()
                                .uppercase(),
                        source =
                            IdentityEvidenceSource
                                .FINANCIAL_ENTITY,
                        confidence =
                            IdentityConfidence.HIGH,
                        userConfirmed = false,
                        observedAtEpochMs = now,
                        incrementOccurrence =
                            false
                    )
                }

                val displayName =
                    cursor.stringOrNull(1)
                IdentityGraphPolicy
                    .normalizeName(
                        displayName
                    )
                    ?.let {
                        upsertAlias(
                            db = db,
                            identityId =
                                identityId,
                            type =
                                IdentityAliasType.NAME,
                            displayValue =
                                displayName!!,
                            normalizedValue =
                                it,
                            source =
                                IdentityEvidenceSource
                                    .FINANCIAL_ENTITY,
                            confidence =
                                IdentityConfidence.HIGH,
                            userConfirmed = false,
                            observedAtEpochMs =
                                now,
                            incrementOccurrence =
                                false
                        )
                    }
            }
        }
    }

    private fun synchronizeTransactions(
        db: SQLiteDatabase,
        limit: Int
    ): Int {
        var observed = 0
        db.rawQuery(
            """
            SELECT t.id,
                   t.counterparty,
                   t.counterparty_phone,
                   t.occurred_at_epoch_ms,
                   t.direction,
                   t.account_id,
                   t.linked_account_id
            FROM transactions t
            LEFT JOIN identity_observations o
              ON o.transaction_id = t.id
            WHERE o.transaction_id IS NULL
              AND (
                  (t.counterparty IS NOT NULL
                   AND TRIM(t.counterparty) != '')
                  OR
                  (t.counterparty_phone IS NOT NULL
                   AND TRIM(t.counterparty_phone) != '')
              )
            ORDER BY t.id ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                limit.toString()
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                if (
                    observeTransaction(
                        db = db,
                        transactionId =
                            cursor.getLong(0),
                        counterparty =
                            cursor.stringOrNull(1),
                        counterpartyPhone =
                            cursor.stringOrNull(2),
                        occurredAtEpochMs =
                            cursor.getLong(3),
                        direction =
                            cursor.getString(4),
                        accountId =
                            cursor.stringOrNull(5),
                        linkedAccountId =
                            cursor.stringOrNull(6)
                    )
                ) {
                    observed += 1
                }
            }
        }
        return observed
    }

    private fun observeTransaction(
        db: SQLiteDatabase,
        transactionId: Long,
        counterparty: String?,
        counterpartyPhone: String?,
        occurredAtEpochMs: Long,
        direction: String,
        accountId: String?,
        linkedAccountId: String?
    ): Boolean {
        val normalizedName =
            IdentityGraphPolicy
                .normalizeName(
                    counterparty
                )
        val normalizedPhone =
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    counterpartyPhone
                )

        if (
            normalizedName == null &&
            normalizedPhone == null
        ) {
            return false
        }

        val localResolution =
            counterparty
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    LocalSeedEntityEnrichmentProvider
                        .resolve(
                            EntityLookupCandidate(
                                counterparty = it,
                                rawMessage = ""
                            )
                        )
                }

        val identityId =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = counterparty,
                    phone =
                        normalizedPhone,
                    knownEntityId =
                        localResolution
                            ?.entity
                            ?.id
                )
        val ownTransferCandidate =
            direction.equals(
                "TRANSFER",
                ignoreCase = true
            ) &&
                !accountId.isNullOrBlank() &&
                !linkedAccountId.isNullOrBlank() &&
                accountId != linkedAccountId

        val observation =
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
                    "account_id",
                    accountId
                )
                put(
                    "linked_account_id",
                    linkedAccountId
                )
                put(
                    "direction",
                    direction
                )
                put(
                    "observed_name",
                    counterparty
                )
                put(
                    "observed_phone",
                    normalizedPhone
                )
                put(
                    "own_transfer_candidate",
                    if (
                        ownTransferCandidate
                    ) 1 else 0
                )
                put(
                    "occurred_at_epoch_ms",
                    occurredAtEpochMs
                )
            }

        val inserted =
            db.insertWithOnConflict(
                "identity_observations",
                null,
                observation,
                SQLiteDatabase
                    .CONFLICT_IGNORE
            )
        if (inserted == -1L) {
            return false
        }

        val knownEntity =
            localResolution?.entity
        val kind =
            IdentityGraphPolicy
                .nodeKindFor(
                    knownEntity?.kind
                )
        val canonicalName =
            knownEntity
                ?.canonicalName
                ?: counterparty
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
                ?: normalizedPhone
                ?: "Unknown counterparty"
        val confidence =
            when (
                localResolution
                    ?.confidence
            ) {
                EntityMatchConfidence.HIGH ->
                    IdentityConfidence.HIGH

                EntityMatchConfidence.LIKELY ->
                    IdentityConfidence.MEDIUM

                null ->
                    IdentityConfidence.LOW
            }
        val source =
            if (knownEntity != null) {
                IdentityEvidenceSource
                    .LOCAL_ENTITY_CATALOG
            } else {
                IdentityEvidenceSource
                    .TRANSACTION_COUNTERPARTY
            }

        upsertNode(
            db = db,
            identityId = identityId,
            kind = kind,
            canonicalName =
                canonicalName,
            ownership =
                IdentityOwnership.UNKNOWN,
            confidence = confidence,
            userConfirmed = false,
            observedAtEpochMs =
                occurredAtEpochMs
        )

        if (
            normalizedName != null &&
            !counterparty.isNullOrBlank()
        ) {
            upsertAlias(
                db = db,
                identityId = identityId,
                type =
                    IdentityAliasType.NAME,
                displayValue =
                    counterparty.trim(),
                normalizedValue =
                    normalizedName,
                source = source,
                confidence = confidence,
                userConfirmed = false,
                observedAtEpochMs =
                    occurredAtEpochMs,
                incrementOccurrence = true
            )
        }
        if (normalizedPhone != null) {
            upsertAlias(
                db = db,
                identityId = identityId,
                type =
                    IdentityAliasType.PHONE,
                displayValue =
                    normalizedPhone,
                normalizedValue =
                    normalizedPhone,
                source =
                    IdentityEvidenceSource
                        .TRANSACTION_COUNTERPARTY,
                confidence =
                    IdentityConfidence.MEDIUM,
                userConfirmed = false,
                observedAtEpochMs =
                    occurredAtEpochMs,
                incrementOccurrence = true
            )
        }

        accountId
            ?.takeIf(
                String::isNotBlank
            )
            ?.let {
                upsertLink(
                    db = db,
                    sourceIdentityId =
                        IdentityGraphPolicy
                            .accountIdentityId(
                                it
                            ),
                    targetIdentityId =
                        identityId,
                    relationType =
                        IdentityRelationType
                            .TRANSACTED_WITH,
                    source =
                        IdentityEvidenceSource
                            .TRANSACTION_COUNTERPARTY,
                    confidence =
                        IdentityConfidence.MEDIUM,
                    userConfirmed = false,
                    observedAtEpochMs =
                        occurredAtEpochMs,
                    incrementOccurrence = true
                )
            }

        return true
    }

    private fun ensureSelfNode(
        db: SQLiteDatabase,
        now: Long
    ) {
        upsertNode(
            db = db,
            identityId =
                IdentityGraphPolicy.SELF_ID,
            kind =
                IdentityNodeKind.SELF,
            canonicalName = "Me",
            ownership =
                IdentityOwnership.SELF,
            confidence =
                IdentityConfidence
                    .USER_CONFIRMED,
            userConfirmed = true,
            observedAtEpochMs = now
        )
    }

    private fun upsertNode(
        db: SQLiteDatabase,
        identityId: String,
        kind: IdentityNodeKind,
        canonicalName: String,
        ownership: IdentityOwnership,
        confidence: IdentityConfidence,
        userConfirmed: Boolean,
        observedAtEpochMs: Long
    ) {
        val existingConfirmed =
            db.rawQuery(
                """
                SELECT user_confirmed
                FROM identity_nodes
                WHERE identity_id = ?
                """.trimIndent(),
                arrayOf(identityId)
            ).use {
                if (it.moveToFirst()) {
                    it.getInt(0) == 1
                } else {
                    null
                }
            }

        if (existingConfirmed == null) {
            val values =
                ContentValues().apply {
                    put(
                        "identity_id",
                        identityId
                    )
                    put(
                        "kind",
                        kind.name
                    )
                    put(
                        "canonical_name",
                        canonicalName
                    )
                    put(
                        "ownership",
                        ownership.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    put(
                        "user_confirmed",
                        if (userConfirmed) 1
                        else 0
                    )
                    put(
                        "first_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "last_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "updated_at_epoch_ms",
                        System.currentTimeMillis()
                    )
                }
            db.insertOrThrow(
                "identity_nodes",
                null,
                values
            )
            return
        }

        val values =
            ContentValues().apply {
                put(
                    "last_seen_epoch_ms",
                    observedAtEpochMs
                )
                put(
                    "updated_at_epoch_ms",
                    System.currentTimeMillis()
                )
                if (
                    !existingConfirmed ||
                    userConfirmed
                ) {
                    put(
                        "kind",
                        kind.name
                    )
                    put(
                        "canonical_name",
                        canonicalName
                    )
                    put(
                        "ownership",
                        ownership.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    if (userConfirmed) {
                        put(
                            "user_confirmed",
                            1
                        )
                    }
                }
            }
        db.update(
            "identity_nodes",
            values,
            "identity_id = ?",
            arrayOf(identityId)
        )
    }

    private fun upsertAlias(
        db: SQLiteDatabase,
        identityId: String,
        type: IdentityAliasType,
        displayValue: String,
        normalizedValue: String,
        source: IdentityEvidenceSource,
        confidence: IdentityConfidence,
        userConfirmed: Boolean,
        observedAtEpochMs: Long,
        incrementOccurrence: Boolean
    ) {
        val existing =
            db.rawQuery(
                """
                SELECT id,
                       occurrence_count,
                       user_confirmed
                FROM identity_aliases
                WHERE identity_id = ?
                  AND alias_type = ?
                  AND normalized_value = ?
                """.trimIndent(),
                arrayOf(
                    identityId,
                    type.name,
                    normalizedValue
                )
            ).use {
                if (it.moveToFirst()) {
                    Triple(
                        it.getLong(0),
                        it.getInt(1),
                        it.getInt(2) == 1
                    )
                } else {
                    null
                }
            }

        if (existing == null) {
            val values =
                ContentValues().apply {
                    put(
                        "identity_id",
                        identityId
                    )
                    put(
                        "alias_type",
                        type.name
                    )
                    put(
                        "normalized_value",
                        normalizedValue
                    )
                    put(
                        "display_value",
                        displayValue
                    )
                    put(
                        "source",
                        source.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    put(
                        "user_confirmed",
                        if (userConfirmed) 1
                        else 0
                    )
                    put(
                        "first_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "last_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "occurrence_count",
                        1
                    )
                }
            db.insertOrThrow(
                "identity_aliases",
                null,
                values
            )
            return
        }

        val values =
            ContentValues().apply {
                put(
                    "display_value",
                    displayValue
                )
                put(
                    "last_seen_epoch_ms",
                    observedAtEpochMs
                )
                if (
                    incrementOccurrence
                ) {
                    put(
                        "occurrence_count",
                        existing.second + 1
                    )
                }
                if (
                    !existing.third ||
                    userConfirmed
                ) {
                    put(
                        "source",
                        source.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    if (userConfirmed) {
                        put(
                            "user_confirmed",
                            1
                        )
                    }
                }
            }
        db.update(
            "identity_aliases",
            values,
            "id = ?",
            arrayOf(
                existing.first.toString()
            )
        )
    }

    private fun upsertLink(
        db: SQLiteDatabase,
        sourceIdentityId: String,
        targetIdentityId: String,
        relationType: IdentityRelationType,
        source: IdentityEvidenceSource,
        confidence: IdentityConfidence,
        userConfirmed: Boolean,
        observedAtEpochMs: Long,
        incrementOccurrence: Boolean
    ) {
        val existing =
            db.rawQuery(
                """
                SELECT id,
                       occurrence_count,
                       user_confirmed
                FROM identity_links
                WHERE source_identity_id = ?
                  AND target_identity_id = ?
                  AND relation_type = ?
                """.trimIndent(),
                arrayOf(
                    sourceIdentityId,
                    targetIdentityId,
                    relationType.name
                )
            ).use {
                if (it.moveToFirst()) {
                    Triple(
                        it.getLong(0),
                        it.getInt(1),
                        it.getInt(2) == 1
                    )
                } else {
                    null
                }
            }

        if (existing == null) {
            val values =
                ContentValues().apply {
                    put(
                        "source_identity_id",
                        sourceIdentityId
                    )
                    put(
                        "target_identity_id",
                        targetIdentityId
                    )
                    put(
                        "relation_type",
                        relationType.name
                    )
                    put(
                        "source",
                        source.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    put(
                        "user_confirmed",
                        if (userConfirmed) 1
                        else 0
                    )
                    put(
                        "first_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "last_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "occurrence_count",
                        1
                    )
                }
            db.insertOrThrow(
                "identity_links",
                null,
                values
            )
            return
        }

        val values =
            ContentValues().apply {
                put(
                    "last_seen_epoch_ms",
                    observedAtEpochMs
                )
                if (
                    incrementOccurrence
                ) {
                    put(
                        "occurrence_count",
                        existing.second + 1
                    )
                }
                if (
                    !existing.third ||
                    userConfirmed
                ) {
                    put(
                        "source",
                        source.name
                    )
                    put(
                        "confidence",
                        confidence.name
                    )
                    if (userConfirmed) {
                        put(
                            "user_confirmed",
                            1
                        )
                    }
                }
            }
        db.update(
            "identity_links",
            values,
            "id = ?",
            arrayOf(
                existing.first.toString()
            )
        )
    }

    private fun recordReviewDecision(
        prompt: IdentityReviewPrompt,
        decision:
            IdentityReviewDecisionType
    ): Boolean {
        val values =
            ContentValues().apply {
                put(
                    "prompt_type",
                    prompt.promptType.name
                )
                put(
                    "identity_id",
                    prompt.identityId
                )
                put(
                    "decision",
                    decision.name
                )
                put(
                    "evidence_count",
                    prompt.observationCount
                )
                put(
                    "updated_at_epoch_ms",
                    System.currentTimeMillis()
                )
            }

        return database.writableDatabase
            .insertWithOnConflict(
                "identity_review_decisions",
                null,
                values,
                SQLiteDatabase
                    .CONFLICT_REPLACE
            ) != -1L
    }

    private fun reviewDecisions(
        db: SQLiteDatabase
    ): List<IdentityReviewDecision> =
        db.rawQuery(
            """
            SELECT prompt_type,
                   identity_id,
                   decision,
                   evidence_count,
                   updated_at_epoch_ms
            FROM identity_review_decisions
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (
                    cursor.moveToNext()
                ) {
                    val promptType =
                        runCatching {
                            IdentityReviewPromptType
                                .valueOf(
                                    cursor.getString(0)
                                )
                        }.getOrNull()
                            ?: continue
                    val decision =
                        runCatching {
                            IdentityReviewDecisionType
                                .valueOf(
                                    cursor.getString(2)
                                )
                        }.getOrNull()
                            ?: continue

                    add(
                        IdentityReviewDecision(
                            promptType =
                                promptType,
                            identityId =
                                cursor.getString(1),
                            decision =
                                decision,
                            evidenceCount =
                                cursor.getInt(3),
                            updatedAtEpochMs =
                                cursor.getLong(4)
                        )
                    )
                }
            }
        }

    private fun aliasesFor(
        db: SQLiteDatabase,
        identityId: String
    ): List<IdentityAlias> =
        db.rawQuery(
            """
            SELECT id,
                   identity_id,
                   alias_type,
                   normalized_value,
                   display_value,
                   source,
                   confidence,
                   user_confirmed,
                   first_seen_epoch_ms,
                   last_seen_epoch_ms,
                   occurrence_count
            FROM identity_aliases
            WHERE identity_id = ?
            """.trimIndent(),
            arrayOf(identityId)
        ).use { cursor ->
            buildList {
                while (
                    cursor.moveToNext()
                ) {
                    add(
                        cursor.toIdentityAlias()
                    )
                }
            }
        }

    private fun resolveAlias(
        db: SQLiteDatabase,
        type: IdentityAliasType,
        normalizedValue: String
    ): IdentityResolution? =
        resolveAliasCandidates(
            db = db,
            type = type,
            normalizedValue =
                normalizedValue
        ).maxByOrNull {
            resolutionRank(it)
        }

    private fun resolveAliasCandidates(
        db: SQLiteDatabase,
        type: IdentityAliasType,
        normalizedValue: String
    ): List<IdentityResolution> =
        db.rawQuery(
            """
            SELECT n.identity_id,
                   n.kind,
                   n.canonical_name,
                   n.ownership,
                   n.confidence,
                   n.user_confirmed,
                   n.first_seen_epoch_ms,
                   n.last_seen_epoch_ms,
                   a.id,
                   a.alias_type,
                   a.normalized_value,
                   a.display_value,
                   a.source,
                   a.confidence,
                   a.user_confirmed,
                   a.first_seen_epoch_ms,
                   a.last_seen_epoch_ms,
                   a.occurrence_count
            FROM identity_aliases a
            JOIN identity_nodes n
              ON n.identity_id = a.identity_id
            WHERE a.alias_type = ?
              AND a.normalized_value = ?
            """.trimIndent(),
            arrayOf(
                type.name,
                normalizedValue
            )
        ).use { cursor ->
            buildList {
                while (
                    cursor.moveToNext()
                ) {
                    val node =
                        IdentityNode(
                            identityId =
                                cursor.getString(0),
                            kind =
                                IdentityNodeKind
                                    .valueOf(
                                        cursor.getString(1)
                                    ),
                            canonicalName =
                                cursor.getString(2),
                            ownership =
                                IdentityOwnership
                                    .valueOf(
                                        cursor.getString(3)
                                    ),
                            confidence =
                                IdentityConfidence
                                    .valueOf(
                                        cursor.getString(4)
                                    ),
                            userConfirmed =
                                cursor.getInt(5) == 1,
                            firstSeenAtEpochMs =
                                cursor.getLong(6),
                            lastSeenAtEpochMs =
                                cursor.getLong(7)
                        )
                    val alias =
                        IdentityAlias(
                            id =
                                cursor.getLong(8),
                            identityId =
                                node.identityId,
                            type =
                                IdentityAliasType
                                    .valueOf(
                                        cursor.getString(9)
                                    ),
                            normalizedValue =
                                cursor.getString(10),
                            displayValue =
                                cursor.getString(11),
                            source =
                                IdentityEvidenceSource
                                    .valueOf(
                                        cursor.getString(12)
                                    ),
                            confidence =
                                IdentityConfidence
                                    .valueOf(
                                        cursor.getString(13)
                                    ),
                            userConfirmed =
                                cursor.getInt(14) == 1,
                            firstSeenAtEpochMs =
                                cursor.getLong(15),
                            lastSeenAtEpochMs =
                                cursor.getLong(16),
                            occurrenceCount =
                                cursor.getInt(17)
                        )
                    add(
                        IdentityResolution(
                            node = node,
                            matchedAlias =
                                alias,
                            confidence =
                                if (
                                    node.userConfirmed ||
                                    alias.userConfirmed
                                ) {
                                    IdentityConfidence
                                        .USER_CONFIRMED
                                } else {
                                    maxConfidence(
                                        node.confidence,
                                        alias.confidence
                                    )
                                }
                        )
                    )
                }
            }
        }

    private fun scalarCount(
        db: SQLiteDatabase,
        sql: String,
        args: Array<String>? = null
    ): Int =
        db.rawQuery(
            sql,
            args
        ).use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }

    private fun promptPriority(
        type: IdentityReviewPromptType
    ): Int =
        when (type) {
            IdentityReviewPromptType
                .CONFIRM_SELF_PHONE -> 0

            IdentityReviewPromptType
                .CONFIRM_SELF_NAME -> 1

            IdentityReviewPromptType
                .CLASSIFY_COUNTERPARTY -> 2
        }

    private fun resolutionRank(
        resolution: IdentityResolution
    ): Int =
        (
            if (
                resolution.node.kind ==
                IdentityNodeKind.SELF
            ) 100 else 0
        ) +
            (
                if (
                    resolution.node
                        .userConfirmed
                ) 50 else 0
            ) +
            confidenceRank(
                resolution.confidence
            )

    private fun confidenceRank(
        confidence: IdentityConfidence
    ): Int =
        when (confidence) {
            IdentityConfidence
                .USER_CONFIRMED -> 40

            IdentityConfidence.HIGH -> 30
            IdentityConfidence.MEDIUM -> 20
            IdentityConfidence.LOW -> 10
        }

    private fun maxConfidence(
        first: IdentityConfidence,
        second: IdentityConfidence
    ): IdentityConfidence =
        if (
            confidenceRank(first) >=
            confidenceRank(second)
        ) {
            first
        } else {
            second
        }

    private fun Cursor.toIdentityAlias():
        IdentityAlias =
        IdentityAlias(
            id = getLong(0),
            identityId = getString(1),
            type =
                IdentityAliasType
                    .valueOf(
                        getString(2)
                    ),
            normalizedValue =
                getString(3),
            displayValue =
                getString(4),
            source =
                IdentityEvidenceSource
                    .valueOf(
                        getString(5)
                    ),
            confidence =
                IdentityConfidence
                    .valueOf(
                        getString(6)
                    ),
            userConfirmed =
                getInt(7) == 1,
            firstSeenAtEpochMs =
                getLong(8),
            lastSeenAtEpochMs =
                getLong(9),
            occurrenceCount =
                getInt(10)
        )

    private fun Cursor.stringOrNull(
        index: Int
    ): String? =
        if (isNull(index)) {
            null
        } else {
            getString(index)
        }

    companion object {
        @Volatile
        private var instance:
            IdentityGraphStore? = null

        fun get(
            context: Context
        ): IdentityGraphStore =
            instance ?: synchronized(this) {
                instance
                    ?: IdentityGraphStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
