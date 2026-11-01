# R1 — Physical-device release acceptance record

**State: NOT RUN. This is a blank repeatable test sheet, not a claim that a phone passed.**

Create a copy of this sheet for each *actual* release candidate. Use only a spare
phone or a disposable test installation containing invented financial data. Do
not run destructive restore or reinstall steps over the owner's only live
ledger. Never put private PDFs, SMS, phone numbers, PINs, passphrases, account
numbers, or keystore contents in GitHub or an issue.

## Candidate and device

| Evidence | Recorded result |
|---|---|
| Test date/time and tester | NOT RUN |
| Physical phone model, Android version and ABI | NOT RUN |
| App Git commit SHA (full 40 characters) | NOT RUN |
| APK SHA-256 and source download/run | NOT RUN |
| Application ID and old/new versionCode | NOT RUN |
| Old installed certificate and candidate signing SHA-256 | NOT RUN |
| Disposable test dataset and expected counts/totals (invented only) | NOT RUN |
| Independent encrypted recovery backup verified before testing | NOT RUN |

**Preflight — all must pass before proceeding:**

- [ ] The APK identifies the exact commit being accepted, has the expected
      application ID, verified release signature and non-decreasing code for a
      fresh install (strictly higher code for an update).
- [ ] An **in-place update** uses a matching signing certificate or a proven
      supported signing lineage. If an old installation is debug-signed or
      incompatible, **STOP**. Do not uninstall, clear storage or replace live
      data to force an update.
- [ ] The test app contains **invented disposable data only**; its expected
      transaction, statement, debt, transfer and account totals are written down
      locally before the operation.
- [ ] A separate encrypted backup can be opened and is available for recovery;
      the tester will not restore over an irreplaceable real ledger.

## Test journeys

Set each result to **PASS**, **FAIL**, or **NOT RUN**; provide only a safe test
reference (run ID, synthetic fixture name, row counts and expected/actual
integer minor-unit totals). `NOT RUN` is never equivalent to PASS.

| Journey | Result | Safe evidence reference |
|---|---|---|
| 1. M-Pesa PDF import and replay | NOT RUN | NOT RUN |
| 2. Historical Equity PDF limited source | NOT RUN | NOT RUN |
| 3. Encrypted backup and restore | NOT RUN | NOT RUN |
| 4. App lock and recovery | NOT RUN | NOT RUN |
| 5. Existing signed APK in-place update | NOT RUN | NOT RUN |

### 1. M-Pesa PDF import and replay

1. Choose a supported invented PDF. Verify selected account/owner, page count,
   row count, opening/closing balance or stated totals, date scope and money-in
   and money-out against the source.
2. Review and save. Confirm only approved cash rows reach Activity, exactly
   once; fees, transfers, reversals, excluded rows, balances and debts retain
   their different meanings. Compare expected and saved totals in minor units.
3. Re-import the same PDF. Confirm **zero extra canonical money rows** and no
   changed financial totals. Import an intentionally truncated, tampered or
   mismatched-account PDF; it must fail safely without partial posting.

**PASS:** complete reconciliation, user review, exact-once insertion and
rejections leave prior money unchanged. **FAIL/STOP:** crash/hang, partial
acceptance, incorrect source/account, duplicate money, missing expected rows,
or unexplained balance/debt difference.

### 2. Historical Equity PDF limited source

1. On a disposable owned bank account, review an invented historical Equity
   profile whose available evidence permits **limited assurance only**.
2. Save and navigate away/back. Confirm the source remains visible in
   Statements with its limited label and that document Money In, Money Out,
   net and calendar-month breakdown agree with its printed totals.
3. Verify it **does not** silently create canonical Activity income, expense,
   owned transfers, debt or fully verified combined cash totals. Reject an
   intentionally unreconciled, wrong-account or incomplete document.

**PASS:** saved visibility and checked per-document totals, without promotion or
double count. **FAIL/STOP:** missing saved source, fabricated totals, an unsafe
verified status, Activity mutation, or partial admission.

### 3. Encrypted backup and restore

1. With **invented** transactions, debts, category rules and both statement
   archive types, record baseline row counts and financial totals.
2. Export an encrypted backup; test wrong passphrase rejection and cancellation.
   Existing test data must remain intact after a failed/cancelled restore.
3. Restore a valid backup into a separate disposable installation. Verify exact
   transaction/account/debt/statement counts and totals, categories, replay and
   user-authoritative corrections. Also restore the known **180,000-iteration**
   legacy compatibility fixture without imposing any new *creation* minimum on
   old restore passphrases.
4. If feasible on the spare installation, test interrupted restore and confirm
   that either the old or complete new state survives, never half-restored money.

**PASS:** correct password restores complete identical state; wrong password,
cancellation and interruption do not destroy pre-existing data. **FAIL/STOP:**
missing/duplicated money, broken old backups, partial archive or silent data
replacement.

### 4. App lock and recovery

1. Test normal PIN lock/unlock, incorrect PIN lockout, background 0/30/60-second
   choices where supported, reboot/boot-time behavior and screenshots/recent-
   apps privacy protection.
2. If the build offers biometrics, test enrolled success, rejection, cancel,
   re-enrollment/invalidation and PIN or phone-credential fallback. Confirm a
   failed biometric action does not authorize backup restore or app unlock.
3. Lock and background the app while sensitive Activity and Statements screens
   are open. Confirm no private records become accessible until authorized.

**PASS:** no bypass, correct timeout/lockout and usable safe fallback.
**FAIL/STOP:** private screen access while locked, bypassed lockout, biometric
callback accepted without required proof, or loss of legitimate recovery.

### 5. Release-signed in-place upgrade

1. Install the previous *compatible release-signed* candidate with invented
   local data. Record app ID, versionCode, signing fingerprint and row totals.
2. Install the new, higher-version release-signed APK as an **Update**, without
   uninstalling or clearing data. Relaunch; verify accounts, transactions,
   settings, statements, lock and encrypted backup remain intact.
3. Attempt a lower-version or wrong-signature artifact only on a disposable
   test install; Android must refuse it rather than silently wipe data.

**PASS:** same signing identity, higher version code, in-place update and
unchanged local data. **FAIL/STOP:** request to uninstall, signing mismatch,
version downgrade, app crash or changed money/settings after update.

## Final release gate

| Gate | Result / link to evidence |
|---|---|
| Exact-SHA JVM/lint/APK/Worker checks | NOT RUN |
| Exact-SHA Android emulator migration, backup, ingestion, replay, debt and aggregate tests | NOT RUN |
| All physical-phone journeys above | NOT RUN |
| Signing continuity and independently stored offline recovery copy | NOT RUN |
| Final R1 release decision | **NOT RUN — BLOCKED** |

Final R1 may be **PASS** only when every required journey and preflight
criterion is PASS on the actual signed release candidate and the independent
emulator/release gates also pass for the same SHA. A known device exception
remains FAIL/NOT RUN until explicitly resolved; passing source tests cannot
be substituted for physical acceptance.

Record safe links to the relevant workflow run, invented fixture results and
redacted screenshots/logs if needed. Keep original financial documents and
credentials exclusively on the test device.
