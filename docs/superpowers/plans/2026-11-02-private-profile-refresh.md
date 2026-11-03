# PesaTrack Private Profile Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Replace the duplicated Profile settings dashboard with the approved private, edge-to-edge cover design; improve local-only editing; remove two redundant More shortcuts.

**Architecture:** Keep More's five-tab navigation intact and make Profile a detail destination without the branded top bar or bottom tabs. Compose the banner from a short full-width background and a foreground avatar overlapping its bottom edge. Persist a user-selected cover image in private app storage; keep optional city/bio in existing private preferences. Reuse the existing money-habits presentation and pay-cycle destination.

**Tech Stack:** Kotlin, Jetpack Compose, Android Photo Picker, JUnit.

**Spec:** User-approved Profile v4 screenshot design and November 2 review: full-bleed short banner ending at avatar midpoint; smaller edit controls; local-only editable cover; simple back arrow; no duplicated settings/help/about; optional city/bio; meaningful money-habits; keep financial and privacy semantics.

## Constraints

- No location permission, login, new network calls, or change to ledger/financial calculations.
- Do not remove Past Transactions/Accounts functionality, only their More shortcuts.
- Do not change the five root tabs, application security, backups, or notifications.
- Preserve existing stored `profile_email`; do not introduce an editable field that is otherwise unused.
- Verify narrowly first, report honestly if Android toolchain is unavailable.

### Task 1: Private profile validation

**Files:** `ProfileDetailsFeedbackPolicy.kt`, `ProfileDetailsFeedbackPolicyTest.kt`.

- Write tests for trimmed required name, optional trimmed city/bio, failed persistence, and rejecting empty display names; run them RED.
- Update the pure profile save policy; run tests GREEN.

### Task 2: Local cover storage

**Files:** new `data/profile/ProfileCoverStore.kt`, existing media decoding helper, and focused tests where feasible.

- Add bounded image handling, crop to cover aspect ratio, atomic local save, reset to default; Android Photo Picker only.
- Keep avatar storage independent; verify policy and compile via Android CI if local SDK is unavailable.

### Task 3: Profile UI and routing

**Files:** `ProfileScreen.kt`, `PersonalDetailsScreen.kt`, `PesaTrackApp.kt`.

- Remove branded top bar and persistent tabs on Profile detail route.
- Implement short full-width purple cover, separate edit cover affordance, avatar above seam, tighter details, smaller Edit profile control.
- Use optional manually-entered city/bio in Profile and Edit Profile; keep money-habits, personal info, one functional Pay cycle navigation link.
- Handle photo picker errors and cover reset without touching financial records.

### Task 4: Remove redundant More links and verify

**Files:** `MoreScreen.kt`, targeted tests/documentation.

- Remove visible `Past transactions` and `Funds` rows without deleting Activity/Accounts routes.
- Run diff/format checks and focused tests; attempt unit tests and assemble without instrumentation. Use required CI for build if local Android SDK unavailable.
- Regenerate final mockup showing the approved refinements; commit on dedicated branch and prepare a reviewable PR (no unapproved merge).

## Review Focus

- Avatar is drawn above cover edge with no visible horizontal seam across the image.
- Cover is truly edge-to-edge, correct short height, with an accessible picker/reset.
- No unintended deletion/overwrite of financial data, email preference, or existing avatar.
- Profile detail navigation returns cleanly to More and does not change root tabs.
- Saved name/city/bio appear on return; blank optional fields do not cause fake placeholders.
