# Home → Activity idle preloading (performance Phase 2)

## Scope and safety
PesaTrack already has screen caches tied to `LedgerUpdateBus.state.revision`.
This change preserves that mechanism and **does not** reuse data when the ledger
revision changes. It does not change balances, transaction parsing, category
logic, storage schemas, or transaction import behaviour.

## New behaviour
- After Home's money picture and supporting data have finished loading for
  the active ledger revision, the navigation root computes Activity's monthly
  transaction core once on `Dispatchers.IO`. It does not show a loader or block
  Home's UI; the task is cancelled when leaving Home.
- The Activity cache holds its fast *core* (month buckets, current and previous
  monthly snapshots, account map, ownership IDs) separately from slower optional
  financial diagnostics and review metadata.
- When Activity opens, a matching cached core can render immediately.
  Diagnostics and review metadata continue loading normally and are then saved
  as the full screen cache.
- If a user switches tabs before the optional metadata finishes, Activity's
  completed monthly snapshot survives for the next visit. No expensive new
  monthly query is needed at the same revision/month.
- Prefetch is bounded to one month and one revision. Importing a transaction
  advances the revision: the old core is **never treated as current**.
  Prefetch failure leaves the normal Activity read path intact.

## Regression testing
A JVM test verifies that a cached core is reused only for an identical
month and ledger revision. The existing Android navigation and financial read
tests continue compiling in the main Android CI workflow.

## Limits
Preloading is deliberately **best effort** after Home finishes loading.
Rapidly opening Activity before the first Home load completes still uses the
normal reader. This is not a claim of zero-latency navigation on every device.
No instrumentation benchmark runs in default CI, so cold/warm nav timing and
visual flicker should be checked on device, with large synthetic histories.
