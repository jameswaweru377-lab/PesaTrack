# PesaTrack Current State

_Complete owned-identity transfer-review guard (8 November 2026; CI and device acceptance pending):_
Only owned bank and wallet accounts with complete, normalized, unambiguous
full identifiers may establish review-only transfer pairs or possible-transfer
suggestions. Null, blank, masked, malformed and partial account identifiers
are withheld, even when two sources share a receipt reference; valid distinct
full identifiers continue to work. Statement archives, repeated-import
idempotence, original cash evidence, Home/Activity totals, decisions, schema,
permissions and backups are unchanged. Nothing is auto-posted; N2 remains
on hold. Synthetic unit regression tests and Android CI are required, with
physical Redmi acceptance separate. No emulator or instrumentation runs.

_Selected Plan subpage designs (6 November 2026; Android CI and device acceptance pending):_
The four selected white/emerald reference layouts are now applied to Plan's
Budgets (circular spending/remaining summary, category filter and limit actions),
Savings Goals (featured goal milestones and other saved-goal progress), Bills
(two-week local calendar, outstanding due rows and reminders) and Pay Cycle
(salary-date timeline and ledger/planning snapshot). The visual summaries reuse
their original local stores, editing sheets, rule actions and trusted Activity
financial evidence. Missing full savings-goal targets do not display fictional
overall progress; payday estimates and unavailable historical planning amounts
are explicitly qualified. Existing approved Plan overview and five-tab shell
remain unchanged. No transaction posting, reconciliation, import, schema,
backup, permission, network or signing behavior changes. A focused pure JVM
policy test covers budget percentages, milestones and calendar dates; emulator
and instrumentation testing are intentionally skipped. GitHub Android CI and
physical Redmi visual acceptance are still required.

_Statement source identity safeguard (6 November 2026; CI and phone acceptance pending):_
Read-only statement coverage now requires a digit-bearing, structurally stable
transaction reference before two independently verified document observations
may be called the same receipt. Generic bank narrations such as TRANSFER and
WITHDRAWAL are not proof: both observed cash rows remain available, combined
multi-document subtotals are withheld as Needs Review, and confirmed-repeat
counts remain zero for those cases. A single source still shows its own
verified cash movements. Nothing changes in Activity, archival persistence,
account balances, user corrections, fee/refund logic, schema, backup, permissions
or release signing. N2 source-to-ledger admission remains on hold.

_Plan Calendar Journey design implementation (6 November 2026; Android CI and physical-device acceptance pending):_
The Plan overview now uses the selected calendar-first white/emerald layout:
current-month selector, month/pay-cycle progress toggle, seven-day upcoming-bill
ribbon, confirmed current pay-cycle availability and starting/committed/saved
figures, budget and goal progress, Bills destination, and upcoming due rows.
A connected Plan Details view displays the same source-derived figures, bill
dates and the original full cash-flow breakdown. Unknown progress is labelled
unavailable, and the month selector opens existing historical Reports rather
than pretending a historical Plan snapshot exists. The calendar selects a day
for display only and does not recalculate finance data. Existing forecasting,
recurring-payment confirmation and funding-gap review remain reachable below
the overview. No ledger, account ownership, bill scheduling, storage, imports,
permissions, backups, network flows or signing changes. Android CI and Redmi
font-scale/layout and navigation checks are pending; emulator and instrumentation
tests are intentionally skipped.

_Stale duplicate-confirmation write guard (4 November 2026; CI/device acceptance pending):_
Activity's possible-duplicate review now sends the exact displayed transaction snapshots to a dedicated SQLite decision writer. Same Transaction fails closed when either persisted row changed, stopped being a candidate, gained a protected review state, or acquired a linked reversal as a separate transaction. Time-sorted card rows are compared by ID rather than positional order. Keep Both remains non-excluding; historical reversed-ID decisions are recognized by canonical pair IDs. Duplicate resolution and any exclusion share one SQLite transaction; the source rows, fees, account balances and financial summary formulas are unchanged. No schema or backup change is introduced. Only synthetic JVM policy and build checks are requested; device persistence and Android instrumentation have not been executed.


_Activity Design 3 implementation (5 November 2026; CI and Redmi acceptance pending):_
The Activity tab now uses the approved black/white/emerald visual language:
search and advanced filters first, a soft-green month selector and four
evidence-backed tiles (Income, Expenses, Transfers, Invested), a separate
review queue callout, boxy category filters, and flat dated transaction rows
with right-aligned daily recorded net. The redundant large Activity heading
and purple page accents are removed. Daily net uses complete trusted cash rows,
not the visible or searched subset; owned transfers are neutral. Confirmed
investment purchases have their own read-only card, subtracted from the
Activity card's displayed consumption expenses without changing canonical
ledger spending, source classifications or the Statements/Home projections.
Unrepresentable daily or investment figures are withheld, not saturated.
Separately journalled debt fees are not attributed to an individual day.
No schema, permissions, import, reconciliation, saved user decisions,
backup, navigation destinations, or release signing changed. Android CI
and physical Redmi visual acceptance are required; instrumentation is skipped.

_Home month context refinement (4 November 2026; CI/device acceptance pending):_
The redundant "Overview" heading and top-level month chip are removed from
Home. A compact accessible month selector now sits beside "Monthly activity"
within the existing white money card, above monthly Money in and Expenses.
The current confirmed pay-cycle available amount remains current even when a
historical month is selected. Net recorded flow names its selected month;
current account balances and upcoming items are not presented as historical.
Financial calculations, local ledger, stored observations, schema, backup,
permissions, signing, and five-tab navigation are unchanged.

_Home reference spacing and local provider marks (4 November 2026; device acceptance pending):_
The approved November Home receives a smaller tappable month chip, more compact
money card, tighter vertical rhythm, and existing locally bundled colored M-Pesa
and Equity provider marks. No account balances or financial classifications are
recomputed by this UI adjustment; unknown balances remain unavailable. All five
navigation destinations retain the previously merged monochrome bottom bar.
Android CI is required; emulator/instrumentation runs are not requested.

_Monochrome Home and shared brand refresh (4 November 2026; device acceptance pending):_
The Home tab now follows the approved quiet white/black/emerald reference: a
calendar-month Overview, one available-money card with recorded monthly income
and expenses, and flat review, owned-account, upcoming, and recent-activity rows.
All amounts and navigation remain backed by the existing trusted local stores;
unknown account balances are marked unavailable and net recorded flow is not
misrepresented as balance growth. The PesaTrack P icon and Pesa/green-Track
wordmark replace purple branding; the shared five-tab bottom bar is black with
an emerald active state. No posting, account reconciliation, schema, SMS import,
backup, security or release-signing logic changed. Android CI and Redmi visual
acceptance are still required; emulator and instrumentation tests were skipped.

_Reports & Insights Design 2 implementation (4 November 2026; CI and device acceptance pending):_
The approved chart-first light Reports screen is being implemented with a four-way
Week/Month/6-month/Year selector, locally selected date, trusted-ledger spending
trend, category progress, previous-period comparison, and drill-down review of
largest eligible and uncategorized expense entries. Paycheck Analysis retains its
existing separate route; annual statement reconciliation evidence remains in
Year. The green wallet illustration is removed from Reports only. A new
PesaTrack-branded offline PDF export uses the same in-memory Activity summary,
category projection and trend observations as the screen; it distinguishes
fees and owned transfers and warns that separately imported source records are
not silently posted. The PDF is user-saved, not encrypted or a bank statement.
No changed ledger classifications, database schema, permissions, network flow,
backup/restore, statement assurance or release signing are intended. JVM CI,
Android lint/build and Redmi visual/export acceptance are still required.

_Possible-duplicate reversal review guard (4 November 2026; CI pending):_
Weak duplicate-review suggestions now exclude any pair containing a REVERSAL,
even when two independently sourced observations share an amount and timestamp.
Ordinary code-less review candidates remain available. This changes only