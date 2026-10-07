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
3. Do not merge until Android CI is green.
4. Prefer small migrations and decompositions over broad rewrites.
5. Update current-state documentation when behavior or release readiness changes.

## Required checks

For Android changes, run or rely on CI for:
- JVM unit tests;
- Android lint;
- debug APK assembly;
- Android instrumentation.

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
