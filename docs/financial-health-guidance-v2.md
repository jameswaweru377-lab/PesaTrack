# Financial Health Guidance V2 — design, research and safety contract

Status: implementation candidate, October 2026. No financial ledger schema/ingestion changes.

## Why this change

The previous four-signal score penalized a rise in spending even when income
rose, scored debt against total principal rather than actual required payments,
and scored transaction fees independently of affordability. A lack of reliable
savings or debt evidence could produce an overly precise number.

The updated Home card retains the approved visual language, uses two-column
signal tiles for phone widths, shows a separate confidence signal, and opens a
scrollable guidance dialog with specific on-device navigation actions.

## Independent research

- Financial Health Network, financial health framework (Spend, Save, Borrow,
  Plan/Protect): https://finhealthnetwork.org/about/what-is-financial-health/
- CFPB Financial Well-Being Scale: https://www.consumerfinance.gov/consumer-tools/financial-well-being/
- CFPB emergency reserve guidance: https://www.consumerfinance.gov/an-essential-guide-to-building-an-emergency-fund/
- CFPB definition of debt-to-income ratio (required MONTHLY debt payments
  relative to monthly income, not total principal):
  https://www.consumerfinance.gov/ask-cfpb/what-is-a-debt-to-income-ratio-en-1791/

**Not affiliated with, scored by, or equivalent to** either external
framework. No licensed FinHealth calculation is used. PesaTrack's optional
0–100 numeric rating is a transparent, nonvalidated product heuristic.
It must never be called a credit score, medical/psychological diagnostic,
investment recommendation or solvency forecast. Users outside the United
States must not be benchmarked against US consumer survey norms.

## Input trust and calculations

1. **Cash flow (35% of optional overall):** trusted observed money out relative
   to observed income for the chosen calendar month. Never infer unrecorded
   salary as zero; never double-count self-transfers, loans, reversals or
   refunds. Unpaid commitments remain clearly disclosed as not yet deducted.
2. **Savings resilience (25%):** *user-entered and recently reconfirmed*
   accessible emergency reserves divided by user-entered essential monthly
   costs. No claim of bank validation. Savings movements, invested balances,
   bank account balances, protected allocations and goals are not substitutes
   for an emergency reserve.
3. **Debt affordability (25%):** user-entered required monthly debt payments
   versus observed monthly income, only where known active debt balances
   are not flagged unknown. The user's observed total outstanding principal
   and last repayment amount are not repayment schedules. Positive active
   debts with zero "required repayment" do not count as proof of affordability.
   A user confirming no due repayments when PesaTrack finds no active debt
   is allowed, but explicitly caveated.
4. **Spending pace (15%):** reviewed, categorized spending against a positive
   monthly budget, normalized to elapsed calendar days. Suppressed before
   a quarter of the month has passed, on historical month views and while
   review/coverage requirements are not met. This is a pace signal, not a bill
   prediction or a guarantee.
5. **Data confidence:** shown separately, explicitly based on owned accounts,
   confirmed salary cycle, at least five trusted rows, pending review count,
   and Other/Uncategorized share (at most 20%). Previous-month trusted
   coverage is also needed for an optional overall score.

Overall score is **suppressed** unless the current month and each of the four
real-money pillars has enough supporting data, and previous-month records are
adequate. Missing evidence is not scored as healthy zero. A full score is
neither promised nor required to see helpful observations.

## User-controlled context

Three optional inputs: accessible emergency reserve, essential monthly
living costs, required monthly debt payments. All amounts are KES, stored in
integer minor units in the app-private `pesatrack_preferences` file, which is
already included in encrypted backup. An explicit zero is distinct from
blank/unknown. Invalid values are rejected before a single preference commit.
Context expires from *assessments* after 30 days or clock rollback; the typed
values remain editable and must be explicitly reconfirmed. User-entered
figures never create payments, loan events, budgets, or account balances.

The core financial policy is pure Kotlin with synthetic unit tests. Optional
Android SharedPreferences persistence has instrumentation tests. No internet
connection, API key, permission, background job, tracking, or telemetry added.

## UX routing and accessibility

Home retains the Financial health card and opens a scrollable "Your financial
guidance" dialog. Four two-across signal tiles are followed by a distinct
Data confidence explanation and a recommended next action. The dialog offers
native links to Activity, Plan, Debts, Accounts and Salary, and an Update
amounts editor. Selected past months never project a future spending pace or
claim a new current-month health rating.

## Verification before release

- Kotlin policy regression tests, Android build, lint.
- Android instrumentation for user-entered preference persistence, validation,
  expiry and nontransaction behavior.
- Phone review on Redmi Note 14: dark/light, small screen, keyboard/insets,
  navigation, dialog scroll, 30-day expiry and stable Home refresh.
- Confirm full/partial/unknown examples; no double-counted transfers,
  loans, refunds or wrongly inferred emergency reserves.
- CI passing does not prove physical-device UX or actual financial accuracy.
- Keep all example amounts synthetic and do not send personal financial
  inputs to remote services.
