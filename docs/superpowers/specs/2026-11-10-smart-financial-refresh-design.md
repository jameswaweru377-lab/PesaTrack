# Smart Financial Refresh — Design

The app should refresh its local financial presentation automatically on the
first launch after an APK update and when the user returns after a meaningful
absence. A new transaction or balance correction already publishes a ledger
revision; this work preserves that path and all stored financial evidence.

## Approaches

- Manual refresh: simple, but requires the user to discover a stale screen.
- **Version-aware foreground refresh (chosen):** use the existing update bus
  to invalidate cached screens, without importing, modifying or inventing money.
- Periodic background recalculation: unnecessary work, subject to Android
  scheduling delays, and unable to obtain an absent bank balance.

## Behavior

- On the first foreground entry for an APK version, invalidate the cached
  financial presentations once. Also invalidate after five minutes away.
- On rapid foreground returns and tab switches, keep current caches.
- Keep existing permission-guarded recent-SMS catch-up and once-per-update
  historical repair; both already publish their own changes.
- A presentation-only refresh must not masquerade as a new financial event or
  trigger change-driven notifications.
- Use the same source data for Home, Activity, Statements, Accounts and Plan.
- Keep missing, stale or unverified balances unavailable; no inferred salary,
  provider connectivity, network call, new permission, database migration or
  financial classification change.
- Preserve approved UI; use the existing Plan attention icon to explain when
  there is still insufficient balance evidence.

## Verification

Test first launch, version change, quick return, timeout and clock rollback.
Test presentation-only revision distinct from financial-data revision. Run
focused JVM tests, then the wider JVM suite and compile if available; GitHub CI
is the fallback when local Gradle distribution access is blocked. Phone check
remains necessary for the actual upgrade and foreground lifecycle.
