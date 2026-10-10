# Personal receivables – isolated implementation plan (10 Oct 2026)

Status: approved product direction; feature branch only. User updated verification policy on 10 October 2026: skip instrumentation unless explicitly requested. Do not merge without green build/accounting checks and the remaining on-device acceptance.

## Scope
- Keep institutional liabilities (Fuliza, M-Shwari, banks) unchanged.
- Add a distinct **Owed to me** view in Accounts > Debts: person cards, balances, event history, add loan and repayment actions.
- Manual loans and repayments are user-authored annotations (not invented cash movements).
- Link a verified outgoing or incoming transaction to a person as a loan or repayment; support partial amounts for shared bills.
- Display a suggestion only when a previously confirmed borrower can be identified safely; never auto-create or auto-settle a personal loan.
- Original wallet/account movements remain intact; loan principal must not be income or spending.
- Explicit unlink/delete action must undo its effect, without erasing original imported transactions.

## Acceptance criteria (design before code)
1. Outgoing KES 5,000 linked entirely as lent money yields cash outflow 5,000, spending 0 and receivable +5,000.
2. Outgoing restaurant KES 1,600 with KES 800 linked as loan yields cash outflow 1,600, spending 800 and receivable +800.
3. Incoming KES 2,000 linked as repayment yields cash inflow 2,000, income 0 and receivable -2,000.
4. Unrelated gifts, same-name contacts, fees, refunds, transfers and unknown events must never be guessed as loans.
5. Sum of all linked loan shares for a transaction cannot exceed its principal. Multiple detections or retry cannot create duplicates.
6. Partial repayment cannot over-settle an identified person. More than one debt for the same person may be aggregated only as explicitly approved.
7. Partial/manual transactions remain labeled as manual and never change bank/M-Pesa balances.
8. Reversal/exclusion and stale links must not silently increase loan balances; negative/deleted historical corrections must be testable and reversible.
9. Existing Fuliza/M-Shwari debt balances, categories, salary, budgets, refund provenance and duplicate detection must retain their meaning.
10. Schema upgrades preserve existing loans and allocations. Rollback and replay do not double-apply money.
11. Existing dark/light themes, touch targets and offline-first privacy remain. No cloud/telemetry permissions, background notification permissions or Python runtime.

## Domain model
Use separate person-linked receivable and event tables, not new providers in DebtStore.
Kotlin/SQLite are authoritative; Python is synthetic, development-only verification.
Linked entries reference canonical transactions. Manual entries are non-cash receivable adjustments with explicit provenance.
An accounting projection must derive spending/income portions rather than mutating the original SMS amount.

## Release gate
Default CI: Gradle unit tests, lint, debug APK assembly and synthetic financial contract checks. Successful main builds publish the rolling APK without waiting for instrumentation. Android migration/UI tests remain available through manual workflow dispatch with run_instrumentation enabled, only when the user explicitly requests them; they are not an automatic PR or publication gate. Synthetic Redmi acceptance remains untested and separate from build evidence. Skipped/cancelled instrumentation must never be reported as passing.