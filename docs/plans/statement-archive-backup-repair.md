# Statement Activity backup completeness — repair proposal

Status: draft, safety-gated; 17 October 2026.

## Defect and scope

StatementActivityStore writes to a second SQLite database
(`pesatrack_statement_activity.db`). The existing encrypted format v1 saves
only canonical `PesaTrackDatabase` tables and backed-up preferences, so a
fresh-device restore can silently omit saved PDF statement rows and category
decisions. The source documents are not stored and may be unrecoverable.

This change is restricted to encrypted backup/export, preview and restore;
the separate statement journal remains independent of SMS Activity, debts,
balances, and financial reconciliation. It does not change PDF extraction,
document ownership or source-row classification.

## Proposed implementation

1. Preserve both SQLite files; use a single SQLite connection with the
   statement database `ATTACH`ed for the backup snapshot and restore.
2. Serialize four statement tables with explicit schema v3 into encrypted
   JSON backup payload v2: batches, original source rows, per-row category
   corrections, and user-remembered category rules.
3. Validate archive table names, full column sets and typed cells before
   any replacement. Restore parents before children; delete children first.
4. Apply the primary ledger replacement, statement replacement and existing
   restore-commit marker in one SQLite rollback-journal transaction.
   Reject an unsafe WAL/memory journal configuration rather than promising
   crash-atomic cross-database restore.
5. Preserve v1 decryption and restore compatibility. A v1 payload excludes
   statements. On an installation with statement history, reject v1 restore
   instead of silently losing or orphaning that history; allow it on a fresh
   empty archive with an explicit warning in the preview.
6. Keep backup limits and passphrase cryptography unchanged. A backup which
   exceeds the existing 64 MiB limit must fail rather than omit the archive.

## Acceptance criteria

- Fresh encrypted v2 backup includes every statement batch, original row
  (including zero movements and source cells), manually chosen category and
  remembered category rule, even if the main SMS ledger has no transactions.
- Preview explicitly counts statement source rows and flags legacy v1
  archives as incomplete.
- Wrong passphrase, missing archive table, malformed row, FK violations and
  failed insertion do not partially replace current records.
- Restoring the same archive repeatedly yields the exact same rows and
  categories, with no added Activity transaction or budget/debt event.
- Fresh-device v1 restore still works when the target has no saved statement
  history; an existing statement history is never silently cleared by v1.
- An interrupted successful restore recovers settings through the preexisting
  durable restore journal. Both SQL files must be crash-consistent.
- Android instrumentation verifies both database files (including a forced
  mid-restore rollback) on a supported API and device; real Redmi backup
  export/preview/restore is required before merge.

## Evidence/limits

The synthetic Android integration tests are checked in with the patch.
They are not equivalent to physical-device verification, and are not called
passed until CI or local Android instrumentation actually runs. No real
customer statements, transaction codes or account identifiers belong in
test fixtures.

## Deferred non-scope work

Refactor independent StatementActivityStore helper instances, harden app-lock
timing/KDF, worker privacy filtering, privacy-policy wording and Chaquopy
process isolation in independent changes. Do not merge these into the backup
repair without separate review.
