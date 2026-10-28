# Equity-first Statements Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status (28 October 2026):** James approved proceeding. The v5 bank archive, backup compatibility and provider-neutral reporting foundation passed Android CI in draft PR #464. After James privately supplied an encrypted Equity PDF and its opening password, an in-memory local extraction verified its full account/header structure, opening balance chain and printed page/grand totals. A guarded Android Equity PDF importer, review-and-save flow and bank account overview are now added to the *same draft PR branch*, pending refreshed Android CI and on-device acceptance. No private source data or password enters code, tests, git or CI.

**Goal:** Admit one verified Equity Bank Kenya statement layout into PesaTrack's isolated Statements history, give it the approved premium account-specific insights and source-detail UI, and prepare the archive/read model for future banks without regressing M-Pesa or Activity.

**Architecture:** Extend the existing statement archive with additive provider-neutral source/leg tables and a versioned backup contract; leave M-Pesa's old rows intact. Route selected documents through a provider-profile registry, lossless offline table extraction, existing universal interpreter, explicit identity/balance checks, and a read-only unified report adapter. One bank account is visible at a time; combined accounts and mirror totals remain Phase 3.

**Tech Stack:** Kotlin 2.4.20, Compose Material 3, Android minSdk 24/target 36, SQLite, embedded Chaquopy/pdfplumber 0.11.9, JUnit4/JVM and Compose Android tests, Gradle 9.5/AGP 9.3.1.

**Spec:** `docs/superpowers/specs/2026-10-28-equity-statements-phase2-design.md` (proposed Phase 2 design and source-backed architecture audit). The Phase 1 approved UI/PDF and `docs/superpowers/plans/2026-10-28-statements-phase1-mpesa-ui.md` remain visual and behavior constraints.

## Global Constraints

- Do not mutate an Activity/ledger entry merely by reviewing or saving a bank statement. Explicit Activity import stays separate and review-gated.
- The current M-Pesa PDF parser, saved rows, manual categories, account IDs, Home and Activity designs must behave identically after migration.
- Only confirmed owned accounts may be linked; never trust a bank title or a filename to prove ownership. No fabricated provider, merchant, receipt, date, balance or category data.
- Preserve the original source row-to-cell mapping; fail a whole import when an amount, fee, balance or source region cannot be accounted for.
- No provider-specific growing `if/else` engine. A provider profile chooses bounded header aliases, statement identity proof and totals requirements; common accounting and UI remain generic.
- Do not offer a clickable Equity PDF button until at least one *real layout* has a validated fixture and full-device admission path. If no sample is supplied, stop at the fixture gate and report unsupported input honestly.
- No raw statements, passwords, bank account numbers or unredacted extracts in git, CI, logs, plugin calls or fixtures; keep real document evaluation on-device.
- Phase 2 shows **one account at a time**. All-accounts totals, KCB and other banks require independent Phase 3/extension approval.
- CI is grouped once when the implementation is ready (`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`). Device instrumentation is explicitly requested before execution under `CONTRIBUTING.md`.

## Review Focus

1. **Incomplete source:** printed Page Total/Grand Total, unknown `Value` column, standalone SMS charge, `Cr`-suffixed balances, and footer outside a discovered table must block partial save until completely understood. Covered by Tasks 0, 2–4 and 8.
2. **Wrong account:** two Equity accounts, masked account number or a manually selected non-Equity bank may never silently attach a file to the wrong account. Covered by Tasks 2 and 5.
3. **Overlapping histories:** re-importing the same file is idempotent; partly overlapping PDFs cannot double-count in the chosen month's dashboard. Covered by Tasks 5–6.
4. **Money meaning:** an owned-wallet transfer, ATM withdrawal, loan disbursement/repayment, reversal or charge cannot become a grocery expense or salary by text guesswork. Covered by Tasks 6–7.
5. **Restore/regression:** existing v3/v4 backups, existing M-Pesa v4 rows and new Equity v5 rows survive upgrade, export, restore and rollback. Covered by Tasks 1, 5 and 8.

---

## File and responsibility map

| File / module | Purpose |
| --- | --- |
| `security/StatementBackupArchive.kt`, `security/PesaBackupManager.kt` | Version-aware current/legacy archive backup/restore, strict table proof and rollback |
| `data/local/StatementArchiveDb.kt` (new, extract existing helper) | One authoritative SQLite helper for old M-Pesa and new provider-neutral tables; v4→v5 migration |
| `data/local/StatementActivityStore.kt` | Keep old M-Pesa journal operations and existing behavior |
| `data/local/StatementBankArchiveStore.kt` (new) | Atomic immutable provider/account/source/leg insertion, dedup, review corrections, querying |
| `domain/statements/StatementSourceProfileRegistry.kt` (new) | Provider-key profiles; Equity aliases and source identity/coverage requirements |
| `domain/statements/StatementBankAdmission.kt` (new) | Source-neutral accepted/block state, accounting and account-identity proof contract |
| `documents/OfflineBankTableReader.kt`, `app/src/main/python/bank_table_extractor.py` (new) | Offline, bounded PDF table evidence without M-Pesa-specific assumptions |
| `domain/statements/StatementTableWire.kt`, `UniversalStatement*` (existing) | Reuse/extend versioned source table transport and strict semantic interpreter as evidence demands |
| `data/local/UnifiedStatementReportReader.kt` (new) | Per-account read adapter over preserved M-Pesa sources and newly verified bank sources |
| `ui/screens/StatementImportScreens.kt` | Existing source-account/import path; add an Equity choice/preview conditionally, preserve M-Pesa flow |
| `ui/screens/StatementIntelligenceDashboard.kt`, `StatementInsightModels.kt`, `StatementDeepDivePages.kt` | Feed real provider-neutral account report into existing approved components; preserve Home/Activity/navigation |
| `app/src/test/**/statements/*`, `app/src/androidTest/**` | Pure parser/evidence/money tests plus SQLite migration, ownership, import and UI contract tests |

## Task 0: Equity screenshot contract and full-source admission proof — **blocking before enabling the importer**

**Files:** Add structurally faithful *synthetic* test fixtures under `app/src/test/resources/statement-lab/equity/`; retain any real sample only locally outside git.

- [ ] Use the **observed PDF screenshot contract**: six columns in order `Date | Value | Particulars | Money Out | Money In | Balance`; `dd-MM-yyyy` dates; bank KES account heading; repeated page headers; separate transaction/SMS charge legs; running balances with `Cr` suffix; `Page Total`, `Grand Total`, `Uncleared Cheques` and free-text footer. Never copy visible personal names, references, account numbers or amounts from the screenshot into fixtures.
- [ ] Confirm whether `Value` is a value-date or another semantic field, whether `Dr` balances can appear, whether the PDF has selectable financial text, whether summaries/totals reconcile across pages, and whether an account identifier is independently visible for ownership proof. The screenshot by itself cannot prove these.
- [ ] Make a safe synthetic replica retaining the **same table grammar** but replacing all private identities, dates and amounts with invented, internally reconciled values. Include **two pages**, repeated headers, a separate SMS charge, balance `Cr` suffixes, a wrapped description if applicable, all three footer groups, and notes outside the table.
- [ ] Define an explicit expected result: source rows, cash debits, credits, running balance trail, currency, fees, opening/closing balances and source-cell inventory. Determine whether the format has enough evidence for a complete import.
- [ ] If the source is image-only, lacks a verifiable balance/account identity, or contains unsupported fees, record that it is **not yet supported**; ask James before expanding scope. No heuristic partial-import fallback.

## Task 1: Repair old backup contract and design lossless v5 migration

**Files:** Modify `security/StatementBackupArchive.kt`, `security/PesaBackupManager.kt`, `data/local/StatementActivityStore.kt`; create `data/local/StatementArchiveDb.kt` when extracting the helper; extend `security/StatementArchiveBackupIntegrationTest.kt`.

**Interfaces:** `StatementArchiveDb(context).readableDatabase/writableDatabase`; `StatementBackupArchive` exposes current schema and accepted *legacy archive payload* schema versions explicitly; data migration never rewrites old M-Pesa records.

- [ ] Write Android tests proving the current v4 live archive fails against old backup schema 3 and that an existing supported v3 payload can be restored losslessly. Add a no-data-loss rollback test and a mixed-old/new backup test.
- [ ] Verify the failure in an authorized Android instrumentation run. A baseline production `main` JVM/CI build alone does **not** prove archive-restore behavior.
- [ ] Implement version-aware archive decoding/validation and atomic restore: accept old supported archive formats with their **original expected table sets**, migrate them into the current DB transaction, then validate. Never simply change `SCHEMA_VERSION` to 4 or 5.
- [ ] Extract/reuse one SQLite open helper, add v5 tables/indexes with foreign keys, preserve v1→v4 migrations, and make the current backup include all new tables/cells. Keep encrypted payload handling unchanged.
- [ ] Run the backup integration matrix: old payload/current app, v4 live data→new backup, v5+Equity round-trip and interrupted/corrupt restore. Stop on any data-loss or incomplete backup.
- [ ] Commit this isolated, fully tested migration within the **one proposed Phase 2 implementation branch**, with no UI changes in this commit.

## Task 2: Reusable provider, account and input-admission policy

**Files:** Create `domain/statements/StatementSourceProfileRegistry.kt`, `StatementBankAdmission.kt`; extend `domain/statements/UniversalStatementTableInterpreterTest.kt`; create `StatementSourceProfileRegistryTest.kt`.

**Interfaces:** `StatementSourceProfileRegistry.forProvider(providerKey: String): StatementSourceProfile?` and `StatementBankAdmission.verify(profile, selectedAccount, extractedSource, tableResult): StatementAdmissionResult` are **proposed signatures**, finalized against the sample. `StatementSourceProfile` includes format support, header aliases, currency and document identity/summary contracts; it does not contain executable provider-specific business logic.

- [ ] Write negative JVM tests: unknown institution, wrong owned account, ambiguous/masked source account, incompatible currency, unrecognized fee and contradictory statement totals must return a **blocked reason**, never accepted rows.
- [ ] Implement mapping from existing confirmed `MoneyAccount`/provider identity into source profiles. Select Equity via existing `KENYAN_BANK_EQUITY`; do not create new fake or duplicate accounts.
- [ ] Reuse `StatementFormatProfile` aliases with the observed Equity table headers only; reject ambiguous or unrecognized financial columns.
- [ ] Test that adding a second declarative profile would require no changes to the interpreter, archive schema or navigation condition tree.
- [ ] Commit provider contract and tests on the same feature branch.

## Task 3: Offline Equity extraction plus strict structural proof

**Files:** Create `documents/OfflineBankTableReader.kt`, `app/src/main/python/bank_table_extractor.py`; modify `domain/statements/StatementTableWire.kt` only if provenance transport requires versioning; extend `UniversalStatementCsvTest.kt`, `StatementTableDiscoveryTest.kt`, `UniversalStatementGeometryTest.kt` and new extractor contract tests.

**Interfaces:** `OfflineBankTableReader.inspect(context, uri, password, profile): StatementExtractionResult`; extraction produces complete ordered `StatementTablePage` cells and bounded provenance, or a typed blocked reason. `UniversalStatementTableInterpreter.interpret(...)` remains the authoritative leg/balance interpreter.

- [ ] Write failing synthetic tests for the observed Equity layout: repeated headings across two pages, `Date/Value/Particulars/Money Out/Money In/Balance`, wrapped descriptions, money-in/out, **`Cr` and (if proven) `Dr` balances**, standalone SMS charge, page/grand totals, uncleared cheques and non-transaction footer. If supported by the real export, add encrypted PDF and CSV equivalence tests.
- [ ] Add rejection tests for dropped/unassigned PDF fragments, unknown money columns, an unproven `Value` column, nonzero fee without a proven leg, totals incorrectly treated as transactions, malformed quotes, ambiguous dates, missing balance chain, image-only PDF and oversized file.
- [ ] Implement a bounded **on-device** pdfplumber extractor for supported API 24–34 and 35+, without network calls; keep `OfflineMpesaPdfReader` and its Python engine untouched. Use the existing interpreter and geometry/table discovery rather than a new bank-specific parser chain.
- [ ] If source cell provenance cannot cross the Python/Kotlin boundary intact, introduce a versioned wire/proof contract and test round-trip integrity. Reject incomplete proof; do not manufacture coordinates or skip footer text.
- [ ] Run focused JVM tests and local sanitized extraction contract tests; then test the actual private source *on-device* if authorized.
- [ ] Commit extractor and tests together.

## Task 4: Equity financial admission and verified preview

**Files:** Modify `StatementBankAdmission.kt`; add `StatementBankAdmissionTest.kt`; create `ui/screens/StatementBankReviewPanel.kt` only if the current import workspace cannot show the existing PDF review flow.

**Interfaces:** `StatementAdmissionResult.Accepted` includes account, provider, currency, verified source id, dated legs, balance/totals evidence and explicit coverage metadata. `Blocked` contains only a safe reason/page/row, not raw sensitive values.

- [ ] Write tests for exact source sum and balance reconciliations; distinction between money-out and classified spending; fees, reversals, credit/debit direction, multi-leg reference and account mismatch.
- [ ] Implement account identity and totals proof before any save operation. A selected bank account alone is not proof that an uploaded statement belongs to it.
- [ ] Show user-readable source row counts, coverage and balance/fee verification status; the user must explicitly approve the *Statements-only* save.
- [ ] Test failure leaves all previous archives and Activity untouched; cancellation or wrong password zeroizes temporary secrets where possible.
- [ ] Commit proof/preview tests and policy.

## Task 5: Immutable bank archive, dedup and v5 backup acceptance

**Files:** Create `data/local/StatementBankArchiveStore.kt`; extend archive backup tables in Task 1; add `data/local/StatementBankArchiveIntegrationTest.kt` plus backup integration tests.

**Interfaces:** `save(accountId: String, admitted: StatementAdmissionResult.Accepted): SaveResult`; `sourcesFor(accountId)` and `legsFor(sourceId)` are bounded read-only operations. Source/leg identities are stable across repeated import and backup restore.

- [ ] Write Android tests: duplicate Equity document is idempotent; overlapping files are both stored but never double-counted in the report; same reference can occur in distinct financial legs; wrong account/provider rejected; failed insert leaves no partial source.
- [ ] Create additive `statement_sources_v2`, `statement_legs_v2` and source-scoped correction table in v5 with indexes, constraints and provenance. Do not copy, reclassify or delete legacy M-Pesa rows.
- [ ] Implement atomic insert and strict source hash, record selected account/provider and verification evidence. No canonical Activity writes or balance changes.
- [ ] Verify v5 encrypted backup/export/restore contains both bank and M-Pesa rows with exact counts, categories and source provenance. Verify v3/v4 payload compatibility separately.
- [ ] Commit archive and migration tests.

## Task 6: One-account report and safe category interpretation

**Files:** Create `data/local/UnifiedStatementReportReader.kt` and `domain/statements/StatementReportItem.kt` (generic row); adjust `ui/screens/StatementInsightModels.kt`, `domain/everyday/StatementInsightProjection.kt`, and related presentation tests only as required; preserve `StatementReportReader` for M-Pesa compatibility.

**Interfaces:** `UnifiedStatementReportReader.read(accountId: String): StatementAccountReport`; `StatementAccountReport` has provider, account, source coverage, selected evidence rows and explicit unknown states; one-account read enforced. Old M-Pesa and new Equity readers adapt to one common presentation model.

- [ ] Write tests showing unchanged M-Pesa totals/screens and correct Equity month/year/category/cash-flow results; missing months remain `null`; same-period alternative sources never sum twice.
- [ ] Define source-neutral UI/evidence rows; map old `StatementClassified` through an adapter. Do not feed Equity lines blindly through M-Pesa-specific `StatementFinancialPolicy` keywords.
- [ ] Add conservative bank classification: raw movement stays known, economic purpose defaults to `UNKNOWN` absent evidence; verified user edits take precedence, merchant/entity learning requires unambiguous proof.
- [ ] Write negative tests for M-Pesa↔Equity own transfer, cash withdrawal, loan principal, fee, refund and conflicting Activity matches; do not create Activity links without exact verified evidence.
- [ ] Make the read model compatible with a future Phase 3 aggregation layer **without computing any combined figure now**.
- [ ] Commit source-neutral reports and tests.

## Task 7: Connect source/account import UI and approved Statements screens

**Files:** Modify `ui/screens/StatementImportScreens.kt`, `StatementEmptyLanding.kt`, `StatementIntelligenceDashboard.kt`, `StatementOverviewPage.kt`/`StatementDeepDivePages.kt` only where generic evidence requires it; extend `StatementOverviewDashboardUiTest.kt`, `StatementWorkspaceNavigationUiTest.kt` and create `StatementEquityImportUiTest.kt`.

**Interfaces:** Existing `statement-activity` navigation remains the only Statements destination; account/provider selection comes from actual saved/confirmed sources. `onImport` remains a Statements-only workflow unless a separately named Activity action is chosen.

- [ ] Write Compose test-source cases for M-Pesa unchanged, confirmed Equity selected, no Equity account yet, supported file-picker type, wrong account, blocked verification and one real Equity report.
- [ ] In the existing import workspace show **source institution → confirmed account → supported document → local verification → preview → save**. Offer a route to existing institution/account setup when no confirmed Equity bank account exists; never auto-create one from a PDF.
- [ ] Keep current M-Pesa PDF import intact. Show Equity PDF/CSV only once its sample-backed adapter passes contract tests; otherwise state unsupported.
- [ ] Route real Equity sources into the current premium account-specific overview, period, category, coverage and source-detail components with Equity labels and cells. No duplicate bottom bar, placeholder account, new Home design, or all-accounts totals.
- [ ] Verify long descriptions, 24-month history, loading/error/retry, narrow screen and large font. No re-reading a PDF for every scope change.
- [ ] Commit UI and regression tests on the feature branch.

## Task 8: Integration, regression evidence and merge gate

**Files:** Extend `CURRENT_STATE.md` **after** test evidence; capture sanitized fixture report in project test artifacts only; no private statement data.

- [ ] Run focused interpreter/admission/coverage/category JVM tests locally (expected pass); run integration/migration/UI source tests on an authorized SDK/emulator, recording what was truly executed.
- [ ] Verify the private real Equity file on the actual Android import flow (if James authorizes and supplies it locally); compare source table counts, running balances, money-in/out and every displayed month. Repeat import and verify idempotency. Never log the private rows.
- [ ] Run `git diff --check`, security scan and full CI command `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --stacktrace` on the exact feature head through one grouped PR workflow.
- [ ] Review financial negative cases, backup v3/v4/v5 restore, no Activity side effects, old M-Pesa visuals and source-specific UI before considering merge. Distinguish compilation from physical-device acceptance.
- [ ] Open **one coherent Phase 2 feature PR** with the audit/spec/plan and implementation. Merge into `main` promptly **only if every required check is green**, the branch is mergeable, sample-admission requirements are met, and the user has approved this proposed architecture. Do not merge unrelated old Statements PRs.

## Implementation handoff

The actual private selectable-text Equity PDF has now passed the local bank extractor and balance/printed-total admission trial. The source remains private and is **not** an automated fixture; CI uses only invented PDFs. The remaining release gate is **on-device PDF import and backup migration/restore instrumentation**. The importer must block unsupported Equity layouts rather than saving partial evidence. Phase 3, not this plan, provides cross-bank aggregation and mirror-transfer-safe **all-accounts** financial totals.
