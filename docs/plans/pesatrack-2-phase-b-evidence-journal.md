# Phase B: evidence journal and canonical link integration

## Scope and authority

This is an additive, local-first slice of the 26-page PesaTrack 2.0 blueprint, based on main `83383c90f2a9` and schema 39. It does not replace the SMS ingestion pipeline, duplicate resolver, owned-transfer confirmations, payroll journal or canonical money ledger. No instrumentation, emulator, manual workflow dispatch, cloud processing, SDK or dependency changes.

## Design

- Schema 40: `evidence_observations` contains a hashed source identity, normalized money facts, account and status; `evidence_links` points to one canonical transaction and stores the canonical projection hash; `evidence_link_audit` retains link/unlink actions. Records contain no raw SMS bodies or document text. SQL constraints and deletion triggers guard orphaned links. No evidence table contributes to Money In/Out.
- The evidence link policy requires an existing owned-source account, matching amount and direction, compatible time and **equal transaction code** when linking statement evidence to an existing transaction. The full account choice remains user-authoritative in the existing import flow. Same-amount/day without a trusted reference remains pending.
- Existing manual received entries and explicitly selected CSV or supported M-Pesa PDF rows create their observation atomically with their existing canonical write. Skipped statement rows with a unique matching reference can attach a provenance observation to the existing cash row without creating money. Conflicts and rejected observations never overwrite established user choices.
- Links point only to an existing canonical row. Replay of the same source identity is idempotent; conflicting facts stop the batch. Deleting a source association or user-rejecting a link never deletes a confirmed transaction. The Transaction Details evidence summary shows when subsequent transaction edits make the link stale.
- Encrypted backup/restore supports older schema 34–39 by adding empty evidence tables and checks evidence identity, account, relation and audit integrity before removing old local state. SQLite changes remain additive; no destructive data rewrite.
- Replaced invalid `transactions.raw_message` lookups in `EverydayMoneyStore` with the canonical `TransactionFingerprint` identity. This is a pre-existing mismatch with the schema, and prevents manual/statement lookup failure.

## Acceptance and negative cases

1. Existing SMS imports, refunds, Fuliza, debts, user corrections, owned transfers and salary totals remain unchanged.
2. One synthetic manual received KES 52,000 records one cash event and one evidence link, never extra income.
3. A selected statement row with a unique verified code/account/amount/date links to existing SMS without posting additional cash.
4. Two identical-amount/day transactions without a common reference are **not** auto-linked. Wrong account, reverse direction, conflicting amount/code and late dates fail closed.
5. Re-importing an identical source does not multiply evidence or ledger rows; conflicting source identity rolls back the atomic batch.
6. Evidence-only unlink and source deletion do not delete the canonical transaction; edited transactions show provenance needing review.
7. Schema 39 upgrades in place to 40; backed-up schemas 34–39 restore with empty added tables. Malformed orphan/duplicate evidence refuses restore.
8. Unit tests, lint, Android debug assembly, synthetic offline financial contracts and Android-test source compilation must pass on the reviewed final PR SHA. **Do not run instrumentation**.

## Not yet delivered by this slice

Full automatic interception of an incoming SMS following an uncoded manual salary, universal backfilling of historical SMS/notification observations, unmatched source-resolution UX, split payroll settlements, OCR and provider-specific statement parsing are separate work. Those require stronger ambiguity and user-decision modeling; this journal deliberately does not auto-merge cash by amount/date. Physical-device/Android instrumentation acceptance remains unverified by request.

## CI economy

Finish all file edits and local source review **before opening the PR**; then permit one ordinary automatically triggered PR build on the combined frozen head. Fix actual failures in one corrective batch if necessary. Do not dispatch manual CI or instrumentation. After successful exact-head review, the user authorizes merging and verifying the automatically triggered post-merge main build/publication.
