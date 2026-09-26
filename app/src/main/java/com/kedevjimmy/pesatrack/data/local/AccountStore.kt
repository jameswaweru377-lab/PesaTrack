package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import java.util.UUID

class AccountStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context)

    @Synchronized
    fun listAccounts(enabledOnly: Boolean = true): List<MoneyAccount> {
        val where = if (enabledOnly) "WHERE enabled = 1" else ""
        return database.readableDatabase.rawQuery(
            """
            SELECT id, name, type, enabled
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
                            enabled = cursor.getInt(3) == 1
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
        return MoneyAccount(id = id, name = name.trim(), type = type)
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
