# Accounting evidence across Home, Activity and Statements

## Decision

The owner approved read-only annual accounting evidence links from Home and
Activity without replacing their month-scoped totals, plus Statements hero labels
Money In / Expenses / Transfers and a detail for gross Money Out.

## Safety limits

The shared matching model only has independently scoped Activity totals, gross
verified Statement movements and reviewed transfer groups. It does **not** have
complete verified statement expense classification across all bank accounts.
An Expense figure derived from Money Out minus Transfers would wrongly include
loans, savings, investments and ambiguous outflows. Until that other work proves
coverage, this card uses an unavailable placeholder rather than a wrong number.

The transfer value is read-only and is withheld for mismatched or unresolved
evidence. The source data remains on-device. No new reconciliation decisions,
posting, network, schema, dashboard calculation or instrumentation.

## Presentation

Home and Activity show a compact notice for the selected month's **year** only
when review evidence is present. It is explicitly described as annual, and links
to existing review-related destinations. Statements preserves its purple hero,
three metric labels and an expandable gross Money Out detail. No fabricated
expense or transfer values.
