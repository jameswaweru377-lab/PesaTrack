# Owed to me: history review proposal

## Intended outcome

From Activity or Accounts → Debts → Owed to me, identify a person, review previous outgoing payments and incoming receipts, and explicitly confirm which were loans or repayments. Gifts and unrelated payments stay outside debt totals. Show confirmed money lent, repaid, and still owed together.

## Selected approach

Extend the existing personal receivable journal with a local history-review service and a dedicated review screen. Reuse canonical imported transactions across M-Pesa and supported banks. Do not create a second SMS parser, provider-specific matching rules, or remote identity service. Searching messages means searching transactions already understood from imported messages; when history is missing, offer the existing SMS import with its existing consent flow.

Alternatives considered: plain categories alone cannot represent partial shared bills or confirm repayments safely; automatic classification of all same-name records can mistake gifts or different people for debt. A reviewable journal extension supports the requested bulk workflow while preserving accounting.

## Activity labels

Show Debts → Owed to me on confirmed loans and Debts → Repayment received on confirmed incoming repayments, with the person's name and confirmed principal. Expose Owed to me in Activity filtering. The existing I owe flow remains separate. Selecting the debt label opens the person/amount confirmation, rather than merely renaming an expense. Shared bills retain the original expense category for the user's remaining share; fees remain spending.

## Entry points and journey

1. After recording money lent in Activity, offer Review this person's history.
2. Add Find in transactions beside manual recording in Owed to me; also offer it on each person's page.
3. Search by name or phone across imported account history, not only the displayed Activity month. Show date, account/provider, counterparty, amount, and why each record matched.
4. Outgoing matches default to unselected. Mark individual rows Money lent, Gift/not a loan, or Leave undecided. Allow an editable lent share for shared bills. Select all eligible displayed loans with a clear count and preview; never include hidden pages or future transactions silently.
5. After loans are selected, show possible incoming repayments for review. Incoming records also default to unselected. Allow partial repayment amounts and exclude unrelated receipts. No receipt silently reduces a debt.
6. Preview confirmed loan total, repayment total, and resulting outstanding before saving. Save the selected batch atomically; validation errors save nothing and identify the affected record.
7. Person history shows loan/repayment records and a summary such as Norman: lent KES 5,000, repaid KES 2,000, still owed KES 3,000. Suggestions remain separate from confirmed amounts.

## Identity and evidence

Normalize valid Kenyan phone formats for matching. Exact phone matches are stronger suggestions; normalized full-name matches are weaker. Partial-name searches help discovery but never establish identity. Conflicting phones are flagged and require individual review; they are excluded from Select all. Names alone never merge people. Existing person selection is explicit. No new permissions, analytics, network processing, or raw-SMS upload.

## Accounting and recovery

Keep original bank/M-Pesa movements, amounts, wallet balances, and transaction IDs intact. Exclude loan principal from personal spending and repayment principal from earned income through existing receivable shares. Deduplicate canonical transactions and reject existing links, excluded/unresolved records, transfers, mirror observations, reversals/refunds, and incompatible category splits. Revalidate source records at save time.

Use integer minor units and checked arithmetic. Reject repayment totals exceeding confirmed valid loans. Process the batch in deterministic chronological order and reject a repayment preceding sufficient confirmed loan evidence; do not assign receipts to individual loans automatically. Existing manual records remain supported. Repeating confirmation cannot insert duplicate debt events. Cancellation makes no financial changes. Removing a link preserves the original transaction; changed evidence continues to show an unavailable balance with correction access.

Gift/not-a-loan decisions are remembered as person-specific review exclusions, never as new cash transactions or global rules. Their storage requires a small versioned migration and encrypted backup/restore coverage. Existing confirmed journal entries are preserved.

## Implementation structure

Keep matching and batch financial validation in small pure Kotlin policies with deterministic JVM tests. Add a dedicated repository for history queries/review exclusions and an atomic batch API beside PersonalReceivableStore. Keep Compose review components separate from the large Activity and transaction-detail screens. Integrate labels and filters without replacing canonical transaction categories.

## Verification and delivery

Test phone normalization, ambiguous names/phones, gifts, partial shares, incoming receipts, fees, chronological repayment constraints, overpayment, duplicates/mirrors, reversals, overflow, stale selection, atomic rollback, replay, and review-exclusion restore. Use synthetic fixtures only. CI must pass unit tests, lint, financial contracts, and debug APK assembly. Instrumentation remains off unless explicitly requested. Add Android integration/migration fixtures without claiming they ran. Work on a new isolated branch/PR from current main; leave PRs #312 and #289 untouched. This request authorizes implementation, not automatic merge of the new PR. Report exact CI evidence and remaining device checks.

