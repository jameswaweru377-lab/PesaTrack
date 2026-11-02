# Annual Reports Accounting Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display non-posting annual accounting evidence on Reports without changing any totals.

**Architecture:** Pure period-scope gate, existing read-only match snapshot, small Compose card under the existing Reports hero. No reconciliation-engine changes.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit, existing GitHub Android CI.

**Spec:** `docs/superpowers/specs/2026-11-02-annual-report-accounting-evidence-design.md`

## Global Constraints

- No real data in fixtures, SQL updates, schema migrations, new dependencies or instrumentation.
- Annual evidence must not be compared to weekly/monthly Reports figures.
- Preserve existing Reports hero and calculations.

## Review Focus

- Week and Month must never display annual evidence.
- Wrong year must not display stale evidence.
- Missing evidence must remain unavailable.
- Unresolved evidence must retain review status.
- All figures remain non-posting and non-comparable.

### Task 1: Period gate and contract

**Files:**
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/SharedAccountingReportScopePolicyTest.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/SharedAccountingReportScopePolicy.kt`

- [x] Write tests for annual-only, year identity, unavailable and non-posting behavior.
- [ ] Run red JVM test; local runner has no Gradle distribution, so GitHub CI is required.
- [x] Add minimal pure policy `forPeriod(period, year, evidence)`.
- [ ] Confirm green in exact-head Android CI.

### Task 2: Reports read-only card

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SharedAccountingReportCard.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ConnectedConceptScreens.kt`

- [x] Load only in the Year period, off main thread.
- [x] Render separate Activity and Statement scopes with review caveat.
- [ ] Confirm compilation and lint in GitHub CI.
- [ ] Check diff, open PR, merge only if all checks pass against current main.
