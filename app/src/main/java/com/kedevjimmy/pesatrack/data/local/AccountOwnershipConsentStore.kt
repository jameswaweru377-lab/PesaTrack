package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** Explicit B2 consent only. Discovery, account existence and a name match never call this. */
class AccountOwnershipConsentStore(context: Context) {
    private val accounts = AccountStore.get(context.applicationContext)
    private val database = PesaTrackDatabase.get(context.applicationContext)
    fun createOwnedAccount(name: String, type: com.kedevjimmy.pesatrack.domain.model.AccountType): com.kedevjimmy.pesatrack.domain.model.MoneyAccount =
        synchronized(CanonicalStoreLock.monitor) {
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                val account = accounts.save(name, type)
                record(db, account.id, true)
                db.setTransactionSuccessful()
                account
            } finally { db.endTransaction() }
        }
    fun recordConsent(accountId: String, owned: Boolean): Long = synchronized(CanonicalStoreLock.monitor) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val revision = record(db, accountId, owned)
            db.setTransactionSuccessful()
            revision
        } finally { db.endTransaction() }
    }
    fun confirmedRevisions(): Map<String, Long> = synchronized(CanonicalStoreLock.monitor) {
        confirmed(database.readableDatabase)
    }

    internal companion object {
        fun record(db: SQLiteDatabase, accountId: String, owned: Boolean): Long {
            check(db.inTransaction()) { "Ownership consent must be saved atomically." }
            require(db.rawQuery("SELECT 1 FROM money_accounts WHERE id=?", arrayOf(accountId)).use { it.moveToFirst() }) { "Account no longer exists." }
            val old = db.rawQuery("SELECT revision FROM account_ownership_consents WHERE account_id=?", arrayOf(accountId)).use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
            require(old < Long.MAX_VALUE) { "Account consent revision cannot be advanced." }
            val revision = old + 1
            val values = ContentValues().apply {
                put("account_id", accountId); put("decision", if (owned) "CONFIRMED" else "REJECTED")
                put("revision", revision); put("updated_at_epoch_ms", System.currentTimeMillis())
            }
            if (old == 0L) db.insertOrThrow("account_ownership_consents", null, values)
            else check(db.update("account_ownership_consents", values, "account_id=?", arrayOf(accountId)) == 1)
            return revision
        }
        fun confirmed(db: SQLiteDatabase): Map<String, Long> = db.rawQuery("""
            SELECT c.account_id,c.revision FROM account_ownership_consents c
            JOIN money_accounts a ON a.id=c.account_id
            WHERE c.decision='CONFIRMED' AND a.enabled=1
        """.trimIndent(), null).use { cursor -> buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getLong(1)) } }
    }
}
