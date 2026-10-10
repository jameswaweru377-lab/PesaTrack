# Remaining UI audit implementation

User approved implementation of the remaining viable findings as one branch and PR. Merge remains a separate review step. Audit screenshots are private evidence and must not be copied into public fixtures.

Already on main: F01/F02 report bar and money precision fixes; F04 Goals contrast; F05 Activity hierarchy; F08 row truncation; F11 correction discoverability; F06 explicit ownership evidence and consent. Other open budget/debt PRs remain independent.

## Scoped work and acceptance criteria

1. F03/F14 Reports: explain category quality and offer useful merchant/category review using existing data and navigation. No automatic recategorization or financial calculation changes. Synthetic tests cover presentation eligibility.
2. F09 Rules: examples before technical fields; explain and preview local match impact before explicit save. Preserve matching semantics. Synthetic fixtures cover preview behavior.
3. F10/F15 Privacy and reminders: concise current status, expandable details, online sharing disclosure adjacent to its opt-in toggle; correct singular/plural reminder days and disable offsets when off. Preserve access/revocation defaults.
4. F07/F12/F13 Plan and accounts: reduce decoration, retain successfully loaded content during refresh with visible loading/error state, consolidate redundant empty copy. Keep cache in composition, not persisted financial storage; re-read when data changes.
5. Split sheet: show exact allocated and remaining amount and disable save until the existing validation policy passes. Preserve fees and refund allocation. Test malformed, partial, duplicate, excess, exact and overflow-sized allocations.
6. F16 Accessibility: review changed screens for wrapping, minimum touch size and large fonts. Add focused Compose regression evidence where viable. Record actual emulator coverage separately from JVM/build validation.

## Verification and delivery

Run focused synthetic tests locally, then Android CI JVM tests, lint and APK on the branch. Check actual source SHA, jobs and artifact evidence. Independent review before final PR delivery. Do not claim device rendering or performance measurements without execution. No release, main workflow changes, or changes to other PRs.
