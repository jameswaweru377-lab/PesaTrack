# PesaTrack — Phase 1 Encrypted Backup & Restore Redesign

Date: 30 October 2026
Status: **Approved for implementation by James, 30 October 2026**
Scope: on-device manual `.ptb` backups and restore only.

## Purpose

Make local backup and restore understandable, private and difficult to misuse.
The existing bottom sheet asks for one passphrase before either choosing a
destination or selecting a backup file. Reorganize it into separate task flows
without changing the financial authority of the local SQLite ledger.

## Existing foundation

- `BackupRestoreSheet` is called from Profile and Settings and uses a Material 3
  bottom sheet. `SecurityScreens.kt` mixes creation, file picking, preview,
  replacement confirmation, errors and results in one composable.
- `PesaBackupManager` exports encrypted portable `.ptb` files, validates them
  with the passphrase before restore, rejects files over 64 MiB and reports
  table/row counts. The app-lock PIN is deliberately **not** in the backup.
- Existing cryptography uses AES-256-GCM with PBKDF2 (180,000 iterations;
  legacy files support a minimum eight-character passphrase). Phase 1 must
  preserve reading existing `.ptb` versions and never mistake UI polish for
  a cryptographic strength upgrade.
- `BackupRestoreJournal` provides an interrupted-restore recovery mechanism,
  **not** a one-click undo of a successful restore.
- Android's system file picker supports a local folder and any installed
  document provider (potentially including Google Drive). This is a manually
  selected file, **not** automatic cloud syncing.

## Goals and acceptance criteria

1. Preserve the approved light, indigo/purple PesaTrack appearance. Use the
   existing 18dp screen padding, shared rounding and responsive accessible
   touch targets.
2. Keep the entry point as a short bottom sheet, but present **two distinct
   actions** instead of two password fields:
   - Create backup
   - Restore backup
3. Each action opens a separate, clearly titled full-screen guided flow. The
   screen can return to the hub without modifying data.
4. Creation collects a backup passphrase and confirmation, checks at least
   **12 characters** for **new** backups, requests a destination using the
   Android save picker, creates the encrypted `.ptb` locally, and reopens the
   saved file to verify it can be decrypted and validated. On success show a
   concise summary and explain where the user chose to save it. Never retain
   or transmit the passphrase.
5. Restore starts with **Choose backup file** and the Android open picker.
   After choosing, ask for *that file's* passphrase (old supported files may
   use eight characters). Only a successfully decrypted, structurally
   validated backup can advance to preview.
6. Preview shows creation date, supported schema, backed-up ledger-table rows,
   saved statement rows, statement-history inclusion, and a clear replacement
   warning. Do not display sensitive monetary totals in the preview.
7. After the user requests replacement, step-up authenticate against the phone
   (biometric when available, system device credential fallback, or configured
   PesaTrack app PIN if neither phone method is available). Authentication
   is **authorization**, never the encryption key. If a device has none of
   these, require an extra explicit warning/confirmation and rely on the
   portable backup passphrase; do not falsely claim biometric protection.
8. Restore again uses the same validated manager and recovery journal. Show
   progress while blocking duplicate taps, a successful result, or an honest
   failure state; cancel/authentication failure must never trigger restore.
9. Clear sensitive text when leaving either flow; avoid putting passwords or
   financial records in logs, navigation arguments, analytics or backups.
10. Back navigation, file-picker cancellation, rotation and error retries must
    preserve safety. No ambiguous 'merge' action: **restore replaces** data.
11. No new network endpoint, Drive OAuth scope, Android permission, signing
    change, database schema change, or automatic background upload.

## Selected approach and tradeoffs

| Alternative | Tradeoff | Decision |
| --- | --- | --- |
| Keep one bottom sheet and show/hide fields | Fewest code changes, still cramped and easy to confuse create with restore | Not chosen |
| **Short action hub + dedicated guided screens** | Clear separation and preview, modest additional Compose state | **Chosen as Phase 1 direction** |
| Build Drive sign-in and automatic backup first | More permissions, consent, key and retention complexity before local restore is trusted | Later phase |

## Visual structure

### A — Entry bottom sheet (approximately half the screen)

**Encrypted backups**
`Your money stays private. Choose what you want to do.`

1. **Create backup** · Save an encrypted `.ptb` file · chevron
2. **Restore backup** · Choose an existing `.ptb` file · chevron

`Files are encrypted before being saved. Your app-lock PIN is not included.`

No shared passphrase fields, no automatic Google connection, no duplicate
PesaTrack logo and no stacked confirmation alert on this hub.

### B — Create backup (guided full screen)

**Create encrypted backup** · Back

1. **Protect**: backup passphrase, confirm passphrase, show/hide toggles and
   plain-language 'Keep it safe—without it the file cannot be opened.'
2. **Save**: tap **Choose where to save**; system save picker suggests
   `PesaTrack-YYYYMMDD-HHmm.ptb`. Device storage and installed document
   providers are equally supported.
3. **Verify & finish**: progress, reread/decrypt saved file, show summary
   and **Done**. Explain explicitly if save succeeded but verification failed.

### C — Restore backup (guided full screen)

**Restore encrypted backup** · Back

1. **Select**: large **Choose .ptb backup** button → system open picker.
2. **Unlock file**: chosen file label, backup passphrase, **Preview backup**.
3. **Review**: date, schema, ledger rows, statement rows, older-backup caution
   when applicable, prominent warning 'This will **replace** your current
   backed-up data and preferences. It will not merge transactions.'
4. **Authorize**: **Restore and replace** → biometric/device credential or
   configured app PIN; failed/cancelled auth returns to review intact.
5. **Finish**: manager restores under its existing journal and app reloads;
   display clear success or actionable failure (including recovery mode).

## Boundaries and implementation structure

- Keep both call sites (`ProfileScreen`, `SettingsScreen`) intact; expose the
  same public `BackupRestoreSheet(onDismiss)` interface.
- Extract the flow UI out of `SecurityScreens.kt` into a focused Compose file
  such as `BackupRestoreFlow.kt`, with a small pure policy/state model where
  independent JVM tests can validate transitions and password rules.
- Keep encryption, payload schema, local ledger writes and recovery journal
  inside existing manager classes; do not reimplement crypto in the UI.
- Use Android Activity Result document picker; never request broad storage
  access. Use built-in platform authentication with API24-safe behavior.
- No 'share backup' button that could accidentally upload private files.

## Testing and release gate

- JVM: new backup minimum length vs legacy restore length; cancellation and
  invalid state transitions; authorization cancellation; duplicate submission.
- Existing backup fixtures: v1/v2 and current schema, damaged file, wrong
  password, oversized file, missing statement history and invalid schema.
- Android UI/manual (only when separately requested): create/save/verify,
  cross-device `.ptb` import, file-picker cancellation, authenticating, wrong
  PIN, replacing a populated ledger, then checking counts and preferences.
- Local JVM tests, lint and debug APK assembly must pass in CI. A green CI run
  is **not** proof that restore is safe on the user's Redmi; keep actual-device
  restore acceptance visibly pending until performed.

## Intentionally deferred

- Automatic Google Drive OAuth / `appDataFolder` upload, schedule, history,
  retry and deletion controls (later opt-in phase).
- New versioned stronger key derivation (audit S3), streaming large backup
  processing (audit D2) and a user-recoverable encrypted **pre-restore safety
  checkpoint**. These require separate security review, backward-compatibility
  tests and user approval; current recovery journal is not an undo feature.
- Password recovery: because the backup is portable and encrypted, neither
  fingerprint nor Google can recover a forgotten backup passphrase.
