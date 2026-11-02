# S2 — Local SQLite encryption decision (2 November 2026)

**Status: threat model documented; encryption NOT implemented. Severity: P2 (plan).**
Tracking: [#585](https://github.com/jameswaweru377-lab/PesaTrack/issues/585).
This is a design and release-gate record, not permission to migrate existing users.

## Verified behavior on `main` at `7b2bf5e8333da69e52e0eb1b526e86d797f44e1a`

- `app/src/main/java/com/kedevjimmy/pesatrack/data/local/PesaTrackDatabase.kt:8-12,1776-1777` uses Android `SQLiteOpenHelper` for `pesatrack.db`, schema 40. No SQLCipher key is supplied.
- `app/src/main/java/com/kedevjimmy/pesatrack/data/local/StatementArchiveDb.kt:9` independently uses Android `SQLiteOpenHelper` for `pesatrack_statement_activity.db` (its own schema). Statement descriptions, references and account evidence are stored there, not only in the main ledger.
- `app/src/main/java/com/kedevjimmy/pesatrack/security/AppLockManager.kt` verifies a PIN/hash in private preferences. This gates the app interface; **it does not encrypt either SQLite database**. PIN/biometric UI state must not become the only way to recover financial history.
- `app/src/main/AndroidManifest.xml:20-23` disables Android app backup; `app/src/main/res/xml/data_extraction_rules.xml` excludes databases from cloud backup and device transfer. Both databases are app-private and benefit from Android's device/file-based encryption and sandbox protections where enabled. Neither protection makes the **SQLite file contents themselves** SQLCipher-encrypted.
- `app/src/main/java/com/kedevjimmy/pesatrack/security/StatementBackupArchive.kt:110-153` attaches the statement archive to the ledger connection during backup/restore, checks the archive schema, and requires compatible rollback journal modes for cross-file crash-atomic restore. `PesaBackupManager.kt:469-508,576-683` depends on this contract. Database sidecars (`-wal`, `-shm`, rollback journals) and temporary migration copies also require protection.
- `docs/PRIVACY_POLICY.md:76` and `README.md:54` already state the app-lock limitation accurately. Do not describe existing databases as encrypted or silently change those claims.

### Threat and realistic impact

| Situation | Present protection and residual exposure |
| --- | --- |
| Lost phone, powered off / securely locked | Android sandbox and device encryption help. Residual risk depends on Android version, security patch, lock-screen strength and device state; no claim of absolute protection. |
| Unlocked phone, compromised OS/root access, invasive forensic extraction, or exploit with access to the app's private directory | The two SQLite files and their sidecars can expose transactions, names, accounts and statement references without knowing the PesaTrack app-lock PIN. **This is S2's confirmed gap.** |
| Another ordinary sandboxed Android app | Normally cannot read PesaTrack's private databases directly; S2 is not proof that all installed apps can access them. |
| User-created `.ptb` backup | Separately passphrase-encrypted using the existing backup format; app-database encryption must not change its restore compatibility or confuse it with ordinary exported files. |

**Judgment:** P2 while device sandboxing, disabled automatic backup and an accurate policy remain in place; reassess if a concrete local data-extraction attack or a distribution requirement changes the threat model. Encryption adds defense in depth, but cannot protect plaintext already displayed or held in memory on a compromised/unlocked device.

## Options and trade-offs — no selection implemented

| Option | APK and licensing | Existing-user migration, key recovery and functionality |
| --- | --- | --- |
| **A. Keep app-private Android SQLite, disclose the limit (recommended pending gates)** | **0 bytes** additional APK; Android platform SQLite license/packaging unchanged. | No schema/key conversion, backup change, app-lock recovery hazard or new native dependency. Leaves the residual file-extraction risk documented above. |
| **B. Modern SQLCipher Community for BOTH database files + randomly generated database key wrapped by Android Keystore** | Native Android AAR increases the arm64 release APK; **rough order-of-magnitude estimate: several MB**, not measured for this project/version. Measure exact before/after signed APK, compressed native libraries, 16-KB-page device install and ABI compatibility. Community has a BSD-style license requiring prominent attribution/license text; paid Commercial/Enterprise packages differ. Pin an exact, supported modern `sqlcipher-android` release; do **not** use the discontinued legacy `android-database-sqlcipher` library. | Large/high-risk conversion from two plaintext files. Requires new encrypted target copies, integrity tests, crash recovery and atomic cutover. Wrapped keys are tied to device Keystore state; lost/invalidated keys could make the local database unreadable. Exported `.ptb` recovery must remain independent, documented and validated. SQLCipher migration and attach semantics must be proved, not assumed. |
| **C. Android Keystore-wrapped key without SQLCipher** | Very small estimated APK difference if only platform Keystore APIs are used; no additional SQLCipher license. | **Not database encryption.** Wrapping a key does nothing to ordinary `SQLiteOpenHelper` bytes. Encrypting selected columns instead would require substantial query/index/search/reconciliation changes, still leave metadata and journals, and risks money correctness. Not recommended as an S2 substitute. |

Technical sources (official): [SQLCipher Community/license obligations](https://www.zetetic.net/sqlcipher/license/), [SQLCipher Android Community AAR](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-community/), [16-KB pages and legacy library EOL](https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/), [SQLCipher export and migration API](https://www.zetetic.net/sqlcipher/sqlcipher-api/), [Android Keystore non-exportable keys](https://developer.android.com/privacy-and-security/keystore), [Android backup guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices).

The previous signed rolling APK was **51,416,511 bytes** (S3 release evidence, 2 November); this is a reference baseline, **not** an S2 size experiment and not a measurement of SQLCipher overhead. No SQLCipher dependency was downloaded, built, or licensed by this decision record.

## If the owner later chooses B: mandatory separate PR and safety sequence

1. **Approve scope and recoverability first:** Which attack matters? Is an on-device Keystore-tied database acceptable when a device is reset, restored to another phone, or the Keystore key becomes invalid? Should user-created `.ptb` backup be a prerequisite with an explicitly verified recovery path? Do not bind the database solely to a remembered app-lock PIN or silently make existing backups unrecoverable.
2. **Tests first on an emulator CI lane that actually runs:** create fixtures for both schema-40 ledger and current statement schema, a fresh install, populated existing-user upgrade, pre-encryption database backup, and supported legacy encrypted `.ptb` versions/180,000-iteration vectors. Assert record counts, integer money totals, identity/reference integrity, no double-counting, app-lock off/on, old backup preview and full restore.
3. **Stage rather than mutate live files in place:** quiesce both database owners; include SQLite journal/WAL/checkpoint behavior; export to **new** encrypted copies using a reviewed SQLCipher mechanism; compare `integrity_check`, foreign keys, schema versions, table/row counts and money aggregates; re-open both encrypted files using a recovered wrapped key. Handle SQLCipher's attached-database and cross-file restore atomicity explicitly. A matching ledger alone is insufficient.
4. **Crash and failure matrix:** inject low storage, wrong/missing/invalidated key, interrupted export, process death before/after each staged step, power loss during cutover, partial archive migration, incompatible native library, failed re-open, and restore after failure. Preserve original files/recovery journal until both replacements are verified; never partly switch one file and then erase the other. A prior APK cannot be used as the rollback if it only understands plaintext files.
5. **Release gates:** full migration/backup tests executed in GitHub CI on emulator (not merely compiled), Android 24 and current target, arm64 16-KB page compatibility, measured release APK delta, minified signed build, actual existing-data in-place update and `.ptb` round-trip on a named physical device, accurate privacy/licensing docs, recovery steps, and an owner-approved rollout/rollback. No release tag until these pass.

**Separate decision required:** S2 must remain **not fixed** after this documentation PR. The implementation must receive its own approval, Issue, branch and small reviewable PRs. Do not overlap it with T1 persistence CI, T2/A1 ledger refactors, backup schema changes, R3 native packaging, or R2 ABI changes.
