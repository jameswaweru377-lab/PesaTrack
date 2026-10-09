# Contributing to PesaTrack

PesaTrack is a local-first Android personal-finance application. Changes must preserve accounting correctness, privacy and deterministic financial interpretation.

## Before changing code

Read:
- `README.md`
- `CURRENT_STATE.md`
- `docs/PRODUCT_CHARTER.md`

Historical project notes are useful context, but current code and `CURRENT_STATE.md` take precedence when they conflict.

## Branch and pull-request workflow

1. Branch from current `main`.
2. Keep one coherent risk area per pull request.
3. Keep work in a draft PR while making a batch of edits. Build, lint, unit tests and a downloadable debug APK run on each PR update.
4. Mark the PR ready for review when the batch is complete. This runs Android instrumentation; opening a non-draft PR also runs it.
5. Later pushes run build checks only, even on a non-draft PR. Before merging changed Android code, rerun instrumentation for the latest revision by converting the PR to draft and marking it ready again. Check that the successful run covers the current PR head and current base. Do not interpret a skipped instrumentation job as a passed device test.
6. Alternatively, use Actions → Android CI → Run workflow on the feature branch with `run_instrumentation` enabled to test its latest head. Use the draft/ready transition to verify the PR merge result against current main.
7. Do not merge Android code until both build checks and fresh instrumentation pass. Configuration-only changes require workflow validation and build checks; they do not change app behavior.
8. Prefer small migrations and decompositions over broad rewrites.
9. Update current-state documentation when behavior or release readiness changes.

## Required checks

For Android changes, run or rely on CI for:
- JVM unit tests;
- Android lint;
- debug APK assembly;
- Android instrumentation.

The debug APK is available as the `pesatrack-debug-apk` artifact after build checks succeed, including while instrumentation is skipped or running. It is a development preview. The rolling APK on `main` is published only after both build and instrumentation succeed for that run; signed releases retain their Android validation gate. A manual Android CI run with `run_instrumentation` disabled builds a preview APK without publishing it.

Review runs and build-only PR updates use separate concurrency groups so a new edit does not cancel an emulator review already in progress. Changes made after that review still need a fresh review run before merge. The targeted legacy-security, compatibility and release-preflight emulator jobs follow the same review timing; weekly compatibility and manual release validation remain enabled.

Worker changes must also pass the Worker syntax/contract test and dry-run deployment validation.

## Financial-intelligence rules

- Unknown evidence stays uncertain; do not guess.
- User-authoritative corrections must not be silently overwritten.
- Keep provider-specific message wording in bounded parsing evidence, not business-logic `if/else` chains.
- Preserve replay/idempotency and transaction rollback behavior.
- Use synthetic/redacted fixtures only.

## Privacy rules

Raw financial SMS, amounts, balances, phone/account numbers and transaction codes must not be sent to optional enrichment services.

Any new network use must be:
- necessary and documented;
- off by default when optional;
- disclosed before sensitive-data access;
- covered by tests where practical.

Never commit secrets, signing material or production credentials.

## UI changes

Preserve the current Home / Activity / Plan / Accounts / More hierarchy unless a separately approved product change requires otherwise. New strings should increasingly move to Android string resources as localization/accessibility work progresses.
