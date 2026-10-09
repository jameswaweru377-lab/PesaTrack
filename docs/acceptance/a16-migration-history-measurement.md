# A16 migration and synthetic history acceptance

These checks use invented records in the instrumentation application's database. They do not read an SMS provider, notification stream, phone contacts, or an existing personal ledger.

## Populated schema upgrade

`PopulatedLedgerMigrationTest.version33UpgradePreservesRelationshipsCorrectionsAndTrustedTotals` starts with the current schema, reconstructs the exact v33 debt-events table without the v34 provenance column, inserts a linked fixture, sets `user_version=33`, and opens the production helper to perform the real upgrade.

The fixture checks:

- Stable transaction IDs, codes, amounts, fees, dates, planning month, account and manual rule classification.
- An explicit refund pointing to its original expense, including category propagation after upgrade.
- Drawdown/repayment event IDs, transaction and related-event links, funding account and user decision, observed outstanding balance, and unknown legacy net-proceeds provenance.
- Budget category and limit.
- Confirmed merchant identity, alias, observation, learned category source and application IDs.
- Exact trusted totals: spending 8,000 minor units after a 2,000 refund; fees 100; transfers 3,000; net -8,100.

`MigrationFixtures.removeV34DebtProvenanceColumn` uses table creation, copying and renaming, which avoids dependence on newer SQLite `DROP COLUMN` syntax. The existing migration suite closes the helper explicitly because the API 24 framework helper does not implement `AutoCloseable`.

Run this fixture with the current Android test toolchain:

```powershell
gradle :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.kedevjimmy.pesatrack.data.local.PopulatedLedgerMigrationTest,com.kedevjimmy.pesatrack.data.local.PesaTrackDatabaseMigrationTest'
```

## Repeatable history measurement

`SyntheticHistoryMeasurementTest.thirtyAndNinetyDayHistoriesHaveStableTotalsAfterDuplicateReplay` runs 30-day/3,000-row and 90-day/9,000-row fixtures in one instrumentation test and one database lifetime, with 100 payments per day and three empty-ledger runs per fixture. The same database file and helper remain open for all six runs across both periods; only transactions with source kind `SYNTHETIC_HISTORY_MEASUREMENT` and their external references are deleted before each run, outside timing. The harness checks `SELECT COUNT(*) FROM transactions = 0` across the entire ledger before insertion, as well as the empty period snapshot. Each day contains amounts 1,000 through 1,099 minor units and a fee of 5 per payment. Times, codes and merchant identities are deterministic.

The measured phases are:

1. Persist already parsed payments through the production `TransactionStore.insertIfNew` path with its ordinary per-record transaction ownership. The store database is opened before timing; the harness holds no transaction on another helper.
2. Query the full period through production Activity snapshot logic, followed by five repeated query samples.
3. Replay every parsed payment through the same persistence API, checking that no rows are inserted and all trusted totals remain identical.

The 30-day totals are spending 3,148,500 and fees 15,000 minor units; the 90-day totals are spending 9,445,500 and fees 45,000. All records use automatic classifications. The snapshots therefore exercise ordinary trust and duplicate-candidate logic as well as persistence.

Measurements report device API/manufacturer/model, fixture version `invented-history-v3`, production per-record transaction mode, row count, per-phase milliseconds and all repeated samples. Three repetitions remain unchanged; timing budgets do not alter semantic expectations. The recorded first snapshot is the first query following insertion in each run. Database creation, connection reuse, the empty-ledger validation and previous repetitions warm the filesystem and query paths; these measurements represent neither cold process startup nor a cold operating-system page cache. Row generation, database/ledger setup and semantic assertions are outside the persistence timing. The stable helper lifetime avoids unlinking a database still referenced by process-scoped stores. No timing threshold determines test success.

Run on a dedicated test emulator or an explicitly designated test device; instrumentation replaces the app's test database:

```powershell
adb logcat -c
gradle :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.kedevjimmy.pesatrack.data.local.SyntheticHistoryMeasurementTest'
adb logcat -d -v raw 'PesaTrackMeasurement:I' '*:S' > history-measurements.jsonl
```

The test also emits lines prefixed `PESATRACK_HISTORY_MEASUREMENT` through instrumentation stdout. Retain those lines and the instrumentation result XML with the tested commit SHA, emulator image, Gradle/SDK versions and machine details. Compare runs using equivalent emulator/device and host configurations; do not treat differences between hardware types as a product regression.

This measurement covers parsed-message per-record persistence, duplicate replay and Activity queries. SMS-provider lookup, parsing, relationship/mirror reconciliation, pay-cycle rebuilding, backup recovery, rendering, and permission handling need their own acceptance evidence.

## Remaining external acceptance gates

Record these results separately before release:

- Supported API 24 and target API 36 clean install/upgrade and permission refusal/revocation.
- Redmi/Xiaomi physical-device notification replay, process death and background restart behavior.
- Large text, TalkBack reading order, focus traversal, contrast and touch-target checks on real screens.
- Explicitly authorized one-/three-month phone import journeys, including end-to-end elapsed time and cancellation/resumption.
- Backup/restore journeys and interruption recovery on representative devices.

The automated synthetic and migration tests provide no physical-device or manual-accessibility result. Timing values remain unrecorded until CI or the designated test device actually executes them.




