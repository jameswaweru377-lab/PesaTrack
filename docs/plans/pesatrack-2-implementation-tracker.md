# PesaTrack 2.0 Advanced — implementation tracker

Status: **A1/A3/A4 and approved onboarding welcome (#328) and account details (#333) merged. Separate M-Shwari savings consent PR pending verification.**  
Reference: **PesaTrack_2_0_Advanced_Development_Blueprint.pdf** (26-page planning document, 10 October 2026). The PDF is a proposal, not proof of implementation.

## Baseline (recheck at every subsequent chat / PR)
- Repository: `jameswaweru377-lab/PesaTrack`.
- Latest A3 branch base: `main` at `958fb2c5b231802a604cee325866d75abe9ddf10` (10 October 2026), merged A1.
- Current-state doc on this base: schema **v37**; live canonical accounting in `TransactionStore`, salary in `SalaryReceiptRepository` / `PayCycleStore`, CSV import in `EverydayMoneyStore`.
- A1 PR #321 branch CI was green at `82fd306`, APK produced; merge commit `958fb2c` now on main. Post-merge main CI [run 38075357968](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38075357968) must be checked independently. Neither certifies the A3 branch.
- Debt/receivable and budget UI PRs were no longer open on the audit; only dependency-update PRs #254-260 remained open.
- `main` was not protected on this audit. Do **not** merge, publish, or alter another agent's branches as part of this tracker.
- Repo authority: `AGENTS.md`, `CURRENT_STATE.md`, `docs/PRODUCT_CHARTER.md`, then current Kotlin and tests. Re-read all on a new baseline.

## Financial accounting invariants (permanent)
1. SMS, notifications, manual receipts, bank statements, and payslips are **independent optional** evidence sources. No forced upload or historical reimport to use the app.
2. A received manual salary can represent a genuine transaction. An expected or planned salary does not.
3. A payslip is payroll evidence, never sufficient proof that spendable money was deposited.
4. One economic movement has one canonical ledger effect even if observed via four sources. Reconcile identities before any insertion; ambiguous same-amount twins remain distinct/pending.
5. Do not treat own-account transfers, mirrored observations, loan principal, repayments, reversals or refunds as earned income.
6. A payroll deduction is not automatically bank spending. SACCO/pension deposits, taxes, healthcare contributions, insurance premiums and loan principal/interest require distinct semantic types and confidence.
7. A payslip-reported SACCO or pension contribution is **not** a verified institution account balance without independent posting evidence.
8. Do not claim an account is owned merely because its provider is named in an SMS/document.
9. Preserve user-authoritative ledger corrections, historical source labels and transaction replay safety.
10. Local-first, no raw financial documents or messages sent to plugins/cloud; never use real user data as repository fixtures.
11. Financial write batches must be atomic or safely replayable. Schema and backup/restore migrations must have tests.
12. Python remains offline development/test tooling; production Android/Kotlin remains financial authority.

## Work package and gate tracker
| Gate | Work | State | Exit evidence |
|---|---|---|---|
| Gate 0 | Inspect live main/PRs/schema/CI | Partly complete | Baseline CI green; Android instrumentation, physical Redmi and populated restore gate not yet independently checked |
| A1 | Pure evidence source taxonomy and authority gate | **Merged into main (#321); branch CI passed** | `FinancialEvidenceContract.kt` plus `FinancialEvidenceContractTest.kt` |
| A2 | Manual salary received vs planned semantics | Not started | Integration tests T01, T02, preserving existing SMS paths and totals |
| A3 | Import 1/2/3 **previous completed** months plus current | **Merged PR #325; branch CI passed** | Date/timezone tests; no change to standalone historical reimport |
| A4 | Small ownership/skip-salary onboarding UX | **Merged PR #327; post-merge main CI green** | Owned/external/unsure and skip journeys in Compose |
| B | Evidence repository/link audit, replay-safe statement/SMS/manual adapters | Not started | Migration/restore/idempotency and provenance; T03, T04, T12 |
| C | Payroll journal, deduction type/asset/liability classifications and multiple settlements | Not started | T05-T09, T16; no invented account balance |
| D | Offline local payslip PDF/image extractor, OCR fallback, field review | Not started | Local only, API24/low-memory, synthetic layout corpus, T14-T15 |
| E | Statement CSV/PDF adapters and safe reconciliation UX | Not started | T10-T13, account/replay/large-statement scenarios |
| Release | CI, lint, APK, migration/backup/restore, full physical device tests | Not started | T01-T20 passing where in scope; verified APK and user-reviewed merge |

## Current change and isolation
- A1 foundation: PR #321 was merged into main after green branch CI; production ingestion still does not use the new evidence contract.
- Current A3 feature branch: `feature/pesatrack2-onboarding-history-window-v1`.
- A3 files: new `domain/rules/OnboardingHistoryWindow.kt` and JVM test `OnboardingHistoryWindowTest.kt`; four onboarding scan/candidate cutoff calls and import-selection explanatory UI copy updated in `SmartOnboardingSetupScreen.kt`, plus this tracker. Existing `HistoryImportWindow` API, standalone historical reimport and other tests untouched.
- No changes to `PesaTrackDatabase.kt`, schema, existing `TransactionStore`, Android permissions, Gradle, home screens, SMS capture, statement import, remote services or existing active PR branches.
- The new policy returns `RECONCILE_CASH_CANDIDATE`, **not** permission to insert a transaction. It is intentionally not wired into production yet.
- The unit tests validate source gates for manual received/planned, SMS, notifications, statement confirmation, unknown ownership, payslip payroll-only, and no assumed deposit. They **do not** establish complete end-to-end T01-T20 acceptance, duplicate safety or migrations.
- Audit correction: `docs/SALARY_UI_AND_CORRECTIONS.md` says manual-only receipts are excluded from totals **when removed**, not while active. Existing `SalaryReceiptRepositoryTest` asserts manual receipt becomes genuine income and its removal subtracts it. A2 remains only the additional planned-vs-received UX/acceptance review; do not change correct accounting.

## Next safe steps
1. Inspect **the A3 branch's exact PR CI head SHA** for `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and APK artifact. Do not equate successful baseline/main CI with this PR.
2. Read and review every line of the new contract. Fix any unsafe source authority or validation logic without modifying `main`.
3. Preserve already-covered manual salary received vs removed behavior; add any missing expected-vs-received UI/contract coverage before A2. A3 must not alter standalone history or pay-cycle semantics.
4. Advance in separate narrow PRs after review, using the same tracker. No releases or merges without user approval and verification.

## Handoff contract to another chat
Read the attached blueprint PDF plus this tracker, then inspect live `main`/PRs and the actual code. Quote branch, base/head SHAs, changed paths, real CI run outcomes and unresolved acceptance IDs. Implement one thin slice at a time on an isolated branch. Preserve canonical ledger authority, no mandatory document uploads, no raw SMS or payslip upload to third parties. Report blockers precisely; no assumed green checks, APK, signed release or device acceptance.

## A4 ownership and salary consent work package
- Branch: `feature/pesatrack2-onboarding-ownership-consent-v1`, created from merged main `f2ecc30b2f24b6dab981406bc55464bdd01e0d82`.
- New pure Kotlin policy: Mine/Not mine/Unsure only acts on currently pending discovered entities; unknown keys and Unsure are ignored. Existing `FinancialUnderstandingStore.confirmEntityOwned` and `markEntityExternal` implement explicit choices, without new schema or SMS parsing.
- Onboarding now exposes three explicit account decisions in the existing branded Compose card, above Continue; manual salary remains optional.
- Salary suggestions now start unconfirmed and stay unconfirmed unless explicitly accepted. A refresh only retains an accepted salary if the full proposed salary observation remains equal; changed suggestions require new consent, never automatic salary-rule creation.
- Added JVM tests for excluded/unsure decisions and salary consent plus a Compose interaction test (instrumentation compilation in CI; device execution remains a separate gate).
- This PR must pass `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, and repository financial contract tests on its own exact head before merge.
- Release caution: additional on-device UI / ownership-reconciliation tests are still required at later release checkpoints. A green branch build alone does not assert every real-data scenario is covered.

## Approved seven-screen onboarding redesign (implementation in progress)
- User approved proposed conceptual seven-screen journey: Welcome > Optional SMS scanning > Confirm detected institutions/accounts > M-Pesa account identity/locked savings > Other bank/SACCO identity > Optional salary > Finish. Reuse the actual PesaTrack theme; **do not use green hardcoding**.
- First isolated work package `feature/pesatrack2-onboarding-welcome-scan-v1`, base main `97450be869c01ea30e16266c18c9ee49cd49bad8` (post-merge main CI [run 38077829487](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38077829487) green).
- Implements real native Compose welcome with optional skip, moves import-month selection BEFORE requesting permissions, permission continuation directly into scan/analysis, improves seven-step progress narrative; puts institution directory behind secondary **Add a missing bank or service** in detected-account review. Existing permission consent, import and ledger processing reused. No mandatory SMS.
- The existing M-Pesa confirmation, identity management, salary, notification and completion screens remain in place until safe new counterparts are connected and tested. **Do not mark all seven screens implemented**.
- Planned subsequent coherent work packages: A4b detected institution/account reconciliation UI and profile identity flow (name only a hint, never ownership proof); A4c distinct M-Shwari Lock Savings semantics and optional bank/SACCO details; A4d salary/finish integration, UI regression and device acceptance.
- Exact branch checks required: unit tests, lint, APK, existing financial contract tests, instrumentation source compilation. Emulator execution requires separate explicit user request. Do not merge until CI passes.

## Account-details screen (approved onboarding next slice)
- Branch `feature/pesatrack2-onboarding-account-details-v1` based on `f3330792af1077fa852fe34a38a9bac7e159b910` (#328 merged). Check main run `38080338842` before merging.
- Adds optional bank/profile screen after existing M-Pesa confirmation or detected-account review. Reuses local profile-name preference `pesatrack_preferences.profile_name` and `AccountStore.updateIdentifier`.
- Only confirmed bank accounts display masked identifiers; a user must explicitly type **complete** account reference (minimum six alphanumeric characters). Four-digit suffix alone is not taken as evidence or used for matching. Name alone does not cause own-transfer classification.
- Skip does not persist any data. No new schema, server, permissions or SMS reclassification logic. Synthetic JVM validation tests added.
- Distinct M-Shwari Lock Savings account confirmation and a dedicated optional salary page remain to be implemented; do not mark the full seven-screen design complete. Await exact-head CI and check post-merge main before subsequent PR.

## Linked M-Shwari savings consent (approved onboarding 3/3, 10 October 2026)
- Base: main `0b154129013a9f8260483592112b4a7bac3edf68` with successful [Android CI run 38081402929](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38081402929).
- Branch: `feature/pesatrack2-onboarding-mshwari-consent-v1`.
- M-Pesa phone confirmation no longer automatically marks M-Shwari Savings or M-Shwari Lock Savings as owned. Ingestion evidence elsewhere remains authoritative; this change does not rewrite it.
- A separate optional savings confirmation screen asks Mine / Not mine / Unsure for **each** of the two distinct products and routes to the existing bank-identity page. Skipping writes nothing.
- Explicit Mine uses the existing account registry and reconciliation; explicit Not mine demotes an existing financial entity; unknown/unsure does not create an account.
- No schema, parser or SMS permissions changes. Unit tests cover independent product ownership and absence of implied M-Shwari accounts when M-Pesa is confirmed.
- Manual salary remains available earlier in review; a dedicated final salary page and ordering to match the seven-screen concept remains a *future separate work package*. Do not claim all screens finished.
- Requires exact-head PR unit tests/lint/APK and eventual post-merge main CI; instrumentation/device flows unverified until explicitly run.

## Final approved onboarding salary screen (next PR after #335)
- Base: merged M-Shwari consent #335 at `d4d43f104368159e683f1fe1754fe08438ab2c90`; post-merge main CI [run 38083542774](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38083542774) must be confirmed before merging the next PR.
- Branch: `feature/pesatrack2-onboarding-salary-finish-v1`.
- Places optional salary choice **after** account details and before setup completion. Review Detected Accounts no longer asks for salary in the same screen.
- Existing salary receipt form remains authoritative: accepted detected salary only creates/reapplies a salary **rule**, not a second cash receipt; manual entry requires money actually received.
- No schema, data parser, new permission or off-device financial evidence. Test initial skip, declining suggested salary, source matching, routing and user intent. A feature-branch CI build does not substitute for emulator/Redmi acceptance.

## Phase B1 — pure evidence matching contract (unwired, no DB migration)
- Branch: `feature/pesatrack2-evidence-match-policy-v1` from main `d5b22af8a7ecc11e0261134e538934797a4def87`. Post-onboarding main CI run [38084525308](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38084525308) pending final confirmation at branch creation.
- Adds an offline pure policy comparing two cash observations with source-scope/native-key replay identity, confirmed account, direction, positive amount, currency, date window and independent transaction references. It returns **review candidates**, never direct ledger authorization.
- A shared reference across sources is only a strong candidate pending unique canonical linkage; same amount/time without a common reference is review only. Different references or accounts are distinct; source identity with contradictory amount/date/ownership is conflict. Payslips and planned income cannot produce cash matches.
- Unwired to `TransactionStore`, SMS ingest, CSV importer and SQLite. No schema, salary, report, backup or totals changes in B1. Existing source-specific dedupe remains active until B2 design/migration tests pass.
- B2 will require additive evidence_observations/evidence_links persistence, foreign keys, unique source scope/native key, account id provenance, migration from v37, backup/restore and correction/replay tests before any engine write. B1 by itself does not complete Phase B.

## Approved seven-screen visual correction — checkbox account review
- Audited `main` at `d5b22af8a7ecc11e0261134e538934797a4def87`: original design image had a single compact checkbox list, not two sets of cards and Mine/Not mine/Unsure chips. Existing permission flow remains in `SmartOnboardingPermissionComponents.kt` and must not be deleted.
- Branch `feature/onboarding-approved-checkbox-review-v1`: unifies detected sources and pending account candidates into one compact **unchecked by default** checkbox list. Only an explicit tick authorizes an account. Checking M-Pesa routes to phone confirmation; it is NOT auto-confirmed from SMS.
- Distinct M-Shwari/Lock Savings confirmation remains separate. Other approved mockup layout differences (M-Pesa detail form, bank detail cards, salary radio controls) are **not yet verified as pixel-matched** and require next isolated UI packages.
- Exact-head Android unit/lint/APK CI, Compose test-source compilation, regression and main post-merge build must pass. Device visual acceptance not yet performed.
