# Reports & Insights — approved Design 2 implementation

Owner: `CHAT-VISUAL-POLISH` · 4 November 2026 · Tracks #399 and #331.

## Selected visual

The owner approved Design 2 (chart-first PesaTrack mockup). Preserve the existing
light/violet theme, 5-tab shell and independent Paycheck Analysis route. Remove
the wallet image from the Reports hero and promote trend, period comparison,
category bars and transaction review without rebuilding the finance ledger.

## User journey

1. Open More → Reports & insights. Choose Week, Month, 6 months or Year and
   optionally a calendar anchor. The reporting window and prior comparison are
   local-calendar ranges, start inclusive/end exclusive.
2. See trusted Activity spending, separately tracked fees, comparison (or
   unavailable baseline), scoped spending trend and existing category totals.
3. Open Largest expenses or Uncategorized; tap an eligible source entry to use
   its existing transaction details/category controls. Review lists are
   presentation only and never modify money or review decisions.
4. Open Paycheck Analysis on its existing route. Annual accounting evidence
   remains visible for the Year scope only.
5. Export report → acknowledge privacy notice → Android chooses save location
   → a PesaTrack-branded PDF is rendered offline. It contains no raw SMS,
   separately imported bank statement cash or invented balances.

## Accounting and privacy boundaries

- Reuse `TransactionStore.activitySnapshotBetween` and the established
  `BudgetSpendingProjection.calculate` adjustments, not invented arithmetic.
- Income, spending, transfers and fees retain their existing distinct labels;
  category allocations may have different coverage than headline spending.
- Source rows with allocations, refunds, non-expense direction or nonpositive
  amount are excluded from the *largest simple expense* review list.
- The PDF is a personal summary (not a bank-issued statement). It is
  user-directed and unencrypted after export; show that warning in-app.
- No schema, SMS access, external processing, workflow or signing changes.

## Verification

Focused JVM tests for calendar scope, comparison, expense review eligibility,
and PDF content. Existing Android CI unit/lint/assemble checks are the
non-emulator gate; font scaling and real Redmi PDF readability require a
separate on-device acceptance walkthrough. Do not claim device PASS from CI.
