# Home Design B: financial snapshot contract

This screen is a *PesaTrack activity-derived indicator*, not the CFPB
Financial Well-Being Scale, Financial Health Network FinHealth Score,
credit score, live account balance or insolvency prediction.

Background:
- CFPB Financial Well-Being Scale: https://www.consumerfinance.gov/consumer-tools/educator-tools/financial-well-being-resources/measure-and-score/
- Financial Health Network's Spend/Save/Borrow/Plan framework: https://finhealthnetwork.org/about/what-is-financial-health/

We do not copy either validated instrument. Our four visual indicators use
available local ledger signals only: budget, aligned-period spending, known
debt principal relative to recorded income, and recognized fees. The simple
weights, rating cutoffs, and points are PesaTrack-specific and NOT externally
validated. Their values must not be labeled "high confidence".

## Data sufficiency

The combined 0–100 score and "At risk" classification are hidden unless ALL:
- A confirmed salary cycle and a confirmed owned account exist.
- Both periods contain at least five trusted transactions.
- No pending reviews remain.
- Other/Uncategorized represents no more than 20% of tracked outflow.
- A positive budget limit, reviewed budget spending and known prior-period
  comparable spending are present.
- Recorded income is positive, and known debt principal is complete.
- Current spending and fees are safely representable.

If these are not satisfied, show "Limited data" with missing indicator values.
An actual "At risk" label also requires at least two corroborating indicator
scores below 40 and an average below 45. Otherwise show Watch closely or On track.

Thresholds are product heuristics for transparent budget attention, NOT
statistical forecasts. Debt principal versus one month of income does not
measure actual repayment affordability.

## Financial correctness

- Never count an own-account transfer as earnings/spending.
- Do not count loan drawdowns as earnings, or principal repayments as ordinary
  expense. Preserve existing trusted ledger semantics.
- Investment event deltas do not imply authoritative holdings or market values.
  Investment tile displays "Value unavailable" until brokerage valuation sources
  are explicitly supported.
- Historical account balances are not a live banking API.
- For an incomplete month, compare only with the elapsed matching days of the
  preceding month. Prior missing data => no percentage.
- Unknown or excluded financial events must not be inferred as zero income.
- All computation is local; no telemetry, server sync or personal SMS sharing.

## Acceptance cases

- Empty account, no salary, partial import, missing prior month: Limited data.
- Too many Other/Uncategorized rows: Limited data, review instructions.
- Pending duplicates or manual relation reviews: Limited data.
- True known multi-indicator pressure with complete coverage: At risk.
- Stable trusted financial picture: On track.
- Missing investment market prices: Value unavailable, never guessed.
- Activity or salary changes update the Home dashboard on ledger revision.
- Previous-month selection shows recorded transactions in that period; no
  current-month totals should leak into historical cards.
