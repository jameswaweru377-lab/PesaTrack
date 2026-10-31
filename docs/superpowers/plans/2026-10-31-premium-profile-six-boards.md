# Premium Profile Six Boards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all approved premium Profile designs with trustworthy local interactions.

**Architecture:** Retain Profile's local page routing and shared PesaTrack card/components. Compose a read-only financial-health summary from the already-loaded Home cache using the existing authoritative HomeFinancialSignalPolicy, and keep state changes in the existing preference/security stores. Extract only small presentation helpers; leave backup cryptography and activity ledger untouched.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android SharedPreferences, existing local stores.

**Spec:** `docs/superpowers/specs/2026-10-31-premium-profile-six-boards.md`

## Global Constraints

- Theme light; four bottom tabs are Home, Activity, Plan, More.
- No new permissions, network services, database/schema or signing changes.
- Preserve existing encrypted backup/restore, PIN/fingerprint and reminder functionality.
- Use honest health evidence and explicit insufficient-data state.
- Only merge after clean CI on latest main, without colliding with active work.

## Review Focus

1. No Home cache or stale ledger revision → explain insufficient financial evidence, never show green.
2. Insufficient confidence despite some negative signs → do not claim a complete health score; say what remains unconfirmed.
3. Invalid optional email or failed preference commit → keep edits and show error, never success.
4. No biometric hardware or disabled app PIN → disable biometric control and keep PIN fallback.
5. Notification permission denied → preserve preferences but show Android permission guidance.

---

### Task 1: Profile and personal details

**Files:** `ui/screens/ProfileScreen.kt`, `ui/screens/PersonalDetailsScreen.kt`, `ui/screens/ProfilePhotoPicker.kt`; unit test `ui/screens/ProfileDetailsFeedbackPolicyTest.kt`.

- [ ] Write and run a failing JVM test for profile-save feedback and validation policy.
- [ ] Implement the pure feedback policy, redesigned hero, separate account/support groups, and polished personal form/photo actions.
- [ ] Show success snackbar only after synchronous preference save succeeds; keep screen open after save; failures retain form.
- [ ] Run focused JVM tests and inspect screen hierarchy.

### Task 2: Better money habits

**Files:** new `ui/screens/ProfileMoneyHabits.kt`, `ui/screens/ProfileScreen.kt`, `ui/PesaTrackApp.kt`; unit test `ui/screens/ProfileMoneyHabitsTest.kt`.

- [ ] Write failing test for Looking good, At risk and insufficient-data presentation with synthetic signals.
- [ ] Build a safe read-only current-month input from `HomeScreenCache`, reusing `evaluateHomeFinancialSignals`; guard stale revision.
- [ ] Create compact tappable Profile card and detail page with truthful reasons/action routing.
- [ ] Run focused test and check null/partial/current states.

### Task 3: Security and notifications

**Files:** `ui/screens/ProfileScreen.kt`, `ui/screens/SettingsDetailPages.kt`.

- [ ] Preserve the security sheet and manager calls; refine page hierarchy/header and lock-timing presentation.
- [ ] Group existing notification toggles with concise explanations and keep Android permission guidance.
- [ ] Run relevant existing security/reminder tests, confirm all controls remain functional.

### Task 4: Appearance and privacy

**Files:** new `ui/screens/ProfileAppearancePage.kt`, `ui/screens/ProfileScreen.kt`, `ui/screens/SettingsDetailPages.kt`.

- [ ] Build light-theme preview with correct four tabs and real system text-size settings action.
- [ ] Keep reduce-motion offscreen if the app cannot honor it globally in this isolated PR.
- [ ] Improve privacy copy and group real access/statement/backup/online-lookup actions; no loss of existing choices.
- [ ] Run UI contract tests or source-level verification.

### Task 5: Help and troubleshooting

**Files:** `ui/screens/ProfileHelpPages.kt`.

- [ ] Refine searchable topics, shorter help text and guided read-only permission checks.
- [ ] Preserve advanced diagnostics and actionable source/settings links.
- [ ] Run relevant screen test compile checks.

### Task 6: About and verification

**Files:** `ui/screens/ProfileScreen.kt`, existing Profile Android test, `CURRENT_STATE.md`.

- [ ] Polish About header, dynamic version and legal links; validate image/dialog behavior.
- [ ] Update Profile UI assertions for approved labels and detail navigation.
- [ ] Run focused tests, JVM suite, lint, debug assembly and inspect diff.
- [ ] Push feature branch, inspect current main/CI/conflicts and merge only after clean checks; report device QA not run.
