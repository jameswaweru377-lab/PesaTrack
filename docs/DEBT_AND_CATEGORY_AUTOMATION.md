# Debt review and remembered categories — 6 October 2026

## Debt

- Repayment candidate matching uses the exact normalized debt product name. Internal provider keys and partial institution overlaps are not repayment evidence: Safaricom airtime payments cannot become Fuliza repayment suggestions simply because the key contains SAFARICOM.
- Existing explicit repayment interpretation remains authoritative. Pending links are retried when the review queue refreshes; user decisions remain protected.
- Optional funding/balance provenance links no longer contribute to Needs Attention. They remain available under Show optional repayment links in Activity's attention panel. Their absence does not mean the repayment was omitted from the debt ledger.
- Debt cards show the last recorded repayment and balance observation time. Settled accounts are collapsed behind a separate toggle.

## Categories

- When an eligible uncategorized outgoing payment has two or more other matching records, choosing a learnable category offers Remember or Only this one.
- Remember updates matching automatically classified or uncategorized expense records across every imported month, with no 500-row cap, and applies to subsequent imports. It supports normal/custom expense categories and explicit Investments allocation.
- Match the exact normalized counterparty name, or the same normalized phone number when present. Never search arbitrary SMS substrings; a conflicting or missing phone does not broaden a phone-based preference.
- Preserve manual/excluded records, incoming funds, rules, structural transfers/refunds, and linked debt repayments. Account identity, amounts and dates remain unchanged.
- Preference, source classification, history application and provenance are saved transactionally. The dialog reports the number updated and retains errors for retry.
- No new database schema is needed. Existing identity preference and application tables are reused.

Validation added: provider/product-overlap negatives; exact-name/phone-conflict tests; Android integration coverage for more than 500 historical matches, future imports, replay idempotency, manual overrides and rollback on write failure. CI must pass before merge.
