# Annual Reports accounting evidence

## Intent and scope

Show the already-merged shared accounting evidence on the Reports screen when **Year** is selected, without modifying Reports totals, financial classifications, matching decisions or the reconciliation engine owned by another chat.

## Approach

Use the existing local `StatementMatchDecisionStore.read(year).sharedAccounting` snapshot. A pure period-scope policy prevents annual amounts from appearing in Week or Month reports. A small read-only card presents separate Activity and Statement figures, reviewed transfers, and an explicit assurance limit. Missing evidence is shown as unavailable, never zero. No schema, financial posting, new permissions or network traffic.

## Alternatives considered

- Replace the Reports hero totals: rejected because annual Statement cash legs are not Activity earnings or spending.
- Add a consolidated balance: rejected because N2 posting/verification gates remain unmet.
- Show a separate annual evidence card: selected as the smallest safe integration.

## Acceptance

Tests prove period and year gating, unavailable evidence and immutable non-posting flags. Standard Android CI performs compilation, unit tests, lint and debug APK assembly; instrumentation remains skipped. Physical-phone reconciliation, report parity and N2 posting remain out of scope.
