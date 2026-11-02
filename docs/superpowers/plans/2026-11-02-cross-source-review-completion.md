# Cross-source review completion implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Complete the non-posting comparison and conflict-review foundations while making future reconciliation proposals reversible and safe.

**Architecture:** Reuse canonical Activity and statement source summaries as separate scopes. Read pending fingerprint conflicts through bounded cursor pages. Project explicit review decisions into reversible, non-posting proposals without touching the ledger.

**Tech Stack:** Kotlin, Jetpack Compose, SQLiteOpenHelper, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-02-cross-source-totals-preview-design.md`

## Global constraints

- No Activity, archive, backup, schema, balance, income or expense mutation.
- Original evidence is never deleted or silently suppressed.
- Amounts are integer minor units; unknown scope is not zero.
- No instrumentation, new permissions, dependencies, telemetry or network calls.
- Use invented examples only and preserve the approved review screen layout.

## Review focus

- Two real equal-value payments remain separate.
- Limited/unverified PDF source cannot claim safe combined cash totals.
- An expired or conflicting review decision produces no accounting proposal.
- A conflict list larger than 200 remains reachable via pagination.
- Undo returns the preview to its previous state without any ledger write.

---

### Task 1: Separate totals comparison

**Files:** Create `domain/statements/CrossSourceTotalsPreviewPolicy.kt` and its focused JUnit test; connect a small read-only summary inside the existing matching review UI and read model.

**Interface:** `project(activity: ActivitySummary?, statement: StatementUnifiedOverviewPolicy.CashSummary, matchCount: Int, unresolvedCount: Int): Preview`. Separate labelled fields; never a combined total or invented difference.

- [ ] Write failing tests for missing Activity, withheld statement, valid independent totals, distinct scopes and negative/overflow guards.
- [ ] Verify RED with a focused JVM test.
- [ ] Implement the minimum pure policy and read-only UI binding.
- [ ] Verify GREEN and unchanged accounting aggregate tests.

### Task 2: Complete pending conflict navigation

**Files:** Update `FingerprintConflictStore.kt`, `FingerprintConflictReviewReader.kt`, `StatementMatchReviewPage.kt`, and focused DB/UI tests.

**Interface:** `readPage(year: Int, beforeId: Long = Long.MAX_VALUE, limit: Int = 50): Page` with `nextBeforeId`, and an explicit `Load older conflicts` action.

- [ ] Write failing tests for 201+ rows, cursor order, year boundaries, replay and unchanged original records.
- [ ] Verify RED with a focused Android/JVM test when available; otherwise mark device assertions unverified.
- [ ] Implement bounded pagination and non-posting UI navigation.
- [ ] Verify GREEN and no data writes from reads.

### Task 3: Reversible reconciliation proposals (non-posting)

**Files:** Create `domain/statements/StatementReconciliationPreviewPolicy.kt` and focused JUnit tests; expose a count/explanation in review UI if safe.

**Interface:** `project(review: StatementMatchReviewPolicy.Review, decisions: Map<Set<String>, Decision>): Preview` where `CONFIRMED`, `NOT_SAME` or undo changes proposals only. Never updates or suppresses a transaction.

- [ ] Write failing tests for confirmed copy, owned transfer, reversed decision, conflicting records, protected fees and missing evidence.
- [ ] Verify RED with focused JVM tests.
- [ ] Implement deterministic projection with no SQL or mutable state.
- [ ] Verify GREEN and ensure existing report totals do not consume this preview.

### Verification and delivery

- [ ] Run focused tests first, then ordinary Android CI + security on the exact head.
- [ ] Confirm instrumentation is skipped; note missing Android/phone verification.
- [ ] Review diff for shared-file conflicts, secrets, privacy and UI regressions.
- [ ] Keep a draft PR until the applicable checks pass; merge only with current-main compatibility and user authorization.
