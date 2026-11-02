# S3 Backup Creation Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the already-approved 12-character minimum at both new encrypted-backup creation APIs without blocking existing eight-character backups.

**Architecture:** Add a small Android-independent passphrase policy consumed by both the screen rules and backup manager. Creation and restoration have distinct validation; leave the existing PBKDF2-180,000, AES-GCM format and historical decrypt iteration range unchanged.

**Tech Stack:** Kotlin/JUnit, AndroidX instrumentation tests, existing Gradle CI.

**Spec:** `docs/superpowers/specs/2026-11-01-audit-remediation-design.md` and the owner's 2 November 2026 confirmation to enforce 12 on creation and preserve 8 on restore.

## Global Constraints

- Preserve Android API 24, application ID, backup wire format, schema, encryption parameters and signed-update continuity.
- No financial calculations, network calls, permissions, Kotlin/JavaScript privacy policies or UI design changes.
- Local JVM proof does not establish physical-device backup restoration; report the acceptance gap.
- No expansion of the existing 1,300-line backup manager beyond replacing validation calls and removing its old helper.

## Review Focus

- Empty and eleven-character new passphrases: creation rejects before writing any file (Task 1 tests).
- Exactly twelve characters: new backup creation remains possible (Task 1 tests).
- Existing eight-character passphrase: preview and restore are still allowed (Task 1 tests).
- Seven-character historical passphrase: restore continues to reject (Task 1 tests).
- An old backup with 180,000 PBKDF2 iterations: remains decryptable; do not rekey or rewrite on restore (Task 1 instrumentation test and existing API24 vector coverage).

---

### Task 1: Enforce creation policy while preserving historical restore

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/security/BackupPassphrasePolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/security/PesaBackupManager.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BackupFlowRules.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/security/BackupPassphrasePolicyTest.kt`
- Modify: `app/src/androidTest/java/com/kedevjimmy/pesatrack/security/BackupRecoveryIntegrationTest.kt`
- Modify: `app/src/androidTest/java/com/kedevjimmy/pesatrack/security/EncryptedBackupFixture.kt` (test-only rekey helper for legacy test vector)

**Interfaces:**
- Produces `BackupPassphrasePolicy.requireForCreation(passphrase: String)`, `requireForRestore(passphrase: String)`, `isValidForCreation(passphrase: String): Boolean`, `isValidForRestore(passphrase: String): Boolean`.
- Consumer manager calls creation check in `exportEncrypted` and `exportEncryptedBytes`; restore/preview paths call legacy check; screen rule checks share the same policy.

- [ ] Step 1: Write JVM tests for rejected lengths 0/7/8/11, accepted 12/13, restore 7 rejected and 8 accepted; write Android tests for both manager creation paths and an eight-character rekeyed 180,000-iteration file preview/restore.
- [ ] Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*BackupPassphrasePolicyTest'` and observe failure caused by the missing policy (RED). Android tests run only with an emulator; compile them in CI.
- [ ] Step 3: Add minimal policy, rewire manager and screen rules; leave encryption header/decrypt range untouched.
- [ ] Step 4: Run focused JVM tests plus `BackupFlowRulesTest`, then broad JVM suite, lint, debug APK and instrumentation compilation if available. Record failures as not verified rather than inventing results.
- [ ] Step 5: Update `CURRENT_STATE.md` and `docs/CRYPTO_API_COMPATIBILITY.md`; create one reviewable S3 PR tracking issue #579 and await exact-head CI.

### Task 2: Verify integration and safely hand off

**Files:** no additional source edits unless tests expose a regression.

**Interfaces:** Consumes Task 1's tested branch; produces exact-head CI and merge/device-gate status.

- [ ] Step 1: Review the full diff for changed money, export file, header/iteration, permissions and UI behavior; assert only creation validation changed.
- [ ] Step 2: Recheck latest main and own PR SHA, wait for required Android and secret-scanning checks; investigate any failure.
- [ ] Step 3: If approved CI and freshness gates pass, merge only this PR into latest main under the owner's prior merge authorization; keep the tracker open for physical-device backup acceptance.
- [ ] Step 4: Report tests, commit/merge SHA, and unmet emulator/device PBKDF2 timing evidence.
