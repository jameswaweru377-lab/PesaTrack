# PesaTrack 2.0 Advanced — implementation tracker

Status: **A1, A3 and A4 merged; approved seven-screen onboarding redesign underway in staged PRs. Welcome + scan sequencing + secondary missing-institution route is current draft.**  
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
