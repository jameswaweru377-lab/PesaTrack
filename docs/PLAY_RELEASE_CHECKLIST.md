# Google Play Release Checklist

_Last reviewed: 7 October 2026_

_Release-engineering status rechecked against code and signed CI: 1 November 2026. Play policy declarations above have **not** been re-reviewed by this update._

This is a release gate, not a claim that Google Play approval is guaranteed. Re-check current Play Console policy before each public release.

## Restricted SMS permission

PesaTrack currently declares `READ_SMS` for historical financial-message import.

Google Play lists **SMS-based money management** (for example, budget tracking/management) as a permitted use that may be eligible for SMS permissions, subject to Google Play review and approval.

Before production submission:
- [x] SMS access is tied to the core money-management experience.
- [x] The app provides a prominent in-app disclosure before requesting SMS access.
- [x] The disclosure states what is read, why it is needed, that processing is local, and that full SMS is not sent to optional merchant lookup.
- [x] The user can continue without historical SMS access.
- [ ] Complete the SMS/Call Log Permissions Declaration in Play Console and select the accurate money-management use case.
- [ ] Ensure the Play Store listing prominently describes the core SMS-based money-management feature.
- [ ] Re-check the current SMS/Call Log policy immediately before submission.

Official reference:
https://support.google.com/googleplay/android-developer/answer/10208820

## Financial features declaration

Google Play requires apps published on Play to complete the Financial features declaration, including apps that do not offer listed financial features.

PesaTrack is a personal money-management/ledger product and does not initiate payments, lend money, broker loans or trade assets. The exact declaration choice must match the Play Console wording available at submission time.

- [ ] Complete **Policy and programs -> App content -> Financial features**.
- [ ] Select only features PesaTrack actually provides.
- [ ] Do not describe debt tracking as PesaTrack providing or facilitating loans.
- [ ] If the console requires a catch-all financial category, review the current "Other" option and describe PesaTrack as local personal money management/ledger software.

Official reference:
https://support.google.com/googleplay/android-developer/answer/13849271

## Privacy and user data

- [x] Repository privacy-policy source exists at `docs/PRIVACY_POLICY.md`.
- [x] Optional online merchant lookup is off by default.
- [x] Full SMS, amounts, balances, account/phone numbers and transaction codes are outside the enrichment contract.
- [ ] Publish the privacy policy at a stable public HTTPS URL and enter that URL in Play Console and the store listing where required.
- [ ] Complete Play Console Data safety using the behavior of the release build, not assumptions from old documentation.
- [ ] Verify every SDK/dependency against the Data safety answers.
- [ ] Capture screenshots/video of the prominent SMS disclosure for review evidence.

## Release engineering

- [x] Gradle Wrapper scripts, properties and `gradle-wrapper.jar` are committed; Android CI uses the wrapper.
- [x] Release builds accept intentional `versionCode` / `versionName` inputs.
- [x] A manual signed-release workflow is defined and verifies AAB/APK signatures.
- [x] The persistent release signing key/secrets are configured outside source and were exercised by the successful signed `main` rolling publisher. This does **not** verify secret environment protection or independent offline recovery.
- [ ] Run and retain the first verified signed Android App Bundle (AAB) for Play.
- [x] Keep the debug APK as an internal workflow artifact. The rolling `ci-apk` preview contains exactly `PesaTrack-Full.apk` and `PesaTrack-Update.apk`, two identical **release-signed** APKs, not public debug builds.
- [x] On a successful `main` push, Android CI validates the tested commit and publishes the signed rolling APK only after build, lint and JVM checks. Emulator instrumentation is optional/manual; none of this is physical-device acceptance.
- [x] GitHub Actions are pinned to immutable commit SHAs.
- [x] Wrangler is pinned to an exact version in CI/deployment workflows.
- [ ] Protect `main` and require CI before merge.
- [ ] Independently confirm protected signing-environment secret scope, maintainer access/2FA and an offline recoverable copy of the **same** release keystore and password recovery instructions.

Evidence for these engineering-state checks: tracked `gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.properties` and `gradle-wrapper.jar`;
`.github/workflows/android-ci.yml` and `release-build.yml`; and the
[successful signed rolling build/publish run 41297723937](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/41297723937)
on source SHA `f247140c5a68f0bf04925bcae4575ccaabeaf0b7`. This is not
proof of a compatible phone update or a Play AAB submission; see the
[release process](RELEASE_PROCESS.md) and the still-open R1 acceptance record.

## Pre-release validation

- [ ] Run a clean-install journey on the target Redmi/Xiaomi device.
- [ ] Validate restricted-settings guidance on a sideloaded debug build.
- [ ] Validate 1-month and 3-month historical import with redacted/owned data.
- [ ] Validate notification capture, duplicate/mirror handling and replay.
- [ ] Validate backup preview, wrong-passphrase behavior, corruption handling and restore confirmation.
- [ ] Validate large-font, TalkBack and contrast-critical flows.
- [ ] Run large-ledger/import performance benchmarks.

## Store listing accuracy

The store listing must not claim that PesaTrack:
- connects directly to banks when it does not;
- sends payments or transfers;
- offers loans, credit or investment products;
- guarantees every message can be interpreted;
- performs cloud backup unless that capability actually exists.

Keep the listing aligned with `docs/PRODUCT_CHARTER.md` and `CURRENT_STATE.md`.
