# Phase 3C — Verified category decisions across financial evidence

## Existing evidence

- Activity is the independently authoritative SMS/user ledger. M-Pesa PDFs are
  immutable historical evidence, with their own per-row category corrections.
- `StatementActivityEvidencePolicy` already projects approved Activity categories
  into the same verified M-Pesa receipt. Explicit Statement corrections win over
  ordinary merchant preferences; protected cash flows retain their meaning.
- `StatementCrossCategoryRepository.rememberBoth` already requires an exact,
  uniquely corroborated M-Pesa receipt and explicit opt-in to teach Activity.
- The Equity archive already has a separate `statement_category_labels_v2` table,
  but there is no supported write operation or row editor. Equity rows commonly
  lack a unique reference; a matching amount, date or narration is not proof.

## Scope of this change

1. Keep M-Pesa ↔ Activity's existing verified, opt-in category bridge intact.
2. Offer an **independent, reversible user label** for a bank statement row.
   The bank source must exist and its immutable signed movement must match the
   selected category direction. The bank archive label can be removed later.
3. Keep bank labels local to their exact source and row, not to a payee name,
   other bank document, Activity transaction, balance, debt or Insights total.
4. Expose an honest row-level label picker in the existing light-theme Equity
   statement detail view, with explicit text about the evidence limitation.
5. Use deterministic unit tests for allowed and rejected category choices;
   guard against bank name-only, duplicate and protected-flow propagation.

## Acceptance

- A reviewed debit can be labelled using an expense category; a reviewed credit
  can be labelled as income. Neither choice changes the bank's money-in/out totals.
- Debits cannot be labelled as income, credits cannot be labelled as an expense;
  zero movements and structural/system/transfer categories are rejected. A
  transfer requires its own confirmed ownership flow in a future phase.
- Changing/removing a bank label does not alter the original PDF leg, the
  canonical Activity database or the label on another PDF copy.
- The current M-Pesa and Activity category behavior is unchanged; imported bank
  narrations are never used to infer a shared identity or transaction category.
- Run focused JVM tests, Android CI build/lint/tests, and only merge after a
  successful final-head check. No signing, release or instrumentation edits.
