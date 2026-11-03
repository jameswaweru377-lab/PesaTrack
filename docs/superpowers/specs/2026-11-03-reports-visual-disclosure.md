# Reports visual disclosure — 3 November 2026

## Owner and scope

Chat owner: `CHAT-VISUAL-POLISH`. This is a presentation-only stage of issue #399, following the global visual-rhythm and approved Profile stages.

## Changes

- Present the three largest categories first, with an explicit action to reveal all categories and collapse back to three.
- Keep the existing complete spending calculation and donut data unchanged. The collapsed legend shows the top three with a count of additional categories rather than squeezing every label next to the donut.
- Keep the definitions and review entry points for **Other** and **Uncategorized**, but explain them on demand. The category-review examples remain a separate, existing control.
- Shorten the secondary Paycheck action label, without changing its route.
- Reuse existing design tokens and ensure the expand/collapse action has a minimum 48dp target.

## Safety and acceptance

- No changes to category totals, transfers, fees, refunds, evidence, reconciliation or statement accounting.
- Expanded list must include every positive-spending category in the existing order, not silently stop after six.
- Large-font and narrow-screen walkthrough on the Redmi Note 14 is required before rating this screen as visually complete.
- Run exact-head Android CI; instrumentation remains excluded under the user's current testing preference.
