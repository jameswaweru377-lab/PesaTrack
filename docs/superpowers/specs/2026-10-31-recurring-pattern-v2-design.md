# Recurring-pattern v2: domain-only design

## Intent

Detect more recurring commitments from invented or trusted local observations,
without modifying categorization, ledgers, bill schedules, permissions or network
access. A suggestion remains evidence, never an automatically created bill.

## Approved scope

- Weekly, fortnightly, fixed day of month (±2 days), month-end, and termly
  (roughly three times a year) patterns.
- One skipped payment between otherwise regular observations.
- Preserve the existing 35% amount tolerance and HIGH/MEDIUM tiers.
- Anchor monthly next dates to the expected calendar day, not elapsed median
  days. Explain each suggestion in plain English.
- No output for too little evidence, volatile amounts or frequent irregular
  shopping. JVM tests only; no Android dependencies or sensitive fixtures.

## Approaches

1. Replace the existing live detector: gives immediate suggestions, but the
   current bill-setup UI supports only monthly and weekly schedules. It could
   misrepresent fortnightly or termly suggestions as monthly commitments.
2. **Chosen: Add an isolated v2 domain entry point**, while preserving the
   existing live `detect` API until the owner approves corresponding setup UI.
   This delivers a fully tested domain detector without risking financial
   display or scheduling changes. The later activation is a separate task.

## Limitations

Termly means an approximately four-month cadence, not a prediction of actual
school calendars. Dates are interpreted in Kenya local time so a midnight
payment is not shifted to the previous day by UTC conversion. Suggestions
remain unconfirmed until the user reviews them.
