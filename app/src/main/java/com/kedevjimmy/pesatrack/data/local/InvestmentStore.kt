package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentAction
import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentFact
import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentStage

enum class InvestmentEventType {
    ORDER_SUBMITTED,
    BUY_EXECUTED,
    SELL_EXECUTED
}

data class InvestmentWriteResult(
    val inserted: Boolean,
    val linkedTransactionId: Long?,
    val changed: Boolean
)

enum class InvestmentPositionQuality {
    OBSERVED_EVENTS_ONLY,
    INCOMPLETE_HISTORY
}

data class InvestmentPositionSnapshot(
    val instrumentKey: String,
    val instrumentLabel: String,
    val netObservedQuantityMicrounits: Long,
    val quality: InvestmentPositionQuality,
    val updatedAtEpochMs: Long
)

/**
 * Local, provider-neutral investment event ledger.
 *
 * This store persists semantic order/execution facts. It never treats
 * a submitted order as an executed holding change, and it only
 * reclassifies a cash transaction when an exact shared reference plus
 * matching value establishes the settlement relationship.
 *
 * Position quantity is intentionally recorded as NET OBSERVED CHANGE
 * from imported execution events. It is not presented as an
 * authoritative brokerage holding unless a future position/baseline
 * source establishes that truth.
 */
class InvestmentStore private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)
    private val transactionStore =
        TransactionStore.get(appContext)

    @Synchronized
    fun record(
        fact: SemanticInvestmentFact,
        references: Set<String>,
        messageFingerprint: String,
        sourcePackage: String?,
        sourceKind: String,
        occurredAtEpochMs: Long
    ): InvestmentWriteResult {
        val eventType = eventType(fact)
        val normalizedReferences =
            references
                .mapNotNull(::normalizeReference)
                .toSet()
        val existingByReference =
            findExistingEventByReference(
                eventType = eventType,
                fact = fact,
                references =
                    normalizedReferences
            )
        val eventKey =
            buildEventKey(
                messageFingerprint = messageFingerprint,
                eventType = eventType,
                fact = fact,
                references =
                    normalizedReferences
            )

        val values = ContentValues().apply {
            put("event_key", eventKey)
            put("event_type", eventType.name)
            put(
                "instrument_key",
                fact.instrumentKey
            )
            put(
                "instrument_label",
                fact.instrumentLabel
            )
            put(
                "quantity_microunits",
                fact.quantityMicrounits
            )
            if (fact.valueMinor != null) {
                put(
                    "value_minor",
                    fact.valueMinor
                )
            } else {
                putNull("value_minor")
            }
            putNull("linked_transaction_id")
            normalizedReferences
                .sorted()
                .firstOrNull()
                ?.let {
                    put(
                        "primary_reference",
                        it
                    )
                }
                ?: putNull("primary_reference")
            if (sourcePackage != null) {
                put("source_package", sourcePackage)
            } else {
                putNull("source_package")
            }
            put("source_kind", sourceKind)
            put(
                "occurred_at_epoch_ms",
                occurredAtEpochMs
            )
            put(
                "created_at_epoch_ms",
                System.currentTimeMillis()
            )
        }

        val rowId =
            if (existingByReference == null) {
                database.writableDatabase
                    .insertWithOnConflict(
                        "investment_events",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE
                    )
            } else {
                -1L
            }

        val inserted = rowId != -1L
        val eventId =
            existingByReference
                ?: if (inserted) {
                    rowId
                } else {
                    eventId(eventKey)
                        ?: return InvestmentWriteResult(
                            inserted = false,
                            linkedTransactionId = null,
                            changed = false
                        )
                }

        normalizedReferences.forEach { reference ->
            val refValues =
                ContentValues().apply {
                    put(
                        "investment_event_id",
                        eventId
                    )
                    put(
                        "reference_value",
                        reference
                    )
                }
            database.writableDatabase
                .insertWithOnConflict(
                    "investment_event_references",
                    null,
                    refValues,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
        }

        if (
            inserted &&
            fact.stage ==
                SemanticInvestmentStage.EXECUTED
        ) {
            applyObservedPositionDelta(fact)
        }

        val linkedTransactionId =
            if (
                fact.stage ==
                    SemanticInvestmentStage.EXECUTED
            ) {
                tryLinkEvent(
                    eventId = eventId,
                    fact = fact,
                    references =
                        normalizedReferences
                )
            } else {
                null
            }

        val changed =
            inserted ||
                linkedTransactionId != null

        if (changed) {
            LedgerUpdateBus.notifyChanged()
        }

        return InvestmentWriteResult(
            inserted = inserted,
            linkedTransactionId =
                linkedTransactionId,
            changed = changed
        )
    }

    @Synchronized
    fun positions():
        List<InvestmentPositionSnapshot> =
        database.readableDatabase
            .rawQuery(
                """
                SELECT instrument_key,
                       instrument_label,
                       net_observed_quantity_microunits,
                       quality,
                       updated_at_epoch_ms
                FROM investment_positions
                ORDER BY instrument_label COLLATE NOCASE ASC,
                         instrument_key ASC
                """.trimIndent(),
                emptyArray()
            )
            .use { cursor ->
                buildList {
                    while (
                        cursor.moveToNext()
                    ) {
                        val quality =
                            when (
                                cursor.getString(
                                    3
                                )
                            ) {
                                POSITION_QUALITY_OBSERVED_EVENTS ->
                                    InvestmentPositionQuality
                                        .OBSERVED_EVENTS_ONLY
                                else ->
                                    InvestmentPositionQuality
                                        .INCOMPLETE_HISTORY
                            }

                        add(
                            InvestmentPositionSnapshot(
                                instrumentKey =
                                    cursor.getString(
                                        0
                                    ),
                                instrumentLabel =
                                    cursor.getString(
                                        1
                                    ),
                                netObservedQuantityMicrounits =
                                    cursor.getLong(
                                        2
                                    ),
                                quality =
                                    quality,
                                updatedAtEpochMs =
                                    cursor.getLong(
                                        4
                                    )
                            )
                        )
                    }
                }
            }

    @Synchronized
    fun reconcileTransaction(
        transactionId: Long,
        references: Set<String>
    ): Boolean {
        val normalizedReferences =
            references
                .mapNotNull(::normalizeReference)
                .toSet()
        if (normalizedReferences.isEmpty()) {
            return false
        }

        val placeholders =
            normalizedReferences
                .joinToString(",") { "?" }
        val eventIds =
            database.readableDatabase.rawQuery(
                """
                SELECT DISTINCT e.id
                FROM investment_events e
                JOIN investment_event_references r
                  ON r.investment_event_id = e.id
                WHERE e.linked_transaction_id IS NULL
                  AND e.settlement_user_decision IS NULL
                  AND e.event_type IN (?, ?)
                  AND r.reference_value IN ($placeholders)
                ORDER BY e.occurred_at_epoch_ms ASC,
                         e.id ASC
                """.trimIndent(),
                buildList {
                    add(
                        InvestmentEventType
                            .BUY_EXECUTED.name
                    )
                    add(
                        InvestmentEventType
                            .SELL_EXECUTED.name
                    )
                    addAll(normalizedReferences)
                }.toTypedArray()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        /*
         * One money movement must not settle multiple executions
         * automatically. Preserve ambiguity for future review.
         */
        if (eventIds.size != 1) {
            return false
        }

        val event =
            eventForLinking(eventIds.single())
                ?: return false
        val transaction =
            transactionStore
                .findByAnyExternalReference(
                    normalizedReferences
                )
                ?: return false

        if (transaction.id != transactionId) {
            return false
        }

        return linkIfStrong(
            event = event,
            transaction = transaction
        )
    }

    private fun tryLinkEvent(
        eventId: Long,
        fact: SemanticInvestmentFact,
        references: Set<String>
    ): Long? {
        if (
            fact.valueMinor == null ||
            references.isEmpty()
        ) {
            return null
        }

        val transaction =
            transactionStore
                .findByAnyExternalReference(
                    references
                )
                ?: return null
        val event =
            eventForLinking(eventId)
                ?: return null

        return if (
            linkIfStrong(
                event = event,
                transaction = transaction
            )
        ) {
            transaction.id
        } else {
            null
        }
    }

    private fun linkIfStrong(
        event: InvestmentEventForLinking,
        transaction: StoredTransaction
    ): Boolean {
        val valueMinor =
            event.valueMinor ?: return false
        if (
            valueMinor <= 0L ||
            transaction.amountMinor != valueMinor
        ) {
            return false
        }

        val action =
            when (event.eventType) {
                InvestmentEventType.BUY_EXECUTED ->
                    SemanticInvestmentAction.BUY
                InvestmentEventType.SELL_EXECUTED ->
                    SemanticInvestmentAction.SELL
                InvestmentEventType.ORDER_SUBMITTED ->
                    return false
            }

        val reclassified =
            transactionStore
                .markInvestmentSettlement(
                    transactionId = transaction.id,
                    action = action,
                    instrumentLabel =
                        event.instrumentLabel
                )
        if (!reclassified) {
            return false
        }

        val values = ContentValues().apply {
            put(
                "linked_transaction_id",
                transaction.id
            )
            put(
                "settlement_confidence",
                "HIGH"
            )
            put(
                "settlement_reason",
                "Exact shared external reference and matching investment value established settlement."
            )
        }
        database.writableDatabase.update(
            "investment_events",
            values,
            "id = ?",
            arrayOf(event.id.toString())
        )
        return true
    }

    @Synchronized
    fun resolveManualSettlement(
        eventId: Long,
        transactionId: Long?
    ): Boolean {
        val event =
            eventForManualReview(eventId)
                ?: return false

        if (transactionId == null) {
            val values = ContentValues().apply {
                put(
                    "settlement_user_decision",
                    DECISION_KEEP_UNLINKED
                )
                putNull("linked_transaction_id")
                putNull("settlement_confidence")
                put(
                    "settlement_reason",
                    "User confirmed that none of the offered cash movements settled this investment execution."
                )
            }
            val changed =
                database.writableDatabase.update(
                    "investment_events",
                    values,
                    "id = ? AND linked_transaction_id IS NULL",
                    arrayOf(eventId.toString())
                ) > 0
            if (changed) {
                LedgerUpdateBus.notifyChanged()
            }
            return changed
        }

        val transaction =
            transactionStore.getById(transactionId)
                ?: return false
        val valueMinor =
            event.valueMinor ?: return false
        if (transaction.amountMinor != valueMinor) {
            return false
        }

        val action =
            when (event.eventType) {
                InvestmentEventType.BUY_EXECUTED ->
                    SemanticInvestmentAction.BUY
                InvestmentEventType.SELL_EXECUTED ->
                    SemanticInvestmentAction.SELL
                InvestmentEventType.ORDER_SUBMITTED ->
                    return false
            }

        val reclassified =
            transactionStore.markInvestmentSettlement(
                transactionId = transactionId,
                action = action,
                instrumentLabel = event.instrumentLabel,
                userConfirmed = true
            )
        if (!reclassified) {
            return false
        }

        val values = ContentValues().apply {
            put(
                "linked_transaction_id",
                transactionId
            )
            put(
                "settlement_user_decision",
                DECISION_LINKED
            )
            put(
                "settlement_confidence",
                "USER_CONFIRMED"
            )
            put(
                "settlement_reason",
                "User confirmed this cash movement settled the investment execution."
            )
        }
        val changed =
            database.writableDatabase.update(
                "investment_events",
                values,
                "id = ? AND linked_transaction_id IS NULL",
                arrayOf(eventId.toString())
            ) > 0

        if (changed) {
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }

    private fun eventForManualReview(
        eventId: Long
    ): InvestmentEventForLinking? =
        database.readableDatabase.rawQuery(
            """
            SELECT event_type,
                   instrument_label,
                   value_minor
            FROM investment_events
            WHERE id = ?
              AND linked_transaction_id IS NULL
              AND settlement_user_decision IS NULL
              AND event_type IN (?, ?)
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                eventId.toString(),
                InvestmentEventType.BUY_EXECUTED.name,
                InvestmentEventType.SELL_EXECUTED.name
            )
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                InvestmentEventForLinking(
                    id = eventId,
                    eventType =
                        InvestmentEventType.valueOf(
                            cursor.getString(0)
                        ),
                    instrumentLabel =
                        cursor.getString(1),
                    valueMinor =
                        if (cursor.isNull(2)) {
                            null
                        } else {
                            cursor.getLong(2)
                        }
                )
            }
        }

    private fun applyObservedPositionDelta(
        fact: SemanticInvestmentFact
    ) {
        val current =
            database.readableDatabase.rawQuery(
                """
                SELECT net_observed_quantity_microunits,
                       quality
                FROM investment_positions
                WHERE instrument_key = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(fact.instrumentKey)
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    cursor.getLong(0) to
                        cursor.getString(1)
                }
            }

        val delta =
            when (fact.action) {
                SemanticInvestmentAction.BUY ->
                    fact.quantityMicrounits
                SemanticInvestmentAction.SELL ->
                    -fact.quantityMicrounits
            }
        val net =
            (current?.first ?: 0L) + delta
        val quality =
            if (
                net < 0L ||
                current?.second ==
                    POSITION_QUALITY_INCOMPLETE
            ) {
                POSITION_QUALITY_INCOMPLETE
            } else {
                POSITION_QUALITY_OBSERVED_EVENTS
            }

        val values = ContentValues().apply {
            put(
                "instrument_key",
                fact.instrumentKey
            )
            put(
                "instrument_label",
                fact.instrumentLabel
            )
            put(
                "net_observed_quantity_microunits",
                net
            )
            put("quality", quality)
            put(
                "updated_at_epoch_ms",
                System.currentTimeMillis()
            )
        }
        database.writableDatabase
            .insertWithOnConflict(
                "investment_positions",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
    }

    private fun eventForLinking(
        eventId: Long
    ): InvestmentEventForLinking? =
        database.readableDatabase.rawQuery(
            """
            SELECT event_type,
                   instrument_label,
                   value_minor
            FROM investment_events
            WHERE id = ?
              AND linked_transaction_id IS NULL
              AND settlement_user_decision IS NULL
            LIMIT 1
            """.trimIndent(),
            arrayOf(eventId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                InvestmentEventForLinking(
                    id = eventId,
                    eventType =
                        InvestmentEventType.valueOf(
                            cursor.getString(0)
                        ),
                    instrumentLabel =
                        cursor.getString(1),
                    valueMinor =
                        if (cursor.isNull(2)) {
                            null
                        } else {
                            cursor.getLong(2)
                        }
                )
            }
        }

    private fun findExistingEventByReference(
        eventType: InvestmentEventType,
        fact: SemanticInvestmentFact,
        references: Set<String>
    ): Long? {
        if (references.isEmpty()) {
            return null
        }

        val placeholders =
            references.joinToString(",") { "?" }
        val valueClause =
            if (fact.valueMinor == null) {
                "e.value_minor IS NULL"
            } else {
                "e.value_minor = ?"
            }
        val args =
            buildList {
                add(eventType.name)
                add(fact.instrumentKey)
                add(
                    fact.quantityMicrounits
                        .toString()
                )
                if (fact.valueMinor != null) {
                    add(fact.valueMinor.toString())
                }
                addAll(references)
            }.toTypedArray()

        val matches =
            database.readableDatabase.rawQuery(
                """
                SELECT DISTINCT e.id
                FROM investment_events e
                JOIN investment_event_references r
                  ON r.investment_event_id = e.id
                WHERE e.event_type = ?
                  AND e.instrument_key = ?
                  AND e.quantity_microunits = ?
                  AND $valueClause
                  AND r.reference_value
                        IN ($placeholders)
                ORDER BY e.id ASC
                LIMIT 2
                """.trimIndent(),
                args
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

        return matches.singleOrNull()
    }

    private fun eventId(
        eventKey: String
    ): Long? =
        database.readableDatabase.rawQuery(
            """
            SELECT id
            FROM investment_events
            WHERE event_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(eventKey)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                null
            }
        }

    private fun buildEventKey(
        messageFingerprint: String,
        eventType: InvestmentEventType,
        fact: SemanticInvestmentFact,
        references: Set<String>
    ): String {
        val identity =
            references
                .sorted()
                .takeIf { it.isNotEmpty() }
                ?.joinToString(",")
                ?: messageFingerprint

        return listOf(
            "investment",
            identity,
            eventType.name,
            fact.instrumentKey,
            fact.quantityMicrounits.toString(),
            fact.valueMinor?.toString().orEmpty()
        ).joinToString("|")
    }

    private fun eventType(
        fact: SemanticInvestmentFact
    ): InvestmentEventType =
        when (fact.stage) {
            SemanticInvestmentStage.ORDER_SUBMITTED ->
                InvestmentEventType.ORDER_SUBMITTED
            SemanticInvestmentStage.EXECUTED ->
                when (fact.action) {
                    SemanticInvestmentAction.BUY ->
                        InvestmentEventType.BUY_EXECUTED
                    SemanticInvestmentAction.SELL ->
                        InvestmentEventType.SELL_EXECUTED
                }
        }

    private fun normalizeReference(
        value: String
    ): String? =
        value.trim()
            .uppercase()
            .replace(
                Regex("""[^A-Z0-9-]"""),
                ""
            )
            .takeIf { it.length >= 5 }

    private data class InvestmentEventForLinking(
        val id: Long,
        val eventType: InvestmentEventType,
        val instrumentLabel: String,
        val valueMinor: Long?
    )

    companion object {
        private const val DECISION_KEEP_UNLINKED =
            "KEEP_UNLINKED"
        private const val DECISION_LINKED =
            "LINKED"

        private const val
            POSITION_QUALITY_OBSERVED_EVENTS =
            "OBSERVED_EVENTS_ONLY"
        private const val
            POSITION_QUALITY_INCOMPLETE =
            "INCOMPLETE_HISTORY"

        @Volatile
        private var instance: InvestmentStore? = null

        fun get(context: Context): InvestmentStore =
            instance ?: synchronized(this) {
                instance
                    ?: InvestmentStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
