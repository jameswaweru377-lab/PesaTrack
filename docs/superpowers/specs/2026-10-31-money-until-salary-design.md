# Money until next salary — safe forecast design

Date: 2026-10-31. Owner-approved Task 3, approaches 1 + 2.

## Boundary

- A small Plan card forecasts the **money left just before the next salary**. It never counts an anticipated salary as cash available today or changes the canonical ledger, Safe today, category, balance, salary receipt, or existing pay cycle.
- A locally confirmed **next payday date and expected amount** takes priority. If either is missing, the existing configured salary pay window may identify a **clearly estimated** payday period. An estimate never becomes a confirmed salary receipt.
- Only a fresh and complete spendable starting balance is eligible. Explicitly user-confirmed total spendable balance is a distinct optional input, expires within 24 hours and is invalidated when new ledger evidence arrives. Otherwise all enabled spendable account balances must be direct recent observations. Savings/protected accounts are excluded.
- Forecast variable expenses and fees from **three entire, completed salary cycles**. Do not extrapolate from an incomplete month or use zero silently. Use the lowest and highest observed daily spending rates as the bounds. Exclude known user-confirmed fixed commitments from those historical variable amounts to avoid counting the same bills twice.
- Deduct explicit remaining commitments and due scheduled bills, once per rule. If an outstanding loan/Fuliza debt has no trustworthy due amount/date, refuse a numeric forecast and say what needs confirmation. Never infer a payment deadline from the latest repayment alone.
- For an estimated window, low uses the later payday and higher daily spending; high uses the earlier payday and lower daily spending. Negative remaining money is shown, not clamped to zero.
- The card explains confirmed/estimated date, excluded future salary, balance source, included bills/variable spending and missing evidence. Four primary UI states: loading, unavailable with reasons/action, available (confirmed or estimated), and recoverable error. Partial evidence is shown without a fabricated range.
- No new permissions, online use, telemetry, schema migrations, or raw data uploads. Preferences hold only user-confirmed forecast inputs; financial ledger stays authoritative.

## Audit findings to keep separate

1. `PayCycleStore.snapshot().safeTodayMinor` derives from calculated cycle remainder and historic cycle length, **not a verified live sum of owned account balances**. Do not recycle it as the forecast's cash starting point.
2. `SalaryExpectationPolicy.nextWindow` provides a window derived from last confirmed pay and configured days; no persisted confirmed next payday currently exists. Add clearly separate forecast preference, never create a salary transaction.
3. Existing due-date handling is monthly/weekly; a suggestion-only fortnightly/termly pattern is not a confirmed bill. Do not invent those schedules.
4. `DebtStore` tracks outstanding debt and repayments but does not establish a reliable next instalment due date or amount. Exclude/flag unscheduled active debts until user supplies credible commitment details.

## Verification

Pure policy fixtures: missing salary, unknown/stale balance, fewer than three complete cycles, confirmed payday preference, cross-month 24th–1st window, late payday, variable spending bounds, commitments and duplicates, active unscheduled debt/Fuliza, negative projected money and overflow. Android unit/build/lint CI; physical Redmi acceptance remains pending.
