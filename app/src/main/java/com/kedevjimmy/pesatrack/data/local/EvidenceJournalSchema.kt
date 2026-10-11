package com.kedevjimmy.pesatrack.data.local

import android.database.sqlite.SQLiteDatabase

/** Provenance only. No cash movement is created by this schema or its triggers. */
internal object EvidenceJournalSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_observations (
                id TEXT PRIMARY KEY NOT NULL CHECK(length(id)=64),
                source_kind TEXT NOT NULL CHECK(source_kind IN ('SMS','NOTIFICATION','MANUAL','STATEMENT')),
                source_scope_hash TEXT NOT NULL CHECK(length(source_scope_hash)=64),
                source_key_hash TEXT NOT NULL CHECK(length(source_key_hash)=64),
                fact_hash TEXT NOT NULL CHECK(length(fact_hash)=64),
                account_id TEXT NOT NULL,
                direction TEXT NOT NULL CHECK(direction IN ('INCOME','EXPENSE')),
                amount_minor INTEGER NOT NULL CHECK(amount_minor>0),
                occurred_at_epoch_ms INTEGER NOT NULL CHECK(occurred_at_epoch_ms>0),
                reference_hash TEXT CHECK(reference_hash IS NULL OR length(reference_hash)=64),
                status TEXT NOT NULL CHECK(status IN ('PENDING','LINKED','REJECTED')),
                created_at_epoch_ms INTEGER NOT NULL CHECK(created_at_epoch_ms>0),
                UNIQUE(source_kind,source_scope_hash,source_key_hash),
                FOREIGN KEY(account_id) REFERENCES money_accounts(id) ON DELETE CASCADE
                    DEFERRABLE INITIALLY DEFERRED
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_links (
                observation_id TEXT PRIMARY KEY NOT NULL,
                transaction_id INTEGER NOT NULL,
                reason TEXT NOT NULL CHECK(reason IN ('CREATED','REFERENCE')),
                transaction_snapshot_hash TEXT NOT NULL CHECK(length(transaction_snapshot_hash)=64),
                created_at_epoch_ms INTEGER NOT NULL CHECK(created_at_epoch_ms>0),
                FOREIGN KEY(observation_id) REFERENCES evidence_observations(id) ON DELETE CASCADE
                    DEFERRABLE INITIALLY DEFERRED,
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
                    DEFERRABLE INITIALLY DEFERRED
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_link_audit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                observation_id TEXT NOT NULL,
                transaction_id INTEGER,
                action TEXT NOT NULL CHECK(action IN ('LINKED','UNLINKED')),
                reason TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL CHECK(created_at_epoch_ms>0)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_evidence_link_transaction ON evidence_links(transaction_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_evidence_pending ON evidence_observations(status,occurred_at_epoch_ms)")
        // Run before the FK cascade removes the link, so backup/restore never sees
        // LINKED evidence with no canonical transaction. Do not silently re-link it.
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS trg_evidence_reject_deleted_transaction BEFORE DELETE ON transactions
            BEGIN
                UPDATE evidence_observations SET status='REJECTED'
                WHERE id IN (SELECT observation_id FROM evidence_links WHERE transaction_id=OLD.id);
            END
        """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS trg_evidence_remove_transaction AFTER DELETE ON transactions
            BEGIN DELETE FROM evidence_links WHERE transaction_id=OLD.id; END
        """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS trg_evidence_remove_observation AFTER DELETE ON evidence_observations
            BEGIN DELETE FROM evidence_links WHERE observation_id=OLD.id; END
        """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS trg_evidence_remove_account AFTER DELETE ON money_accounts
            BEGIN DELETE FROM evidence_observations WHERE account_id=OLD.id; END
        """.trimIndent())
    }
}
