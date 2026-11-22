# Income Recognition Evidence — Safe Integration Design

**Authority:** Owner-approved seven-page “Income Recognition & Financial Evidence Engine” PDF, 22 November 2026. Option A: Activity Money In means confirmed genuine external income.

## Intent and constraints

Separate actual earnings from credits, cash deposits, owned transfers, loan principal, refunds and unknown origins without requiring salary SMS or manufacturing deposits. Retain local-only evidence, existing history, corrections, duplicate exclusion, current UI, ledger and account-balance mechanisms. Monetary amounts remain checked integer minor units.

**N2 remains HOLD.** The current statement archives must not automatically post to Activity, change assurance, or promote statement-only salary as verified income. CHAT-STATEMENTS owns open stacked reconciliation PRs; do not change their files or bypass their review decisions. Physical-device and instrumentation acceptance remain unverified. No database migration, permission, dependency, online service or new ledger.

## Approaches considered

- **Replace the ledger and auto-post statements:** rejected; violates N2 and risks duplicate records.
- **Change only the Activity label:** rejected; leaves the same false-income logic in Home, Plan and pay-cycle totals.
- **Chosen: one read-only recognition policy over the current canonical ledger, reused by period projections and pay-cycle calculations.** Classify based on existing evidence and user decisions, keep uncertain credits out of confirmed income, and expose a diagnostic breakdown without writing new transactions. This preserves backwards compatibility while future verified statement-admission work remains gated.

## Contracts

1. Confirm user-classified receipts and explicit manual income, or reliable third-party receipt evidence with an identified source; do not equate every positive movement with earnings.
2. Exclude linked own-account transfers, cash deposits without external-purpose evidence, loans, refunds, reversals, investment reallocations and pending/excluded items. Keep unresolved credits distinct from earned income.
3. Reuse the one recognition policy for Activity, Home/Reports through Activity snapshots, and pay-cycle additional income. Do not make account balances depend on recognition.
4. Preserve existing normalized identity, duplicate/mirror, and refund matching; do not broaden N2 statement posting.
5. Regression fixtures cover missing upstream salary, later external salary evidence (using existing canonical rows), transfer cycles, manual corrections, uncertain deposits, loans/refunds, multiple periods, duplicate/excluded candidates, and no salary precondition.

## Future gated work

Statement-driven backfill, missing-leg metadata persistence, FX-linked transfers, broad reconciliation confirmation/undo and automatic old-statement promotion require separate review of N2 gates and coordination with CHAT-STATEMENTS. This change may offer read-only evidence projection but must not imply those are shipped.
