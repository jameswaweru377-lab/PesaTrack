# PesaTrack Profile and Settings Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the six approved Profile/Settings mockup screens as a focused, local-first Android refinement.

**Architecture:** Reuse existing Profile, Settings, AppLock and reminder routes. Add a private profile-photo store and a bounded crop UI, surface real security preferences through `AppLockManager`, and factor a task-oriented help flow into its own Compose file. Preserve finance and backup engines.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Activity photo picker, JVM policy tests, Android Compose UI contract tests.

**Spec:** `docs/superpowers/specs/2026-10-30-profile-settings-refinement-design.md`

## Global Constraints

- No new network permission, telemetry, cloud account, ledger mutation, database migration or change to backup encryption.
- Preserve `FLAG_SECURE`, existing four-tab navigation, default 30-second background lock grace and existing app-lock PIN behavior.
- Keep backup/restore, reminders and permission routes working; add no placeholder or inert controls.
- Android minSdk 24, light profile design, local photo picker, bounded decode, private photo persistence, accessible controls.
- Instrumentation is manual opt-in; run targeted JVM/unit checks and rely on ordinary PR CI for lint and APK assembly.

## Review Focus

1. Corrupt/oversized image URI: refuse safely, retain existing photo, show error (Task 1 test).
2. Very wide/tall photo and extreme drag/zoom: crop remains in image bounds, no OOM (Task 1 test).
3. Device without biometrics or app PIN: toggle disabled and explains why; PIN route still works (Task 2 UI test).
4. App restart/old security preferences: default remains 30s and biometric behavior is unchanged (Task 2 timing test).
5. No SMS/notification access: troubleshooting says not checked/permission missing and never imports or repairs automatically (Task 3 UI test).

---

### Task 1: Private photo and personal information

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ProfilePhotoPicker.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ProfilePhotoCropPolicy.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/data/profile/ProfilePhotoStore.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PersonalDetailsScreen.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/ProfilePhotoCropPolicyTest.kt`

**Interfaces:**
- `ProfilePhotoStore(context).load(): Bitmap?`, `save(bitmap: Bitmap): Boolean`, `clear(): Boolean` manage one bounded private avatar file, leaving existing avatar unchanged on failure.
- `ProfilePhotoCropPolicy.cropRect(imageWidth, imageHeight, zoom, panX, panY): CropRect` is pure math; UI crop preview uses the same math.
- `ProfilePhotoPicker` is a composable photo-preview/edit control with `onPhotoSaved` notification.

- [ ] Write crop-policy tests for square, landscape/portrait, extreme zoom/offset and invalid dimensions; verify failing tests.
- [ ] Implement the smallest bounded crop policy; run its JVM tests green.
- [ ] Add local image read/write/replace/remove with a sampled decode limit and atomic temp-file replacement; keep emoji fallback.
- [ ] Implement photo picker and drag/zoom crop dialog; wire name/email/photo save and errors in Personal Details.
- [ ] Extend Compose UI contract for picker and current editable fields; review accessibility semantics and commit the task.

### Task 2: Profile hierarchy and accurate security preferences

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ProfileScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/security/AppLockManager.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/security/AppLockTimingPolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/MainActivity.kt`
- Modify: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/ProfileReferenceLayoutTest.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/security/AppLockTimingPolicyTest.kt`

**Interfaces:**
- `AppLockManager.isBiometricUnlockEnabled()/setBiometricUnlockEnabled(Boolean)` controls only app biometric availability (not platform enrollment).
- `AppLockManager.backgroundLockGraceMs()/setBackgroundLockGraceMs(Long)` returns a bounded allowed value, default `30_000L`.
- `AppLockTimingPolicy.shouldRequireUnlock(..., graceMs: Long = LOCK_GRACE_MS)` preserves prior callers/default.

- [ ] Add failing timing tests for default, zero, 30s and 60s intervals and stale boot markers.
- [ ] Implement durable security preferences with safe defaults; gate biometric route in MainActivity and retain PIN fallback.
- [ ] Update Profile hero to display local cropped photo when present and keep emoji fallback.
- [ ] Remove Profile financial mini-stats/route and More settings duplication; retain all other existing routes.
- [ ] Refine Security page using real app-lock actions, biometric availability, lock timing, backup/privacy/access routes.
- [ ] Update and review Profile UI contract tests, then commit.

### Task 3: Accessible reminders, help and guided troubleshooting

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SettingsDetailPages.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ProfileHelpPages.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ProfileScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SettingsScreen.kt`
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/PrivacyReminderSettingsUiTest.kt`
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/ProfileReferenceLayoutTest.kt`

**Interfaces:**
- `ChoiceSwitch` retains current signature, uses branded Material3 switch colors, full-row tap target and accessibility semantics.
- `HelpPage(...)` retains current entrypoints and opens topic routes; diagnostics callback stays unchanged.
- `ProfileTroubleshootingPage` only reads permission/source status and routes to existing user-confirmed actions.

- [ ] Update UI contracts to cover enabled/off switches and visible real help destinations.
- [ ] Implement high-contrast switch colors and dependent-option disabled states without changing reminder persistence.
- [ ] Implement searchable help topic list and step-by-step missing-transaction troubleshooting; keep advanced diagnostics reachable.
- [ ] Wire existing settings, source selection, import and account routes; do not invoke history reprocessing from a safe-check button.
- [ ] Review accessible labels, empty states and long-text layout; commit.

### Task 4: Verification and handoff

**Files:**
- Modify: `CURRENT_STATE.md` (short Profile implementation note only)

**Interfaces:** No new runtime API.

- [ ] Run focused JVM tests, then `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` if the local environment permits.
- [ ] Inspect diff for screenshots/`FLAG_SECURE`, backup, permissions, `MainActivity`, financial ledger, duplicate nav and secret changes.
- [ ] Record checked commit and exact CI result; open focused PR without touching overlapping active branches.
- [ ] Do not merge or alter signed rolling releases without task-specific approval; report remaining manual-device visual/security checks.
