# Shared accounting view — non-posting integration

## Goal

Give future Home, Activity, Statements and Reports screens one reusable, truthful read model of the current Activity accounting baseline, statement gross cash coverage, and reviewed cross-source transfer evidence.

## Existing facts

Activity remains the canonical ledger. Statement cash coverage is an independent account-movement scope and may be withheld. Confirmed cross-source matches are reversible metadata, not authorization to modify either ledger. N2 still holds app-wide posting and totals reconciliation pending stronger evidence.

## Approaches

1. **Add verified statement cash to Activity totals:** rejected; duplicates and owned transfers would inflate income.
2. **Subtract confirmed transfer amounts from Activity totals:** rejected; an Activity transfer could already have been classified, and archived statement legs are not necessarily posted there.
3. **Chosen:** one immutable projection containing separate labelled baselines, current confirmed transfer preview and missing-evidence status. Screens can consume the same model without making financial writes.

## Contract

- `SharedAccountingViewPolicy.project(year, totals, cashLegs)` returns a single read-only `View`.
- Activity economic income/spending are unchanged; statement gross cash is displayed only when verified and supported.
- Confirmed internal-transfer amounts are labelled preview-only. No synthetic net or corrected income/spending is calculated.
- Missing Activity, withheld statements or ambiguous evidence yields `REVIEW_REQUIRED`, never a fabricated zero.
- `postingAllowed` and `comparableTotals` remain false while N2 is held.
- The existing matching review snapshot owns the read and publishes the shared value. No new SQL read or write, schema, backup, security, permission or workflow change.

## Acceptance

Tests for distinct scopes, withheld amounts, missing Activity, reversible metadata/undo and unsupported year. Normal Android CI, privacy scan, and explicit instrumentation skip. Future accounting posting requires a separate decision and gates.
