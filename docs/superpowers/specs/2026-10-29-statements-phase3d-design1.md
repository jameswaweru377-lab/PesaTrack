# Phase 3D Design 1 - All-Accounts Statements Landing

**Approved by James:** 29 October 2026, first screen of the Phase 3D proposal.
**Reference:** PesaTrack Phase 3D design sheet, leftmost "Statements" screen.
**Scope:** Only Design 1, plus minimum navigation and source-state integration. Account detail and coverage/review redesigns remain independent follow-up approvals.

## Intent

Help the user see which historical M-Pesa and Equity statement months are represented, which months may overlap, and how to open source-specific evidence. Daily Activity remains the live SMS/user ledger; Statements remain independently imported historical evidence. Phase 3D does not merge these domains or replace Insights.

## Presentation contract

- Preserve the approved PesaTrack light theme, purple gradient, rounded cards, dark typography, semantic colored icons and existing Home/Activity/Plan screens.
- Keep the application navigation exactly as it is; do not create an extra Statements bottom bar.
- Use the brand header, `Statements` title, year selector, `All accounts` account menu, purple `N accounts on file` hero, two metrics for months on file and needs review, Jan-Dec non-monetary coverage matrix, account rows and existing import action.
- Only account rows with admitted, confirmed owned source evidence appear. M-Pesa and Equity are expected sources; future banks enter through existing verified importer/admission architecture, not decorative fake cards.
- The number `X / 12 months` means any imported evidence in X distinct months of a year, not guaranteed full-month completeness.
- Coverage cells are colored on-file (purple M-Pesa, blue bank), unresolved (amber), no imported rows (grey). Descriptions must be accessible to TalkBack. Tapping a cell takes the user to that account's current details.
- Account labels must not disclose long unmasked numeric identifiers. Do not use real account data or transactions in test screenshots.
- `Needs review` may open the affected existing account screen; it must not pretend to automatically reconcile an unreferenced Equity row.
- Empty state shows a single import call-to-action, not fake charts or finance amounts. Errors and read limits show error/retry, not zeros.
- Do **not** display a combined money-in, money-out, spending or income total. The two sides of an own transfer can appear in two source archives.

## Data contract

- `StatementEvidenceReadModel.read(yearStart, nextYearStart)` supplies only confirmed owned account evidence on `Dispatchers.IO`.
- `StatementSourceCoveragePolicy.MonthCoverage` is the sole source of overlap status and verified copies. No new fuzzy matching or persistence.
- Pure `StatementUnifiedOverviewPolicy.project(year, coverage)` emits account-month states and non-monetary summary counts. Missing months are UI gaps, not financial zero records. Multiple currencies in the same account/month collapse to one status, with `NEEDS_REVIEW` taking precedence.
- Year queries are Nairobi-calendar bounded and cancellable. Cache by selected year and explicit data-refresh revision to avoid repeated imports/parse while moving through pages.
- Existing M-Pesa/Equity account screens, source-local categories, Activity↔M-Pesa strict category bridge, and statement import sheet are reused. No migration, permissions, network calls, or new parser.

## Acceptance

1. More > Statements enters the all-accounts landing by default.
2. One source or two sources both render; account/year controls navigate to existing detail pages.
3. Duplicate same-leg PDF copies do not increase months represented; ambiguous account-months show review.
4. Missing months display no import, never KES 0. Cross-account income/spending totals do not exist in the projection or UI.
5. Importing then returning refreshes source coverage; back returns to Statements, not out of the whole feature.
6. Focused synthetic policy tests, Android test compilation, lint, unit tests and debug assembly pass in CI; physical-device UI verification remains separate.

## Future Design 2/3

The Equity account-detail redesign and Coverage & Review redesign are **not approved by this Design 1 decision**. Keep current detail pages functional and postpone their new layouts and additional review resolution actions.
