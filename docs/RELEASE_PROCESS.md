# PesaTrack Release Process

_Last updated: 29 October 2026_

PesaTrack keeps the rolling debug APK separate from signed release artifacts.

## Versioning

Release builds accept explicit values through:

- `PESATRACK_VERSION_NAME` — semantic-style version such as `0.2.0`.
- `PESATRACK_VERSION_CODE` — positive Android integer that must increase for every Play release.

Normal debug development keeps the current defaults when these values are not supplied.

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

### Current repository behavior (29 October 2026; not yet upgrade-ready)
- `.github/workflows/android-ci.yml` publishes `PesaTrack-debug.apk` and a commit-specific debug APK after checks on `main`. The debug keystore is not explicitly persisted across GitHub runners. This makes in-place updates **unreliable**; the workflow also uses the Gradle default `versionCode=1` unless configured otherwise.
- The **Signed Android Release** workflow already has conditional release signing support and explicit version inputs, but it is **manual** and does not automatically publish the rolling `ci-apk` asset. Do not assume its secrets are configured or that its signing certificate matches any debug build on a user's phone.
- A green workflow proves the stated build/tests passed, **not** that a new APK will install over an older APK on a physical Redmi device.

### Proposed permanent update channel (requires separate implementation approval)
1. Select and securely retain **one signing identity** for every PesaTrack APK intended to upgrade the same installed application. Store required secrets only in GitHub Actions encrypted secrets and keep an independently protected offline backup.
2. Assign a **strictly increasing `versionCode`** for each user-facing build; maintain a readable version name and immutable source commit association.
3. Build and verify the signed APK. Before release, check APK package name, version code, signing-certificate fingerprint and integrity; compare the certificate against the expected previous user-installable build. Never print passwords or private keys.
4. Publish an unambiguous APK link with commit and version details. A single signed APK can be both a **fresh install** (on a phone without PesaTrack) and an **update** (on a phone with a compatible earlier install); making two identically functional files is unnecessary.
5. On a test device, first install the previous approved APK with synthetic local data, then install the new APK **without uninstalling**. Verify it offers Update, retains the local database/settings and launches. Report phone testing separately from CI.
6. **Existing debug-install migration:** first compare the phone's installed certificate with the new signing certificate. If incompatible, **stop** and prepare a verified, encrypted local backup/restore and explicit user-approved one-time reinstallation. Never promise direct compatibility, never discard the user's data and never request that personal statement contents or credentials be uploaded.
7. Ordinary sideloaded Android apps still require the user to approve installation; this policy does **not** promise silent/background self-updating.

**Status:** Documentation of the agreed target and safeguards only. This section does not enable stable signing, create credentials, change workflows, or establish successful on-phone update testing.

## Before a public Play release

Complete `docs/PLAY_RELEASE_CHECKLIST.md`, including privacy-policy publication, restricted SMS permission declaration, Financial features declaration, Data safety, real-device acceptance and branch protection.

## Gradle Wrapper

The audit still requires a committed Gradle Wrapper. The current CI/release workflows pin Gradle 9.5 through the Gradle setup action until the wrapper files are committed. Do not add an incomplete wrapper that omits `gradle-wrapper.jar`.
