# Contributing to PesaTrack

PesaTrack is a local-first Android personal-finance application. Changes must preserve accounting correctness, privacy and deterministic financial interpretation.

## Before changing code

Read:
- `README.md`
- `CURRENT_STATE.md`
- `docs/PRODUCT_CHARTER.md`
- `AGENTS.md`
- `DESIGN.md`
- `docs/AUDIT_REMEDIATION_TRACKER.md`

Historical project notes are useful context, but current code and `CURRENT_STATE.md` take precedence when they conflict.

## Branch and pull-request workflow

1. Branch from current `main`.
2. Keep one coherent risk area per pull request and batch related edits before review.
3. PR updates and merges run build, lint, unit tests and debug APK assembly. These checks must pass before merge.
4. Emulator instrumentation runs only when the user explicitly requests it for the current task. Default to build, unit tests, lint and APK assembly, then publication on a successful push to main. Do not enable automatic instrumentation for a PR, add it as a publication dependency, or run it to satisfy an older plan without the user's current request. Marking a PR ready or pushing more edits does not start emulator tests.
   The user reinstated the instrumentation pause during B2 PR #341. The temporary branch exception has been removed. Do not dispatch emulator review until a new explicit request resumes it; record incomplete emulator acceptance honestly.
5. For Android device review, use Actions → Android CI → Run workflow on the feature branch and enable `run_instrumentation`. Inspect the successful run's source revision; skipped instrumentation means no device tests ran.
6. Device compatibility and Android 7 security workflows are manual review tools. Their existing emulator suites are retained; weekly automatic compatibility runs are disabled.
7. Prefer small migrations and decompositions over broad rewrites.
8. Update current-state documentation when behavior or release readiness changes.

## Required checks

For every Android change, run or rely on CI for:
- JVM unit tests;
- Android lint;
- debug APK assembly.

Run Android instrumentation when explicitly requested for device review. Signed releases also default to build-only validation; enable run_instrumentation only after the user explicitly requests emulator review.

The debug APK is available as the `pesatrack-debug-apk` workflow artifact after build checks succeed. Successful `main` pushes also publish one **release-signed** APK under exactly two rolling attachment names: `PesaTrack-Full.apk` and `PesaTrack-Update.apk`. These are identical APKs, not two different patch formats. The `ci-apk` attachments are replaced on each successful signed publication; signing secrets, certificate continuity and increasing version codes are required. A manual Android CI run defaults to build-only; enabling `run_instrumentation` adds emulator tests. Manual runs retain artifacts and do not overwrite `ci-apk`. This private preview is signed, but CI does not prove an in-place upgrade or data retention on a real phone. Read `docs/RELEASE_PROCESS.md` before APK-signing changes.

Build-only and manual emulator-review runs use separate concurrency groups so editing does not cancel an active manual review.

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

Preserve the four visible bottom tabs **Home / Activity / Plan / More** unless a separately approved product change requires otherwise. Accounts is an internal destination accessible from More/Profile, not a fifth bottom tab. Follow `DESIGN.md` for the current light-first purple direction, and keep historical emerald/dark proposals as history. New strings should increasingly move to Android string resources as localization/accessibility work progresses.
