package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.identity.IdentityAlias
import com.kedevjimmy.pesatrack.domain.identity.IdentityAliasNormalizer
import com.kedevjimmy.pesatrack.domain.identity.IdentityAliasType
import com.kedevjimmy.pesatrack.domain.identity.IdentityCandidate
import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.identity.IdentityEvidenceSource
import com.kedevjimmy.pesatrack.domain.identity.IdentityKind
import com.kedevjimmy.pesatrack.domain.identity.IdentityNode
import com.kedevjimmy.pesatrack.domain.identity.IdentityObservation
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import java.security.MessageDigest

class IdentityGraphStore private constructor(
    context: Context
) {
    private val database =
        PesaTrackDatabase(
            context.applicationContext
        )


    @Synchronized
    fun backfillExistingData() {
        val accounts =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT id,
                           name,
                           type,
                           enabled,
                           identifier,
                           opening_balance_minor,
                           opening_balance_as_of_epoch_ms
                    FROM money_accounts
                    """.trimIndent(),
                    null
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                MoneyAccount(
                                    id =
                                        cursor.getString(
                                            0
                                        ),
                                    name =
                                        cursor.getString(
                                            1
                                        ),
                                    type =
                                        AccountType
                                            .valueOf(
                                                cursor.getString(
                                                    2
                                                )
                                            ),
                                    enabled =
                                        cursor.getInt(
                                            3
                                        ) == 1,
                                    identifier =
                                        if (
                                            cursor.isNull(
                                                4
                                            )
                                        ) {
                                            null
                                        } else {
                                            cursor.getString(
                                                4
                                            )
                                        },
                                    openingBalanceMinor =
                                        if (
                                            cursor.isNull(
                                                5
                                            )
                                        ) {
                                            null
                                        } else {
                                            cursor.getLong(
                                                5
                                            )
                                        },
                                    openingBalanceAsOfEpochMs =
                                        if (
                                            cursor.isNull(
                                                6
                                            )
                                        ) {
                                            null
                                        } else {
                                            cursor.getLong(
                                                6
                                            )
                                        }
                                )
                            )
                        }
                    }
                }

        val now =
            System.currentTimeMillis()
        accounts.forEach {
            observeOwnedAccount(
                account = it,
                observedAtEpochMs =
                    now
            )
        }

        val counterparties =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT counterparty,
                           counterparty_phone,
                           occurred_at_epoch_ms
                    FROM transactions
                    WHERE (
                        counterparty IS NOT NULL
                        AND TRIM(counterparty) != ''
                    )
                       OR (
                        counterparty_phone IS NOT NULL
                        AND TRIM(counterparty_phone) != ''
                    )
                    ORDER BY occurred_at_epoch_ms ASC
                    """.trimIndent(),
                    null
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                Triple(
                                    if (
                                        cursor.isNull(
                                            0
                                        )
                                    ) {
                                        null
                                    } else {
                                        cursor.getString(
                                            0
                                        )
                                    },
                                    if (
                                        cursor.isNull(
                                            1
                                        )
                                    ) {
                                        null
                                    } else {
                                        cursor.getString(
                                            1
                                        )
                                    },
                                    cursor.getLong(2)
                                )
                            )
                        }
                    }
                }

        counterparties.forEach {
            (name, phone, occurredAt) ->
            observeCounterparty(
                name = name,
                phone = phone,
                observedAtEpochMs =
                    occurredAt
            )
        }
    }

    @Synchronized
    fun observeCounterparty(
        name: String?,
        phone: String?,
        observedAtEpochMs: Long
    ): IdentityObservation? {
        val cleanName =
            name?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
        val normalizedPhone =
            phone?.let(
                IdentityAliasNormalizer::
                    normalizeKenyanPhone
            )

        if (
            cleanName == null &&
            normalizedPhone == null
        ) {
            return null
        }

        val byPhone =
            normalizedPhone?.let {
                findSingleByAlias(
                    IdentityAliasType.PHONE,
                    it
                )
            }
        val byName =
            cleanName?.let {
                findSingleByAlias(
                    IdentityAliasType.NAME,
                    it
                )
            }

        val existing =
            byPhone ?: byName
        val identityId =
            existing?.identityId
                ?: observedIdentityId(
                    normalizedPhone =
                        normalizedPhone,
                    normalizedName =
                        cleanName
                )
        val canonicalName =
            existing?.canonicalName
                ?: cleanName
                ?: phone
                ?: "Unknown"

        upsertNode(
            identityId = identityId,
            kind =
                existing?.kind
                    ?: IdentityKind.UNKNOWN,
            canonicalName =
                canonicalName,
            userConfirmed =
                existing?.userConfirmed
                    ?: false,
            observedAtEpochMs =
                observedAtEpochMs
        )

        cleanName?.let {
            observeAlias(
                identityId = identityId,
                type =
                    IdentityAliasType.NAME,
                value = it,
                source =
                    IdentityEvidenceSource
                        .TRANSACTION_COUNTERPARTY,
                confidence =
                    IdentityConfidence
                        .OBSERVED,
                observedAtEpochMs =
                    observedAtEpochMs
            )
        }

        normalizedPhone?.let {
            observeAlias(
                identityId = identityId,
                type =
                    IdentityAliasType.PHONE,
                value = it,
                source =
                    IdentityEvidenceSource
                        .TRANSACTION_PHONE,
                confidence =
                    IdentityConfidence
                        .OBSERVED,
                observedAtEpochMs =
                    observedAtEpochMs
            )
        }

        return IdentityObservation(
            identityId = identityId,
            canonicalName =
                canonicalName,
            kind =
                existing?.kind
                    ?: IdentityKind.UNKNOWN,
            matchedBy =
                if (normalizedPhone != null) {
                    IdentityAliasType.PHONE
                } else {
                    IdentityAliasType.NAME
                }
        )
    }

    @Synchronized
    fun observeOwnedAccount(
        account: MoneyAccount,
        providerKey: String? = null,
        observedAtEpochMs: Long =
            System.currentTimeMillis()
    ): String {
        val identityId =
            accountIdentityId(
                account.id
            )
        upsertNode(
            identityId = identityId,
            kind = IdentityKind.ACCOUNT,
            canonicalName =
                account.name,
            userConfirmed = true,
            observedAtEpochMs =
                observedAtEpochMs
        )
        observeAlias(
            identityId = identityId,
            type = IdentityAliasType.NAME,
            value = account.name,
            source =
                IdentityEvidenceSource.ACCOUNT,
            confidence =
                IdentityConfidence.CONFIRMED,
            observedAtEpochMs =
                observedAtEpochMs
        )

        account.identifier
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?.let { identifier ->
                observeAlias(
                    identityId = identityId,
                    type =
                        IdentityAliasType
                            .ACCOUNT_REFERENCE,
                    value = identifier,
                    source =
                        IdentityEvidenceSource
                            .ACCOUNT,
                    confidence =
                        IdentityConfidence
                            .CONFIRMED,
                    observedAtEpochMs =
                        observedAtEpochMs
                )

                if (
                    IdentityAliasNormalizer
                        .looksLikeKenyanPhone(
                            identifier
                        )
                ) {
                    confirmSelfAlias(
                        type =
                            IdentityAliasType.PHONE,
                        value = identifier,
                        observedAtEpochMs =
                            observedAtEpochMs
                    )
                }
            }

        providerKey
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?.let {
                observeAlias(
                    identityId = identityId,
                    type =
                        IdentityAliasType
                            .PROVIDER_KEY,
                    value = it,
                    source =
                        IdentityEvidenceSource
                            .PROVIDER,
                    confidence =
                        IdentityConfidence
                            .CONFIRMED,
                    observedAtEpochMs =
                        observedAtEpochMs
                )
            }

        return identityId
    }

    @Synchronized
    fun confirmSelfAlias(
        type: IdentityAliasType,
        value: String,
        observedAtEpochMs: Long =
            System.currentTimeMillis()
    ) {
        ensureSelf(
            observedAtEpochMs
        )
        val normalized =
            IdentityAliasNormalizer
                .normalize(
                    type,
                    value
                )
        if (normalized.isBlank()) {
            return
        }

        // Explicit user confirmation wins for this
        // alias. Remove the same alias from
        // unconfirmed observations to avoid two
        // competing identities for the user's phone
        // or transaction name.
        database.writableDatabase
            .delete(
                "identity_aliases",
                """
                alias_type = ?
                AND normalized_value = ?
                AND identity_id != ?
                """.trimIndent(),
                arrayOf(
                    type.name,
                    normalized,
                    SELF_ID
                )
            )

        observeAlias(
            identityId = SELF_ID,
            type = type,
            value = value,
            source =
                IdentityEvidenceSource
                    .USER_CONFIRMATION,
            confidence =
                IdentityConfidence.CONFIRMED,
            observedAtEpochMs =
                observedAtEpochMs
        )
    }

    @Synchronized
    fun confirmKind(
        identityId: String,
        kind: IdentityKind,
        canonicalName: String? = null,
        observedAtEpochMs: Long =
            System.currentTimeMillis()
    ): Boolean {
        require(
            kind != IdentityKind.SELF ||
                identityId == SELF_ID
        ) {
            "Only the stable Self node can use SELF kind."
        }

        val current =
            node(identityId)
                ?: return false
        val values =
            ContentValues().apply {
                put(
                    "kind",
                    kind.name
                )
                put(
                    "canonical_name",
                    canonicalName
                        ?.trim()
                        ?.takeIf(
                            String::isNotBlank
                        )
                        ?: current
                            .canonicalName
                )
                put(
                    "user_confirmed",
                    1
                )
                put(
                    "updated_at_epoch_ms",
                    observedAtEpochMs
                )
            }

        return database.writableDatabase
            .update(
                "identity_nodes",
                values,
                "identity_id = ?",
                arrayOf(identityId)
            ) > 0
    }

    @Synchronized
    fun candidateSummaries(
        minimumObservations: Int = 2,
        limit: Int = 25
    ): List<IdentityCandidate> {
        val ids =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT n.identity_id
                    FROM identity_nodes n
                    JOIN identity_aliases a
                      ON a.identity_id =
                         n.identity_id
                    WHERE n.kind = ?
                      AND n.user_confirmed = 0
                    GROUP BY n.identity_id
                    HAVING MAX(
                        a.observation_count
                    ) >= ?
                    ORDER BY
                        MAX(
                            a.observation_count
                        ) DESC,
                        MAX(
                            a.last_seen_epoch_ms
                        ) DESC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(
                        IdentityKind.UNKNOWN
                            .name,
                        minimumObservations
                            .coerceAtLeast(1)
                            .toString(),
                        limit.coerceIn(
                            1,
                            100
                        ).toString()
                    )
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                cursor.getString(0)
                            )
                        }
                    }
                }

        return ids.mapNotNull {
            identityId ->
            val current =
                node(identityId)
                    ?: return@mapNotNull null
            val aliases =
                aliasesFor(identityId)

            IdentityCandidate(
                node = current,
                aliases = aliases,
                totalObservations =
                    aliases.maxOfOrNull {
                        it.observationCount
                    } ?: 0
            )
        }
    }

    @Synchronized
    fun node(
        identityId: String
    ): IdentityNode? =
        database.readableDatabase
            .rawQuery(
                """
                SELECT identity_id,
                       kind,
                       canonical_name,
                       user_confirmed,
                       created_at_epoch_ms,
                       updated_at_epoch_ms
                FROM identity_nodes
                WHERE identity_id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(identityId)
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    IdentityNode(
                        identityId =
                            cursor.getString(0),
                        kind =
                            IdentityKind
                                .valueOf(
                                    cursor.getString(
                                        1
                                    )
                                ),
                        canonicalName =
                            cursor.getString(2),
                        userConfirmed =
                            cursor.getInt(3) == 1,
                        createdAtEpochMs =
                            cursor.getLong(4),
                        updatedAtEpochMs =
                            cursor.getLong(5)
                    )
                }
            }

    @Synchronized
    fun aliasesFor(
        identityId: String
    ): List<IdentityAlias> =
        database.readableDatabase
            .rawQuery(
                """
                SELECT alias_id,
                       identity_id,
                       alias_type,
                       alias_value,
                       normalized_value,
                       source,
                       confidence,
                       observation_count,
                       first_seen_epoch_ms,
                       last_seen_epoch_ms
                FROM identity_aliases
                WHERE identity_id = ?
                ORDER BY
                    observation_count DESC,
                    last_seen_epoch_ms DESC
                """.trimIndent(),
                arrayOf(identityId)
            ).use { cursor ->
                buildList {
                    while (
                        cursor.moveToNext()
                    ) {
                        add(
                            IdentityAlias(
                                aliasId =
                                    cursor.getString(
                                        0
                                    ),
                                identityId =
                                    cursor.getString(
                                        1
                                    ),
                                type =
                                    IdentityAliasType
                                        .valueOf(
                                            cursor.getString(
                                                2
                                            )
                                        ),
                                value =
                                    cursor.getString(
                                        3
                                    ),
                                normalizedValue =
                                    cursor.getString(
                                        4
                                    ),
                                source =
                                    IdentityEvidenceSource
                                        .valueOf(
                                            cursor.getString(
                                                5
                                            )
                                        ),
                                confidence =
                                    IdentityConfidence
                                        .valueOf(
                                            cursor.getString(
                                                6
                                            )
                                        ),
                                observationCount =
                                    cursor.getInt(7),
                                firstSeenEpochMs =
                                    cursor.getLong(8),
                                lastSeenEpochMs =
                                    cursor.getLong(9)
                            )
                        )
                    }
                }
            }

    private fun findSingleByAlias(
        type: IdentityAliasType,
        value: String
    ): IdentityNode? {
        val normalized =
            IdentityAliasNormalizer
                .normalize(
                    type,
                    value
                )
        if (normalized.isBlank()) {
            return null
        }

        val ids =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT DISTINCT identity_id
                    FROM identity_aliases
                    WHERE alias_type = ?
                      AND normalized_value = ?
                    LIMIT 2
                    """.trimIndent(),
                    arrayOf(
                        type.name,
                        normalized
                    )
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                cursor.getString(0)
                            )
                        }
                    }
                }

        return ids.singleOrNull()
            ?.let(::node)
    }

    private fun upsertNode(
        identityId: String,
        kind: IdentityKind,
        canonicalName: String,
        userConfirmed: Boolean,
        observedAtEpochMs: Long
    ) {
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
                        .trim()
                        .ifBlank {
                            "Unknown"
                        }
                )
                put(
                    "user_confirmed",
                    if (userConfirmed) {
                        1
                    } else {
                        0
                    }
                )
                put(
                    "created_at_epoch_ms",
                    observedAtEpochMs
                )
                put(
                    "updated_at_epoch_ms",
                    observedAtEpochMs
                )
            }

        database.writableDatabase
            .insertWithOnConflict(
                "identity_nodes",
                null,
                values,
                SQLiteDatabase
                    .CONFLICT_IGNORE
            )

        val update =
            ContentValues().apply {
                put(
                    "updated_at_epoch_ms",
                    observedAtEpochMs
                )
                if (userConfirmed) {
                    put(
                        "user_confirmed",
                        1
                    )
                }
                if (
                    kind != IdentityKind.UNKNOWN
                ) {
                    put(
                        "kind",
                        kind.name
                    )
                }
            }
        database.writableDatabase
            .update(
                "identity_nodes",
                update,
                "identity_id = ?",
                arrayOf(identityId)
            )
    }

    private fun observeAlias(
        identityId: String,
        type: IdentityAliasType,
        value: String,
        source: IdentityEvidenceSource,
        confidence: IdentityConfidence,
        observedAtEpochMs: Long
    ) {
        val trimmed =
            value.trim()
        val normalized =
            IdentityAliasNormalizer
                .normalize(
                    type,
                    trimmed
                )
        if (
            trimmed.isBlank() ||
            normalized.isBlank()
        ) {
            return
        }

        val aliasId =
            aliasId(
                identityId = identityId,
                type = type,
                normalized = normalized
            )

        val exists =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT 1
                    FROM identity_aliases
                    WHERE alias_id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(aliasId)
                ).use {
                    it.moveToFirst()
                }

        if (!exists) {
            val values =
                ContentValues().apply {
                    put(
                        "alias_id",
                        aliasId
                    )
                    put(
                        "identity_id",
                        identityId
                    )
                    put(
                        "alias_type",
                        type.name
                    )
                    put(
                        "alias_value",
                        trimmed
                    )
                    put(
                        "normalized_value",
                        normalized
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
                        "observation_count",
                        1
                    )
                    put(
                        "first_seen_epoch_ms",
                        observedAtEpochMs
                    )
                    put(
                        "last_seen_epoch_ms",
                        observedAtEpochMs
                    )
                }
            database.writableDatabase
                .insertWithOnConflict(
                    "identity_aliases",
                    null,
                    values,
                    SQLiteDatabase
                        .CONFLICT_IGNORE
                )
            return
        }

        database.writableDatabase
            .execSQL(
                """
                UPDATE identity_aliases
                SET observation_count =
                        observation_count + 1,
                    alias_value = ?,
                    source = CASE
                        WHEN confidence =
                             'CONFIRMED'
                        THEN source
                        ELSE ?
                    END,
                    confidence = CASE
                        WHEN confidence =
                             'CONFIRMED'
                        THEN confidence
                        ELSE ?
                    END,
                    last_seen_epoch_ms = ?
                WHERE alias_id = ?
                """.trimIndent(),
                arrayOf<Any>(
                    trimmed,
                    source.name,
                    confidence.name,
                    observedAtEpochMs,
                    aliasId
                )
            )
    }

    private fun ensureSelf(
        observedAtEpochMs: Long
    ) {
        upsertNode(
            identityId = SELF_ID,
            kind = IdentityKind.SELF,
            canonicalName = "You",
            userConfirmed = false,
            observedAtEpochMs =
                observedAtEpochMs
        )
    }

    private fun observedIdentityId(
        normalizedPhone: String?,
        normalizedName: String?
    ): String {
        val basis =
            normalizedPhone
                ?.let {
                    "phone:$it"
                }
                ?: "name:" +
                    IdentityAliasNormalizer
                        .normalize(
                            IdentityAliasType.NAME,
                            normalizedName
                                .orEmpty()
                        )

        return "identity:observed:" +
            hash(basis).take(24)
    }

    private fun accountIdentityId(
        accountId: String
    ): String =
        "identity:account:" +
            accountId

    private fun aliasId(
        identityId: String,
        type: IdentityAliasType,
        normalized: String
    ): String =
        "alias:" +
            hash(
                identityId +
                    "|" +
                    type.name +
                    "|" +
                    normalized
            ).take(32)

    private fun hash(
        value: String
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") {
                "%02x".format(it)
            }

    companion object {
        const val SELF_ID =
            "identity:self"

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
