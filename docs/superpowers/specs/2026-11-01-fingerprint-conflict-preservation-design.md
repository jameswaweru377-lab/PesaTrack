# Fingerprint conflict preservation — design

## Intent

Keep distinct incoming financial evidence available for local review when an existing Activity transaction has the same code-derived fingerprint but contradictory core facts. This is a safety stage, not reconciliation.

## Boundaries

- Never post a second transaction, delete or reclassify an existing one, change balances or totals, or infer a transfer.
- Keep financial descriptions, account identities and evidence on-device; never send them to diagnostics or remote services.
- Genuine replay of identical evidence remains idempotent. Weak counterparty spelling differences alone are not proof of a collision.
- Preserve the existing transaction as authority while storing conflicts separately with an explicit `PENDING` review state.
- Preserve conflicts through encrypted backup/restore and database upgrades; test migrations from schema 40.
- Do not merge or release until exact-head CI, executed Android persistence tests, signed physical-device persistence and financial release gates pass.

## Approaches

1. **Chosen: separate non-posting conflict inbox.** Record normalized evidence and source provenance in a new SQLite table linked to the original row; preserve replay idempotency with a unique evidence digest. The existing ledger continues unchanged.
2. Change the fingerprint uniqueness rule to insert multiple ledger rows: rejected because this immediately risks double-counting and old schema assumptions.
3. Attach conflict text to an existing transaction: rejected because it overwrites provenance and cannot robustly represent independent observations.

## Staged behavior

At ingestion, compare amount, direction/type, fee and independently evidenced occurrence facts against an already stored code-fingerprint row. If facts contradict, write a non-posting conflict snapshot, deduplicated for repeated imports. Record the source kind and times; never record the raw SMS body in the inbox. A later review screen lists unresolved conflicts, shows the original Activity row and the competing observation, and allows metadata-only dismissal/confirmation. Neither decision modifies ledger accounting. The separate cross-source review drafts remain unmerged and must be integrated against the final combined candidate later.

## Critical edge cases

- Same code/amount replay with a later notification timestamp should not create a conflict.
- Same code with a different amount or fee must preserve the incoming observation.
- Same code with a contradictory type/direction must be preserved, even if amount matches.
- Concurrent replay must create at most one conflict per source/evidence identity.
- Backup restore and schema 40 migration must preserve existing transactions and all saved conflicts.

## Release scope

This branch may implement the storage and review seam with synthetic tests. Totals reconciliation, automatic linking, imported statement assurance, release signing, and distribution are explicitly deferred.
