# PesaTrack Release Process

_Last updated: 7 October 2026_

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
3. Enter an intentional version name and a version code greater than the last release.
4. The workflow validates the Worker contract, runs JVM tests and release lint, then builds both:
   - signed AAB for Play;
   - signed APK for controlled testing.
5. The workflow verifies both signatures and produces SHA-256 checksums.
6. Download the workflow artifact and perform final device/store validation before publishing.

The workflow has repository read permission only. It does not automatically publish to Google Play or create a public GitHub release.

## Before a public Play release

Complete `docs/PLAY_RELEASE_CHECKLIST.md`, including privacy-policy publication, restricted SMS permission declaration, Financial features declaration, Data safety, real-device acceptance and branch protection.

## Gradle Wrapper

The audit still requires a committed Gradle Wrapper. The current CI/release workflows pin Gradle 9.5 through the Gradle setup action until the wrapper files are committed. Do not add an incomplete wrapper that omits `gradle-wrapper.jar`.
