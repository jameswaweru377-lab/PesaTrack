package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.BalanceObservationFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialEventKind
import com.kedevjimmy.pesatrack.domain.interpretation.InterpretationConfidence
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import java.security.MessageDigest

enum class FinancialEventStatus {
    APPLIED,
    PENDING_REVIEW
}

data class DiscoveredFinancialEntity(
    val entityKey: String,
    val displayName: String,
    val providerKey: String?,
    val accountTypeHint: String,
    val ownershipHint: AccountOwnershipHint,
    val natureHint: FinancialAccountNature,
    val confirmedAccountId: String?,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long
)

data class ObservedAccountBalance(
    val entityKey: String,
    val displayName: String,
    val accountId: String?,
    val balanceMinor: Long,
    val confidence: InterpretationConfidence,
    val observedAtEpochMs: Long
)

data class PendingFinancialTransfer(
    val eventKey: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val sourceEntityKey: String,
    val destinationEntityKey: String,
    val confidence: InterpretationConfidence,
    val occurredAtEpochMs: Long,
    val sourcePackage: String?,
    val sourceKind: String
)

class FinancialUnderstandingStore private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)
    private val accountStore = AccountStore.get(appContext)
    private val stableOwnedAccountIds = mapOf(
        "system:cash" to
            AccountStore.DEFAULT_CASH_ID,
        "provider:SAFARICOM_MPESA" to
            AccountStore.DEFAULT_MPESA_ID
    )

    @Synchronized
    fun observeEntity(
        reference: FinancialAccountReference,
        observedAtEpochMs: Long
    ): String? {
        val accountId = resolveOwnedAccount(reference)
        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("entity_key", reference.entityKey)
            put("display_name", reference.displayName)
            if (reference.providerKey != null) {
                put("provider_key", reference.providerKey)
            } else {
                putNull("provider_key")
            }
            put(
                "account_type_hint",
                reference.accountTypeHint.name
            )
            put(
                "ownership_hint",
                reference.ownershipHint.name
            )
            put("nature_hint", reference.nature.name)
            if (accountId != null) {
                put("confirmed_account_id", accountId)
            } else {
                putNull("confirmed_account_id")
            }
            put("first_seen_epoch_ms", observedAtEpochMs)
            put("last_seen_epoch_ms", observedAtEpochMs)
        }

        database.writableDatabase.insertWithOnConflict(
            "financial_entities",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        val update = ContentValues().apply {
            put("display_name", reference.displayName)
            if (reference.providerKey != null) {
                put("provider_key", reference.providerKey)
            }
            put(
                "account_type_hint",
                reference.accountTypeHint.name
            )
            if (
                reference.ownershipHint !=
                AccountOwnershipHint.UNKNOWN
            ) {
                put(
                    "ownership_hint",
                    reference.ownershipHint.name
                )
            }
            if (
                reference.nature !=
                FinancialAccountNature.UNKNOWN
            ) {
                put("nature_hint", reference.nature.name)
            }
            if (accountId != null) {
                put("confirmed_account_id", accountId)
            }
            put(
                "last_seen_epoch_ms",
                observedAtEpochMs
            )
        }
        database.writableDatabase.update(
            "financial_entities",
            update,
            "entity_key = ?",
            arrayOf(reference.entityKey)
        )

        return accountId
    }

    @Synchronized
    fun recordBalance(
        fact: BalanceObservationFact,
        messageFingerprint: String,
        observedAtEpochMs: Long,
        sourcePackage: String?,
        sourceKind: String
    ): Boolean {
        val accountId = observeEntity(
            fact.account,
            observedAtEpochMs
        )
        val observationKey = hash(
            "balance|" +
                messageFingerprint +
                "|" +
                fact.account.entityKey +
                "|" +
                fact.balanceMinor
        )

        val values = ContentValues().apply {
            put("observation_key", observationKey)
            put("entity_key", fact.account.entityKey)
            if (accountId != null) {
                put("account_id", accountId)
            } else {
                putNull("account_id")
            }
            put("balance_minor", fact.balanceMinor)
            put("confidence", fact.confidence.name)
            put("observed_at_epoch_ms", observedAtEpochMs)
            if (sourcePackage != null) {
                put("source_package", sourcePackage)
            } else {
                putNull("source_package")
            }
            put("source_kind", sourceKind)
            put(
                "created_at_epoch_ms",
                System.currentTimeMillis()
            )
        }

        val inserted = database.writableDatabase
            .insertWithOnConflict(
                "account_balance_observations",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            ) != -1L

        if (
            fact.account.nature ==
                FinancialAccountNature.LIABILITY
        ) {
            DebtStore.get(appContext)
                .recordObservedOutstanding(
                    reference = fact.account,
                    outstandingMinor =
                        fact.balanceMinor,
                    observedAtEpochMs =
                        observedAtEpochMs
                )
        } else if (
            inserted &&
            accountId != null
        ) {
            DebtStore.get(appContext)
                .reconcilePendingRepaymentBalancesAround(
                    accountId = accountId,
                    entityKey =
                        fact.account.entityKey,
                    observedAtEpochMs =
                        observedAtEpochMs
                )
        }

        if (inserted) {
            LedgerUpdateBus.notifyChanged()
        }
        return inserted
    }

    @Synchronized
    fun recordTransfer(
        event: TransferEventFact,
        messageFingerprint: String,
        occurredAtEpochMs: Long,
        sourcePackage: String?,
        sourceKind: String,
        status: FinancialEventStatus,
        linkedTransactionId: Long? = null
    ): String {
        observeEntity(event.source, occurredAtEpochMs)
        observeEntity(event.destination, occurredAtEpochMs)

        val eventKey = hash(
            "event|" +
                messageFingerprint +
                "|" +
                event.kind.name +
                "|" +
                event.source.entityKey +
                "|" +
                event.destination.entityKey +
                "|" +
                event.amountMinor
        )

        val values = ContentValues().apply {
            put("event_key", eventKey)
            put(
                "event_type",
                FinancialEventKind.TRANSFER.name
            )
            put("amount_minor", event.amountMinor)
            put("fee_minor", event.feeMinor)
            put(
                "source_entity_key",
                event.source.entityKey
            )
            put(
                "destination_entity_key",
                event.destination.entityKey
            )
            put("confidence", event.confidence.name)
            put("status", status.name)
            if (linkedTransactionId != null) {
                put(
                    "linked_transaction_id",
                    linkedTransactionId
                )
            } else {
                putNull("linked_transaction_id")
            }
            put(
                "occurred_at_epoch_ms",
                occurredAtEpochMs
            )
            if (sourcePackage != null) {
                put("source_package", sourcePackage)
            } else {
                putNull("source_package")
            }
            put("source_kind", sourceKind)
            put(
                "created_at_epoch_ms",
                System.currentTimeMillis()
            )
        }

        database.writableDatabase.insertWithOnConflict(
            "financial_events",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        // A later pass can resolve a previously unknown institution.
        val update = ContentValues().apply {
            put("status", status.name)
            if (linkedTransactionId != null) {
                put(
                    "linked_transaction_id",
                    linkedTransactionId
                )
            }
        }
        database.writableDatabase.update(
            "financial_events",
            update,
            "event_key = ?",
            arrayOf(eventKey)
        )

        return eventKey
    }

    @Synchronized
    fun pendingEntities(): List<DiscoveredFinancialEntity> =
        database.readableDatabase.rawQuery(
            """
            SELECT entity_key, display_name, provider_key,
                   account_type_hint, ownership_hint,
                   nature_hint, confirmed_account_id,
                   first_seen_epoch_ms, last_seen_epoch_ms
            FROM financial_entities
            WHERE confirmed_account_id IS NULL
              AND ownership_hint = ?
              AND nature_hint != ?
            ORDER BY last_seen_epoch_ms DESC
            """.trimIndent(),
            arrayOf(
                AccountOwnershipHint.UNKNOWN.name,
                FinancialAccountNature.LIABILITY.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DiscoveredFinancialEntity(
                            entityKey = cursor.getString(0),
                            displayName = cursor.getString(1),
                            providerKey =
                                if (cursor.isNull(2)) {
                                    null
                                } else {
                                    cursor.getString(2)
                                },
                            accountTypeHint = cursor.getString(3),
                            ownershipHint =
                                AccountOwnershipHint.valueOf(
                                    cursor.getString(4)
                                ),
                            natureHint =
                                FinancialAccountNature.valueOf(
                                    cursor.getString(5)
                                ),
                            confirmedAccountId =
                                if (cursor.isNull(6)) {
                                    null
                                } else {
                                    cursor.getString(6)
                                },
                            firstSeenEpochMs = cursor.getLong(7),
                            lastSeenEpochMs = cursor.getLong(8)
                        )
                    )
                }
            }
        }

    @Synchronized
    fun latestBalances(): List<ObservedAccountBalance> =
        database.readableDatabase.rawQuery(
            """
            SELECT o.entity_key,
                   e.display_name,
                   o.account_id,
                   o.balance_minor,
                   o.confidence,
                   o.observed_at_epoch_ms
            FROM account_balance_observations o
            JOIN financial_entities e
              ON e.entity_key = o.entity_key
            WHERE o.id IN (
                SELECT latest.id
                FROM account_balance_observations latest
                WHERE latest.id = (
                    SELECT newest.id
                    FROM account_balance_observations newest
                    WHERE newest.entity_key = latest.entity_key
                    ORDER BY newest.observed_at_epoch_ms DESC,
                             newest.id DESC
                    LIMIT 1
                )
            )
            ORDER BY o.observed_at_epoch_ms DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ObservedAccountBalance(
                            entityKey = cursor.getString(0),
                            displayName = cursor.getString(1),
                            accountId =
                                if (cursor.isNull(2)) {
                                    null
                                } else {
                                    cursor.getString(2)
                                },
                            balanceMinor = cursor.getLong(3),
                            confidence =
                                InterpretationConfidence.valueOf(
                                    cursor.getString(4)
                                ),
                            observedAtEpochMs = cursor.getLong(5)
                        )
                    )
                }
            }
        }

    @Synchronized
    fun confirmEntityOwned(
        entityKey: String
    ): String? {
        val entity = entityByKey(entityKey) ?: return null
        require(
            entity.natureHint !=
                FinancialAccountNature.LIABILITY
        ) {
            "Debt products cannot be added as cash or savings accounts."
        }
        val accountType = runCatching {
            com.kedevjimmy.pesatrack.domain.model.AccountType
                .valueOf(entity.accountTypeHint)
        }.getOrDefault(
            com.kedevjimmy.pesatrack.domain.model.AccountType.OTHER
        )
        val account = accountStore.findOrCreate(
            name = entity.displayName,
            type = accountType
        )

        val values = ContentValues().apply {
            put(
                "ownership_hint",
                AccountOwnershipHint.USER_OWNED.name
            )
            put("confirmed_account_id", account.id)
            put(
                "last_seen_epoch_ms",
                maxOf(
                    entity.lastSeenEpochMs,
                    System.currentTimeMillis()
                )
            )
        }
        database.writableDatabase.update(
            "financial_entities",
            values,
            "entity_key = ?",
            arrayOf(entityKey)
        )

        val observationValues = ContentValues().apply {
            put("account_id", account.id)
        }
        database.writableDatabase.update(
            "account_balance_observations",
            observationValues,
            "entity_key = ? AND account_id IS NULL",
            arrayOf(entityKey)
        )

        DebtStore.get(appContext)
            .reconcilePendingChainsForFundingAccount(
                accountId = account.id,
                entityKey = entityKey
            )

        LedgerUpdateBus.notifyChanged()
        return account.id
    }

    @Synchronized
    fun markEntityExternal(
        entityKey: String
    ): Boolean {
        val values = ContentValues().apply {
            put(
                "ownership_hint",
                AccountOwnershipHint.EXTERNAL.name
            )
            putNull("confirmed_account_id")
        }
        val changed = database.writableDatabase.update(
            "financial_entities",
            values,
            "entity_key = ?",
            arrayOf(entityKey)
        ) > 0
        if (changed) {
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }

    @Synchronized
    fun isUserOwned(
        entityKey: String
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT ownership_hint, confirmed_account_id
            FROM financial_entities
            WHERE entity_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(entityKey)
        ).use { cursor ->
            cursor.moveToFirst() &&
                cursor.getString(0) ==
                    AccountOwnershipHint.USER_OWNED.name &&
                !cursor.isNull(1)
        }

    @Synchronized
    fun entityByKey(
        entityKey: String
    ): DiscoveredFinancialEntity? =
        database.readableDatabase.rawQuery(
            """
            SELECT entity_key, display_name, provider_key,
                   account_type_hint, ownership_hint,
                   nature_hint, confirmed_account_id,
                   first_seen_epoch_ms, last_seen_epoch_ms
            FROM financial_entities
            WHERE entity_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(entityKey)
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                DiscoveredFinancialEntity(
                    entityKey = cursor.getString(0),
                    displayName = cursor.getString(1),
                    providerKey =
                        if (cursor.isNull(2)) null
                        else cursor.getString(2),
                    accountTypeHint = cursor.getString(3),
                    ownershipHint =
                        AccountOwnershipHint.valueOf(
                            cursor.getString(4)
                        ),
                    natureHint =
                        FinancialAccountNature.valueOf(
                            cursor.getString(5)
                        ),
                    confirmedAccountId =
                        if (cursor.isNull(6)) null
                        else cursor.getString(6),
                    firstSeenEpochMs = cursor.getLong(7),
                    lastSeenEpochMs = cursor.getLong(8)
                )
            }
        }

    @Synchronized
    fun pendingTransfersForEntity(
        entityKey: String
    ): List<PendingFinancialTransfer> =
        database.readableDatabase.rawQuery(
            """
            SELECT event_key, amount_minor, fee_minor,
                   source_entity_key,
                   destination_entity_key,
                   confidence, occurred_at_epoch_ms,
                   source_package, source_kind
            FROM financial_events
            WHERE status = ?
              AND event_type = ?
              AND (
                    source_entity_key = ?
                    OR destination_entity_key = ?
                  )
            ORDER BY occurred_at_epoch_ms ASC, id ASC
            """.trimIndent(),
            arrayOf(
                FinancialEventStatus.PENDING_REVIEW.name,
                FinancialEventKind.TRANSFER.name,
                entityKey,
                entityKey
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PendingFinancialTransfer(
                            eventKey = cursor.getString(0),
                            amountMinor = cursor.getLong(1),
                            feeMinor = cursor.getLong(2),
                            sourceEntityKey =
                                cursor.getString(3),
                            destinationEntityKey =
                                cursor.getString(4),
                            confidence =
                                InterpretationConfidence
                                    .valueOf(
                                        cursor.getString(5)
                                    ),
                            occurredAtEpochMs =
                                cursor.getLong(6),
                            sourcePackage =
                                if (cursor.isNull(7)) null
                                else cursor.getString(7),
                            sourceKind = cursor.getString(8)
                        )
                    )
                }
            }
        }

    @Synchronized
    fun markEventApplied(
        eventKey: String,
        linkedTransactionId: Long?
    ) {
        val values = ContentValues().apply {
            put(
                "status",
                FinancialEventStatus.APPLIED.name
            )
            if (linkedTransactionId != null) {
                put(
                    "linked_transaction_id",
                    linkedTransactionId
                )
            }
        }
        database.writableDatabase.update(
            "financial_events",
            values,
            "event_key = ?",
            arrayOf(eventKey)
        )
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun accountIdFor(
        reference: FinancialAccountReference
    ): String? {
        val stored = database.readableDatabase.rawQuery(
            """
            SELECT confirmed_account_id
            FROM financial_entities
            WHERE entity_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(reference.entityKey)
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
        return stored ?: resolveOwnedAccount(reference)
    }

    private fun resolveOwnedAccount(
        reference: FinancialAccountReference
    ): String? {
        if (
            reference.ownershipHint !=
            AccountOwnershipHint.USER_OWNED
        ) {
            return null
        }

        return stableOwnedAccountIds[
            reference.entityKey
        ] ?: accountStore.findOrCreate(
            name = reference.displayName,
            type = reference.accountTypeHint
        ).id
    }

    companion object {
        fun fingerprint(
            title: String?,
            body: String
        ): String {
            val canonical = buildString {
                append(
                    title.orEmpty()
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                        .uppercase()
                )
                append("|")
                append(
                    body.replace(Regex("""\s+"""), " ")
                        .trim()
                        .uppercase()
                )
            }
            return hash(canonical)
        }

        private fun hash(value: String): String {
            val digest = MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )
            return digest.joinToString("") { byte ->
                "%02x".format(byte)
            }
        }

        @Volatile
        private var instance:
            FinancialUnderstandingStore? = null

        fun get(
            context: Context
        ): FinancialUnderstandingStore =
            instance ?: synchronized(this) {
                instance ?: FinancialUnderstandingStore(
                    context.applicationContext
                ).also {
                    instance = it
                }
            }
    }
}
