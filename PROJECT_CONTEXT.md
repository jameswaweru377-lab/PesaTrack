# PesaTrack Project Context

Updated: 2026-09-26

## Purpose

PesaTrack is a personal Android finance companion designed around Kenyan M-Pesa usage. It is intended for private sideloading, not Play Store distribution.

The application should automatically turn M-Pesa activity into a reliable local money ledger, learn deterministic recipient/category rules, track monthly budgets and commitments, show savings/goal progress, and surface useful context such as budget pace and safe-to-spend amounts.

## Product direction already agreed

The app should not behave like a judgmental expense tracker. Spending that follows the user's plan is not inherently bad. The core experience should distinguish current money, committed money, savings/reserves, genuinely free money, and safe-to-spend pace.

Signature concepts planned:

1. Available to Live / genuinely free money.
2. Safe today amount based on remaining cycle and protected commitments.
3. Smart recipient rules learned from explicit user corrections.
4. M-Pesa-native transaction types: Send Money, Buy Goods, PayBill, airtime, cash withdrawal, reversal, fees and transfers.
5. Goals with progress and impact simulation.
6. Monthly/annual insights that explain what changed, rather than dumping charts.
7. Local-first privacy and optional biometric protection.

## Technical choices

- Native Android.
- Kotlin + Jetpack Compose.
- Package: `com.kedevjimmy.pesatrack`.
- Minimum SDK: 24.
- Target SDK: 36.
- Compile SDK: 36.
- Compose BOM: 2026.04.01 (Compose 1.11 generation, chosen for stable API 36 compatibility).
- Material 3 stable.
- Room and WorkManager are planned for the next phase and will be added when their persistence/background code is introduced.
- Source of truth must remain deterministic local data, not AI.
- Any future AI should explain already-derived facts, never invent transaction truth.

## Design system

Light mode uses warm ivory, clean white surfaces, deep navy text, emerald primary, soft mint containers and restrained amber attention states.

Dark mode uses a near-black blue/green background, lifted dark teal surfaces, mint primary, warm amber secondary and off-white text.

Typography currently uses Android's bundled sans-serif family with a strong weight/size hierarchy so the app stays offline and does not depend on downloadable fonts.

## Navigation

Primary tabs:

- Home
- Activity
- Plan
- Goals

Insights are intentionally embedded where they matter instead of becoming a fifth analytics dump.

## Reference UI implementation

The full visual alignment pass based on the approved PesaTrack concept screens is merged into `main` and verified by Android CI.

Reference-aligned scope:

- refined light/dark palette and typography hierarchy
- custom PesaTrack shield/track app icon and in-app brand mark
- reusable landscape hero treatment and premium card language
- redesigned onboarding
- redesigned Home
- redesigned Activity
- redesigned Plan
- redesigned Goals
- Transaction Details inner page
- Goal Details inner page
- Activity -> Transaction Details navigation
- Goals -> Goal Details navigation

All financial values in these screens remain preview data until the local data layer is connected. The reference UI passed Android CI on the PR branch and again on merged `main` (run #11).

## Redmi responsiveness and appearance correction

A second visual pass was implemented from real Redmi Note 14 screenshots after the first reference-UI build exposed narrow-screen and dark-mode problems.

Implemented on this pass:

- phone-width metric areas switch from four squeezed columns to 2x2 layouts where appropriate
- Home summary cards use vertical content hierarchy so values and status pills do not collide
- Activity transaction rows prioritize recipient + amount and move category/time into a secondary line
- Plan budget rows use full-width progress bars instead of three competing horizontal columns
- Transaction Details uses a 2x2 metadata grid on phone widths and removes the decorative sun behind the amount
- Goal Details stacks its actions on narrow phones
- semantic green/amber/blue/violet/red containers now have dedicated dark-mode surfaces and foregrounds
- hero landscape artwork automatically follows the active theme
- added Profile screen
- added Settings screen
- added persistent System / Light / Dark theme preference stored locally on-device
- Profile/Settings remain inner pages and do not pretend to be one of the four primary money tabs

The correction was validated with Android unit tests and debug APK assembly on CI before merge.

## M-Pesa ingestion foundation

The first real data-ingestion slice is implemented using Android Notification Listener access rather than requiring restricted SMS permissions.

Implemented:

- `MpesaNotificationListenerService` receives newly posted notifications after the user explicitly grants Notification Access.
- Notification bodies are filtered locally; unrelated notifications are ignored.
- M-Pesa confirmation text is parsed into structured fields using the deterministic `MpesaParser`.
- Person-to-person counterparty parsing prioritizes the recipient/sender name and strips trailing full or masked phone tokens.
- Parsed transactions are stored in an app-private SQLite ledger.
- Raw M-Pesa notification/SMS bodies are **not persisted**.
- Transactions are deduplicated by transaction code when available and a deterministic SHA-256 fingerprint as fallback.
- Stored fields include transaction code, type, amount, fee, counterparty name, optional balance, observed time and ingestion source.
- If a confirmation includes a new M-Pesa balance, PesaTrack stores it as a **last known observed balance** with timestamp. It is not treated as continuously live.
- Settings now includes an M-Pesa Connection screen with Notification Access controls and local diagnostics.
- The Android app still declares no Internet permission; cleartext traffic is explicitly disabled.
- Unit tests cover parser name cleanup, M-Pesa notification detection and transaction fingerprint stability.

Why Notification Access first:

- Modern Android treats `READ_SMS` and `RECEIVE_SMS` as restricted permissions whose availability depends on installer/platform policy.
- Notification Listener access is explicitly granted by the user in Android Settings and is suitable for testing real-time M-Pesa confirmation capture on the Redmi Note 14.
- Historical SMS import remains a separate follow-up experiment after the real-time path is proven on-device.

The first code-complete ingestion build passed Android CI run #17.

## Smart money rules and real ledger phase

The interrupted smart-money phase was recovered from branch `feature/smart-money-rules-ledger`, completed, and validated before merge.

Implemented behavior:

- Persistent Smart Rules are stored locally and survive app restarts.
- First-run flow is now Onboarding -> Money Setup -> Home. Even skipping onboarding still passes through Money Setup; the user can explicitly defer rule creation from there.
- Money Setup supports three rule types:
  - salary/income source
  - recurring expense
  - own-account transfer
- Salary rules are not hardcoded to any bank or employer. The user defines notification sender/title text and/or a message phrase.
- Salary rules can assign income to the same planning month or the following planning month. This supports salary received at the end of one month being used to fund the next month's plan.
- Configured non-M-Pesa money notifications can be parsed for an amount after a saved rule matches.
- M-Pesa rules can match normalized counterparty names.
- One-off/unrecognized transactions are intentionally labeled **Needs review** rather than guessed.
- Activity is backed by the real local transaction ledger rather than preview/demo transactions.
- Activity provides real captured totals for spending, income, fees and net cash flow.
- Activity filters include All, Expenses, Income, Transfers and Needs review.
- Tapping Activity opens the exact stored transaction by database ID.
- Transaction Details is backed by the stored record and supports:
  - one-time manual classification
  - optional "use this for future payments too" smart-rule creation
  - marking an item as an own-account transfer
  - excluding an item from budget calculations
- Income direction is preserved during manual review unless explicitly changed to a transfer; descriptive categorization cannot accidentally turn captured incoming money into spending.
- Savings and own-account movements are treated as transfers rather than consumption spending.
- Transaction records now persist direction, category, review state, matched rule ID, planning month and optional referenced reversal code.
- Reversal-aware summaries omit a known reversed original transaction when the reversal identifies the original transaction code.
- Withdrawals default to transfer-to-cash rather than immediate spending.
- Existing unreviewed transactions can be re-evaluated when a new counterparty rule is saved.
- Planning month calculations remain compatible with the app's Android API 24 minimum.

Tests added/expanded:

- counterparty smart-rule matching
- amount caps on person rules
- configured salary notification sender + message matching
- disabled notification rules
- same-month vs next-month salary planning
- generic configured bank-money notification parsing

The final implementation state passed Android CI run #23 before the documentation update.

## Historical SMS backfill and canonical dates

PesaTrack now separates the date when money actually moved from the time the app learned about the transaction.

Implemented:

- Database schema v3 adds `occurred_at_epoch_ms`.
- Existing ledger rows migrate safely by using their previous received timestamp as the initial occurrence timestamp.
- New M-Pesa notifications resolve the transaction date embedded in the confirmation when the format is recognized.
- If the embedded date cannot be parsed, PesaTrack safely falls back to the source notification/SMS timestamp.
- Activity monthly calculations now use `occurred_at_epoch_ms`, not import/observation time.
- Activity and Transaction Details display the true transaction date.
- Historical SMS Import is available from Settings with:
  - This month
  - Last 30 days
  - Last 90 days
- Historical import queries the phone SMS provider only after explicit `READ_SMS` permission is granted.
- Imported M-Pesa messages flow through the same deterministic parser, Smart Rules, Needs Review, reversal and duplicate handling as live notifications.
- Configured bank/salary SMS messages can also be imported when they match a saved notification smart rule.
- Imported messages keep their original SMS/transaction date and do not become transactions dated on the day of import.
- Re-importing the same history is safe because transaction-code/fingerprint deduplication remains active.
- Raw SMS bodies are processed in memory and are not stored in the local ledger.
- If Android or the installer refuses the hard-restricted SMS permission, the importer reports that limitation and does not attempt to bypass Android security; real-time Notification Access remains usable.
- Salary planning wording is now `Month it arrives` versus `Following month` to distinguish transaction date from budget-cycle assignment.

Date semantics:

1. `occurred_at_epoch_ms` = when the financial transaction actually happened.
2. `received_at_epoch_ms` = when the phone/source received or posted the message.
3. `created_at_epoch_ms` = when PesaTrack stored/imported the structured record.
4. `planning_month` = which budget cycle the transaction, especially income, is assigned to.

Home and Plan must use these semantics and must never group historical imports by import time.

The implementation passed Android CI run #26 before the documentation update.

## Salary-driven pay cycles, My Accounts and simplified setup

PesaTrack now uses salary events to define budgeting cycles instead of assuming a fixed calendar payday.

Implemented:

- Added persistent My Accounts with default M-Pesa and Cash accounts.
- Users can add their own bank, savings, wallet or other accounts; no bank/employer is hardcoded.
- Smart Rule creation is preset-first to reduce complexity:
  - Monthly salary
  - Rent
  - Internet
  - Electricity
  - Food / lunch
  - Transport
  - Family support
  - Savings
  - My own transfer
  - Other recurring
  - Custom
- Presets automatically choose the normal category/direction behavior. The user only provides the recognition text, optional custom label, account(s), and an optional planned amount.
- Rules can recognize either an M-Pesa counterparty name or a configured notification sender/message phrase.
- Salary rules start pay cycles. A fixed payday is not required.
- Salary rules can optionally define a usual pay window, including a cross-month window such as 24th -> 1st. The window is a confidence check, not the salary identifier.
- The first recognized salary starts a pay cycle.
- The next accepted salary closes the prior cycle and starts the next one, regardless of whether it arrives on the 24th, 31st, 1st, or another normal date.
- Tracked remainder from the closed cycle becomes carryover into the next cycle.
- Salary-like credits less than 14 days after the current cycle start are held for confirmation instead of silently opening another cycle.
- Salary-like credits outside an optional usual pay window are also held for confirmation.
- A pending salary can be confirmed as:
  - Start new cycle
  - Extra income in the current cycle
- This supports unusual December pay dates without hardcoding December behavior.
- Pay-cycle timing policy is unit-tested for 24th/31st/1st ranges, early December salary, no-fixed-window salary, and duplicate-like credits.
- Transactions now store their primary account, optional linked account, pay-cycle ID and salary-cycle review state.
- Existing M-Pesa transactions migrate to the M-Pesa account; withdrawals link M-Pesa -> Cash.
- Cash withdrawal remains a transfer, not spending.
- Own-account movements remain transfers and do not increase income or spending totals.
- Historical M-Pesa transactions are assigned to M-Pesa even though their ingestion source is historical SMS.
- Existing income rules migrate into salary/pay-cycle rules and stop relying on the old calendar-month offset.
- Historical imports rebuild pay cycles once at the end of the batch.
- Recurring fixed commitments are protected immediately when a pay cycle starts.
- Flexible rules such as food/transport only become protected commitments when the user explicitly enters a planned amount.
- Planned savings can also reserve money until the savings transfer happens.
- Home is now backed by the real pay-cycle ledger:
  - Available to Live
  - salary + carryover starting funds
  - commitments
  - deliberate savings
  - spending + fees
  - last-known M-Pesa balance with observation time
  - Needs Review count
  - salary confirmation card
- Plan is now backed by the same real pay-cycle ledger and shows protected commitments, paid progress and pay-cycle accounting.
- Safe Today is shown only after PesaTrack has at least one completed cycle from which to estimate cycle length; otherwise it says Learning.
- Calendar-month Activity remains separate from pay-cycle planning.
- The old Historical Import wording is now more explicit:
  - Settings -> Import M-Pesa SMS history -> Request SMS access

The first full implementation build reached green unit tests and debug APK assembly on Android CI run #32 before the final wording/context update.

## Current implementation

Baseline: `main` after the verified reference UI merge

Bootstrap scope:

- Android/Gradle structure.
- Compose Material 3 shell.
- light/dark theme foundations.
- reference-aligned onboarding/privacy screen.
- reference-aligned Home screen.
- reference-aligned Activity screen.
- reference-aligned Plan screen.
- reference-aligned Goals screen.
- Transaction Details screen.
- Goal Details screen.
- custom PesaTrack app icon/brand mark.
- Activity and Goals inner-page navigation.
- bottom navigation.
- deterministic `MpesaParser`.
- `SmartRuleEngine`.
- parser/rule unit tests.
- Android CI.
- debug APK artifact upload on successful CI.

Activity, Transaction Details, Home and Plan are connected to the persistent local ledger. Goals still contain preview values and must only be connected to deliberate goal allocations/savings transfers; unspent money must never be treated as automatically saved.

## Build rule

Do not call an implementation successful merely because code was written.

For every implementation phase:

1. Make a narrow, understandable change.
2. Run the relevant build/test.
3. If it fails, inspect the exact failure.
4. Fix the actual cause before continuing.
5. Only mark the implementation complete when the relevant check passes.

## Immediate next phase after pay-cycle merge

1. Install the latest debug APK on the Redmi Note 14.
2. Go to Settings -> Import M-Pesa SMS history -> Request SMS access and test This month first.
3. If Android grants SMS access, verify older M-Pesa messages appear in Activity on their original transaction dates; then optionally test 30/90 days.
4. If SMS access is refused by HyperOS/installer policy, keep real-time Notification Access and add a statement/history-file fallback instead of bypassing Android security.
5. Add the user's real bank account under My Accounts, then create a Salary preset using the actual bank notification sender/message pattern.
6. Validate that a recognized salary starts a pay cycle, recurring commitments are protected immediately, and the next accepted salary closes the prior cycle with carryover.
7. Test an unusual/early salary-like credit and confirm both Start new cycle and Extra income paths.
8. Validate Equity/bank -> M-Pesa own-account transfer rules so movement between owned accounts never becomes duplicate income/spending.
9. Add month/pay-cycle history selection to Activity and Plan after the live cycle is proven.
10. Connect Goals only to deliberate savings/goal allocations and then add biometric/PIN protection before broader historical use.

## Important constraints

- Raw SMS content should not leave the phone.
- Never silently classify uncertain transactions as expenses; uncertain items go to Needs Review.
- Transfers between the user's own stores of money are not spending.
- Cash withdrawal is a transfer to Cash until cash is explicitly spent.
- Reversals must neutralize the original transaction rather than become fake income.
- Fees should be tracked separately.
- Do not fabricate account balances or financial insights.
- UI must remain readable on the Redmi Note 14 and adapt cleanly to larger Android form factors.
