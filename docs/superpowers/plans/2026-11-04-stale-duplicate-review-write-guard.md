# Stale Duplicate Review Write Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent stale Activity duplicate confirmations from excluding a changed transaction, while preserving financial results and the existing Keep Both bridge.

**Architecture:** Add a pure pair-key and current-eligibility validator, extract the duplicate decision writer from `TransactionStore` into a focused SQLite writer, pass the Activity card's full snapshots to the writer, and make exclusion plus decision atomic.

**Tech Stack:** Kotlin, Android SQLiteOpenHelper, Jetpack Compose Activity, JUnit, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-04-stale-duplicate-review-write-guard.md`

## Global Constraints
- Exact main baseline 659a5dd3ecc1db39f1f447490fa8ada4649b460f; use the isolated branch `fix/duplicate-confirmation-guard-20261104`.
- No migrations, new permissions, changes to financial totals, account identity, source import, backups or UI design.
- Preserve reversed-ID Keep Both behavior and existing corrections, fees and balance anchors.
- No emulators or instrumentation execution; JVM unit and CI checks mandatory, physical phone behavior remains unverified.

## Review Focus
1. Corrupt pair ID `1:broken:2`: reject, never drop malformed tokens to form a pair. Test in Task 1.
2. Reversed order `2:1` for Keep Both: accept as same unordered pair and reject second decision. Test in Task 1 / writer.
3. Same two IDs, edited amount and identical-looking card: reject via full snapshot comparison. Test in Task 2.
4. A known reversal or manually classified observation: reject Same Transaction. Test in Task 1 and writer.
5. SQLite fails to insert resolution after a row exclusion: transaction must roll back. Verify in writer integration when physical tests are authorized; not claimed by JVM tests.

---

### Task 1: Pure preconditions
**Files:** `app/src/main/java/com/kedevjimmy/pesatrack/domain/rules/PossibleDuplicatePolicy.kt`; `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/PossibleDuplicatePolicyTest.kt`.

**Interfaces:** `parsePairIds(pairKey: String): Pair<Long, Long>?`; `canConfirm(pairKey: String, shownFirst: PossibleDuplicateObservation, shownSecond: PossibleDuplicateObservation, currentFirst: PossibleDuplicateObservation, currentSecond: PossibleDuplicateObservation): Boolean`.

- [ ] Add unit tests for valid/reversed IDs, malformed/duplicate/negative IDs, valid current snapshots and stale amount, state, reversal, reference, source and time.
- [ ] Run Android CI on the test-only draft and verify the expected RED from missing functions.
- [ ] Implement the pure methods by exact ID parsing, equality check and existing `isCandidate`.
- [ ] Rerun focused JVM suite and broader CI after implementation.

### Task 2: Atomic persistence and full-card snapshots
**Files:** Create `app/src/main/java/com/kedevjimmy/pesatrack/data/local/DuplicateResolutionWriter.kt`; extract `TransactionStore.resolvePossibleDuplicate` and `chooseDuplicateCanonical` from `TransactionStore.kt`; modify only existing duplicate-confirmation call sites in `ActivityScreen.kt`. Add safe synthetic JVM contract tests and compile existing Android integration tests.

**Interfaces:** `TransactionStore.resolvePossibleDuplicate(pairKey: String, sameTransaction: Boolean, expectedFirst: StoredTransaction? = null, expectedSecond: StoredTransaction? = null): Boolean`. The writer receives `PesaTrackDatabase`, `Context` and `loadTransaction: (Long) -> StoredTransaction?`.

- [ ] Verify baseline reversed-ID Keep Both Android integration fixture remains supported.
- [ ] Extract writer with re-read under lock and SQLite transaction; require complete snapshots for Same Transaction and recheck pure candidate eligibility.
- [ ] Use exact unordered IDs to detect previous decisions; preserve the original canonical-selection scoring.
- [ ] Pass card.first/card.second snapshots for both Activity buttons, accepting either display order while mapping snapshots to persisted transaction IDs; retain refresh behavior. Compile synthetic Android tests for historical reversed keys, post-preview fee edits and a third-row reversal (do not execute instrumentation).
- [ ] Run CI (JVM, lint, APK compile, security) without emulator/instrumentation execution. Review exact PR diff and merge only with exact-head green and latest main incorporated.
- [ ] Verify main push build and Full/Update rolling APK release; document remaining device acceptance.
