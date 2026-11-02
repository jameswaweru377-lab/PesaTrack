# Fingerprint Conflict Preservation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve contradictory same-code incoming observations as reviewable, non-posting local evidence without changing any financial totals.

**Architecture:** Keep the canonical `transactions` table unchanged. Add a focused conflict policy and local conflict store with schema migration, then hook existing duplicate-return paths without modifying the ledger record. Defer screen integration until tested storage and backup are established.

**Tech Stack:** Kotlin, existing SQLiteOpenHelper, JUnit, Android instrumentation, existing encrypted SQLite backup.

**Spec:** `docs/superpowers/specs/2026-11-01-fingerprint-conflict-preservation-design.md`

## Global Constraints

- No changes to canonical transaction amounts, classification, balances or totals.
- No raw SMS in conflict rows or logs; use synthetic fixtures only.
- No new network requests, permissions or dependencies.
- Android 7 compatibility stays unchanged.
- Do not touch another chat's PR branch; remain draft until all release gates.

## Review Focus

- Same-code replays with different observation times remain non-conflicts (Task 1).
- Same code but contradictory amount, fee or type is retained for review (Task 1/2).
- Identical conflicting imports never multiply inbox entries (Task 2).
- Database schema 40 upgrades retain all existing ledger rows (Task 2).
- Encrypted backup/restore and interrupted persistence preserve evidence without posting money (Task 3).

---

### Task 1: Pure collision policy

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/ingestion/FingerprintConflictPolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/ingestion/FingerprintConflictPolicyTest.kt`

**Interfaces:** `FingerprintConflictPolicy.isConflicting(stored: FingerprintFacts, incoming: FingerprintFacts): Boolean`; `FingerprintFacts` contains normalized code, type, amountMinor, feeMinor and known occurrence evidence.

- [ ] Write synthetic JUnit tests for same facts, differing arrival times, contradictory amount/fee/type, missing code and uncertain dates.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*FingerprintConflictPolicyTest'` and confirm expected failure.
- [ ] Implement the smallest pure policy, excluding free-text spelling-only clues.
- [ ] Run the focused JVM tests and record count.

### Task 2: Local non-posting evidence store

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/FingerprintConflictStore.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/PesaTrackDatabase.kt`
- Modify (extraction only): `app/src/main/java/com/kedevjimmy/pesatrack/data/local/TransactionStore.kt`
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/data/local/FingerprintConflictStoreTest.kt`

**Interfaces:** `recordConflict(existingTransactionId: Long, incoming: FingerprintConflictEvidence): Boolean`; `listPending(): List<FingerprintConflictEvidence>`; unique normalized source/evidence digest, never a ledger insert.

- [ ] Write failing emulator tests for contradictory observations, replay, unchanged canonical rows/totals, and schema-40 upgrade.
- [ ] Run the exact instrumentation class and confirm the expected failure.
- [ ] Add the migration and record in the duplicate-return path using transaction-safe SQLite operations.
- [ ] Run focused JVM, schema and emulator tests.

### Task 3: Backup and review seam

**Files:**
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/security/FingerprintConflictBackupTest.kt`
- Modify only if required by failing tests: existing encrypted backup validation/restore contract.
- Create or extend: isolated read-only conflict-list UI and its Compose UI tests after storage is green.

- [ ] Write failing encrypted backup/restore and UI tests with invented evidence.
- [ ] Run them to verify expected failure.
- [ ] Implement only required backup/restore and read-only navigation behavior.
- [ ] Run all focused tests, Android CI, full device suite and financial-safety gates against the exact candidate.
- [ ] Review privacy, unchanged totals, current-main integration and signed physical-device persistence before any merge proposal.
