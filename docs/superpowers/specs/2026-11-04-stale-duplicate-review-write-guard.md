# Stale duplicate confirmation write guard

## Problem
Activity renders a weak possible-duplicate pair, but `TransactionStore.resolvePossibleDuplicate` currently trusts only its ID string. If a transaction changes after the card renders, selecting **Same Transaction** can exclude a now-unrelated or protected record. The exclusion and decision are not currently one atomic write.

## Approved boundary
- Apply to weak possible-duplicate review of stored SMS/financial observations. This does not infer statement ownership, post statement rows to Activity, or change income/spending formulas.
- The Activity review card must pass both originally displayed `StoredTransaction` snapshots. Re-read both records at decision time under `CanonicalStoreLock`; compare the complete persisted snapshots. Reject stale cards without any write.
- **Same Transaction** additionally requires the current pair still meets `PossibleDuplicatePolicy.isCandidate`; reject reversals, user corrections, changed amounts, identities, sources and time windows.
- **Keep Both** remains non-excluding and supports existing reverse-ID callers for explicit transfer-review coordination, but displayed Activity cards receive the same stale-snapshot guard.
- Reject malformed IDs, self-pairs and any already decided unordered pair. Persist one canonical pair key.
- Exclude a corroborating copy and insert its resolution together inside one SQLite transaction; notify/rebuild only after successful commit. Preserve both underlying rows, amounts, fees, balance observations and source references.
- No new schema, network, permissions, runtime dependency or UI design. No emulator or instrumentation execution per owner instruction; JVM unit tests and Android CI still apply.

## Risks and accepted limits
A past decision with no recorded snapshot cannot be retrospectively verified. Keep Both's legacy caller intentionally omits a snapshot for compatibility, so this path only enforces valid pair IDs, preexisting decisions and protected transfer pairs; it never excludes a row. Existing duplicate detection remains review-only and is not upgraded into automated posting.

## Acceptance
- A current displayed eligible pair can be confirmed once.
- A changed stored row (including any financial field outside the rule's observation) is rejected without an exclusion or resolution.
- A reversal, user-classified row, incompatible sources or mismatched amounts is rejected even if a caller submits apparently matching snapshots.
- A malformed or already resolved pair is rejected; reversed order cannot repeat the decision.
- Existing reversed-ID Keep Both behavior remains.
