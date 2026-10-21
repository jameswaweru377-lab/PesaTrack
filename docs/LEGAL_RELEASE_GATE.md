# PesaTrack pre-release legal, security and privacy gate

This checklist must be completed **before a public store release or legally binding Terms acceptance**. The Settings legal screens are pre-release informative summaries, not final contracts.

## Mandatory ownership and legal review
- [ ] Identify the actual legal publisher/controller, physical or business address, support email, private privacy/security contact and jurisdiction.
- [ ] Obtain legal review of `docs/TERMS_OF_SERVICE_DRAFT.md` and `docs/PRIVACY_POLICY.md` by a Kenyan lawyer. Publish verified, dated, versioned terms and privacy pages at stable publicly accessible URLs.
- [ ] Assess ODPC registration applicability (including exemptions and exceptions) with the actual publisher's facts. Document the outcome and required actions, rather than asserting automatic exemption.
- [ ] Document lawful basis for processing user and counterparties' personal data; provide access, correction, objection and deletion channels where applicable.
- [ ] If Terms acceptance is required, implement an explicit unbundled user choice, record accepted document version and timestamp locally, and provide a way to read the Terms again. **Do not auto-consent** or make SMS access part of Terms acceptance.
- [ ] Have an incident/breach notification and response process appropriate for the deployed services and applicable law.

## Play Store and permissions
- [ ] Obtain Google Play review/declaration approval if distributing an APK requiring `READ_SMS` for the SMS-based money management exception; do not assume it is automatically granted.
- [ ] Provide prominent just-in-time SMS/notification/audio disclosures, no pre-checked consent; confirm decline and revocation paths work.
- [ ] Publish a complete and accurate Google Play Data safety form based on actual binaries, backend services, SDKs, retention and exports. Revalidate after updates.
- [ ] Verify online business-name lookup is off by default, sanitizes data on device, and presents any third-party endpoint and cross-border data-transfer implications honestly.
- [ ] Verify on-device Android speech recognition is required, or update voice privacy notices if fallback to an online recognizer is ever introduced.

## Security and financial accuracy
- [ ] Validate single-statement deletion and restore/reimport behaviour on a real Android device; ensure external backups are handled explicitly.
- [ ] Enable and review GitHub Dependabot alerts/security updates and secret scanning where available. The offline build policy scanner is not a full dependency vulnerability scanner.
- [ ] Conduct threat modeling for unencrypted local SQLite primary/statement databases. Document Android sandbox limitations; consider a carefully tested migration strategy for encryption at rest **without risking existing finance or backup data**.
- [ ] Test backup encryption, restore, failed import recovery, secure PIN verification, inactivity lock and protected recovery surfaces.
- [ ] Audit exception strings, analytics/crash reports and debug logging to exclude names, amounts, account numbers, SMS text and PDF contents.
- [ ] Ensure app-private databases are excluded from Android automatic backup, verify the rule on actual devices and confirm exported files are user controlled.
- [ ] Verify imported statement history cannot automatically change balance, debt, spending or income. Test canonical reconciliation with overlapping SMS and PDF evidence, reference reuse, time drift and partial duplicates.
- [ ] Review and test performance of leading-wildcard searches and large statement archives on midrange Android devices (e.g. 5k, 25k, 100k entries). Do not claim a measured speedup without a benchmark.
- [ ] Perform a production build review, dependency/security scan and permission verification.
- [ ] Test Settings legal pages, onboarding disclosure readability, deletion/export choices, and accessible support contact on a device.

## Grounding and official references
- Kenyan Office of the Data Protection Commissioner: https://www.odpc.go.ke/rights-of-a-data-subject/
- ODPC registration guidance: https://www.odpc.go.ke/faqs/
- Google Play SMS or call-log use rules: https://support.google.com/googleplay/android-developer/answer/10208820?hl=en-GB

None of these drafts or technical steps guarantees immunity from a legal claim.
