package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import java.util.UUID

class AccountStore private constructor(context: Context) {
    private val appContext =
        context.applicationContext
    private val database =
        PesaTrackDatabase(appContext)

    @Synchronized
    fun listAccounts(enabledOnly: Boolean = true): List<MoneyAccount> {
        val where = if (enabledOnly) "WHERE enabled = 1" else ""
        return database.readableDatabase.rawQuery(
            """
            SELECT id, name, type, enabled, identifier,
                   opening_balance_minor, opening_balance_as_of_epoch_ms
            FROM money_accounts
            $where
            ORDER BY
                CASE type
                    WHEN 'MOBILE_WALLET' THEN 0
                    WHEN 'BANK' THEN 1
                    WHEN 'SAVINGS' THEN 2
                    WHEN 'CASH' THEN 3
                    ELSE 4
                END,
                name COLLATE NOCASE
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MoneyAccount(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            type = AccountType.valueOf(cursor.getString(2)),
                            enabled = cursor.getInt(3) == 1,
                            identifier = cursor.getStringOrNull(4),
                            openingBalanceMinor = cursor.getLongOrNull(5),
                            openingBalanceAsOfEpochMs = cursor.getLongOrNull(6)
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun save(
        name: String,
        type: AccountType,
        id: String = UUID.randomUUID().toString()
    ): MoneyAccount {
        require(name.isNotBlank()) { "Give this account a name." }
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", id)
            put("name", name.trim())
            put("type", type.name)
            put("enabled", 1)
            put("created_at_epoch_ms", now)
            put("updated_at_epoch_ms", now)
        }
        database.writableDatabase.insertWithOnConflict(
            "money_accounts",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        val account =
            MoneyAccount(
                id = id,
                name = name.trim(),
                type = type
            )
        IdentityGraphStore
            .get(appContext)
            .observeOwnedAccount(account)
        return account
    }

    @Synchronized
    fun findOrCreate(
        name: String,
        type: AccountType
    ): MoneyAccount {
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "Give this account a name." }

        val existing = database.readableDatabase.rawQuery(
            """
            SELECT id, name, type, enabled, identifier,
                   opening_balance_minor, opening_balance_as_of_epoch_ms
            FROM money_accounts
            WHERE LOWER(name) = LOWER(?)
              AND type = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(normalized, type.name)
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                MoneyAccount(
                    id = cursor.getString(0),
                    name = cursor.getString(1),
                    type = AccountType.valueOf(cursor.getString(2)),
                    enabled = cursor.getInt(3) == 1,
                    identifier = cursor.getStringOrNull(4),
                    openingBalanceMinor = cursor.getLongOrNull(5),
                    openingBalanceAsOfEpochMs = cursor.getLongOrNull(6)
                )
            }
        }

        return existing ?: save(normalized, type)
    }

    @Synchronized
    fun updateDetails(
        id: String,
        identifier: String?,
        openingBalanceMinor: Long?,
        openingBalanceAsOfEpochMs: Long? = System.currentTimeMillis()
    ) {
        val account = listAccounts(enabledOnly = false)
            .firstOrNull { it.id == id }
            ?: error("Account not found.")

        val normalizedIdentifier = when {
            identifier.isNullOrBlank() -> null
            account.type == AccountType.MOBILE_WALLET ->
                OwnAccountTransferDetector.normalizeKenyanPhone(identifier)
                    ?: throw IllegalArgumentException(
                        "Enter a valid Kenyan mobile number, for example 0712345678."
                    )
            else -> identifier.trim()
        }

        val values = ContentValues().apply {
            if (normalizedIdentifier == null) putNull("identifier") else put("identifier", normalizedIdentifier)
            if (openingBalanceMinor == null) {
                putNull("opening_balance_minor")
                putNull("opening_balance_as_of_epoch_ms")
            } else {
                put("opening_balance_minor", openingBalanceMinor)
                put(
                    "opening_balance_as_of_epoch_ms",
                    openingBalanceAsOfEpochMs ?: System.currentTimeMillis()
                )
            }
            put("updated_at_epoch_ms", System.currentTimeMillis())
        }

        database.writableDatabase.update(
            "money_accounts",
            values,
            "id = ?",
            arrayOf(id)
        )
        IdentityGraphStore
            .get(appContext)
            .observeOwnedAccount(
                account.copy(
                    identifier =
                        normalizedIdentifier,
                    openingBalanceMinor =
                        openingBalanceMinor,
                    openingBalanceAsOfEpochMs =
                        if (
                            openingBalanceMinor ==
                                null
                        ) {
                            null
                        } else {
                            openingBalanceAsOfEpochMs
                                ?: System
                                    .currentTimeMillis()
                        }
                )
            )
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        val values = ContentValues().apply {
            put("enabled", if (enabled) 1 else 0)
            put("updated_at_epoch_ms", System.currentTimeMillis())
        }
        database.writableDatabase.update(
            "money_accounts",
            values,
            "id = ?",
            arrayOf(id)
        )
    }

    companion object {
        const val DEFAULT_MPESA_ID = "account-mpesa"
        const val DEFAULT_CASH_ID = "account-cash"

        @Volatile
        private var instance: AccountStore? = null

        fun get(context: Context): AccountStore =
            instance ?: synchronized(this) {
                instance ?: AccountStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)
