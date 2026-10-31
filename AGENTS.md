Before any task, read AGENT_WORKING_METHOD.md and follow it. If it conflicts with another document, the code and test evidence win, and you must fix the document in the same change.

# PesaTrack — Agent Development Instructions

## Mission
Build a trustworthy, local-first Android financial companion for Kenya. Prioritize accounting correctness, privacy, reliability, and maintainability over feature count.

These instructions apply to AI-assisted development in this repository, including supported Codex and ChatGPT workflows.

## Read before modifying code
1. Read README.md, CONTRIBUTING.md, CURRENT_STATE.md, and docs/PRODUCT_CHARTER.md.
2. Inspect the actual code, relevant tests, current branch, and open PRs.
3. Treat current implementation and verified test evidence as more authoritative than outdated historical notes.
4. Preserve existing product decisions unless the user explicitly approves a change.

## Available plugin guidance
Use installed, authorized tools when useful and supported by the current environment.

- GitHub: Inspect source, issues, PRs, commits and CI. Prefer targeted changes on separate branches.
- Context7: Consult current public Android, Kotlin, Jetpack Compose, Gradle or dependency documentation when API accuracy matters. Never include private financial data in queries.
- Superpowers: Apply available planning, systematic debugging and test-driven development guidance.
- Figma: Inspect approved design context or prepare editable UI proposals when visual work is required.
- MagicPath: Explore interactive UI alternatives in isolated prototypes; do not replace native Android implementation with web prototype code.
- Miro / tldraw: Use when architecture diagrams, workflows or user journeys improve understanding.
- Vercel: Use only for approved Vercel-hosted components. Never assume the Android ledger belongs in the cloud.
- PostHog: Do not activate financial telemetry, event collection or third-party analytics without explicit user approval.
- Runway / Descript / Metricool / vidIQ: Use only for separately requested media or marketing work.

A plugin may be unavailable in a particular ChatGPT or Codex environment. Check availability rather than claiming it was used. Do not invoke tools unnecessarily.

## Financial correctness
- Keep income, expenses, self-transfers, mirror observations, loans, repayments, fees, refunds, reversals and savings movements distinct.
- Never count the two sides of an owned-account transfer as additional income or spending.
- Never treat loan principal as earned income or automatically turn debt repayment principal into a new purchase.
- Do not invent balances, debt amounts, transaction identities or unsupported financial facts.
- Preserve user-authoritative corrections and deterministic replay/idempotency.
- Ambiguous financial evidence must remain uncertain.
- Keep provider-specific message language in bounded evidence and grammar, not expanding provider-specific business-logic chains.
- Preserve atomic ledger writes and tested migrations.

## Privacy and security
- Treat SQLite on the Android device as the financial source of truth.
- Never transmit raw financial SMS, personal transactions, account identifiers, balances, credentials or sensitive user data to third-party plugins or remote AI services.
- Use sanitized synthetic Kenyan financial examples for tests and external research.
- No new telemetry, permissions, cloud synchronization or external data processing without explicit approval.
- Do not commit secrets, signing keys or production credentials.

## Engineering workflow
1. Identify the requested change and acceptance criteria.
2. Inspect existing behavior and related regressions before editing.
3. Choose the smallest safe implementation.
4. Implement deterministic tests and relevant negative cases.
5. Verify code with appropriate local checks and CI.
6. Review the diff for unintended financial, security and UI effects.
7. Report what changed, what passed, what remains unverified and what should happen next.

Prefer focused fixes and architecture hardening over unapproved feature expansion.

## Incremental architecture during feature work
Apply these rules when completing an approved feature or fix. Improve only the relevant code; do not create a separate whole-app refactoring project.

1. Inspect the responsibilities in files being changed. Extract closely related, testable pieces when this makes the requested work safer; do not refactor unrelated areas or enforce arbitrary file-splitting for its own sake.
2. Keep Compose screens focused on display and user interaction, domain policies focused on financial rules, and repositories/stores focused on data access. Reuse a single authoritative calculation or classification instead of copying logic between screens or sources.
3. Do not introduce Hilt, Room, a new framework, or a database replacement as a routine cleanup. Any migration needs a demonstrated benefit, a separate risk review, and the owner's approval when it affects user data or compatibility.
4. Preserve approved UI designs, the four-tab navigation, local-first privacy, ledger meaning, backup/restore compatibility, and existing behavior. An architectural extraction must not silently change financial results or user decisions.
5. Add focused regression tests for extracted logic and compare before/after behavior. Follow the existing financial, replay, migration, and device-verification rules where relevant; report what was not tested.
6. Check current `main`, open PRs, and active feature ownership before editing shared files. Work in a scoped branch; do not overwrite another chat's changes, and stop to coordinate when overlap cannot be handled safely.
7. Keep ordinary CI fast: reuse the current JVM tests, lint, and APK checks. Do not add slow mandatory checks or enable instrumentation without an explicit task-specific request.

## APK update continuity — required for future releases
- The user installs PesaTrack directly on a Redmi phone and needs **in-place APK updates without losing local transactions, statements, settings or categories**. Protect this requirement in every change to Android builds, package identity, CI or releases.
- Android decides whether an APK can update the installed app using the same application ID, a compatible signing identity (certificate or supported signing lineage), and a non-downgrade `versionCode`. APK filenames such as `fresh.apk` or `update.apk` are only labels; they do not make an APK upgrade-compatible.
- **Rolling update implementation (29 October 2026):** `.github/workflows/android-ci.yml` retains debug validation and builds a release-signed APK on successful `main` builds. The `ci-apk` GitHub release must have **exactly two assets**: `PesaTrack-Full.apk` and `PesaTrack-Update.apk`. These are byte-identical copies of one signed APK, replaced each time; no per-commit APK attachments or accumulated history. A new `versionCode` is mandatory, even though filenames stay fixed. The publish script fails when required signing secrets are missing, the signer differs from an earlier signed update, or the version does not increase; **never** fall back to debug or unsigned APKs. Actual secret provisioning and phone compatibility still need evidence.
- Preserve the existing Android application ID. Keep signing material in protected GitHub secrets with a separately protected offline recovery copy; never put keys or passwords in source, output or logs. The independent `release-build.yml` signed manual workflow still exists.
- **Before migrating an existing phone installation:** compare the installed APK signing certificate to the proposed update. If they differ, do not ask the user to uninstall until a private, encrypted backup/restore path is verified and the user explicitly approves a one-time migration. There is no safe assumption that an old runner's debug key can be recovered.
- Do not describe GitHub CI passing as proof of on-device update compatibility. Record the checked commit, APK signing fingerprint, application ID, version code, and real-phone upgrade result (if actually tested). Do not silently switch release channels or replace the user's installed app.
- See [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md) for the update policy, current limitations and safe rollout checks. Changing workflows, signing or publishing releases requires separate task-specific approval.

## Concurrent development
- Before editing, check the canonical task Issue and its `Chat owner: CHAT-<TOPIC>` claim using [the development tracker](docs/tracker/README.md). Put `Tracks #<issue>` and the same alias in the PR; treat overlapping claims and paths as coordination warnings, not permission to overwrite another chat.
- Do not modify another agent's active branch, worktree, PR or in-progress task.
- Check the target branch before writing.
- Keep unrelated changes on separate branches and PRs.
- Do not merge, release, delete branches, change workflows or alter signing configuration without task-specific authorization.
- If concurrent changes overlap, identify the conflict before implementation.

## Verification and completion
For Android changes, use the repository's documented unit tests, lint and APK assembly checks. Run instrumentation or physical-device tests when required.

Financial changes also need synthetic regression fixtures, negative cases, duplicate/mirror safety, replay and migration checks where relevant.

Do not claim tests, CI, deployments or APK publication succeeded without evidence. Report the checked commit SHA and exact remaining validation.

Documentation-only changes must not be represented as application builds.

## Final report
Always summarize:
- Verified facts
- Files changed
- Tests and CI evidence
- Remaining risks and unverified behavior
- Recommended safest next step
