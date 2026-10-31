# Equity Grand-Total Reconciled Statements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show truthful source-specific Equity Money In, Money Out, net movement and monthly summaries when its saved full-document grand totals and running-balance chain already reconciled.

**Architecture:** Keep `GRAND_TOTAL_AND_CHAIN_ONLY` and the additive SQLite archive as authoritative source evidence. A small pure Kotlin projection rechecks every saved row against source totals before reporting. Source-list/detail Compose cards display it as *Grand-total reconciled* without promoting to FULL reports or Activity.

**Tech Stack:** Kotlin/JVM, Jetpack Compose, existing `StatementBankArchiveStore`, SQLite v6 archive, Android CI.

**Spec:** `docs/superpowers/specs/2026-10-31-equity-grand-total-reconciled-design.md`

## Global Constraints

- No raw private PDF, customer/account IDs, balances or references in tests, GitHub, logs, screenshots or external APIs. Fixtures are synthetic.
- No new schema, migrations, dependencies, permissions, network, analytics, signing, backup changes or release workflow changes.
- No cross-source aggregation: overlapping saved bank PDFs may otherwise double count the same cash leg.
- No canonical Activity, account balances, categorized income/spending, bridge, verified global summaries or automatic matching changes.
- `GRAND_TOTAL_AND_CHAIN_ONLY` must still lack independent printed opening/page totals; never label these as FULL or invent printed evidence.
- Avoid shared landing files owned by open PR #502; recheck `main` and outstanding PRs before publication or merge.

## Review Focus

1. Corrupt row count or source totals: report must be absent, even when source-level totals were previously stored.
2. An overlapping second imported PDF: report must display one selected source's amounts, not an account-wide sum.
3. Nairobi midnight boundary: each dated movement belongs to the correct calendar month.
4. Overflow, `Long.MIN_VALUE`, duplicate leg numbers, mixed source IDs or zero movements: return an unavailable summary rather than render a plausible but wrong one.
5. Existing M-Pesa and FULL bank financial reporting: untouched and identical after this feature.

---

### Task 1: Pure reconciled-source projection and tests

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementGrandTotalReconciledPolicy.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementGrandTotalReconciledPolicyTest.kt`

**Interfaces:**
- Produce `StatementGrandTotalReconciledPolicy.Row(sourceId: String, number: Int, occurredAt: Long, movementMinor: Long)`.
- Produce `StatementGrandTotalReconciledPolicy.Month(key: String, rows: Int, moneyInMinor: Long, moneyOutMinor: Long)` and `Report(rows: Int, moneyInMinor: Long, moneyOutMinor: Long, netMinor: Long, months: List<Month>)`.
- Produce `project(sourceId: String, expectedRows: Int, expectedMoneyInMinor: Long, expectedMoneyOutMinor: Long, rows: List<Row>): Report?`. Return `null` for any failed invariant or checked arithmetic.

- [ ] Write **RED** invented-data JVM tests for two-month totals, net, display row count, Nairobi boundary and every Review Focus negative. No Android or archive import.
- [ ] Run targeted JVM tests; confirm expected pre-implementation compile/test failure.
- [ ] Implement one-source checked sum, exact source/leg-count match, continuous unique 0-based row indices, nonzero movements, valid times, `Math.addExact` / `subtractExact`, and Nairobi month grouping.
- [ ] Rerun targeted tests and full JVM tests; record counts and all failures.
- [ ] Commit only the source policy and its tests.

### Task 2: Saved-source UI and assurance wording

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementLimitedBankOverviewPage.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementLimitedSourcePage.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementGrandTotalReconciledCards.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt` (only strings for existing limited admission and save)
- Modify: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementUnifiedOverviewUiTest.kt` (specific existing limited source page assertions)

**Interfaces:**
- Source-list cards display each document's independently reconciled grand sums, but never aggregate different PDFs.
- Source-detail page loads saved legs once and calls `project(...)` with exactly that `source.id` and the stored source totals, showing a cash summary/months only on non-null result; otherwise display a clear unavailable/error state while keeping source rows accessible.
- Text reads *Grand-total reconciled* and explains that opening/page totals were not independently printed, without claiming complete FULL bank evidence.

- [ ] Add **RED** Compose UI tests for a reconciled source label, per-document totals, source-detail summary and inaccessible/corrupt rows; update the prior limited-only string expectations. If emulator execution is unavailable, record the tests as compiled-only, not passed.
- [ ] Implement focused small composables and source-detail projection, maintaining 48dp actions and the approved PesaTrack light-purple styling without new raw colors/radii.
- [ ] Change only the existing import review/save explanatory strings (no admission, archive or Activity behavior changes).
- [ ] Compile Android UI-test sources and run Android lint/debug assembly in CI. Inspect the diff and explicit UI/error states.
- [ ] Commit UI and tests, with no unrelated navigation/PR #502 edits.

### Task 3: Evidence, docs, PR and merge gate

**Files:**
- Modify: `CURRENT_STATE.md` and `README.md` to describe the reconciled source-only reports; no new release promises.

**Interfaces:** Keeps existing SQLite/backup contract and security scans unchanged.

- [ ] Run synthetic offline Equity extractor tests (grand totals and broken document remain fail-closed) plus available Python lab tests.
- [ ] Validate `git diff --check`, privacy/no source PDF, and no ledger/backup/schema change.
- [ ] Recheck newest `main`, PR #502 and open PR overlaps. Resolve without overwriting another chat's branch.
- [ ] Push scoped branch and open PR; require final-head Android JVM, lint, debug APK and security checks green. Instrumentation and real Redmi use must be explicitly reported as run or not run.
- [ ] Merge only after the current-head checks, source diff and overlap are safe, using the user's prior merge-when-safe authorization; otherwise stop and report blocker. A successful merge does not prove the physical phone has installed or accepted the new APK.

## Self-review

- Verified global totals and Activity stay untouched; grand-total reconciled is source-specific, not FULL.
- The domain report's complete stored-row checks prevent a corrupt read from displaying a trusted summary.
- No new migration needed; existing saved source metadata already has printed-grand-total-checked incoming/outgoing sums and immutable legs.
- Private 14-page PDF remains outside the repository and CI.
