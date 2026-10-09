# PesaTrack Current State

_Last refreshed: 9 October 2026_

This file is the short engineering source of truth. Historical audits, design explorations and `PROJECT_CONTEXT.md` are useful background, but they must not override current code or this snapshot when they conflict.

## Product status

PesaTrack is feature-complete enough for private/internal beta use, but it is still in a release-hardening phase before external/public distribution.

The current product includes the canonical ledger, provider-neutral financial interpretation, transfers/refunds/debts, salary cycles, budgets, saving goals, bills, reports, app lock, encrypted backup/restore, historical SMS import, notification capture, Data Health and Message Lab.

## Data and accounting

- Schema: **v33**
- `PesaTrackDatabase.get(...)` is the process-scoped database owner.
- Canonical financial-message ingestion is transaction-wrapped.
- Replay/idempotency and rollback are covered by Android tests.
- User-authoritative corrections must not be silently overwritten by replay.
- Unknown/ambiguous evidence must remain uncertain rather than being guessed.

## Offline financial laboratory

- Python is development-only: fixed invented fixtures and read-only comparison
  with actual Kotlin outputs; no device SMS or ledger import, network/model
  integration, or Python runtime shipped in the APK.
- Four contract groups cover sender/account ownership evidence, configured
  transfers, debt interpretation histories and remembered merchant identities.
  29 authored scenarios at eight monetary scales produce 232 regression cases;
  unchanged name-only cases are repeats, not independent message formats.
- Configured notification transfers require a unique sender-derived source and
  unique whole destination identifier. Body bank names and ambiguous accounts
  do not establish an own transfer. Existing shared-reference mirror matching
  remains available when complementary observations establish both sides.
- Explicit remembered merchant corrections normalize ordinary Unicode spaces,
  retaining exact-name and phone safeguards. Unknown merchant names do not
  automatically imply groceries or consent.
- Fresh-database Android histories verify lender separation, observed versus
  estimated outstanding debt, repayment replay and remembered-category consent.
  These do not diagnose a particular installed user's debt or repair previous
  account-review decisions. Voice entry is outside this change.
- Release readiness still requires current-head Android CI and physical-device
  acceptance with invented data. See `tools/python_lab/README.md` for commands,
  acceptance criteria and coverage limits.

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
2. `instrumentation`: run Android instrumentation only when explicitly requested through a manual workflow run with `run_instrumentation` enabled (default off).
3. `publish`: after successful build checks on a push to `main`, replace the rolling development `ci-apk` asset without requiring instrumentation. Manual review/build runs retain artifacts but cannot replace the rolling release.

PR updates and merges run build checks automatically. Marking a PR ready does not start another run. Android 7 security and API compatibility emulator suites are manual review tools; the former weekly compatibility schedule is disabled. Manual signed releases still require same-commit Android validation.

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

The next test expansion should add broader migration fixtures, performance benchmarks and repeatable physical-device acceptance. API-level emulator coverage remains available through explicit manual reviews.

## Repository and release hardening

- Android CI third-party actions are pinned to immutable commit SHAs.
- CI/deployment use an exact Wrangler version.
- Dependabot watches Gradle and GitHub Actions dependencies.
- `SECURITY.md`, `CONTRIBUTING.md` and `CODEOWNERS` define review/privacy expectations.
- The repository contains a privacy-policy source and a Play release checklist.
- SMS-permission surfaces provide an explicit disclosure before requesting historical SMS access.
- Release builds accept explicit version code/name inputs and a manual signed AAB/APK workflow is defined; signing material remains outside the repository and the first signed run is still a release gate.
- Historical SMS import reports scan/match/insert progress, supports cooperative cancellation, and retains a short-lived checkpoint so interrupted imports can resume by SMS date/id without rescanning the completed portion. Activity review/duplicate work is now bounded to unresolved/recent rows, duplicate matching stops at the policy time window, and month/account lookups use v33 ledger indexes. Physical-device large-ledger measurement still remains.

## Release status

Not yet public-store ready. Remaining release blockers include:
- protect `main` with required CI checks;
- clean stale branches;
- configure signing secrets, prove the signed release workflow end-to-end, and commit the Gradle wrapper;
- publish the privacy policy at a stable HTTPS URL and complete Google Play READ_SMS / Financial features declarations;
- Redmi/Xiaomi real-history acceptance;
- performance hardening for long imports/large ledgers;
- architecture decomposition of the largest stores/screens.

## Architecture rule for next work

Do not start another broad feature or visual redesign phase before hardening/decomposition. Preserve the current Home / Activity / Plan / Accounts / More product hierarchy and keep parser expansion evidence-driven.
