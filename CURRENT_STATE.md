# PesaTrack Current State

_Last refreshed: 7 October 2026_

This file is the short engineering source of truth. Historical audits, design explorations and `PROJECT_CONTEXT.md` are useful background, but they must not override current code or this snapshot when they conflict.

## Product status

PesaTrack is feature-complete enough for private/internal beta use, but it is still in a release-hardening phase before external/public distribution.

The current product includes the canonical ledger, provider-neutral financial interpretation, transfers/refunds/debts, salary cycles, budgets, saving goals, bills, reports, app lock, encrypted backup/restore, historical SMS import, notification capture, Data Health and Message Lab.

## Data and accounting

- Schema: **v32**
- `PesaTrackDatabase.get(...)` is the process-scoped database owner.
- Canonical financial-message ingestion is transaction-wrapped.
- Replay/idempotency and rollback are covered by Android tests.
- User-authoritative corrections must not be silently overwritten by replay.
- Unknown/ambiguous evidence must remain uncertain rather than being guessed.

## Remote enrichment privacy

- Raw SMS, amounts, balances, account numbers and transaction codes must never be sent to the enrichment Worker.
- Online merchant/entity lookup is **off by default**.
- A user can enable or disable online lookup from Settings.
- Only a descriptor that passes `EntityDescriptorPrivacyPolicy` may be sent.
- The Android client supplies a random install-scoped UUID solely for fair rate-limit partitioning; it is not authentication and contains no financial/device identity.
- The Worker applies both per-install/anonymous and edge-wide rate limits and fails closed if limiter bindings are unavailable.

## CI and APK publication

The Android CI workflow has three responsibilities:

1. `build`: Worker validation, unit tests, lint, debug APK assembly, then upload the APK as a workflow artifact.
2. `instrumentation`: run Android instrumentation independently.
3. `publish`: run only after **both** jobs succeed on `main`, then replace the rolling `ci-apk` asset.

The build/test jobs require only read access to repository contents. Release write permission is scoped to the publish job.

## Tests

Current repository coverage includes JVM tests plus Android instrumentation. Important existing test areas include:
- message relevance/extraction;
- corpus health;
- duplicate/mirror and replay behavior;
- debt and salary logic;
- migration;
- canonical ingestion atomicity;
- connected UI journeys;
- security backup/restore integration.

The next test expansion should add scheduled API-level coverage, broader migration fixtures, performance benchmarks and repeatable physical-device acceptance.

## Repository and release hardening

- Android CI third-party actions are pinned to immutable commit SHAs.
- CI/deployment use an exact Wrangler version.
- Dependabot watches Gradle and GitHub Actions dependencies.
- `SECURITY.md`, `CONTRIBUTING.md` and `CODEOWNERS` define review/privacy expectations.
- The repository contains a privacy-policy source and a Play release checklist.
- SMS-permission surfaces provide an explicit disclosure before requesting historical SMS access.

## Release status

Not yet public-store ready. Remaining release blockers include:
- protect `main` with required CI checks;
- clean stale branches;
- signed/versioned release workflow and committed Gradle wrapper;
- publish the privacy policy at a stable HTTPS URL and complete Google Play READ_SMS / Financial features declarations;
- Redmi/Xiaomi real-history acceptance;
- performance hardening for long imports/large ledgers;
- architecture decomposition of the largest stores/screens.

## Architecture rule for next work

Do not start another broad feature or visual redesign phase before hardening/decomposition. Preserve the current Home / Activity / Plan / Accounts / More product hierarchy and keep parser expansion evidence-driven.
