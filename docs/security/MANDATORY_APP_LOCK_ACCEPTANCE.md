# Mandatory app-lock acceptance — PR #678

Status: **NOT ACCEPTED** until each gate below has evidence. The Android CLI/JVM workflow passing is necessary but not sufficient. Do not use emulator or instrumentation testing.

## Code / JVM acceptance
- [ ] Reconcile this branch with current `main` without discarding other approved UI, data, or security changes; review the resulting diff.
- [ ] Run Android CLI assemble + JVM unit tests on the **resulting exact head SHA**, and verify all required checks.
- [ ] Exercise PIN creation (6, 7, 8 digits); reject 4, 5, 9, nondigit, and mismatching PINs.
- [ ] Confirm existing 4-digit PINs can still unlock after upgrading; changing the PIN requires 6–8 digits.
- [ ] Verify wrong PIN, lockout progression, lockout expiry, process death, reboot, and clock changes.
- [ ] Verify no option disables the mandatory lock; biometric authentication cannot bypass PIN enrolment.
- [ ] Review recovery authorization: only Android-confirmed device credential can authorize PIN replacement; cancelled or unsuccessful authorization must preserve the old PIN and all data.
- [ ] Confirm recovery and lock screens cover underlying activity, Compose dialogs, bottom sheets, accessibility, and touch; verify FLAG_SECURE for lock and recovery.
- [ ] Confirm document picker, backup/restore, unfinished forms, and notification deep links resume safely after unlocking, preserving state without showing data before authentication.
- [ ] Confirm financial data cannot be accessed when returning from background after the configured lock interval or on cold process start.
- [ ] Confirm privacy/security documentation accurately distinguishes app lock from database-at-rest encryption.

## Signed upgrade — actual device / release candidate
- [ ] Install the prior **signed** production APK with a known dataset and app preferences.
- [ ] Upgrade in place with the new APK signed with the **same signing identity**, without uninstalling or clearing storage.
- [ ] Verify existing balances, transaction history, linked accounts, categories, backup/restore settings and PIN enrolment survive.
- [ ] Repeat with a prior installation without a PIN: onboarding is preserved, mandatory PIN setup blocks financial access, records survive.
- [ ] Repeat with an existing legacy PIN: existing PIN unlocks, and PIN change/recovery behaves correctly.
- [ ] Confirm the upgrade's Android package ID, versionCode increase, and matching signing certificate.

## Physical Android acceptance
- [ ] Verify wrong/right PIN, biometric success/failure/cancel, PIN change and phone-credential recovery.
- [ ] Open a dialog/bottom sheet, send app to background and resume: nothing appears above PIN gate.
- [ ] Start a document-picker or backup/restore action, switch away and return: authenticate then resume intended flow.
- [ ] Test device rotation, system process reclamation if possible, task switcher/recents, screen capture policy and notification launch while locked.
- [ ] Test with immediate lock and each supported grace interval.
- [ ] Record device, Android version, app version, signer validation, results, and any redacted evidence.

**Merge rule:** no merge while GitHub reports mergeable=false, required exact-head checks fail, or critical acceptance items are unverified. A passing CI workflow is not evidence of physical-device or signed-upgrade acceptance.
