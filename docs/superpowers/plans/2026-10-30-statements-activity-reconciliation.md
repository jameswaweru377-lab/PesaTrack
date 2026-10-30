# Statements ↔ Activity evidence reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe, read-only correlation of Activity messages and independent Statement cash legs, including a three-observation self-transfer, without changing financial totals or existing UI.

**Architecture:** A pure projection guards Activity source provenance and cash-leg roles. Another pure policy groups independently confirmed observations by reference, signed movement and account before identifying unique opposite-leg *transfer candidates*, vetoing protected evidence and contradictory PDF copies. A new opt-in read-model method composes the existing statement archive, bounded original-message SQL reader and both policies; regular Statements reads and summaries are unchanged.

**Tech Stack:** Kotlin/JVM, JUnit 4, SQLite read query, Android Gradle CI.

**Spec:** `docs/superpowers/specs/2026-10-30-statements-activity-reconciliation.md`

## Global Constraints

- No network/telemetry, real financial fixtures, schema migration, Activity writes or UI redesign.
- Reuse confirmed owned account IDs and the existing statement read model; cap snapshot to 20,000 observations.
- Never treat `statement_import` or manual ledger entries as independent message evidence.
- No combined income/spending numbers or category propagation in this slice.

## Review Focus

- Same exact reference repeated in the same source: keep ambiguous; test duplicate Activity examples.
- PDF rows echoed into canonical Activity by earlier imports: exclude to prevent circular proof; test source kinds.
- Fee, debt and reversal role: do not prove a transfer; test every protected case.
- One wallet leg represented by both Activity and a PDF, bank opposite leg: detect one transfer, not a three-way conflict; test triad.
- Multiple equivalent PDFs with unproven overlap: keep uncertainty; test competing PDFs and contradictory same-reference amounts.

### Task 1: Pure original-message Activity projection

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementActivityObservationPolicy.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementActivityObservationPolicyTest.kt`

**Interface:** `project(rows: List<ActivityLeg>, ownedAccountIds: Set<String>): List<StatementEvidenceCorrelationPolicy.Observation>`; `ActivityLeg` has explicit transaction ID, account, source kind, type, signed direction, reference, amount, time and protected-role flags.

- [ ] Write synthetic tests for real SMS/notification legs, manual and PDF-import exclusion, unknown account, contradictory direction/type, protected financial roles, missing reference and stable deterministic ordering.
- [ ] Run focused JUnit test, expect red due to missing projection API.
- [ ] Implement strict original-message-only conversion; valid source groups use signed movement and `Source.ACTIVITY`; do not persist anything.
- [ ] Run focused JUnit test, expect green; commit Task 1.

### Task 2: Cluster-first pure financial-event proof

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementCrossSourceEventPolicy.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementCrossSourceEventPolicyTest.kt`

**Interface:** `reconcile(observations: List<Observation>, ownedAccountIds: Set<String>): Result`; `Result` exposes independently proved same-leg groups, owned transfer *review candidates* between groups, ambiguous IDs and unlinked IDs; no amounts or database writes.

- [ ] Write tests for exact same-leg, two-account transfer, Activity+wallet PDF+bank triad, repeated/colliding references, unconfirmed account, protected roles, timestamp/currency/sign mismatches.
- [ ] Run focused JUnit test, expect red due to missing proof API.
- [ ] Implement bounded reference-amount bucketing, cross-PDF amount-conflict preflight, protected-Activity veto and uniquely corroborated same-leg clustering before two-sided transfer proposals; ambiguous observations never suppress evidence.
- [ ] Run focused JUnit test, expect green; commit Task 2.

### Task 3: Opt-in on-device integration and verification

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/StatementEvidenceReadModel.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/StatementOriginalMessageEvidenceReader.kt`
- Add tests to Task 1 / Task 2 files as needed.
- Update: `CURRENT_STATE.md` (scoped handoff only).

**Interface:** `readWithActivityEvidence(startInclusive: Long, endExclusive: Long, accountFilter: String? = null): ActivityEvidenceSnapshot` returns the independent statement `Snapshot`, guarded Activity observations and cross-source event proof. Existing `read()` remains unchanged.

- [ ] Verify integration shape against current `TransactionStore.transactionsBetween` and confirmed accounts; write additional negative synthetic adapter-policy tests where needed.
- [ ] Implement a bounded, account/source-filtered read-only Activity SQL projection with overflow sentinel; never call the maintenance-writing Activity summary path.
- [ ] Verify focused Kotlin unit tests, full JVM unit suite, lint and debug assembly (locally when dependencies permit; otherwise GitHub Actions).
- [ ] Review diff for money authority, privacy, performance, and no UI/CI/release changes; commit and open one PR from the latest main; inspect CI before any merge decision.
