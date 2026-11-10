# Shared Money Figures — Design (10 November 2026)

## Intent

PesaTrack must show reliable current account money in Home, Activity, Accounts and Plan even when an Equity salary receipt is missing. A missing salary is missing income evidence, not a missing bank balance. Plan's main Available amount is verified spendable bank/wallet money less unpaid commitments; it also shows the account total separately.

## Approved approach

Improve the existing local SQLite-backed balance and reconciliation engine incrementally. Preserve canonical transactions, original source evidence, saved decisions, existing statement admission gates and approved Compose layouts. Do not rebuild the ledger or invent salary or bank balances.

The alternative — replacing the ledger and migrating history — would increase the risk to on-device data and is not approved.

## Shared meanings

- **Recorded spendable account balance:** sum of individual owned, enabled, non-cash spendable bank/wallet accounts, using direct observed or dated user-confirmed balance evidence. Protected savings remain separate. Unverified or missing member evidence makes the total unavailable, not zero. Historical Activity month snapshots use their month-end evidence; they are not today's balance.
- **Money In:** trusted external receipts in the selected calendar month. Self-transfers and loan drawdowns are not salary/income. Missing Equity income is not fabricated from a pay date or a later transfer to M-Pesa.
- **Pay-cycle:** optional salary-receipt anchored spending analysis and forecasts. It may be unknown while current account money remains known.
- **Plan Available:** current verified spendable account money less still-unpaid scheduled bills/confirmed commitments. Amounts already paid or already moved to protected savings must not be subtracted twice. When the available amount is not trustworthy, label it unavailable and show the reason.
- **Statement balances:** historical provider-printed closing/running balances with their own statement date and scope. They must not masquerade as today's multi-account balance; no new statement-to-ledger admission is authorized.

## First independently testable implementation slice

1. Centralize direct-observation/user-confirmed balance eligibility and checked aggregation. Home account tiles, Activity and Plan reuse the same eligibility contract. Do not silently upgrade reconstructed balances to confirmed cash.
2. Stop gating Home's current available figure on salary-cycle presence. Derive it from the same spendable-account/remaining-bill projection Plan uses, not from cycle income. Label it as money after unpaid bills; keep the current account balance visible separately.
3. Apply the same owned non-cash account scope to current Home/Activity/Plan totals. Preserve historical Activity as-of dates, protected savings separation, overflow/unknown behavior, and existing UI design.
4. Run synthetic regression cases for missing salary cycle, duplicate evidence, unreadable/missing bank balance, stale balance, protected savings, overflowing total, unpaid bill and no current pay cycle. Preserve existing self-transfer tests; no new transfer classifier in this slice.

## Deliberate constraints and next slices

- No schema migration, financial posting, transaction reclassification, new permissions, network/AI requests, or automatic bank statement admission in this slice. N2 remains on hold.
- This slice aligns **current balance display**. A later independently tested slice may strengthen salary check-in and provider-neutral transaction matching, only after collecting failing synthetic examples. Phone validation remains required to diagnose the reported M-Pesa discrepancy.
- Android JVM tests, lint and debug APK must pass on the exact GitHub head before any release claim. No emulator or instrumentation run; user tests on the Redmi. If local Gradle is unavailable, CI evidence is required and local testing must be reported as blocked.
