# Smart Financial Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically refresh local financial screens after app updates and foreground returns without changing money records.

**Architecture:** Keep the existing ledger revision for screen invalidation; add a separate data-change counter so presentation refreshes cannot schedule changed-data notifications. A tiny foreground coordinator uses a pure policy for update/elapsed-time gating.

**Tech Stack:** Kotlin, Android SharedPreferences, Kotlin StateFlow, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-10-smart-financial-refresh-design.md`

## Global Constraints

- Preserve local SQLite, current financial arithmetic, approved UI, in-place APK data and current permissions.
- No network access, new dependencies, migrations or instrumented tests.
- A missing provider balance remains unknown.

## Review Focus

- Repeated quick foreground visits must not reread expensive screens.
- A new version must refresh even within the five-minute interval.
- Clock rollback and first launch must trigger a refresh safely.
- Presentation refresh must not trigger new-data notifications.
- Actual new ledger data must still trigger notifications and UI updates.

---

### Task 1: Distinguish presentation refresh from financial change

**Files:** `data/local/LedgerUpdateBus.kt`, `planning/SmartNoticeLifecycle.kt`, unit test for bus.

**Interfaces:** `LedgerUpdateBus.requestPresentationRefresh()`, `LedgerUpdateState.dataRevision`.

- [ ] Write a failing unit test asserting a presentation refresh advances screen revision but leaves dataRevision unchanged, while a real change advances both.
- [ ] Run focused test and confirm failure for missing behavior.
- [ ] Implement the minimal state and update SmartNoticeLifecycle to observe dataRevision.
- [ ] Run focused test; inspect all subscribers for unintended side effects.

### Task 2: Refresh on update and foreground return

**Files:** new `ingestion/FinancialRefreshPolicy.kt`, new `ingestion/FinancialRefreshCoordinator.kt`, `MainActivity.kt`, pure policy unit test.

**Interfaces:** `FinancialRefreshPolicy.shouldRefresh(nowMs, lastRefreshMs, currentVersionCode, previousVersionCode)`, `FinancialRefreshCoordinator.onForeground(context)`.

- [ ] Write failing policy tests for first run, new version, elapsed interval, short return and clock rollback.
- [ ] Run focused tests to see missing-policy failure.
- [ ] Implement policy and SharedPreferences-backed foreground coordinator using the existing bus and current app versionCode.
- [ ] Call coordinator from MainActivity.onStart alongside existing SMS catch-up.
- [ ] Run focused then broad JVM tests and an APK compile if tooling is available.

### Task 3: Document and verify

**Files:** `CURRENT_STATE.md`; no UI redesign.

- [ ] Update the current-state guidance with actual triggers and explicit missing-bank limitation.
- [ ] Review diff, run `git diff --check`, inspect GitHub CI and report phone-test requirements.
