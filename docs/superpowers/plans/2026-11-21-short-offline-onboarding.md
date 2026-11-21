# Short Offline Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace repeated and cluttered PesaTrack first-run pages with a compact offline onboarding, accurate import/restore progress and a themed mandatory PIN setup.

**Architecture:** Keep the existing `SmartOnboardingSetupScreen` state machine, consent store, importer, ownership policy and app-lock manager. Simplify only the reachable Compose pages and route entry. Reuse a themed progress card for import and actual backup restoration.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android permissions, local SQLite, Python source UI contracts.

**Spec:** `docs/superpowers/specs/2026-11-21-short-offline-onboarding-design.md`

## Global Constraints

- No network account, new permissions, financial semantic changes or data migrations.
- The existing READ_SMS disclosure and safe skip path must work; never pretend a permission was granted.
- Android instrumentation and emulators are not run. CI must run JVM, lint and Android compile.
- Theme tokens and minimum 48dp touch targets; no unapproved color scheme.

## Review Focus

1. Denied SMS permission continues safely without a fake import-complete claim.
2. Android activity restoration does not reintroduce the duplicate welcome.
3. Skipping account ownership does not auto-create owned bank accounts.
4. Losing local PIN entry on rotation cannot unlock the app accidentally.
5. A failed/cancelled restore cannot display success or hide a destructive replacement warning.

---

### Task 1: One first-run entry

**Files:** `ui/PesaTrackApp.kt`, `ui/screens/OnboardingScreen.kt`, `tools/test_premium_ui_contract.py`.

**Interfaces:** The app's existing `SmartOnboardingSetupScreen(scaffoldPadding, onDone)` handles the single welcome; compatibility navigation route uses the same component.

- [ ] Add failing UI contract asserting there is one first-run welcome and the old `OnboardingScreen` is not called by app navigation.
- [ ] Run `python3 -m unittest tools/test_premium_ui_contract.py -v` and observe that specific failure.
- [ ] Route first-run and compatibility navigation directly to smart setup; retire duplicate welcome file if unused.
- [ ] Re-run source contract (green) and `git diff --check`.

### Task 2: Short SMS/import/ownership experience

**Files:** `ui/screens/ApprovedMoneySetupPages.kt`, `ui/screens/ApprovedPrivacyConsentPage.kt`, `ui/screens/OnboardingDiscoveryPage.kt`, `ui/screens/SmartOnboardingSetupScreen.kt`, new `ui/screens/OnboardingProgressPanel.kt`, `tools/test_premium_ui_contract.py`.

**Interfaces:** `ApprovedAnalysisPage(importing, progressStep, reviewMode, onContinue)` keeps the same signature and uses `OnboardingProgressPanel(title, description, status, icon, finished)`; consent remains user-authored.

- [ ] Add failing UI contract covering a non-fabricated import indicator, shorter privacy cards, and account review not being preselected.
- [ ] Run source UI contracts, verify specific failure.
- [ ] Add themed status/progress surface and use it in the existing analysis page; compact the disclosure and ownership presentation without changing state transitions or import dates.
- [ ] Re-run UI source contracts and verify the staging/status are correct.

### Task 3: Reference-inspired local PIN and genuine restore progress

**Files:** `ui/screens/MandatoryAppLockSetupScreen.kt`, `ui/screens/BackupRestoreFlow.kt`, `tools/test_premium_ui_contract.py`.

**Interfaces:** Existing `AppLockManager.setPin` and restore state (`RestoreStep.REVIEW` + `busy`) stay authoritative; `OnboardingProgressPanel` from Task 2 is presentation-only.

- [ ] Add failing source contract ensuring PIN form has an offline-themed hero, masked local PIN confirmation (not registration), and actual-restore-only progress surface.
- [ ] Run contract, verify expected failure.
- [ ] Update PIN layout and restoration busy presentation; retain all existing authorization, validation and storage logic.
- [ ] Re-run source contracts, check diff, and verify no permission or schema changes.

### Task 4: Documentation and end-to-end verification

**Files:** `CURRENT_STATE.md`, `README.md`, the branch diff.

- [ ] Update onboarding description and record device-only acceptance limitations.
- [ ] Run `python3 -m unittest tools/test_premium_ui_contract.py -v`, `git diff --check`, and the available local Gradle commands; capture exact blockers.
- [ ] Open scoped PR and require Android CI JVM unit tests, lint and debug APK build with instrumentation disabled.
- [ ] Review changed files, app-lock continuity, financial/backup semantics and actual CI result; do not claim physical Redmi acceptance.
