# R3 minified release acceptance — NOT RUN on a physical phone

Source: finding R3, tracking issue #548. A successful Gradle build is not
evidence that PDF parsing, encrypted backups or app lock work at runtime.

## Reproducible unsigned size comparison

Fill these from the `r3-release-shrink-evidence` artifact of the exact PR head:

- [ ] PR head SHA: `NOT RUN`
- [ ] Exact pre-change main SHA: `NOT RUN`
- [ ] Unminified APK size, bytes: `NOT MEASURED`
- [ ] R8/resource-shrunk APK size, bytes: `NOT MEASURED`
- [ ] Saved bytes and percent: `NOT MEASURED`
- [ ] Package ID, minSdk=24, ARM64-only ABI and Chaquopy asset parity: `NOT VERIFIED`
- [ ] R8 missing-class warnings reviewed with the generated configuration: `NOT VERIFIED`

Both APKs are unsigned and must **not** be distributed as PesaTrack updates.
The comparison does not prove that embedded Python will start on Android.

## Signed candidate and device gate

Do not use the `main` auto-publisher as the *first* runtime experiment: it
replaces the rolling public signed APKs. A protected way to sign a non-main
candidate with the existing release identity, and a backed-up test phone,
must be arranged by the owner without exposing or changing the keystore.

- [ ] Identify exact signed candidate SHA/version/signing certificate.
- [ ] Same-phone in-place update retains pre-existing saved ledger and settings.
- [ ] M-Pesa PDF import: review/confirm a valid invented statement; reject an incomplete one.
- [ ] Equity PDF import: review/confirm and reconcile amount/date/account.
- [ ] Encrypted backup: create, wrong-passphrase rejection, restore and compare records.
- [ ] App lock: PIN, background timeout and any enabled biometric unlock.
- [ ] Execute T1 emulator migration, backup/restore, replay and money-aggregate suite on candidate SHA.
- [ ] Record phone model, Android/API, test date, PASS/FAIL details and screenshots with financial data redacted.

Status: **NOT RUN**. Keep the R3 PR draft and current signed rollback APK
until every required release criterion passes. No release tag is authorized.
