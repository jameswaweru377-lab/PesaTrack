"""Synthetic SQLite verification of the exact Kotlin evidence DDL. No Android instrumentation."""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/data/local/EvidenceJournalSchema.kt"
DATABASE = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/data/local/PesaTrackDatabase.kt"


def database():
    assert "DATABASE_VERSION = 40" in DATABASE.read_text(encoding="utf-8")
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    db.executescript("""
        CREATE TABLE money_accounts (id TEXT PRIMARY KEY NOT NULL);
        CREATE TABLE transactions (id INTEGER PRIMARY KEY NOT NULL);
        INSERT INTO money_accounts(id) VALUES ('account-mpesa');
        INSERT INTO transactions(id) VALUES (1);
    """)
    source = SCHEMA.read_text(encoding="utf-8")
    blocks = re.findall(r'db\.execSQL\("""(.*?)"""\.trimIndent\(\)\)', source, re.DOTALL)
    assert len(blocks) >= 6, "Kotlin evidence schema statements not parsed"
    for statement in blocks:
        db.executescript(statement.strip() + ";\n")
    return db


def observation(key="a", state="LINKED"):
    return (key * 64, "STATEMENT", "b" * 64, "c" * 64, "d" * 64,
            "account-mpesa", "INCOME", 5200000, 1790000000000, None, state, 1000)


class EvidenceJournalSqliteTest(unittest.TestCase):
    def test_insert_link_replay_and_delete_never_change_cash(self):
        db = database()
        self.addCleanup(db.close)
        db.execute("""INSERT INTO evidence_observations
            (id,source_kind,source_scope_hash,source_key_hash,fact_hash,account_id,
            direction,amount_minor,occurred_at_epoch_ms,reference_hash,status,created_at_epoch_ms)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""", observation())
        db.execute("""INSERT INTO evidence_links
            (observation_id,transaction_id,reason,transaction_snapshot_hash,created_at_epoch_ms)
            VALUES (?,?,'REFERENCE',?,?)""", ("a" * 64, 1, "e" * 64, 1000))
        self.assertEqual(db.execute("SELECT COUNT(*) FROM transactions").fetchone()[0], 1)
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("""INSERT INTO evidence_observations
                (id,source_kind,source_scope_hash,source_key_hash,fact_hash,account_id,
                direction,amount_minor,occurred_at_epoch_ms,reference_hash,status,created_at_epoch_ms)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""", observation())
        db.execute("DELETE FROM transactions WHERE id=1")
        self.assertEqual(db.execute("SELECT COUNT(*) FROM evidence_links").fetchone()[0], 0)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM evidence_observations").fetchone()[0], 1)
        self.assertEqual(db.execute("SELECT status FROM evidence_observations").fetchone()[0], "REJECTED")
        # A deleted movement must not be silently resurrected by provenance replay.


    def test_foreign_keys_invalid_amount_and_source_rejected(self):
        db = database()
        self.addCleanup(db.close)
        template = """INSERT INTO evidence_observations
            (id,source_kind,source_scope_hash,source_key_hash,fact_hash,account_id,
            direction,amount_minor,occurred_at_epoch_ms,reference_hash,status,created_at_epoch_ms)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"""
        for bad in (
            list(observation("b"))[:],
            list(observation("c"))[:],
            list(observation("d"))[:],
        ):
            if bad[0] == "b" * 64: bad[7] = 0
            if bad[0] == "c" * 64: bad[1] = "PAYSLIP"
            if bad[0] == "d" * 64: bad[5] = "account-not-owned"
            if bad[0] == "d" * 64:
                # The account FK is DEFERRABLE INITIALLY DEFERRED: it fails at COMMIT.
                db.execute(template, bad)
                with self.assertRaises(sqlite3.IntegrityError):
                    db.commit()
                db.rollback()
            else:
                with self.assertRaises(sqlite3.IntegrityError):
                    db.execute(template, bad)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM evidence_observations").fetchone()[0], 0)

    def test_orphan_link_is_rejected(self):
        db = database()
        self.addCleanup(db.close)
        db.execute("""INSERT INTO evidence_links
            (observation_id,transaction_id,reason,transaction_snapshot_hash,created_at_epoch_ms)
            VALUES (?,?,'REFERENCE',?,?)""", ("a" * 64, 999, "e" * 64, 1000))
        with self.assertRaises(sqlite3.IntegrityError):
            db.commit()
        db.rollback()


if __name__ == "__main__":
    unittest.main()
