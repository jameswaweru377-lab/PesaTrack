# PesaTrack 2.0 Advanced — implementation tracker

Status: **Phase A foundation slice in draft review — NOT released or connected to live ingestion**  
Reference: **PesaTrack_2_0_Advanced_Development_Blueprint.pdf** (26-page planning document, 10 October 2026). The PDF is a proposal, not proof of implementation.

## Baseline (recheck at every subsequent chat / PR)
- Repository: `jameswaweru377-lab/PesaTrack`.
- Branch base: `main` at `f9730a5596f65b8233af4fb53cc40d4586fdcead` (10 October 2026).
- Current-state doc on this base: schema **v37**; live canonical accounting in `TransactionStore`, salary in `SalaryReceiptRepository` / `PayCycleStore`, CSV import in `EverydayMoneyStore`.
- Baseline Android CI: [successful main workflow 38072584173](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/38072584173); this **does not** prove this branch builds.
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
| A1 | Pure evidence source taxonomy and authority gate | **Implemented in feature branch; CI pending** | `FinancialEvidenceContract.kt` plus `FinancialEvidenceContractTest.kt` |
| A2 | Manual salary received vs planned semantics | Not started | Integration tests T01, T02, preserving existing SMS paths and totals |
| A3 | Import 1/2/3 **previous completed** months plus current | Not started | Date/timezone tests; no change to standalone historical reimport |
| A4 | Small ownership/skip-salary onboarding UX | Not started | Owned/external/unsure and skip journeys in Compose |
| B | Evidence repository/link audit, replay-safe statement/SMS/manual adapters | Not started | Migration/restore/idempotency and provenance; T03, T04, T12 |
| C | Payroll journal, deduction type/asset/liability classifications and multiple settlements | Not started | T05-T09, T16; no invented account balance |
| D | Offline local payslip PDF/image extractor, OCR fallback, field review | Not started | Local only, API24/low-memory, synthetic layout corpus, T14-T15 |
| E | Statement CSV/PDF adapters and safe reconciliation UX | Not started | T10-T13, account/replay/large-statement scenarios |
| Release | CI, lint, APK, migration/backup/restore, full physical device tests | Not started | T01-T20 passing where in scope; verified APK and user-reviewed merge |

## Current change and isolation
- Feature branch: `feature/pesatrack2-evidence-contract-v1`.
- Files added:
  - `app/src/main/java/com/kedevjimmy/pesatrack/domain/evidence/FinancialEvidenceContract.kt`
  - `app/src/test/java/com/kedevjimmy/pesatrack/domain/evidence/FinancialEvidenceContractTest.kt`
  - this tracker.
- No changes to `PesaTrackDatabase.kt`, schema, existing `TransactionStore`, Android permissions, Gradle, home screens, SMS capture, statement import, remote services or existing active PR branches.
- The new policy returns `RECONCILE_CASH_CANDIDATE`, **not** permission to insert a transaction. It is intentionally not wired into production yet.
- The unit tests validate source gates for manual received/planned, SMS, notifications, statement confirmation, unknown ownership, payslip payroll-only, and no assumed deposit. They **do not** establish complete end-to-end T01-T20 acceptance, duplicate safety or migrations.
- Review note: `docs/SALARY_UI_AND_CORRECTIONS.md` says manual-only receipts are excluded from totals; `TransactionStore.accountingEligible` does not explicitly exclude `sourceKind=manual_setup`. Verify the intended salary-vs-cash totals independently before Phase A2, rather than applying an untested fix.

## Next safe steps
1. Inspect **this branch's exact PR CI head SHA** for `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and APK artifact. Do not equate successful baseline/main CI with this PR.
2. Read and review every line of the new contract. Fix any unsafe source authority or validation logic without modifying `main`.
3. Test current manual salary behavior on synthetic seeded Android SQLite scenarios; resolve the docs/ledger total discrepancy and establish actual expected behavior **before** implementing A2.
4. Advance in separate narrow PRs after review, using the same tracker. No releases or merges without user approval and verification.

## Handoff contract to another chat
Read the attached blueprint PDF plus this tracker, then inspect live `main`/PRs and the actual code. Quote branch, base/head SHAs, changed paths, real CI run outcomes and unresolved acceptance IDs. Implement one thin slice at a time on an isolated branch. Preserve canonical ledger authority, no mandatory document uploads, no raw SMS or payslip upload to third parties. Report blockers precisely; no assumed green checks, APK, signed release or device acceptance.
