# Encrypted Backup & Restore Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the crowded backup/restore form with a safe action hub and two guided on-device flows.

**Architecture:** Preserve the public `BackupRestoreSheet(onDismiss)` entry point; move its implementation to `BackupRestoreFlow.kt`, using separate Create and Restore screens and the existing `PesaBackupManager`. A small pure rule/policy model handles strength and flow constraints. Device authentication is a separate step before data replacement, never a decryption mechanism.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Android Storage Access Framework, platform BiometricPrompt/KeyguardManager, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-30-backup-restore-phase1-design.md`

## Global constraints

- No new Android permission, network dependency, Drive API, APK signing change, database migration or backup file format change.
- Keep existing `.ptb` v1/v2 restore behavior; creation UI requires 12 characters, legacy restoration accepts 8.
- Keep approved light PesaTrack appearance and existing Profile/Settings call sites.
- Never claim an actual Redmi restore without physical-device evidence; instrumentation stays paused.
- Never upload raw financial data or passwords; do not merge without separate approval.

## Review focus

1. OpenDocument cancellation leaves no stale selected URI/passphrase: verify with screen state reset tests and manual review.
2. Entering a wrong passphrase or an oversized/damaged `.ptb` never advances to preview or mutation: existing manager validation tests and error-path UI review.
3. A cancelled/failed system credential or app PIN never invokes `restoreEncrypted`: verify authorization policy and manual UI review.
4. Document-provider create returns URI but reread fails: fail with clear warning, no success claim.
5. Rotation while busy must not invite a second restore: in-memory `busy` and sheet dismissal guard; device verification pending.

---

### Task 1: Pure flow rules and deterministic JVM tests

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BackupFlowRules.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/BackupFlowRulesTest.kt`

**Interfaces:**
- Produces `BackupFlowRules.createError(passphrase: String, confirmation: String): String?`
- Produces `BackupFlowRules.restoreError(passphrase: String): String?`
- Produces `BackupFlowRules.canRestore(previewReady: Boolean, acknowledgedReplacement: Boolean, busy: Boolean): Boolean`

- [ ] Write unit tests for 11-character create rejection, 12+ accepted, mismatch rejected, 8-character restore accepted, shorter restore rejected, restore requires preview/acknowledgment/non-busy.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*BackupFlowRulesTest'` expecting RED before policy exists. Record local SDK/Gradle blocker if any, then use CI evidence instead.
- [ ] Implement smallest pure `BackupFlowRules` object.
- [ ] Rerun focused test expecting PASS (or record environment blocker and route to CI).
- [ ] Commit test and policy.

### Task 2: Action hub and guided Create / Restore UI

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BackupRestoreFlow.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SecurityScreens.kt` (remove old sheet, retain other security surfaces)

**Interfaces:**
- Consumes `BackupFlowRules` from Task 1 and existing `PesaBackupManager`, `BackupRecoveryStatus`, `AppLockManager`.
- Produces existing external `@Composable fun BackupRestoreSheet(onDismiss: () -> Unit)` unchanged.

- [ ] Write a behavior regression checklist/test for separation of create/restore, picker-first restore, validation and reauthentication; use existing manager tests for real encrypted archive safety.
- [ ] Implement a compact sheet with two distinct actions and clear offline/Drive-manual copy.
- [ ] Implement full-screen Create: 12-character passphrase+confirm; system save picker; local AES encrypted export; reread/validate file before claiming success.
- [ ] Implement full-screen Restore: select `.ptb` first, enter file passphrase, preview summary, explicit replacement acknowledgment, system credential/biometric or app-PIN fallback, restore and recreate activity.
- [ ] Ensure busy/error/cancel/back cases cannot accidentally write data; keep secret strings out of saved state/logs.
- [ ] Run `git diff --check`, focused JVM tests, `:app:lintDebug`, `:app:assembleDebug` when toolchain available; record blockers otherwise.
- [ ] Commit UI change.

### Task 3: PR and CI verification

**Files:**
- Modify: `CURRENT_STATE.md` only for verified new feature status, if useful; no workflow/build file changes.

- [ ] Compare changed files against current remote `main` and independently review money/privacy/security side effects.
- [ ] Push new branch and open PR; inspect Android CI build, lint, unit tests and APK assembly; correct errors on the same branch until green.
- [ ] Mark Redmi manual create/restore/upgrade verification as pending; request merge decision only after CI passes.
