# Home Month Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this scoped plan task-by-task. Steps use checkbox syntax.

**Goal:** Remove the redundant Home heading and scope the retained month selector to monthly financial activity.

**Architecture:** Keep period and current-cycle money separate in one tiny pure presentation model. Reuse the existing Compose money card and month dialog; modify no financial aggregator.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit, Android Gradle CI.

**Spec:** `docs/superpowers/specs/2026-11-04-home-month-scope.md`

## Global Constraints

- Keep current cycle available amount and selected month's flow separate.
- Preserve five-tab navigation, existing local accounting and approved monochrome styling.
- Skip emulator/instrumentation; use JVM tests and existing Android CI.

## Review Focus

- Selecting a previous month must not hide a still-confirmed current cycle balance.
- Missing current cycle evidence must still show unavailable.
- Changing the month must affect only monthly flow, not the current amount.
- A historical net-flow caption must not imply it is the current month.
- The month picker must remain keyboard/TalkBack accessible and tappable.

---

### Task 1: Separate current available money from selected-month activity

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeSummaryScope.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/HomeSummaryScopeTest.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeScreen.kt`

**Interfaces:**
- Produces: `homeSummaryScope(confirmedCycle: Boolean, currentAvailableMinor: Long?, selectedNetFlowMinor: Long?, monthLabel: String): HomeSummaryScope`.
- Consumers: HomeScreen's existing display formatting and Home money card.

- [ ] Write JUnit tests for current/historical month independence, missing cycle, and truthful month caption.
- [ ] Run the focused JVM test; it must fail because `homeSummaryScope` is missing.
- [ ] Implement the pure scope model and use it for the money-card arguments.
- [ ] Re-run the focused JVM test; it must pass.

### Task 2: Move the selector into the card and verify

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeMonochromeComponents.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeScreen.kt`
- Modify: `CURRENT_STATE.md`

**Interfaces:**
- `MonochromeAvailableMoney` gains `month: String` and `onMonth: () -> Unit`, retains existing money arguments.
- Month chooser still updates the existing `selectedHomeMonthOffset`.

- [ ] Remove the top Overview/month row.
- [ ] Place accessible 48dp-target month chip beside "Monthly activity" above Money in / Expenses.
- [ ] Read the focused diff and verify no money computations or other screen style changed.
- [ ] Run JVM tests, lint and APK checks through existing CI; review exact-head status before merging.
