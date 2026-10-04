# Home loading and ingestion recovery — 2026-10-04

Baseline: main@922298bec6868a549fd5211da1ea550407f51da5.
Symptom: Home stays on "Preparing your money picture" and messages stop progressing.

## Cause found in code

Home starts PayCycleStore.rebuild on Dispatchers.IO. That method acquires its
store monitor before beginning a SQLite transaction. The ingestion coordinator
begins its canonical SQLite transaction before entering TransactionStore, which
can call PayCycleStore.rebuild. Concurrent work can therefore wait in a cycle:
Home owns the cycle monitor and waits for SQLite; ingestion owns SQLite and waits
for the cycle monitor. Other independent store monitors expose the same inversion.

This is a verified code-level deadlock path. The supplied screenshot does not
include thread stacks, so it does not prove the installed device hit that exact path.

## Recovery

- Canonical stores and ingestion acquire one shared reentrant monitor before
  accessing SQLite. Nested calls reuse it; existing SQLite transactions retain
  rollback/commit semantics. Do not add independent synchronized store monitors.
- Never hold this monitor during network requests or coroutine suspension.
- Home opening no longer forces a cycle rebuild.
- Home coalesces refresh requests, preserves coroutine cancellation, reports a
  startup exception with Try again, and reloads after updates received during startup.
- No schema change, data deletion, parser rewrite, or reinstall is required.

## Verification

Android regressions cover concurrent ingestion, Home reads and cycle rebuilding;
automatic salary receipts; idempotent manual salary; empty Home startup; and
startup failure followed by successful retry. The normal CI pipeline runs unit
tests, lint, APK assembly, and emulator instrumentation before release.

Physical-device acceptance remains required: install the updated APK over the
existing app, reopen Home, confirm Activity messages and balances, and check salary.
Do not clear app storage to apply this fix. Do not claim all parsing/accounting
issues are solved by this concurrency repair.
