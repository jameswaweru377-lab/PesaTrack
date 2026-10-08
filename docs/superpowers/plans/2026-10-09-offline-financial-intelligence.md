# Approved offline financial intelligence

The user approved implementation without AI subscriptions or external financial-data processing. Kotlin and SQLite remain authoritative. Python is a development-only synthetic test laboratory; no Python runtime or model enters the APK. Do not merge this work.

## Acceptance criteria

1. Currency typography can be understood across institutions without guessing amounts or economic income. Unknown movements remain unresolved.
2. Generate at least 2,400 deterministic, invented messages from the reviewed sanitized corpus. Kotlin checks authored record kinds, scaled monetary fields and replay determinism. No SMS or ledger import is accepted. These are template variations, not independent bank formats or an ingestion-deduplication proof.
3. Show a read-only comparison of recorded net spending in two completed calendar months. Preserve review, unknown balances and funding-gap warnings ahead of insights. Missing history yields no claim.
4. Reuse existing recurring-pattern, salary-cycle and explicit exact-merchant category preferences. No inferred consent, remote AI, new training model or financial direction changes.
5. Savings forecasts use the last up to six completed months of trusted goal-linked net allocations, including zero and negative months. Require three observed months. Planned contributions must never be labelled actual pace. Use integer arithmetic, bounded horizons and explicit insufficient-history/no-progress states.
6. Existing interpreter, learning, recurring, salary, debt, replay and Android CI tests pass at the final PR head. No schema, Gradle dependency, network client or production ingestion behavior changes beyond currency whitespace normalization. Review limitations explicitly.

## Implementation sequence

- Add Python generator contract tests, observe red, implement generator, verify green. Add Kotlin generated-corpus checks and CI generation before unit tests.
- Add pure `domain/intelligence/OfflineMoneyPolicy.kt` and JVM tests for calendar history, net withdrawals, exact arithmetic, uncertainty, spending thresholds and overflow. Connect goals and home to trusted snapshots without maintenance writes.
- Add normalization regression tests. Observe red in CI before the normalization fix; run final complete Android CI after correction.
- Verify the existing exact-match correction and recurring/salary tests through full CI; avoid parallel changes to debt PR #289.
- Create an isolated draft PR stacked on #290; do not merge. Obtain a fresh code review, fix findings, inspect current-head CI and publish evidence.

## Limits and deferred research

No universal bank parser, machine learning, online AI, automated financial advice, new repayment-interest inference or promise of goal dates. Existing budgeting, debt and pay-cycle engines retain their authority. Goal ranges are historical contribution scenarios, not guarantees; missing transactions may affect them. Synthetic tests cannot establish real-world coverage.
