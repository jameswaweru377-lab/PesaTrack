# PesaTrack automation recovery

Updated: 2026-10-04
Baseline: `main@3b924c9da9cc51fd03c3ada8903d615c6674ed21`.

## Product decision

The ordinary experience is automatic: grant data access, choose history, see money.
The user should not configure parsers, approve every transaction, classify every
recipient, or enter a phone number before recognized accounts can be tracked.
Optional edits personalize the app; they are not prerequisites for correct totals.

Account ownership must come from resolved financial evidence. A provider name,
frequent recipient, equal amount, or nearby timestamp alone does not prove that
an account belongs to the user or that two observations describe one transaction.
An explicit "not mine" decision takes precedence over automatic inference.

## What the baseline already fixed

PR #230 accepts resolved payments even when their spending category is unknown,
stops generic recurring-pattern guesses from reserving money, and displays a
planning shortfall separately from available cash. Do not reimplement these fixes
or describe them as proof that the user's installed APK has been repaired.

## Changes in this recovery

- Resolve an explicitly owned asset account without requiring a manually entered
  identifier. This applies to bank/wallet/savings/cash account types, rather than
  adding per-bank business-logic branches.
- Keep recognized but ownership-unknown entities pending. Keep liabilities out of
  asset account creation. Preserve explicit external decisions and existing account links.
- Attach previously unassigned balance observations when replay resolves ownership,
  even if the message was already imported. Do not duplicate the balance observation.
- Remove optional counterparty labeling from Home/Activity financial attention
  counts. The explicit identity-management page can still offer personalization.
- Present setup as three tasks: history, discovery, live updates. Manual salary
  entry and unresolved account customization are optional expandable controls.
  A detected salary suggestion remains visible so its meaning is not hidden.
- Extract the discovery page from the large onboarding screen before changing it.
- Schedule local history repair once per installed update when setup is complete,
  SMS permission is already granted, and there is previous import/captured SMS history.
  Repair never prompts for permission, clears data, uploads SMS, or scans an older
  range than the previously selected/captured range. Work is bounded to 100 days.
  Existing installs without a stored import range use captured historical-SMS dates;
  notification-only installs do not authorize a new inbox scan.
- A null SMS-provider query is an error, not a successful empty repair. Failed work
  is retried; it is never stamped successful before import and reconciliation finish.

## Transfer reconciliation follow-up

PR #232 preserves an own-account path when complementary bank/wallet observations
share a reference, instead of overwriting the first account with the latest receipt.
The pair becomes one transfer, fees remain counted once, and replay preserves the
structural result. Activity describes the named source and destination.

A bank name alone no longer proves that an incoming payment is a self-transfer.
For example, a person can send money from the same bank the user uses. Existing
bank-name-only decisions can be repaired to wallet income during replay, including
correcting the primary account and clearing the old linked path. Explicit one-off
user decisions remain protected even if a conflicting learned rule matches later.

## Research informing the direction

Sources reviewed on 2026-10-04:

- [axio Google Play listing and user reviews](https://play.google.com/store/apps/details?id=com.daamitt.walnut.app&hl=en): visible reviews report missed bank messages and duplicate spending that require manual correction. These are individual reports, not a representative survey. The design implication is to prioritize capture, matching, and trustworthy totals over feature count.
- [FinArt Android FAQ](https://finart.app/faq.html): describes showing account balances from the latest available SMS. The implication for PesaTrack is to show observed balance and timestamp, and distinguish it from a budget calculation.
- [FinArt automatic tracking description](https://finart.app/sms-expense-tracker/): describes automatically organizing transaction alerts. These are vendor claims, not independent accuracy evidence.
- [GOV.UK details guidance](https://design-system.service.gov.uk/components/details/): supports hiding information only some users need while keeping essential information visible. The salary suggestion remains visible; manual configuration is optional.

## Next implementation priorities

1. **Prove the money picture on real device data.** Record parser-coverage and
   financial-attention rates separately. Check current M-Pesa/bank balance, current
   debts, refunds, and transfer totals against messages. A green synthetic suite
   cannot prove the user's KES -95,000 symptom has disappeared.
2. **Expand cross-account matching.** Use shared references and complementary
   account paths; require a unique match. Infer a self number only from corroborated
   own-account movement, never merely the most frequent destination. Cover separate
   transfers of equal amounts, delayed SMS, third-party recipients, fees, and both
   arrival orders. Preserve user corrections on replay.
3. **Improve coverage from actual sanitized examples.** Prioritize the dominant
   unsupported transactional shapes for Equity/Branch/M-Shwari and other lenders as
   actually observed; do not invent provider-specific fixtures to claim coverage.
   Offers, eligibility, OTPs and bundle expiry information are not posted spending.
   A real bundle purchase deduction is financial activity and must be counted once.
4. **Finish ordinary navigation.** Home summarizes balances, spending, commitments
   and active debts; Accounts explains each named account and balance freshness;
   Activity opens the same underlying transaction. Technical parser evidence belongs
   in Data Health. Account identifiers should be displayed clearly without inventing
   missing numbers or owner names.
5. **Visual/device review.** Keep the existing palette and financial cards while
   making hierarchy and wording clearer. Verify Redmi Note 14, large font, dark/light
   themes, first launch, denied permissions, empty history and offline states.

## Acceptance cases

Added JVM tests protect the ownership boundary, optional identity attention, and
automatic-repair history bounds. Android tests exercise number-free M-Shwari
transfer ingestion/replay, bank account attachment, balance repair, external
decisions, and continuing setup without manual salary/account forms.

Do not claim "fully fixed" until CI and real-device acceptance agree. Do not require
the user to reinstall or erase the local ledger to apply a parser/account fix.

## Home loading and salary follow-up

Opening Home previously rebuilt the entire pay-cycle history, ran transaction
reconciliation and review triage, and computed relationship candidates through a
debt-account reader that also reconciled events. Those operations ran before the
first usable overview. They also competed with automatic history repair for store
monitors. Home now reads persisted state through a separate reader; it does not
start reconciliation, remote enrichment, or a pay-cycle rebuild. SQLite WAL lets
readers proceed while history is written. Ingestion still rebuilds cycles when
money changes. Duplicate checks inspect only unresolved records within the
existing 90-second candidate window rather than comparing every history pair.

Loading failures show recovery and Accounts/Activity navigation instead of an
unhandled coroutine error. A 15-second watchdog replaces an endless spinner with
navigation while the single outstanding load finishes. Revision updates coalesce;
loaded content stays visible during refresh. Home spending uses the same trusted
transaction/refund IDs as Activity, avoids Main-thread refund lookups, and no
longer subtracts refunds twice from the already-net spending total.

Home offers Add salary for a first salary as well as an overdue one, with named
account, amount, and actual arrival date. Saving, pending-cycle decisions, and
protection edits run on IO with visible errors; repeat submission is disabled.
Explicit manual salary starts its cycle without another pay-window confirmation.
A manual-only rule never labels unrelated incoming SMS as salary. Existing employer
rules continue to recognize salary and deduplicate replay. A manually selected
income account is treated as explicit ownership evidence for account display.
Payday snapshots no longer omit a recognized wallet merely because its phone
number has not been entered.

Regression coverage includes first/manual/early salary, known-employer automatic
salary/replay, read-only Home while transaction/debt writer monitors are held,
failure recovery, delayed-load recovery, and the actual first-salary UI save.
These are emulator checks; the user's installed database and Redmi still require
verification. This does not claim every provider format or device state is covered.

Accounts/Debts now use the persisted debt reader for display, and Plan reads the
saved cycle instead of rebuilding it whenever the tab opens. The Compose suite
also opens Accounts, Activity and Plan through the real navigation host and
returns to Home. Recurring-plan acceptance still rebuilds the cycle when it
changes the plan.
