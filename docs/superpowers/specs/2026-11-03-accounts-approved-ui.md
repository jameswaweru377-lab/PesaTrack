# Accounts UI — approved five-state design

Scope: native Jetpack Compose presentation for (1) Accounts overview,
(2) Debts / I owe, (3) Choose accounts, (4) Account details and
(5) Debts / Owed to me. The reference images were supplied privately
and must not be committed, since they show real financial data.

## Visual contract

- Light background, PesaTrack indigo accent, soft outlined white grouped
  account cards, consistent section spacing and colored provider tiles.
- Accounts: concise heading and Add action; Accounts / Debts segmented
  control; balance availability count; Everyday money and Savings groups;
  compact Debt overview. Hide/show amounts without altering source data.
- Debts: I owe / Owed to me segmented control, known-outstanding summary,
  compact lender cards, unknown balances called out separately, settled
  records collapsed.
- Choose accounts: standalone full-screen picker with Back and Done
  actions in the top bar, automatic saving when selecting, search and
  filter chips, selected items shown first; no bottom Save button.
- Account details: recorded balance, evidence quality/date, activity and
  transfers, optional linked savings, and a balance-check shortcut.
- Owed to me: the same Debts structure and compact person rows while
  keeping the current personal-receivables editing and history flows.

## Data and privacy invariants

- No remote asset loading or telemetry. Institution identifiers stay local.
- No invented account balance, fake activity, or inferred debt settlement.
- An unknown debt must not increase or silently disappear from the known
  total; unobserved balances are explicitly labelled.
- Changing an owned institution must continue to update matching Activity
  through OwnedInstitutionActivityRefresher, and failed writes should be
  clearly reported and not presented as saved.
- Account and receivable detail/history navigation must continue working.
- A provider tile uses a locally bundled, source-checked image where available
  (M-Pesa, Equity, Absa, I&M); other brands have local name/color fallbacks.
  Sources and reuse information are in `docs/PROVIDER_LOGO_SOURCES.md`.
  Do not call third-party logo services from the app.
