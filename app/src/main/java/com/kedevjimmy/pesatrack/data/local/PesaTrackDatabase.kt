package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class PesaTrackDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_code TEXT,
                fingerprint TEXT NOT NULL UNIQUE,
                type TEXT NOT NULL,
                amount_minor INTEGER NOT NULL,
                fee_minor INTEGER NOT NULL,
                balance_minor INTEGER,
                counterparty TEXT,
                occurred_at_text TEXT,
                received_at_epoch_ms INTEGER NOT NULL,
                source_package TEXT,
                source_kind TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX idx_transactions_code ON transactions(transaction_code) WHERE transaction_code IS NOT NULL"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_received_at ON transactions(received_at_epoch_ms DESC)"
        )
        db.execSQL(
            "CREATE INDEX idx_transactions_balance ON transactions(balance_minor) WHERE balance_minor IS NOT NULL"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema v1. Future versions must use explicit migrations; financial data
        // must never be silently destroyed.
    }

    companion object {
        private const val DATABASE_NAME = "pesatrack.db"
        private const val DATABASE_VERSION = 1
    }
}
