# Strong transaction-reference evidence in statement source coverage

Date: 2026-11-06
Owner: CURRENT FINANCIAL-INTEGRITY CONVERSATION — issue #601

## Problem
The read-only `StatementSourceCoveragePolicy` currently accepts any 8–48 character alphanumeric field as an exact receipt identity. Some bank PDFs populate `reference` with generic all-letter narration such as `TRANSFER` or `WITHDRAWAL`. Two documents with matching time and amount can therefore be folded into one verified repeat without a unique transaction identifier, suppressing an observed cash leg and reporting an unsafe multi-document subtotal.

## Approved behavior
- For repeat proof, require a normalized 8–48 character alphanumeric reference containing at least one digit, consistent with the existing cross-source match and evidence policy. Treat all-letter reference labels as non-identifying, not as proof that rows are the same movement.
- In a multi-source account/currency/month, generic-label cash observations must stay separate, contribute zero *confirmed repeat copies*, mark the month `NEEDS_REVIEW`, and withhold authoritative combined cash subtotals (`null`).
- A single verified source PDF still shows its observed cash total as `SINGLE_SOURCE`; strong digit-bearing identifiers across separate documents retain currently verified repeat behavior.
- Preserve original source observations, account/month boundaries, user decisions, Activity ledger/totals, balances, fees/refunds, and source assurance. This is a read-only calculation, not a new ingestion or reconciliation posting rule.

## Safety gates
1. New synthetic JVM regression must fail against the current policy and pass after the one-condition policy change.
2. Existing repeat, fee, account separation, single-document, uncertainty, and source-order tests continue passing.
3. Exact-head Android CI JVM tests, lint, debug APK build and security scan must pass before merge.
4. No instrumentation, emulator or phone execution requested. User performs eventual Redmi acceptance; N2 financial-posting admission remains on hold.

## Risks
Changing the cash-coverage status from `VERIFIED_REPEAT` to `NEEDS_REVIEW` for generic labels is intentional. Affected combined source-only subtotals become unavailable until sufficient identity evidence is present. No saved rows are removed or modified.