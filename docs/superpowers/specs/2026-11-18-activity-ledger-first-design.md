# Activity ledger-first dark redesign — approved design

**Intent.** The Activity tab should put the user's transaction history in the first screen instead of repeating Home's large financial overview. James selected the first of three dark-mode concepts on 18 November 2026, with one explicit addition: preserve the trusted daily **Net** total at the right of each day heading.

**Selected approach — ledger first.** The existing five-tab, coffee-black/peach design system stays. Activity gets a simple title, compact date control, on-demand search and advanced filters, a slim conditional review entry, three immediately visible ledger filters, and softly differentiated grouped transaction surfaces. Day headings continue to show positive, negative, zero or unavailable net. Transactions remain individually tappable, with all existing review and pagination paths reachable.

**Approaches considered.** The selected ledger-first concept removes the monthly hero and prioritises visible rows; the compact-overview concept kept a reduced photo summary but still pushed history lower; the editorial timeline concept used paired summary tiles and more decorative transaction cards. The owner explicitly chose ledger first.

**Preservation requirements.**
- Keep Month and Pay cycle reads and their existing selection dialogs. Keep search, Transfers, Needs Review, repayment and advanced actions reachable by disclosure.
- Keep the current five-tab navigation, dark/light theme switching, and the approved neutral category tiles with direction-aware icon colour.
- Reuse the existing trusted `activityDesign3DailyNet` for each day, calculated over the complete trusted period ledger, unaffected by text search, filters, pagination or transfer neutrality.
- Do not change ledger calculations, classifications, accounts, balances, statements, review decisions, permissions, storage, backups or signing.
- Avoid example financial amounts as production defaults. Use existing theme tokens rather than adding a palette.

**Visual acceptance.** The first transaction/date group is visible without crossing the previous financial hero and full-width search/filters; row group surfaces read separately from the black background; no duplicate “Recent activity / See all” heading remains in the full Activity list; the daily net remains right-aligned.

**Verification.** Focused JVM regression tests first, repository Android CI (JVM, lint, debug APK) on the exact PR head and code review before the owner-approved merge. No emulator or instrumentation execution. Real-device Redmi visual and APK in-place upgrade acceptance remain separate.
