package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState

enum class PaydayBalanceQuality {
    OBSERVED,
    RECONSTRUCTED,
    USER_ENTERED,
    UNKNOWN
}

enum class PaydaySpendability {
    SPENDABLE,
    PROTECTED
}

data class PaydaySalaryAnchor(
    val transactionId: Long,
    val occurredAtEpochMs: Long,
    val amountMinor: Long,
    val accountId: String?
)

data class PaydayAccountSnapshot(
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val balanceMinor: Long?,
    val quality: PaydayBalanceQuality,
    val referenceEpochMs: Long?,
    val spendability: PaydaySpendability
)

data class PaydaySnapshot(
    val id: String,
    val salaryTransactionId: Long,
    val salaryEpochMs: Long,
    val salaryAccountId: String?,
    val salaryAmountMinor: Long,
    val liquidCarryoverMinor: Long,
    val protectedSavingsMinor: Long,
    val debtOutstandingMinor: Long,
    val unknownLiquidAccountCount: Int,
    val knownLiquidAccountCount: Int,
    val accounts: List<PaydayAccountSnapshot>
) {
    val hasCompleteLiquidPicture: Boolean
        get() =
            knownLiquidAccountCount > 0 &&
                unknownLiquidAccountCount == 0
}

class PaydaySnapshotStore private constructor(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val database =
        PesaTrackDatabase(appContext)
    private val accountStore =
        AccountStore.get(appContext)

    @Synchronized
    fun rebuildForSalaryAnchors(
        anchors: List<PaydaySalaryAnchor>
    ) {
        val db =
            database.writableDatabase
        val validIds =
            anchors.map {
                snapshotId(
                    it.transactionId
                )
            }.toSet()

        db.beginTransaction()
        try {
            val existingIds =
                db.rawQuery(
                    """
                    SELECT id
                    FROM payday_snapshots
                    """.trimIndent(),
                    null
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

            existingIds
                .filterNot {
                    it in validIds
                }
                .forEach { staleId ->
                    db.delete(
                        "payday_snapshot_accounts",
                        "snapshot_id = ?",
                        arrayOf(staleId)
                    )
                    db.delete(
                        "payday_snapshots",
                        "id = ?",
                        arrayOf(staleId)
                    )
                }

            anchors.forEach {
                rebuildSnapshot(
                    db = db,
                    anchor = it
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun snapshotForSalaryTransaction(
        transactionId: Long
    ): PaydaySnapshot? =
        readSnapshot(
            snapshotId(transactionId)
        )

    @Synchronized
    fun snapshotForCycleStart(
        cycleStartTransactionId: Long
    ): PaydaySnapshot? =
        snapshotForSalaryTransaction(
            cycleStartTransactionId
        )

    private fun rebuildSnapshot(
        db: SQLiteDatabase,
        anchor: PaydaySalaryAnchor
    ) {
        val snapshotId =
            snapshotId(
                anchor.transactionId
            )
        val snapshotEpoch =
            maxOf(
                0L,
                anchor.occurredAtEpochMs - 1L
            )

        val accounts =
            accountStore
                .listAccounts(
                    enabledOnly = true
                )
                .filter {
                    it.type in
                        TRACKED_ACCOUNT_TYPES
                }
                .filter {
                    shouldIncludeAccount(
                        db = db,
                        account = it,
                        snapshotEpochMs =
                            snapshotEpoch,
                        salaryAccountId =
                            anchor.accountId
                    )
                }
                .map {
                    accountSnapshotAt(
                        db = db,
                        account = it,
                        snapshotEpochMs =
                            snapshotEpoch
                    )
                }

        val liquid =
            accounts.filter {
                it.spendability ==
                    PaydaySpendability
                        .SPENDABLE
            }
        val protected =
            accounts.filter {
                it.spendability ==
                    PaydaySpendability
                        .PROTECTED
            }

        val liquidCarryoverMinor =
            liquid
                .mapNotNull {
                    it.balanceMinor
                }
                .sum()
        val protectedSavingsMinor =
            protected
                .mapNotNull {
                    it.balanceMinor
                }
                .sum()
        val unknownLiquidCount =
            liquid.count {
                it.balanceMinor == null
            }
        val knownLiquidCount =
            liquid.size -
                unknownLiquidCount

        val debtOutstandingMinor =
            debtOutstandingAt(
                db = db,
                snapshotEpochMs =
                    snapshotEpoch
            )

        val now =
            System.currentTimeMillis()
        val values =
            ContentValues().apply {
                put("id", snapshotId)
                put(
                    "salary_transaction_id",
                    anchor.transactionId
                )
                put(
                    "salary_epoch_ms",
                    anchor.occurredAtEpochMs
                )
                if (
                    anchor.accountId !=
                    null
                ) {
                    put(
                        "salary_account_id",
                        anchor.accountId
                    )
                } else {
                    putNull(
                        "salary_account_id"
                    )
                }
                put(
                    "salary_amount_minor",
                    anchor.amountMinor
                )
                put(
                    "liquid_carryover_minor",
                    liquidCarryoverMinor
                )
                put(
                    "protected_savings_minor",
                    protectedSavingsMinor
                )
                put(
                    "debt_outstanding_minor",
                    debtOutstandingMinor
                )
                put(
                    "unknown_liquid_account_count",
                    unknownLiquidCount
                )
                put(
                    "known_liquid_account_count",
                    knownLiquidCount
                )
                put(
                    "created_at_epoch_ms",
                    now
                )
                put(
                    "updated_at_epoch_ms",
                    now
                )
            }

        db.insertWithOnConflict(
            "payday_snapshots",
            null,
            values,
            SQLiteDatabase
                .CONFLICT_REPLACE
        )

        db.delete(
            "payday_snapshot_accounts",
            "snapshot_id = ?",
            arrayOf(snapshotId)
        )
        accounts.forEach { account ->
            val accountValues =
                ContentValues().apply {
                    put(
                        "snapshot_id",
                        snapshotId
                    )
                    put(
                        "account_id",
                        account.accountId
                    )
                    put(
                        "account_name",
                        account.accountName
                    )
                    put(
                        "account_type",
                        account.accountType.name
                    )
                    if (
                        account.balanceMinor !=
                        null
                    ) {
                        put(
                            "balance_minor",
                            account.balanceMinor
                        )
                    } else {
                        putNull(
                            "balance_minor"
                        )
                    }
                    put(
                        "balance_quality",
                        account.quality.name
                    )
                    if (
                        account.referenceEpochMs !=
                        null
                    ) {
                        put(
                            "reference_epoch_ms",
                            account.referenceEpochMs
                        )
                    } else {
                        putNull(
                            "reference_epoch_ms"
                        )
                    }
                    put(
                        "spendability",
                        account.spendability.name
                    )
                }
            db.insertWithOnConflict(
                "payday_snapshot_accounts",
                null,
                accountValues,
                SQLiteDatabase
                    .CONFLICT_REPLACE
            )
        }
    }

    private fun shouldIncludeAccount(
        db: SQLiteDatabase,
        account: MoneyAccount,
        snapshotEpochMs: Long,
        salaryAccountId: String?
    ): Boolean {
        if (
            account.id ==
            AccountStore.DEFAULT_MPESA_ID &&
            account.identifier.isNullOrBlank()
        ) {
            return false
        }

        if (
            account.id ==
            AccountStore.DEFAULT_CASH_ID
        ) {
            return account.openingBalanceMinor !=
                null ||
                hasAccountActivityBy(
                    db,
                    account.id,
                    snapshotEpochMs
                )
        }

        if (
            account.id ==
            salaryAccountId
        ) {
            return true
        }

        if (
            account.openingBalanceMinor !=
            null
        ) {
            return true
        }

        return isConfirmedAccount(
            db,
            account.id
        ) ||
            hasAccountActivityBy(
                db,
                account.id,
                snapshotEpochMs
            )
    }

    private fun isConfirmedAccount(
        db: SQLiteDatabase,
        accountId: String
    ): Boolean =
        db.rawQuery(
            """
            SELECT 1
            FROM financial_entities
            WHERE confirmed_account_id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(accountId)
        ).use {
            it.moveToFirst()
        }

    private fun hasAccountActivityBy(
        db: SQLiteDatabase,
        accountId: String,
        snapshotEpochMs: Long
    ): Boolean =
        db.rawQuery(
            """
            SELECT 1
            FROM transactions
            WHERE (
                    account_id = ?
                    OR linked_account_id = ?
                  )
              AND occurred_at_epoch_ms <= ?
              AND review_state != ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                accountId,
                accountId,
                snapshotEpochMs.toString(),
                ReviewState.EXCLUDED.name
            )
        ).use {
            it.moveToFirst()
        }

    private fun accountSnapshotAt(
        db: SQLiteDatabase,
        account: MoneyAccount,
        snapshotEpochMs: Long
    ): PaydayAccountSnapshot {
        val prior =
            latestBalanceAnchorAtOrBefore(
                db = db,
                accountId = account.id,
                epochMs =
                    snapshotEpochMs
            )

        if (prior != null) {
            val delta =
                accountDelta(
                    db = db,
                    accountId =
                        account.id,
                    fromExclusive =
                        prior.epochMs,
                    toInclusive =
                        snapshotEpochMs
                )
            val exact =
                delta == 0L &&
                    snapshotEpochMs -
                        prior.epochMs <=
                        DIRECT_OBSERVATION_WINDOW_MS
            return PaydayAccountSnapshot(
                accountId = account.id,
                accountName = account.name,
                accountType = account.type,
                balanceMinor =
                    maxOf(
                        0L,
                        prior.balanceMinor +
                            delta
                    ),
                quality =
                    if (exact) {
                        prior.quality
                    } else {
                        PaydayBalanceQuality
                            .RECONSTRUCTED
                    },
                referenceEpochMs =
                    prior.epochMs,
                spendability =
                    spendability(
                        account.type
                    )
            )
        }

        val openingBalance =
            account.openingBalanceMinor
        val openingEpoch =
            account.openingBalanceAsOfEpochMs
        if (
            openingBalance != null &&
            openingEpoch != null &&
            openingEpoch <= snapshotEpochMs
        ) {
            val delta =
                accountDelta(
                    db = db,
                    accountId = account.id,
                    fromExclusive =
                        openingEpoch,
                    toInclusive =
                        snapshotEpochMs
                )
            return PaydayAccountSnapshot(
                accountId = account.id,
                accountName = account.name,
                accountType = account.type,
                balanceMinor =
                    maxOf(
                        0L,
                        openingBalance +
                            delta
                    ),
                quality =
                    if (
                        openingEpoch ==
                        snapshotEpochMs
                    ) {
                        PaydayBalanceQuality
                            .USER_ENTERED
                    } else {
                        PaydayBalanceQuality
                            .RECONSTRUCTED
                    },
                referenceEpochMs =
                    openingEpoch,
                spendability =
                    spendability(
                        account.type
                    )
            )
        }

        val later =
            earliestBalanceAnchorAfter(
                db = db,
                accountId = account.id,
                epochMs =
                    snapshotEpochMs
            )

        if (
            later != null &&
            later.epochMs -
                snapshotEpochMs <=
                MAX_BACKCAST_WINDOW_MS
        ) {
            val laterDelta =
                accountDelta(
                    db = db,
                    accountId =
                        account.id,
                    fromExclusive =
                        snapshotEpochMs,
                    toInclusive =
                        later.epochMs
                )
            return PaydayAccountSnapshot(
                accountId = account.id,
                accountName = account.name,
                accountType = account.type,
                balanceMinor =
                    maxOf(
                        0L,
                        later.balanceMinor -
                            laterDelta
                    ),
                quality =
                    PaydayBalanceQuality
                        .RECONSTRUCTED,
                referenceEpochMs =
                    later.epochMs,
                spendability =
                    spendability(
                        account.type
                    )
            )
        }

        return PaydayAccountSnapshot(
            accountId = account.id,
            accountName = account.name,
            accountType = account.type,
            balanceMinor = null,
            quality =
                PaydayBalanceQuality
                    .UNKNOWN,
            referenceEpochMs = null,
            spendability =
                spendability(
                    account.type
                )
        )
    }

    private fun latestBalanceAnchorAtOrBefore(
        db: SQLiteDatabase,
        accountId: String,
        epochMs: Long
    ): BalanceAnchor? =
        db.rawQuery(
            """
            SELECT balance_minor,
                   observed_at_epoch_ms,
                   confidence,
                   0 AS source_priority
            FROM account_balance_observations
            WHERE account_id = ?
              AND observed_at_epoch_ms <= ?

            UNION ALL

            SELECT balance_minor,
                   occurred_at_epoch_ms,
                   COALESCE(
                       evidence_confidence,
                       'MEDIUM'
                   ),
                   1 AS source_priority
            FROM transactions
            WHERE account_id = ?
              AND balance_minor IS NOT NULL
              AND occurred_at_epoch_ms <= ?
              AND review_state != ?

            ORDER BY observed_at_epoch_ms DESC,
                     source_priority ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                accountId,
                epochMs.toString(),
                accountId,
                epochMs.toString(),
                ReviewState.EXCLUDED.name
            )
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                BalanceAnchor(
                    balanceMinor =
                        cursor.getLong(0),
                    epochMs =
                        cursor.getLong(1),
                    quality =
                        PaydayBalanceQuality
                            .OBSERVED
                )
            }
        }

    private fun earliestBalanceAnchorAfter(
        db: SQLiteDatabase,
        accountId: String,
        epochMs: Long
    ): BalanceAnchor? =
        db.rawQuery(
            """
            SELECT balance_minor,
                   observed_at_epoch_ms,
                   confidence,
                   0 AS source_priority
            FROM account_balance_observations
            WHERE account_id = ?
              AND observed_at_epoch_ms > ?

            UNION ALL

            SELECT balance_minor,
                   occurred_at_epoch_ms,
                   COALESCE(
                       evidence_confidence,
                       'MEDIUM'
                   ),
                   1 AS source_priority
            FROM transactions
            WHERE account_id = ?
              AND balance_minor IS NOT NULL
              AND occurred_at_epoch_ms > ?
              AND review_state != ?

            ORDER BY observed_at_epoch_ms ASC,
                     source_priority ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                accountId,
                epochMs.toString(),
                accountId,
                epochMs.toString(),
                ReviewState.EXCLUDED.name
            )
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                BalanceAnchor(
                    balanceMinor =
                        cursor.getLong(0),
                    epochMs =
                        cursor.getLong(1),
                    quality =
                        PaydayBalanceQuality
                            .OBSERVED
                )
            }
        }

    private fun accountDelta(
        db: SQLiteDatabase,
        accountId: String,
        fromExclusive: Long,
        toInclusive: Long
    ): Long {
        if (
            toInclusive <=
            fromExclusive
        ) {
            return 0L
        }

        return db.rawQuery(
            """
            SELECT amount_minor,
                   fee_minor,
                   direction,
                   account_id,
                   linked_account_id
            FROM transactions
            WHERE occurred_at_epoch_ms > ?
              AND occurred_at_epoch_ms <= ?
              AND review_state != ?
              AND review_state != ?
              AND type != ?
              AND (
                    account_id = ?
                    OR linked_account_id = ?
                  )
            ORDER BY occurred_at_epoch_ms ASC,
                     id ASC
            """.trimIndent(),
            arrayOf(
                fromExclusive.toString(),
                toInclusive.toString(),
                ReviewState.EXCLUDED.name,
                ReviewState.NEEDS_REVIEW.name,
                MpesaTransactionType.REVERSAL.name,
                accountId,
                accountId
            )
        ).use { cursor ->
            var delta = 0L
            while (
                cursor.moveToNext()
            ) {
                val amount =
                    cursor.getLong(0)
                val fee =
                    cursor.getLong(1)
                val direction =
                    runCatching {
                        MoneyDirection.valueOf(
                            cursor.getString(2)
                        )
                    }.getOrDefault(
                        MoneyDirection.NEUTRAL
                    )
                val source =
                    if (
                        cursor.isNull(3)
                    ) {
                        null
                    } else {
                        cursor.getString(3)
                    }
                val destination =
                    if (
                        cursor.isNull(4)
                    ) {
                        null
                    } else {
                        cursor.getString(4)
                    }

                if (
                    source == accountId
                ) {
                    delta +=
                        when (direction) {
                            MoneyDirection.INCOME ->
                                amount
                            MoneyDirection.EXPENSE ->
                                -(amount + fee)
                            MoneyDirection.TRANSFER ->
                                if (
                                    destination ==
                                    accountId
                                ) {
                                    -fee
                                } else {
                                    -(amount + fee)
                                }
                            MoneyDirection.NEUTRAL ->
                                0L
                        }
                }

                if (
                    destination == accountId &&
                    source != accountId &&
                    direction ==
                    MoneyDirection.TRANSFER
                ) {
                    delta += amount
                }
            }
            delta
        }
    }

    private fun debtOutstandingAt(
        db: SQLiteDatabase,
        snapshotEpochMs: Long
    ): Long {
        val debtIds =
            db.rawQuery(
                """
                SELECT id
                FROM debt_accounts
                """.trimIndent(),
                null
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

        return debtIds.sumOf {
                debtId ->
            db.rawQuery(
                """
                SELECT outstanding_after_minor
                FROM debt_events
                WHERE debt_account_id = ?
                  AND occurred_at_epoch_ms <= ?
                  AND outstanding_after_minor
                        IS NOT NULL
                ORDER BY occurred_at_epoch_ms DESC,
                         id DESC
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    debtId,
                    snapshotEpochMs.toString()
                )
            ).use { cursor ->
                if (
                    cursor.moveToFirst()
                ) {
                    maxOf(
                        0L,
                        cursor.getLong(0)
                    )
                } else {
                    0L
                }
            }
        }
    }

    private fun readSnapshot(
        id: String
    ): PaydaySnapshot? {
        val row =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT id,
                           salary_transaction_id,
                           salary_epoch_ms,
                           salary_account_id,
                           salary_amount_minor,
                           liquid_carryover_minor,
                           protected_savings_minor,
                           debt_outstanding_minor,
                           unknown_liquid_account_count,
                           known_liquid_account_count
                    FROM payday_snapshots
                    WHERE id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(id)
                ).use { cursor ->
                    if (
                        !cursor.moveToFirst()
                    ) {
                        null
                    } else {
                        SnapshotRow(
                            id =
                                cursor.getString(0),
                            salaryTransactionId =
                                cursor.getLong(1),
                            salaryEpochMs =
                                cursor.getLong(2),
                            salaryAccountId =
                                if (
                                    cursor.isNull(3)
                                ) {
                                    null
                                } else {
                                    cursor.getString(3)
                                },
                            salaryAmountMinor =
                                cursor.getLong(4),
                            liquidCarryoverMinor =
                                cursor.getLong(5),
                            protectedSavingsMinor =
                                cursor.getLong(6),
                            debtOutstandingMinor =
                                cursor.getLong(7),
                            unknownLiquidAccountCount =
                                cursor.getInt(8),
                            knownLiquidAccountCount =
                                cursor.getInt(9)
                        )
                    }
                }
                ?: return null

        val accounts =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT account_id,
                           account_name,
                           account_type,
                           balance_minor,
                           balance_quality,
                           reference_epoch_ms,
                           spendability
                    FROM payday_snapshot_accounts
                    WHERE snapshot_id = ?
                    ORDER BY account_name
                        COLLATE NOCASE
                    """.trimIndent(),
                    arrayOf(id)
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                PaydayAccountSnapshot(
                                    accountId =
                                        cursor.getString(0),
                                    accountName =
                                        cursor.getString(1),
                                    accountType =
                                        AccountType.valueOf(
                                            cursor.getString(2)
                                        ),
                                    balanceMinor =
                                        if (
                                            cursor.isNull(3)
                                        ) {
                                            null
                                        } else {
                                            cursor.getLong(3)
                                        },
                                    quality =
                                        PaydayBalanceQuality
                                            .valueOf(
                                                cursor.getString(4)
                                            ),
                                    referenceEpochMs =
                                        if (
                                            cursor.isNull(5)
                                        ) {
                                            null
                                        } else {
                                            cursor.getLong(5)
                                        },
                                    spendability =
                                        PaydaySpendability
                                            .valueOf(
                                                cursor.getString(6)
                                            )
                                )
                            )
                        }
                    }
                }

        return PaydaySnapshot(
            id = row.id,
            salaryTransactionId =
                row.salaryTransactionId,
            salaryEpochMs =
                row.salaryEpochMs,
            salaryAccountId =
                row.salaryAccountId,
            salaryAmountMinor =
                row.salaryAmountMinor,
            liquidCarryoverMinor =
                row.liquidCarryoverMinor,
            protectedSavingsMinor =
                row.protectedSavingsMinor,
            debtOutstandingMinor =
                row.debtOutstandingMinor,
            unknownLiquidAccountCount =
                row.unknownLiquidAccountCount,
            knownLiquidAccountCount =
                row.knownLiquidAccountCount,
            accounts = accounts
        )
    }

    private fun spendability(
        type: AccountType
    ): PaydaySpendability =
        if (
            type ==
            AccountType.SAVINGS
        ) {
            PaydaySpendability.PROTECTED
        } else {
            PaydaySpendability.SPENDABLE
        }

    private fun snapshotId(
        salaryTransactionId: Long
    ): String =
        "payday:$salaryTransactionId"

    private data class BalanceAnchor(
        val balanceMinor: Long,
        val epochMs: Long,
        val quality: PaydayBalanceQuality
    )

    private data class SnapshotRow(
        val id: String,
        val salaryTransactionId: Long,
        val salaryEpochMs: Long,
        val salaryAccountId: String?,
        val salaryAmountMinor: Long,
        val liquidCarryoverMinor: Long,
        val protectedSavingsMinor: Long,
        val debtOutstandingMinor: Long,
        val unknownLiquidAccountCount: Int,
        val knownLiquidAccountCount: Int
    )

    companion object {
        private val TRACKED_ACCOUNT_TYPES =
            setOf(
                AccountType.MOBILE_WALLET,
                AccountType.BANK,
                AccountType.CASH,
                AccountType.SAVINGS
            )
        private const val
            DIRECT_OBSERVATION_WINDOW_MS =
            5L * 60L * 1000L
        private const val
            MAX_BACKCAST_WINDOW_MS =
            60L * 24L * 60L * 60L *
                1000L

        @Volatile
        private var instance:
            PaydaySnapshotStore? =
            null

        fun get(
            context: Context
        ): PaydaySnapshotStore =
            instance ?: synchronized(this) {
                instance
                    ?: PaydaySnapshotStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
