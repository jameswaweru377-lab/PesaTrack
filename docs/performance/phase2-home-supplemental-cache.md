# Home supplemental refresh optimization (Performance Phase 2)

Home's primary snapshot is already revision-cached. Before this change, two
additional `LaunchedEffect` readers still ran whenever Home was recreated:
investment observations and month-aligned historical spending.

Home now retains these **supplemental** reads in the same navigation-scoped cache.
It uses them only when the corresponding ledger revision matches. Historical
comparisons additionally require the same selected month offset and calendar
day (so yesterday's partial-month comparison does not survive overnight).

Re-entering Home from Activity at an unchanged revision/month/day therefore
does not repeat these two database queries or reset the historical comparison
to a loading state. The real ledger revision still invalidates data.
Missing or failed cache reads retain the existing normal reader and retry path.

No financial calculations, M-Pesa parsing, database schemas or debt records
are changed.

Tests check invalidation by ledger revision, selected month, and calendar day.
The regular Android CI compiles instrumented UI tests, but its emulator
instrumentation job remains skipped; verify visuals and latency on device.
