# Core Pages Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved redesign for Plan, Accounts, More, and Settings while preserving Home and making theme-only color changes to Activity.

**Architecture:** Keep each screen’s existing Compose destination, data loading, callbacks, and navigation contracts. Update composable presentation in place, using the existing theme palette and components; Activity’s composable structure and ordering remain unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, existing PesaTrack components and Android CI.

**Spec:** `C:/Users/Developer/Documents/Codex/2026-10-18/so-you-remember-the-pesatrack-repository/outputs/pesatrack-core-pages-redesign.html` and current user scope: redesign Plan, Accounts, More, Settings; do not change Home; Activity theme styling only.

## Global Constraints

- Do not modify Home screen files or its behavior.
- Keep Activity content order, hierarchy, interactions, and layout unchanged; adjust only theme-dependent styling.
- Preserve existing financial data, ownership rules, computations, callbacks, and navigation.
- Do not copy illustrative mockup values into app content.
- Preserve light and dark theme support and use theme tokens for surface/text colors.
- Keep account balance unavailable states distinct from zero; keep debt separate from income/spending.

## Review Focus

- Salary absent versus confirmed: Plan must retain its existing truthful states and never imply unreceived income.
- No bills/accounts or unavailable balances: redesign must keep empty and unavailable states explicit.
- Owned versus unconfirmed accounts/debts: preserve existing filtering and transfer-matching actions.
- Light and dark themes: all revised screens must remain legible in both.
- Activity scope: color-only edits must not change hierarchy, sizing, or interactions.

---

### Task 1: Plan screen presentation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanScreen.kt`

**Interfaces:** Keep `PlanScreen`, `PlanHero`, existing state and callback signatures unchanged.

- [ ] Rework the Plan header, monthly-plan hero, upcoming bills/empty state, salary CTA, and budget/goals/pay-cycle sections to match the approved compact visual hierarchy.
- [ ] Retain existing amounts, calculations, unavailable/loading/error states, and every existing action.
- [ ] Inspect the diff to confirm only Plan presentation changed and the module remains syntactically valid.

### Task 2: Accounts screen presentation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/AccountsAndDebtsScreens.kt`

**Interfaces:** Keep `AccountsScreen`, account/debt state models, selection state, and callbacks unchanged.

- [ ] Restyle the Accounts/Debts switcher, account list heading, account rows, balance freshness/unavailable presentation, and add/manage affordances to the approved compact design.
- [ ] Preserve ownership filtering, account identities, account and debt totals, debt-specific screens, and existing empty states.
- [ ] Inspect the diff to confirm no financial or ownership logic changed.

### Task 3: More screen presentation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/MoreScreen.kt`

**Interfaces:** Keep `MoreScreen` and `MoreRow` callbacks and parameters unchanged.

- [ ] Group Saved statements, expandable money tools, Account & app, and History and help with the approved typography, compact rows, spacing, and surfaces.
- [ ] Keep every existing destination and its action reachable, including optional tools.
- [ ] Inspect the diff to confirm navigation callbacks and expansion behavior are unchanged.

### Task 4: Settings screen presentation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SettingsScreen.kt` (and shared row/page presentation in `SettingsDetailPages.kt` only if required by the approved main Settings view)

**Interfaces:** Keep `SettingsScreen`, detail-page routing, switches/sheets, and all callbacks unchanged.

- [ ] Restyle the Settings title/profile block and grouped Preferences, Privacy & data, and Help and more rows to match the approved compact page.
- [ ] Preserve appearance, reminders, privacy, message access, app lock, backup/restore, automatic sorting, help, and about actions.
- [ ] Inspect the diff to ensure detail-page behavior and settings state remain unchanged.

### Task 5: Activity theme styling only

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ActivityScreen.kt`

**Interfaces:** Keep `ActivityScreen` and all child composable signatures and ordering unchanged.

- [ ] Replace theme-dependent hard-coded colors in the existing Activity summary and related UI with matching `MaterialTheme.colorScheme` tokens or theme-derived values.
- [ ] Preserve category/status colors where they encode transaction meaning, and preserve all Activity layout, hierarchy, spacing, and interactions.
- [ ] Review the Activity diff to confirm it contains styling-only changes.

### Task 6: Integration and delivery

**Files:**
- No additional app files unless required to fix a CI compile/lint failure caused by these edits.

- [ ] Review the complete diff and confirm Home files are untouched and Activity changes are styling-only.
- [ ] Run `git diff --check`, then open a pull request and rely on the repository’s Android CI checks for build, lint, and existing tests.
- [ ] After successful CI, merge the pull request to `main`, wait for the main workflow to complete, and retrieve the resulting APK artifact for the user.
