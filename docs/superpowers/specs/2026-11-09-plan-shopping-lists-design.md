# Plan Shopping Lists — First Release Design

**Owner decision:** Begin implementation now; ship a small local-first planning feature before photo scanning.

## Intent

Help someone plan monthly grocery or household purchases, estimate the cost, and start next month's list from a previous list without retyping everything.

## User experience

- In **Plan**, a compact Shopping lists entry opens a dedicated page under the existing five-tab shell.
- The page can create a named list for a selected month, display earlier lists, edit or remove lists, and reuse a list for the current month with a confirmation action.
- A list has editable items: name, quantity, optional unit price in KSh and a bought checkbox. Show the estimated total of priced items and clearly indicate any missing prices.
- New lists and cloned items default to *not bought*. Reusing a list is a user action, never an automatic transaction.
- Empty, loading, error and save-failure states must have clear next actions. All financial estimates are explicitly labelled and never confused with bank balances or expenses.

## Constraints

- Do not create or modify canonical ledger transactions, budgets, debts or balances; no new permission or network call.
- Store list metadata in private Android preferences, included by the existing encrypted-backup preference export. Do not alter database schema or backup wire format.
- Use integer minor units; reject invalid prices, quantities, empty names, unreasonable list sizes and arithmetic overflow.
- Preserve approved Plan visuals, navigation, typography and existing performance caches; keep this view separate from expensive financial reads.
- No OCR in first release. Later, with separately reviewed dependency/privacy checks, add on-device photo text extraction with user review before saving items.

## Considered approaches

1. **Local lists and explicit reuse (selected):** Smallest useful release, predictable prices, no new dependency or sensitive data movement.
2. Local photo OCR immediately: more convenient, but requires a new dependency and careful handling of handwriting, quantities and confidence; defer.
3. Automatic purchase suggestions and transaction matching: useful later, but risks inferring purchases incorrectly; keep separate from first release.

## Acceptance

1. Create, edit, delete and reopen a monthly shopping list after app restart.
2. Add/edit/remove/check items; estimates update deterministically, missing prices remain visible.
3. Reuse a previous list in the current month without changing the original or marking items as bought.
4. Invalid monetary input cannot be persisted; list data is recoverable through the existing encrypted preferences backup path.
5. None of these actions changes recorded income, expenses, transfers, balances or budgets.
