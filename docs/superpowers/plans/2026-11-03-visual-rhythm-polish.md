# PesaTrack Visual Rhythm Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the current PesaTrack interface calmer and more consistent while preserving approved UI and all financial behaviour.

**Architecture:** Adjust the existing `PesaDesignTokens` and `PesaTypography` foundation, then small, self-contained Compose layout changes in the shared bar and visible controls. Do not add a parallel design system or touch current Accounts PR #604 files.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Android CI.

**Spec:** `docs/superpowers/specs/2026-11-03-visual-rhythm-polish.md`

## Global Constraints

- Current `main` baseline: `6f182c8d268701a60cb84ece120b72b1a5825f81`.
- Five bottom destinations: Home, Activity, Plan, Accounts, More.
- 16dp normal screen margin; minimum 48dp interactive tap areas.
- Preserve existing wording, values, approved visual identity and privacy.
- Preserve the merged Accounts PR #604 files and notification PR #608 files.
- Do not add dependencies or instrumentation runs.

## Review Focus

- Five tabs at 320dp width: labels visible and selected tab still distinguishable.
- Android text scaling (1.3x/1.5x): labels and money must not disappear.
- Long pay-cycle date and large KES amounts: no new truncation or clipping.
- Dark Android system with PesaTrack light: neutral cards should remain legible.
- Keyboard, TalkBack and insets: >=48dp targets, preserved click actions.

---

### Task 1: Shared tokens and typography

**Files:**
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/theme/PesaVisualRhythmTest.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/theme/PesaDesignTokens.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/components/PesaComponents.kt`

**Interfaces:** Existing `PesaDesignTokens` fields and `PesaTypography` properties.

- [ ] Add exact-role tests: 16dp horizontal margin, 12dp section gap, 48dp minimum tap target, 64dp bar, 48dp indicator, 1dp neutral card elevation; headline and display emphasis are Bold, not ExtraBold.
- [ ] Run focused JVM test (expect RED); if local Gradle distribution is unavailable, record the environment blocker rather than claim RED.
- [ ] Apply minimal token/typography/shared-card changes.
- [ ] Run focused JVM test and `git diff --check`, record actual results.

### Task 2: Bottom navigation

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeReferenceDesign.kt`
- Modify: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/HomeReferenceLayoutTest.kt`

**Interfaces:** `ReferenceHomeBottomBar(MainTab, (MainTab) -> Unit)` unchanged.

- [ ] Correct old five-destination instrumentation assertion; add checks for all five and Accounts tap.
- [ ] Refine selected container to 48dp inside a 64dp bar with 64dp outer tap, lighter surface shadow and 12sp labels.
- [ ] Keep the gradient selected state and click routing; run source diff and focused verification.

### Task 3: Screen control polish

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ActivityReferenceSummary.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanVibrantComponents.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BillsReferenceScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/MoreScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/theme/MoreVisualTokens.kt`

**Interfaces:** Existing public/internal composable signatures unchanged.

- [ ] Give the Activity month and analytics controls accessible 48dp targets; keep chart/ledger data unchanged.
- [ ] Give Plan date/insights controls 48dp targets, tone down neutral shadows, balance title/hero text weight.
- [ ] Give Bills month selector a 48dp target while preserving its month menu and selected value.
- [ ] Make More workspace tiles slightly less tall and reduce neutral shadows and non-brand heading heaviness.
- [ ] Run targeted checks and visually inspect changed Compose modifiers for overflow risk.

### Task 4: Documentation and CI

**Files:** `CURRENT_STATE.md`, this plan and PR description.

- [ ] Record visual-only scope and phone QA limitations.
- [ ] Run `git diff --check` and the available focused JVM test, lint, and APK checks.
- [ ] Push scoped branch and create a draft PR; verify exact-head Android CI before requesting merge.
- [ ] Compare diff against the merged Accounts and notification changes.
