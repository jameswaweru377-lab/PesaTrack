# Google Play Release Checklist

_Last reviewed: 7 October 2026_

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

- [ ] Commit and use the Gradle Wrapper for reproducible local/CI builds.
- [x] Release builds accept intentional `versionCode` / `versionName` inputs.
- [x] A manual signed-release workflow is defined and verifies AAB/APK signatures.
- [ ] Configure the release signing key/secrets outside the repository.
- [ ] Run and retain the first verified signed Android App Bundle (AAB) for Play.
- [ ] Keep the rolling debug APK separate from public release artifacts.
- [x] Debug APK publication is gated on build + lint + unit tests + instrumentation.
- [x] GitHub Actions are pinned to immutable commit SHAs.
- [x] Wrangler is pinned to an exact version in CI/deployment workflows.
- [ ] Protect `main` and require CI before merge.

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
