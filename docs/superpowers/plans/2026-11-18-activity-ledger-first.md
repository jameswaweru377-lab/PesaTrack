# Activity Compact-Overview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the selected dark-mode Activity layout while retaining the exact trusted daily net beside each date.

**Architecture:** Keep `ActivityScreen` as the existing ledger/review state owner. Extract compact presentation controls into `BrownActivityPresentation.kt`, retain the photo-backed expenses overview with Money in and recorded Balance / Partial balance, and keep the trusted day calculation unchanged. Activity owns a compact marked title and actions rather than repeating the centered Home brand header. Continue using shared `homeColors()` and theme tokens for both appearances.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4, Gradle, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-18-activity-ledger-first-design.md`

## Global Constraints

- Preserve all canonical accounting, full-ledger daily net, transfer/review semantics, original Month/Pay cycle selection and five-tab navigation.
- No new permissions, dependencies, DB/backup/signing changes, instrumentation or emulator runs.
- New/changed functions should stay focused and use shared theme tokens, 48dp interaction targets and contrasted dark surfaces.
- Gradle distribution download is blocked in this workspace; exact-head GitHub Actions Android CI is the required build/test verification path.

## Review Focus

- Search active when a month changes: filtered rows still belong to the selected period and can be reset.
- `Needs Review` reachable via the slim notice and advanced filters even when the initial count is zero.
- Transfers stay in advanced filters without changing income/outcome/net classification.
- Pay cycle and Month modes retain their existing calculations and selection dialogs.
- Missing or overflowing trusted net stays “Net unavailable”, not an invented amount.

---

### Task 1: Compact Activity controls and navigation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BrownActivityPresentation.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ActivityScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/components/AppScaffold.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/BrownActivityPresentationTest.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/components/ActivityHeaderPolicyTest.kt`

**Interfaces:** Existing `selectedFilter`, `searchQuery`, `showExtraFilters`, period and review callbacks remain authoritative. Define pure primary/advanced filter policy and global branded-header rule so presentation is testable without instrumentation.

- [ ] Step 1: Add pure policy tests for three primary visible filters, accessible Transfers/Review advanced keys, the conditional review label, and Activity's own header ownership.
- [ ] Step 2: Run focused JVM test and confirm it fails on absent new policy functions; if Gradle download is blocked, record the blocker rather than claim a red test.
- [ ] Step 3: Implement the compact header, photo-backed two-metric overview, on-demand search/advanced filters, one-line conditional review, Transactions / All activity heading, and three-filter first row; remove the redundant recent heading without touching money calculations.
- [ ] Step 4: Run focused tests; expect green, or delegate validation to exact-head GitHub CI if local Gradle unavailable.

### Task 2: Keep the daily net and distinguish transaction surfaces

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BrownActivityLedger.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/ActivityVisualGroupingPolicyTest.kt`
- Existing financial test: `ActivityDesign3MoneyTest.kt`

**Interfaces:** Keep `BrownActivityDateHeader(dayEpochMs, netMinor)` and `BrownActivityRow(...)` signatures; date net comes only from `activityDesign3DailyNet` over `trustedTransactions`.

- [ ] Step 1: Add net formatting cases for negative, positive, zero and unavailable values.
- [ ] Step 2: Run the focused test red when possible.
- [ ] Step 3: Extract the net formatter without changing its arithmetic, and improve theme-aware group/row surface contrast with existing tokens.
- [ ] Step 4: Run the focused and daily financial tests; expect green on CI.

### Task 3: Verify and integrate

**Files:**
- Modify: `CURRENT_STATE.md`
- Modify: `README.md`
- Add: this spec and plan.

**Interfaces:** No financial policy or data interface changes.

- [ ] Step 1: Update current state and README to describe the Activity interaction and retained daily net.
- [ ] Step 2: Run `git diff --check` and focused source inspection, then commit the scoped branch.
- [ ] Step 3: Push/open a PR tagged `Chat owner: CHAT-ACTIVITY-LEDGER-FIRST-20261118`, run exact-head Android CI/JVM/lint/APK + required security and tracker checks, inspect review and merge only if green.
- [ ] Step 4: Report the resulting main SHA and Redmi acceptance gap; do not claim device or signed upgrade validation.
