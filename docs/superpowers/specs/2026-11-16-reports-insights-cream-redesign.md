# Reports & Insights — approved cream redesign (16 November 2026)

Implementation owner: CHAT-REPORTS-UI. Scope: existing Reports & Insights Compose UI only.

## Visual reference and acceptance
- Preserve the current PesaTrack cream, coffee-brown, subtle-green theme, wordmark and five-tab shell.
- Centre `Reports & insights` with a functional stemless back chevron.
- Render selected reporting date as a plain, tap-accessible calendar row (not an outlined button), retaining Week / Month / 6 months / Year.
- Prioritise one Money spent summary and a clear percentage comparison to the previous matching period. Label ongoing periods as in progress; show missing-baseline state instead of a fabricated change.
- Render the existing six-month/daily/year spending trend with visible KES-axis ticks, horizontal guide lines and bucket labels. Do not change trend buckets or ledger figures.
- Display an actionable category-review prompt when at least half of categorised spending is labelled Other. This prompt uses the existing nonposting review drilldown, not recategorisation without user choice.
- Retain all category totals, expand/collapse, explanatory help, largest and uncategorised review sheets, Paycheck Analysis, Year accounting evidence and offline PDF export.
- Keep report money totals from existing trusted Activity and category projection calculations; statement-only evidence and unreviewed transactions must not be added to trusted spending.
- Use existing theme tokens and icons; do not introduce new dependencies, permissions, persistence, financial posting, telemetry, network calls or release-signing changes.

## Verification
- GitHub Android CI JVM/lint/assemble checks are required on the exact PR head.
- Device-only visual validation is still needed for Redmi font scaling, 6/12-month chart labels, date picker, scroll/end-to-end navigation and PDF readability.
- Skip emulator and instrumentation testing.
