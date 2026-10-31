# Money until next salary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Plan card that safely estimates a pre-payday remaining-money range without inventing salary or balances.

**Architecture:** An Android-free forecast policy receives trusted facts from a small read-only Android repository; an isolated Compose card shows four data states and records voluntary, local confirmations. Existing ledger and pay cycle logic remain unchanged.

**Tech Stack:** Kotlin, JUnit4, Jetpack Compose, SQLite read-only APIs, Android SharedPreferences.

**Spec:** `docs/superpowers/specs/2026-10-31-money-until-salary-design.md`

## Global Constraints

- One Task 3 PR from 31 October latest main; do not merge without task-specific approval.
- Do not add to `TransactionStore.kt`, `ActivityScreen.kt` or change existing money totals, category or salary-receipt behaviour.
- User data remains local; no permission, API, telemetry, schema, dependency or APK workflow changes.
- Display estimated window as estimated, include future salary only as informational text, and show incomplete evidence rather than a number.
- New/changed files max 800 lines; functions max 80 lines; Compose uses approved Plan tokens and 16dp margins.

## Review Focus

1. A last salary on 1 October with 24th–1st window chooses the correct 24 October–1 November cycle, not a past window.
2. An overdue payday never silently rolls to a future window while the salary receipt is missing.
3. An active Fuliza loan without a confirmed repayment schedule blocks numeric projections.
4. Repeated commitments appearing as both Plan and Bills are never subtracted twice.
5. A new ledger event after manual balance confirmation invalidates that balance.

---

### Task 1: Pure forecast policy

**Files:** Create `app/src/main/java/com/kedevjimmy/pesatrack/domain/rules/SalaryForecastPolicy.kt`; test `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/SalaryForecastPolicyTest.kt`.

**Interfaces:** Consumes `SalaryForecastFacts(now, confirmedDate, confirmedAmount, expectedWindow, balance, balanceAsOf, completedCycles, commitments, unscheduledDebt)`; produces `SalaryForecastResult` with status, low/high and transparent explanations.

- [ ] Write isolated tests asserting missing evidence, confirmed precedence, estimated range, 3 cycles, negative result, overdue and debt guards; each expectation hand-calculated.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*SalaryForecastPolicyTest'` (or CI equivalent); expect failure because forecast policy does not exist.
- [ ] Implement pure policy using only checked integer minor-unit calculations, eligibility checks and range bounds.
- [ ] Run focused tests; expect zero failures. Commit code/tests.

### Task 2: Read-only financial evidence and local confirmation

**Files:** Create `app/src/main/java/com/kedevjimmy/pesatrack/data/local/SalaryForecastRepository.kt` and small UI-facing models as needed.

**Interfaces:** Produces `SalaryForecastFacts` from verified account balances, exactly three complete cycle activity snapshots, confirmed bill rules, pay-window rule, active debt status; local confirmed exact payday/amount and manual cash balance are stored separately from salary receipts.

- [ ] Write regression tests for schedule expansion/commitment uniqueness and balance evidence invalidation; expect the new functionality to fail before production code.
- [ ] Implement read-only collection and separate local preferences; do not write Activity, pay cycles or salary records.
- [ ] Run focused tests and `:app:testDebugUnitTest`; commit code/tests.

### Task 3: Minimal Plan card and accurate documentation

**Files:** Create `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/MoneyUntilSalaryCard.kt`; modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanScreen.kt` (one `item`); update `CURRENT_STATE.md` and relevant README/Design docs if they describe the feature.

**Interfaces:** Consumes repository results and policy. Date/amount confirmation never calls salary receipt workflow; manual balance is explicitly labelled as user-entered and expiring.

- [ ] Implement the four card states with accessible themed components, clear included/excluded text, and a compact confirmation flow.
- [ ] Run Android CI `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`; expect zero failures, inspect exact run/commit.
- [ ] Review diff and PR against latest main, check there is no overlap with other chats and no existing-money behavior change. Leave unmerged for approval.
