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
2. **Complete cross-account matching.** Use shared references and complementary
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
