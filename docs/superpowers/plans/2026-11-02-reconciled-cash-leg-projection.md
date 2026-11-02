# Reconciled Cash-Leg Projection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Compute safe, reversible internal-transfer and same-leg projections from confirmed review metadata, without changing posted totals.

**Architecture:** A pure Kotlin policy consumes existing strong review groups and confirmed current-hash decisions. The matching review page displays informative projected transfer amounts only; original SQL tables remain authoritative and untouched.

**Tech Stack:** Kotlin, JUnit 4, Jetpack Compose.

**Spec:** `docs/superpowers/specs/2026-11-02-reconciled-cash-leg-projection-design.md`

## Global Constraints

- No statement, Activity, archive, balance, category, backup or schema writes.
- No instrumented test execution or emulator setup.
- Retain N2 hold on app-wide accounting, despite manually waived device checks for metadata-only review.
- No real financial identifiers in tests, logs or PR descriptions.

## Review Focus

- Genuine equal-value payments with different reference remain distinct.
- Protected principal/fee, refund, reversal, loan and excluded roles never contribute.
- Same-leg and transfer groups overlap: fail closed.
- Partial or inconsistent statement assurance does not create sums.
- Overflow and stale decisions never synthesize amounts.

---

### Task 1: Pure confirmed cash-leg projection

**Files:** `domain/statements/StatementReconciledLegProjectionPolicy.kt`, `StatementReconciledLegProjectionPolicyTest.kt`.

- [ ] Write synthetic triad and negative tests before implementation.
- [ ] Verify expected red result on available test runner, or record its absence.
- [ ] Implement strict validation and checked arithmetic, with no side effects.
- [ ] Run focused JVM tests and regression suite.

### Task 2: Read-only review display

**Files:** `data/local/StatementMatchDecisionStore.kt`, `ui/screens/StatementMatchReviewPage.kt`.

- [ ] Reuse confirmed decision map and candidate rows in the existing snapshot.
- [ ] Explain projected internal-transfer amount and count without claiming app-wide reconciliation.
- [ ] Verify UI compiles and all money totals still originate from existing paths.

### Task 3: GitHub integration

- [ ] Verify latest main, clean diff, secrets/privacy, and exact-head Android CI.
- [ ] Confirm instrumentation skipped.
- [ ] Merge only if the entire change is non-posting and its applicable N2 checks pass.
