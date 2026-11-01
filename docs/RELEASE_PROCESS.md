# PesaTrack Release Process

_Last updated: 29 October 2026_

PesaTrack's rolling user-installable release uses the **existing protected release-signing identity**. The debug APK remains an internal workflow artifact and is not a user update download.

## Two rolling downloads

After Android CI succeeds on a push to `main`, its publish job builds a release-signed APK from the **same tested commit**. The rolling GitHub release at tag `ci-apk` contains exactly two APK attachments:

- `PesaTrack-Full.apk` — complete app, intended for a first installation.
- `PesaTrack-Update.apk` — **the same signed bytes** intended for installation over a compatible earlier app.

These filenames stay fixed. The job replaces both attachments and deletes old debug/commit-specific APK attachments after a successful signed upload. Android does not support safe in-place upgrades via APK filename or a special smaller delta file: only app ID, signing identity and `versionCode` govern that decision.

Do not mistake the attached APKs for Android instrumentation or physical-phone upgrade proof. The existing Redmi installation may have been signed by an ephemeral CI debug key; in that case, this new release certificate is not directly compatible. Never uninstall or reset until encrypted backup/restore and a one-time migration are verified and explicitly approved.

**R1 device acceptance before external/public release:** copy
[`docs/acceptance/R1_DEVICE_ACCEPTANCE_TEMPLATE.md`](acceptance/R1_DEVICE_ACCEPTANCE_TEMPLATE.md)
for the exact signed release candidate. Record the physical device, Android
version, full source SHA, APK/signing identity, five journey outcomes and
actual invented-data evidence. **NOT RUN or FAIL blocks acceptance.** The
rolling `ci-apk` downloads remain development/update preview artifacts;
publishing them does not automatically make the build physically accepted.

**Signing prerequisite:** all four `PESATRACK_RELEASE_*` secrets below must be provisioned with a persistent, separately backed-up release key. If unavailable, the rolling publication job **fails closed**, leaving the existing release as-is. It does not substitute an unsigned or newly signed debug APK.

## Versioning

Release builds accept explicit values through:

- `PESATRACK_VERSION_NAME` — semantic-style version such as `0.2.0`.
- `PESATRACK_VERSION_CODE` — positive Android integer that must increase for every Play release.

Normal debug development keeps the current defaults when these values are not supplied. The main-branch rolling publisher supplies `PESATRACK_VERSION_CODE=200000+GITHUB_RUN_NUMBER` and `PESATRACK_VERSION_NAME=0.1.<GITHUB_RUN_NUMBER>`. The numeric code grows across successful new workflow runs; rerunning a published run is refused to prevent same-code replacement. If a separately made manual signed release has a higher code, it cannot be downgraded by this rolling channel. The publisher checks the previously attached signed Update APK and rejects versions that do not increase.

## Signing secrets

Never commit a keystore or signing password.

Both **Android CI** rolling publication and the manual **Signed Android Release** workflow consume these GitHub repository secrets:

- `PESATRACK_RELEASE_KEYSTORE_BASE64`
- `PESATRACK_RELEASE_STORE_PASSWORD`
- `PESATRACK_RELEASE_KEY_ALIAS`
- `PESATRACK_RELEASE_KEY_PASSWORD`

`PESATRACK_RELEASE_KEYSTORE_BASE64` is the base64 representation of the release keystore. Keep the original keystore in a separately protected offline location. Losing the signing material can prevent future updates under the same signing identity.

### Permanent signing identity provisioned on 29 October 2026

One new permanent release identity was created after confirming that the four release-signing secrets were absent. All four secret names were verified after provisioning under `jameswaweru377-lab`; their values must never appear in repository files or logs.

- Key: RSA 4096-bit, password-encrypted PKCS12, certificate valid until **29 October 2056**.
- Public certificate SHA-256: `34189AD700D284EF66AA785CDEAEEB5CA4D1F128EE37EAFACB777E54DE461B94`.
- Windows primary keystore: `%LOCALAPPDATA%\PesaTrackRelease\keys\pesatrack-release.p12`.
- Windows encrypted recovery copy: `%LOCALAPPDATA%\PesaTrackRelease\recovery\pesatrack-release.p12`; recovery instructions are alongside it in `RECOVERY.txt`.
- Both folders restrict access to the owning Windows account and SYSTEM. The backup was checked for matching bytes, opened with the password, and its private key exercised through a certificate request before any release retry.
- The owner approved the recovery copy on **C:**. This protects against accidental loss of one copy, **not loss of the C: drive**. A separate offline copy remains an owner follow-up.

The keystore and private-key passwords are the same setup password and are not saved in recovery instructions. Setup required the owner to acknowledge saving the password securely; retrieve it from that separate password recovery record (for example, their password manager). Do not ask the owner to paste it into chat. Never regenerate or replace this identity for an ordinary build or retry. If the local key or a required secret becomes unavailable, stop and recover the existing identity; do not silently create another one.

### Offline recovery copy

1. Copy the entire local `recovery` folder to private offline media. Never attach its keystore to a PR, issue, release, chat or email.
2. Compare SHA-256 file checksums of the original and copied encrypted keystore locally, without sharing the file contents.
3. On a trusted machine, verify that the offline copy opens with the separately recorded password and contains alias `pesatrack-release`. Confirm its public certificate fingerprint matches the value above. Enter passwords interactively; never put them in shell arguments or scripts.
4. Keep the password recovery record separately from the offline keystore. Disconnect the media and store it securely. Keep both copies when moving or replacing this Windows computer.

GitHub Actions materializes a temporary runner copy from its protected secret. That is a build input, not the independent recovery backup.

### Routine rolling APK release and safe retry

1. Merge a focused, reviewed PR only after its checks pass. A push to `main` runs Android CI validation, then the signed rolling publisher from that exact tested commit.
2. Manual dispatch of Android CI does **not** publish the rolling release; its publish condition requires a `push` to `main`. The independent manual workflow below produces artifacts without changing `ci-apk`.
3. If signing prerequisites previously blocked publication, first verify the permanent key and backup, then retry only the failed publish job of that original main-push run. Do not create an empty commit or change workflow triggers merely to retry.
4. Never rerun an already published run to replace its APK: its version code is unchanged and the publisher correctly refuses it. Use a newer successful main-push run with a higher code.
5. Verify the final release has exactly `PesaTrack-Full.apk` and `PesaTrack-Update.apk`, compare their bytes, verify their public signing fingerprint against the permanent identity, and inspect application ID/version metadata. Keep phone acceptance separate from CI evidence.

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
- The **Signed Android Release** workflow remains independently/manual, with explicit version inputs; its secrets are reused by the rolling publisher. The four secret names and local encrypted recovery copy were verified during permanent-key provisioning on 29 October 2026. A new rolling signed APK is verified only after the main workflow publish job succeeds and the published APK certificate/metadata have been checked.
- A green workflow proves the stated build/tests and signed-APK metadata checks passed, **not** that the new APK successfully updated the user's installed Redmi app without data loss.

### Safeguards still required
1. Store a secure offline copy of the **same** signing keystore configured in GitHub secrets; do not generate a fresh key per build.
2. When upgrading from a previously approved **signed** update, the publisher checks that the signing fingerprint is unchanged and `versionCode` increases before replacing either asset.
3. On a test device, install the previous approved signed APK with invented local data, then install the new signed update **without uninstalling**. Verify it offers Update, retains its database/settings and launches. Report phone testing separately from CI.
4. **Existing debug-install migration:** first compare the phone's installed certificate with the new signing certificate. If incompatible, **stop** and prepare a verified, encrypted local backup/restore and explicit user-approved one-time reinstallation. Never promise direct compatibility, never discard the user's data and never request personal statement contents or credentials to be uploaded.
5. Ordinary sideloaded Android apps still require the user to approve installation; this policy does **not** promise silent/background self-updating.

## Before a public Play release

Complete `docs/PLAY_RELEASE_CHECKLIST.md`, including privacy-policy publication, restricted SMS permission declaration, Financial features declaration, Data safety, real-device acceptance and branch protection.

In particular, complete the R1 device record linked above **after** the final
release build and any minifier/ABI changes. Physical M-Pesa and Equity PDF
imports, backup/legacy restore, app lock, and a compatible in-place upgrade
must all PASS on a named physical phone with disposable invented data.

## Gradle Wrapper

The Gradle Wrapper scripts, properties and `gradle-wrapper.jar` are committed. CI/release workflows use the wrapper through the Gradle setup action. Preserve the complete wrapper when updating its pinned Gradle version.
