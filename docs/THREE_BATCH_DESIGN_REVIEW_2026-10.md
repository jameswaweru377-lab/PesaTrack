# PesaTrack three-batch design review — October 2026

This document records the consolidated recommendations from the three screenshot-review batches and what was implemented in PR #242.

## Implemented in this pass

- Strengthened dark-theme surface hierarchy while retaining the emerald PesaTrack identity.
- Made pre-salary Home states contextual instead of presenting an invented daily allowance.
- Added a Home warning/CTA when Other + Uncategorized dominate the spending picture.
- Clarified Accounts hero semantics: total shown is known balances, with a count of accounts that actually have a known balance.
- Standardized Profile terminology around money-data coverage and confirmed accounts.
- Added a direct salary CTA to the waiting-for-salary Plan state.
- Removed misleading month-over-month income percentages when the current month has no recorded income.
- Added Reports data-quality guidance and a Review spending action when categorization quality is poor.
- Simplified zero-bill states to one first-bill experience instead of repeating KES 0 / no bills / no bills yet.
- Simplified the zero-goal state; the hero CTA remains the primary action.
- Replaced default goal/bill AlertDialogs with a branded PesaTrack bottom sheet.
- Reworded technical form labels such as “payee on receipts” into user-facing language.
- Made transaction-detail category layout adaptive: compact phones use two columns so labels are readable.
- Moved Data Health and Import History under Advanced tools in More.
- Removed inactive “planned” controls from the primary Settings surface.
- Removed unnecessary .00 from summary money formatting while preserving actual cents.
- Kept bottom navigation and overall page hierarchy because they are already strong and consistent.

## Intentionally deferred

These require product/data-model work rather than a visual-only patch:

- Goal total target independent of monthly contribution.
- Optional goal deadline and contribution frequency.
- Goal source/destination account modeling beyond the current matching rule.
- A persistent app-wide “money picture confidence” score with drill-down reasons.
- Backup/restore and app lock until the features are actually implemented.
- Automatic utility-bill creation. Bills remain explicitly user-defined.

## Product principles confirmed

1. Never guess balances.
2. Never invent bills or goals.
3. Distinguish zero from unknown/not-started.
4. Surface data-quality problems instead of presenting low-quality analytics as complete.
5. Prefer context-sensitive empty states over showing every feature all the time.
6. Keep advanced parser/diagnostic tooling secondary to everyday money workflows.
