# Phase 3D Design 1 - All-Accounts Statements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first approved Statements design as the default all-account overview with safe coverage and navigation, without changing the existing account-specific functionality.

**Architecture:** A pure projection consumes the Phase 3B read-only source-coverage policy. The existing Statements dashboard reads one Nairobi calendar year in a coroutine and displays a new Compose landing; the account and import actions route to existing pages.

**Tech Stack:** Kotlin, Jetpack Compose Material3, existing local evidence readers, JUnit4, existing Android Compose tests and GitHub Android CI.

**Spec:** `docs/superpowers/specs/2026-10-29-statements-phase3d-design1.md`

## Global Constraints

- Latest `main` base; isolated feature branch; no merges from obsolete PR #460.
- Activity remains the canonical independent daily ledger; Statements remains historical imported source evidence.
- Never combine own-account transfers as income/spending or infer a bank duplicate from narration alone.
- No new cloud, permissions, source writes, migration, signing changes or provider-specific business logic.
- Preserve approved four-tab Home/Activity/Plan/More and M-Pesa/Equity account pages.
- CI instrumentation remains manual/paused per `CONTRIBUTING.md`; Android test compilation remains required.

## Review Focus

1. M-Pesa and Equity each have a cash leg from the same own transfer; no global total appears in UI.
2. An unreferenced overlapping Equity month stays `NEEDS_REVIEW` and cannot become KES 0.
3. A year contains no represented source: show import action, not false financial metrics.
4. Imported months can be partial; `months on file` is not 'complete 12-month coverage'.
5. A failed read or over-limit query yields retryable error, not 'no statements'.

---

### Task 1: Pure source coverage projection

**Create:** `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementUnifiedOverviewPolicy.kt`
**Test:** `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementUnifiedOverviewPolicyTest.kt`

**Interface:** `project(year: Int, coverage: List<StatementSourceCoveragePolicy.MonthCoverage>): Overview`. `Overview` holds `accountRows`, `accountMonths` (Jan-Dec), `monthsOnFile`, `monthsNeedingReview`, `verifiedRepeatCopies`, `unresolvedCandidates`; **no monetary aggregate**.

- [ ] Write synthetic tests: union of same-month accounts, verified repeated copy, missing month, overlap unknown, multi-currency review, no evidence.
- [ ] Verify tests fail with no policy, then implement status priority (`NEEDS_REVIEW` > `ON_FILE` > `NO_IMPORT`) and safe distinct-month counts.
- [ ] Run focused tests and commit. Infrastructure blocker in this environment: Android SDK not installed; do not claim local test success.

### Task 2: Default Statements screen and existing workflow

**Create:** `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementUnifiedOverviewPage.kt`
**Modify narrowly:** `StatementIntelligenceDashboard.kt`, `StatementBankOverviewPage.kt`
**Test:** `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementUnifiedOverviewUiTest.kt`

- [ ] Add Compose tests for visible account count/status without global totals, account row click, and empty-state import.
- [ ] Add `ALL_ACCOUNTS` root, year selection, bounded IO coverage read, stable reuse across navigation, read-error retry. Distinguish source failure from no import.
- [ ] Build the design-one cards/matrix/account list from live projection; accessible labels and masked identifiers; no nested bottom nav.
- [ ] Reuse current bank/M-Pesa screens for drilldown, preserve selected year when supported and navigate back to overview. Reuse `StatementImportSheet` unchanged.
- [ ] Compile Android tests, check semantics and visual layout; commit.

### Task 3: Combined regression and delivery

- [ ] Run focused policy tests and standard Android check: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --stacktrace`.
- [ ] If local Android SDK absent, use GitHub Actions for the actual compile/lint/unit-test verdict and document the limitation. Do not invoke manual emulator instrumentation unless explicitly requested.
- [ ] Review source changes and diff for privacy, unrelated UI, import behavior, cancelled reads and accidental hardcoded financial figures.
- [ ] Open one consolidated PR against latest main. Wait for green CI and fix failures on the same PR. Merge/publish only under applicable task-specific authorization; inspect generated APK release assets afterward.

## Deferred by approval scope

New Equity account-detail design, global Coverage & Review screen, manual duplicate resolution, new banks, and cross-product Insights remain separate future steps.
