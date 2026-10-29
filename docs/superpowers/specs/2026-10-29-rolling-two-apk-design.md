# Rolling signed APK channel — approved behavior

## Intent

PesaTrack's private, direct-install Android builds should publish one rolling GitHub release with exactly two downloadable APK assets: `PesaTrack-Full.apk` and `PesaTrack-Update.apk`. Every successful main-branch build replaces those two names; older per-commit APK attachments are removed. Both names are copies of the same complete, signed APK (no unsupported binary patch/delta delivery).

## Constraints

- Keep application ID `com.kedevjimmy.pesatrack`, databases, permissions, current UI, and all financial logic untouched.
- Preserve the already required main-branch unit/lint/build gates and optional-only instrumentation.
- Use the existing **release** signing credentials in protected repository secrets; never produce a publicly accessible or ephemeral signing key.
- Strictly increase Android `versionCode` across accepted rolling releases. A human-readable `versionName` and source commit must identify the build.
- Verify the built APK's package, signature, alignment and version. Compare signer identity and version against the previous rolling **signed** APK when one exists; fail rather than publish an incompatible replacement.
- Do not publish debug APKs under user-installable Full/Update names. Missing secrets are an explicit failure, not permission to publish an unsigned or ephemeral-debug substitute.
- Do not promise that the first signed APK can update a previously installed ephemeral-debug APK. A different certificate requires a separate consented backup/migration, never blind uninstall.
- Release asset names remain stable. Release notes describe current commit/version and the device-testing limitation. The fixed Git tag's auto-generated source archive may represent its original commit, so release notes must state the authoritative source SHA.

## Approach

Chosen: retain the existing `Android CI` validation job, then on successful pushes to `main`, build a signed release from the same source commit in the publish job and replace the two rolling assets after verification. Reuse the signing variables in `release-build.yml` and fail closed if not provisioned. This costs a second Android assembly on `main` but keeps private signing material out of PR validation and prevents an unverified debug fallback.

Rejected: renaming the current debug APK (no stable signing identity) and publishing version-specific APKs indefinitely (not the requested two-file channel).

## Acceptance

1. PR CI remains secret-free and still gates merge.
2. Main signed publication requires all four existing secrets and fails clearly if absent.
3. APK signature/package/version checks occur before any release mutation.
4. Existing rolling signed signer/`versionCode` must be compatible with the replacement.
5. A successful release contains exactly the two named APK assets; they have equal SHA-256.
6. Physical-device in-place upgrade and data retention are marked unverified until checked on an installed device.
