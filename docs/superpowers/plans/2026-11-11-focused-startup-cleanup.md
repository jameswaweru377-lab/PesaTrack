# Focused startup cleanup implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the visibly outdated violet startup surfaces without changing the approved UI, app routing or financial behavior.

**Architecture:** Limit modifications to the Android launch window and Compose startup screen. Keep splash timing and MainActivity routing unchanged. Use dedicated startup colours matching the approved brown/cream visual language rather than modifying application-wide theme semantics.

**Tech Stack:** Kotlin, Jetpack Compose, Android XML resources, JVM/Robolectric tests.

**Spec:** `docs/superpowers/specs/2026-11-11-focused-code-health-design.md`

## Global Constraints

- Do not alter PDF/Python, ML Kit OCR, finances, local data, permission, backup, app ID, signing, release workflows or approved navigation.
- Keep the existing app-lock and onboarding transition behavior and 650-ms splash timer.
- Keep this change separate from PR #549 (R8 shrinking) and current shopping/investment PRs.
- Physical-device visual acceptance must not be inferred from compilation.

## Review Focus

1. Cold launch on a light-mode Redmi: initial window and Compose splash should have no violet flash.
2. Cold launch with app lock enabled: lock gate still appears after splash.
3. Fresh onboarding: onboarding still appears through its existing path.
4. Returning from background: no unexpected navigation reset or repeated old splash.
5. Accessibility font scaling: splash text/illustration remain usable.

---

### Task 1: Startup visual alignment

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StartupSplashScreen.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/StartupVisualContractTest.kt` (new)

**Interfaces:** Existing `StartupSplashScreen(darkTheme: Boolean)` signature remains unchanged. Android `Theme.PesaTrack` stays the existing launch theme.

- [ ] Write tests asserting the startup XML window backgrounds and splash light/dark colour tokens use approved brown/cream, not legacy violet; include resource tests where available.
- [ ] Run the focused test and confirm it fails on the current old colours (not just because Gradle is unavailable).
- [ ] Introduce the smallest startup palette changes and keep existing splash content, route and timer intact.
- [ ] Run focused test, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and inspect outputs. If Gradle remains blocked, mark this task unverified and do not push/merge.
- [ ] Verify diff contains no unrelated code and commit the isolated theme change.

### Task 2: Retired Accounts carousel verification

**Files:**
- Inspect: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SelectedAccountsDesign.kt`
- Inspect: `app/src/main/res/drawable-nodpi/accounts_card_*.webp`
- Inspect: `docs/AUDIT_APK_SIZE_2026-11-11.md`

**Interfaces:** The approved static Accounts layout and all remaining `Heritage*` functions remain unchanged.

- [ ] Re-run repository-wide symbol/resource searches to confirm no call sites for removed `SelectedAccountsCompass` or `HeritageAccountCard` and no references to three retired images.
- [ ] Run unit tests, lint and APK assembly against the combined candidate; if unavailable, explicitly hold merge.
- [ ] Compare before/after APK bytes and visually verify Accounts on physical phone before release.

### Task 3: Measure Home performance before optimisation

**Files:**
- Inspect: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeScreen.kt`
- Inspect: `app/src/main/java/com/kedevjimmy/pesatrack/ui/PesaTrackApp.kt`
- Inspect: existing lifecycle/ledger refresh tests and timing logs.

**Interfaces:** Read-only measurement at this stage.

- [ ] Record cold/warm startup and Home time-to-first-content using Android profiling on device or CI instrumentation explicitly approved for this task.
- [ ] Identify actual slow calls or excess re-composition from measurements, not assumptions.
- [ ] Open a separately scoped plan/change only when measurements justify it; do not bundle speculative cache or database refactors with visual cleanup.

## Acceptance and handoff

Do not merge or publish without green relevant CI and explicit confirmation of any unverified phone checks. Report exact commit, tests, APK delta, screenshots/visual checks and outstanding risks.
