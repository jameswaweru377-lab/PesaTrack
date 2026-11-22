# Income Recognition Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make genuine external income a conservative, shared on-device projection, separate from unresolved account credits.

**Architecture:** Pure evidence classifier and summary on existing transaction columns; extract shared calculation from Activity's ledger and reuse the classifier in pay-cycle additional income. Keep transfer matching and statement archives unchanged.

**Tech Stack:** Kotlin, SQLite, JUnit4, Android Gradle.

**Spec:** `docs/superpowers/specs/2026-11-22-income-recognition-evidence-design.md`

## Global Constraints

- N2 HOLD: no automatic bank-statement posting or confidence promotion.
- No emulator or instrumentation. Physical phone verification remains user-owned.
- No new permissions, dependencies, migrations, data deletion, fake deposits or changes to approved navigation.
- No changes to another chat's transfer/statement reconciliation branches.

## Review Focus

- A first observed owned transfer cannot become salary when the upstream bank SMS is absent.
- A source-only deposit or balance snapshot is not an earnings event.
- A delayed genuine receipt is counted once, existing duplicates and excluded rows are untouched.
- Changing to pay-cycle mode cannot resurrect unresolved income.
- Existing user-classified receipts remain authoritative; a refund or debt principal cannot become income.

### Task 1: Classification contract

**Files:** Create `data/local/IncomeRecognitionPolicy.kt`; test `data/local/IncomeRecognitionPolicyTest.kt`.

**Interfaces:** Produce `IncomeRecognitionPolicy.classify(IncomeRecognitionEvidence): IncomeRecognitionDecision` and `IncomeRecognitionPolicy.isConfirmedIncome(StoredTransaction): Boolean`.

- [ ] Write JUnit synthetic cases for user-confirmed salary, referenced external receipts, missing-leg owned transfers, unidentified bank deposits, loans, refund, reversed and repeated observations.
- [ ] Run focused JUnit; expect failure for missing policy (local Gradle may be blocked; use exact-SHA CI as needed).
- [ ] Implement pure recognition; no side effects, source-only promotion or fuzzy ownership matching.
- [ ] Run test and compare exact outcomes, commit scoped changes.

### Task 2: Shared Activity projections

**Files:** Modify `TransactionStore.kt` only by extracting the current income decision; modify `ActivityDesign3Money.kt`; test `ActivityDesign3MoneyTest.kt` and new classification fixture.

**Interfaces:** Consume `IncomeRecognitionPolicy.isConfirmedIncome`; produce consistent `ActivitySummary.incomeMinor` and `activityTrustedIncomeIds`. Preserve receipts/fees/net and balance calculations.

- [ ] Add failing regression test where an automatic unidentified credit is excluded from trusted Income row IDs.
- [ ] Replace direction-only income projection with shared policy, respecting receivable principal offsets.
- [ ] Run focused tests and whole JVM suite in CI, review diff, commit.

### Task 3: Pay-cycle consistency and handoff

**Files:** Modify `PayCycleStore.kt`; test new pay-cycle recognition policy fixtures; update `CURRENT_STATE.md`, `README.md`.

**Interfaces:** Pay-cycle additional-income query supplies the same existing evidence fields to `IncomeRecognitionPolicy.classify`. No salary requirement for balances.

- [ ] Add test proving pay-cycle and calendar-month classification agree for identical credits.
- [ ] Filter pay-cycle additional income through shared recognition logic without changing stored salary anchors.
- [ ] Run lint, JVM and Android APK checks in GitHub Actions; do not run instrumentation.
- [ ] Record exact SHA, CI URL, nonposting/N2 gaps and physical acceptance status; merge only if all required gates allow.
