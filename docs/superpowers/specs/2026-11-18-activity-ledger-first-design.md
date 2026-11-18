# Activity compact-overview dark redesign — approved screenshot

**Intent.** Simplify the Activity tab without losing its helpful period summary. James attached the selected compact-overview mockup on 18 November 2026 and approved the controls above Transactions, with one explicit addition: preserve the trusted daily **Net** total at the right of each day heading.

**Selected approach — compact overview.** The existing five-tab, coffee-black/peach design system stays. Activity gets a small mark and title, search and advanced-filter buttons, Month / Pay cycle pills, date selector, a **compact photo-backed monthly expense summary** with two footer metrics (Money in and Balance / Partial balance), a slim conditional review link, a Transactions heading with All activity reset, three immediately visible filters, and differentiated transaction surfaces. Day headings continue to show positive, negative, zero or unavailable net. Transactions remain individually tappable, with all existing review and pagination paths reachable.

**Approaches considered.** Three dark concepts were shown. The screenshot supplied with the implementation request is the approved visual source: compact expense card and Transactions heading remain, and daily net from the previous screen is added to its date rows.

**Preservation requirements.**
- Keep Month and Pay cycle reads and their existing selection dialogs. Preserve Expenses, Money in and recorded Balance summary without adding illustrative or reconstructed values. Keep search, Transfers, Needs Review, repayment and advanced actions reachable by disclosure.
- Keep the current five-tab navigation, dark/light theme switching, and the approved neutral category tiles with direction-aware icon colour.
- Reuse the existing trusted `activityDesign3DailyNet` for each day, calculated over the complete trusted period ledger, unaffected by text search, filters, pagination or transfer neutrality.
- Do not change ledger calculations, classifications, accounts, balances, statements, review decisions, permissions, storage, backups or signing.
- Avoid example financial amounts as production defaults. Use existing theme tokens rather than adding a palette.

**Visual acceptance.** Header, compact photographic hero and Transactions heading match the attached selected design; no persistent search field or bulky review card; row group surfaces read separately from the black background; no duplicate “Recent activity / See all” heading remains; the daily net remains right-aligned on every date group.

**Verification.** Focused JVM regression tests first, repository Android CI (JVM, lint, debug APK) on the exact PR head and code review before the owner-approved merge. No emulator or instrumentation execution. Real-device Redmi visual and APK in-place upgrade acceptance remain separate.
