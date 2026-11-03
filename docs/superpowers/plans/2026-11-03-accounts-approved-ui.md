# Accounts visual refresh execution plan

1. Branch from current main; compare open PR ownership and approved
   references, preserve all financial data flows.
2. Create shared compact account/debt/provider presentation components
   with theme-driven status colors and touch targets.
3. Replace overview/debt card layouts with grouped, compact screens and
   wire the fifth (Owed to me) state to existing receivables.
4. Redesign the picker with top Done, search/category chips, locally saved
   changes, and an optional identifier edit affordance.
5. Refresh Account Details without changing account snapshot or Activity
   querying semantics.
6. Run focused checks; run Gradle JVM/lint/assemble where available, then
   verify a GitHub PR's exact head with CI before requesting merge approval.

Note: local Gradle distribution is not cached and its distribution URL is
not accessible in this executor. CI will be the compilation gate.
