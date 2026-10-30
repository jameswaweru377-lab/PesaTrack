# Statements ↔ Activity evidence reconciliation — design

## Intent

Let the Statements evidence layer identify proven overlap with live financial-message Activity and rigorously corroborated transfers between confirmed own accounts. This is a prerequisite for a later, separately designed Insights report; it is **not** permission to merge the two ledgers.

## Scope and authority

- Activity is the canonical daily ledger; imported PDFs remain separate, immutable statement archives.
- Compare only eligible original SMS/notification Activity observations, never manual rows or `statement_import` rows that originated in a PDF. The latter would be circular confirmation.
- Confirmed ownership, exact stable reference, exact amount, currency, account, direction and bounded event time are mandatory. A name, phone fragment, narration or amount/time proximity never proves a transfer.
- Build unique **cash-leg groups** before linking opposite cash legs. Thus an Activity wallet receipt and a wallet PDF receipt can represent one leg of the same bank-to-wallet transfer rather than becoming an artificial third transfer.
- Protect exclusions, reversals, refunds, debts, structurally linked records, uncertain sign and unrecognized references; never change ledger categories, balances, the statement archive, or its coverage money figures.
- Unsupported/no-reference bank rows remain unlinked and are not inferred from matching amounts. Existing source overlap warnings remain authoritative.
- Results are a read-only, bounded snapshot accessible on demand to a future review/Insights UI, **not** a new persistent transaction table or automatic financial classification.

## Approaches considered

1. Merge imported rows into Activity: rejected; would mix source authority, miscount duplicates and risk user corrections.
2. Compare each Activity row directly to all statement rows: rejected; quadratic and fails when one money leg appears in three sources.
3. **Selected:** guarded Activity observation projection plus reference-indexed, same-leg-first grouping and read-only transfer corroboration. Preserves existing stores and limits risk.

## Acceptance

- Same-leg Activity + statement proof is distinguishable from owned transfer evidence, including a three-source bank/wallet/Activity example.
- Duplicate Activity or PDF candidates, fee/principal differences and same-reference collisions are ambiguous, never silently resolved.
- Unconfirmed ownership, missing reference, contradictory sign, dates outside limit, different currency, manual rows, statement-import rows, excluded/loan/refund/reversal events cannot be promoted to proof.
- Replaying the same snapshot gives identical output. Activity transactions, PDF rows and existing Statements overview totals are never modified.
- Pure synthetic Kotlin tests and Android CI build/lint/unit pass; phone acceptance stays separate.

## Deliberately deferred

- UI for explicit review and user-confirmed own-account identities, account-number capture, Equity narration-only linking, bank-to-Activity category propagation, bank parser expansion, consolidated Insights totals.
- No database schema, signing, workflow or approved Compose screen change in this milestone.
