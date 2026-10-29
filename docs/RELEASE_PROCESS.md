# PesaTrack Release Process

_Last updated: 29 October 2026_

PesaTrack's rolling user-installable release uses the **existing protected release-signing identity**. The debug APK remains an internal workflow artifact and is not a user update download.

## Two rolling downloads

After Android CI succeeds on a push to `main`, its publish job builds a release-signed APK from the **same tested commit**. The rolling GitHub release at tag `ci-apk` contains exactly two APK attachments:

- `PesaTrack-Full.apk` — complete app, intended for a first installation.
- `PesaTrack-Update.apk` — **the same signed bytes** intended for installation over a compatible earlier app.

These filenames stay fixed. The job replaces both attachments and deletes old debug/commit-specific APK attachments after a successful signed upload. Android does not support safe in-place upgrades via APK filename or a special smaller delta file: only app ID, signing identity and `versionCode` govern that decision.

Do not mistake the attached APKs for Android instrumentation or physical-phone upgrade proof. The existing Redmi installation may have been signed by an ephemeral CI debug key; in that case, this new release certificate is not directly compatible. Never uninstall or reset until encrypted backup/restore and a one-time migration are verified and explicitly approved.

**Signing prerequisite:** all four `PESATRACK_RELEASE_*` secrets below must be provisioned with a persistent, separately backed-up release key. If unavailable, the rolling publication job **fails closed**, leaving the existing release as-is. It does not substitute an unsigned or newly signed debug APK.

## Versioning

Release builds accept explicit values through:

- `PESATRACK_VERSION_NAME` — semantic-style version such as `0.2.0`.
- `PESATRACK_VERSION_CODE` — positive Android integer that must increase for every Play release.

Normal debug development keeps the current defaults when these values are not supplied. The main-branch rolling publisher supplies `PESATRACK_VERSION_CODE=200000+GITHUB_RUN_NUMBER` and `PESATRACK_VERSION_NAME=0.1.<GITHUB_RUN_NUMBER>`. The numeric code grows across successful new workflow runs; rerunning a published run is refused to prevent same-code replacement. If a separately made manual signed release has a higher code, it cannot be downgraded by this rolling channel. The publisher checks the previously attached signed Update APK and rejects versions that do not increase.

## Signing secrets

Never commit a keystore or signing password.

The manual **Signed Android Release** workflow expects these GitHub repository secrets:

- `PESATRACK_RELEASE_KEYSTORE_BASE64`
- `PESATRACK_RELEASE_STORE_PASSWORD`
- `PESATRACK_RELEASE_KEY_ALIAS`
- `PESATRACK_RELEASE_KEY_PASSWORD`

`PESATRACK_RELEASE_KEYSTORE_BASE64` is the base64 representation of the release keystore. Keep the original keystore in a separately protected offline location. Losing the signing material can prevent future updates under the same signing identity.

## Build flow

1. Open GitHub Actions -> **Signed Android Release**.
2. Choose **Run workflow**.
3. Enter an intentional version name and a version code greater than the last release. Leave run_instrumentation off by default; enable it only when the user explicitly requests emulator validation. When enabled, emulator checks must pass before signing.
4. The workflow validates the Worker contract, runs JVM tests and release lint, then builds both:
   - signed AAB for Play;
   - signed APK for controlled testing.
5. The workflow verifies both signatures and produces SHA-256 checksums.
6. Download the workflow artifact and perform final device/store validation before publishing.

The workflow has repository read permission only. It does not automatically publish to Google Play or create a public GitHub release.

## In-place APK updates (phone installation policy)

**Goal:** A user who already has PesaTrack installed should be able to tap a newer approved APK and choose **Update**, keeping the same local financial database and app settings. A file called `PesaTrack-update.apk` is not inherently different from a first-install APK; Android determines upgrade eligibility, not the filename.

### What Android requires
1. **Same application ID** (`com.kedevjimmy.pesatrack`) and compatible Android install target.
2. **Compatible signing certificate** (or supported signing-key rotation lineage) with the APK already installed on the phone.
3. **No version-code downgrade.** Give each newer installable APK a version code higher than its predecessor. A readable `versionName` helps users identify the release but does not replace `versionCode`.

### CI implementation and validation status (29 October 2026)
- `.github/workflows/android-ci.yml` is configured to validate debug builds and then build a **signed release** on `main` from the same commit for rolling publication. Tests in `tools/test_publish_rolling_apks.py` use entirely synthetic artifacts and mocked GitHub CLI commands to check two-asset publication, signer mismatch, downgrade prevention and missing version metadata.
- The **Signed Android Release** workflow remains independently/manual, with explicit version inputs; its secrets are reused by the rolling publisher. **Secret provisioning has not been verified by local tests.** Do not assume that a new rolling signed APK was published until the main workflow publish job succeeds.
- A green workflow proves the stated build/tests and signed-APK metadata checks passed, **not** that the new APK successfully updated the user's installed Redmi app without data loss.

### Safeguards still required
1. Store a secure offline copy of the **same** signing keystore configured in GitHub secrets; do not generate a fresh key per build.
2. When upgrading from a previously approved **signed** update, the publisher checks that the signing fingerprint is unchanged and `versionCode` increases before replacing either asset.
3. On a test device, install the previous approved signed APK with invented local data, then install the new signed update **without uninstalling**. Verify it offers Update, retains its database/settings and launches. Report phone testing separately from CI.
4. **Existing debug-install migration:** first compare the phone's installed certificate with the new signing certificate. If incompatible, **stop** and prepare a verified, encrypted local backup/restore and explicit user-approved one-time reinstallation. Never promise direct compatibility, never discard the user's data and never request personal statement contents or credentials to be uploaded.
5. Ordinary sideloaded Android apps still require the user to approve installation; this policy does **not** promise silent/background self-updating.

## Before a public Play release

Complete `docs/PLAY_RELEASE_CHECKLIST.md`, including privacy-policy publication, restricted SMS permission declaration, Financial features declaration, Data safety, real-device acceptance and branch protection.

## Gradle Wrapper

The audit still requires a committed Gradle Wrapper. The current CI/release workflows pin Gradle 9.5 through the Gradle setup action until the wrapper files are committed. Do not add an incomplete wrapper that omits `gradle-wrapper.jar`.
