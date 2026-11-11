# Home loading performance — measurement protocol

Baseline source: `main@24d4d238076da7f3e14ad0134119b6dbf5be5afc` (11 November 2026). The candidate adds timing checkpoints; **it does not yet claim a speedup**.

## What is measured

`PesaTrackPerf` logs only constant stage names and elapsed milliseconds. No transaction texts, balances, account numbers, payees or other personal financial data are emitted by these checkpoints.

| Metric | Meaning | Threshold for a log |
|---|---|---:|
| `Home core ready in Nms` | Data reads until Home's primary money picture is published | >400 ms |
| `Home refresh took Nms` | Core plus secondary insight/health refresh | >400 ms |
| `home.accountBalances` | Confirmed account balance snapshot read | >=120 ms |
| `home.activityAndBudgetEvidence` | Canonical activity and budget projection | >=120 ms |
| `home.payCycleSnapshot`, `home.ledgerSummary` | Separate authoritative reads | >=120 ms |
| `home.budgets`, `home.debts`, `home.accountOwnership`, `home.salaryRules`, `home.upcomingBills` | Additional core reads | >=120 ms |
| `home.supplementalInsight`, `home.supplementalReviews`, `home.supplementalHealth` | Lower-priority secondary information | >=120 ms |

No line for a stage means it was under the logging threshold, not that it did not run. These are read durations, not total Android first-frame or visual-render times.

## Safe Redmi check

1. Install a CI-verified, compatible update only after confirming signing identity/versionCode and safeguarding local app data. Never uninstall for performance testing.
2. On your Windows PC with authorised ADB and the phone unlocked, run `adb logcat -s PesaTrackPerf:I '*:S'` in a terminal; leave it running locally. This prints performance timing lines without fetching transactions.
3. Open Home from a cold app start five times and record each `Home core ready` and `Home refresh` timing. Repeat for five Home↔Activity returns and five longer-background returns. Do not confuse a cached Home return with a cold read.
4. Record the median of each set and identify the repeated slowest stage. A before/after improvement requires **the same phone, ledger, app build type and test sequence**. Redact unrelated logs if sharing.

## Decision rule

- Prioritise a read only if it is reproducibly slow and materially holds first content; then add targeted regression tests before optimisation.
- Preserve balance authority, refund/transfer semantics, history refresh and review decisions.
- Do not refactor the canonical ledger or caches based solely on source appearance.
- Await unit tests, lint, APK assembly and real-phone measurement before asserting improvement or merging the instrumentation branch.

## Current verification

`bash scripts/check_home_perf_stages.sh`: source-level checks passed. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain`: attempted; Gradle tasks did not start because the wrapper could not download Gradle 9.5.0 (`Network is unreachable`). No device timing samples yet.
