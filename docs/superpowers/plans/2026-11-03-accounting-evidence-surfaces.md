# Accounting Evidence Surfaces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface already-verified annual accounting evidence without changing financial calculations or implying gross withdrawals are expenses.

**Architecture:** Reuse `StatementMatchDecisionStore` read-only snapshots in small Compose surfaces. Keep a pure policy for truthful Statements metrics. Neither screen calculates new posted totals.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit, existing Android CI.

**Spec:** `docs/superpowers/specs/2026-11-03-accounting-evidence-surfaces-design.md`

## Global Constraints

- No schema, imports, permissions, telemetry, user financial data, or instrumentation.
- Preserve month-scoped Home and Activity calculations.
- No automatic treatment of Statement Money Out as Expenses.

## Review Focus

- Gross cash out must stay separate from verified expenses.
- Incomplete evidence must withhold cash and transfers.
- Mismatched cross-source evidence must not display a reviewed transfer figure.
- Annual evidence must not be presented as monthly totals.
- Missing evidence must not invent zero balances.

### Task 1: Truthful Statement cards

**Files:** `StatementOverviewCardsPolicy.kt`, `StatementOverviewCardsPolicyTest.kt`, `StatementUnifiedOverviewPage.kt`.

- [x] Write policy tests for gross-vs-expense, mismatch and withheld states.
- [ ] Verify red and green via exact-head Android CI (local Gradle unavailable).
- [x] Implement minimal domain projection and preserve gross out in details.

### Task 2: Read-only annual notice

**Files:** `SharedAccountingEvidenceNotice.kt`, `HomeScreen.kt`, `ActivityScreen.kt`.

- [x] Read shared snapshot on IO, show only reviewed/uncertain evidence.
- [x] Preserve current financial cards and provide existing navigation callbacks.
- [ ] Verify security scan, Kotlin tests, lint and APK in GitHub CI.
- [ ] Merge only when exact-head required checks pass on current main.
