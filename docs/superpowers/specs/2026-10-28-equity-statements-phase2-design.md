# PesaTrack Statements Phase 2 — Equity-first Design (Proposed)

**Status:** Architecture approved on 28 October 2026. The private encrypted original has since passed a local, in-memory source reconstruction and financial reconciliation (without saving details). The guarded Equity PDF importer and account-specific UI are on the Phase 2 work branch pending Android CI and private on-device acceptance. Other banks and arbitrary layouts remain unsupported.

**Inspected baseline:** the exact source tree merged to `main` on 28 October 2026 (`3b5fb0d3a2dbb5c36f8a9b98b4d34f16b5c8f704`; tree `8a60c8440239f1aa3ff7ae7e940a21101902f99d`). Source code and existing tests override older documentation. Existing unmerged Statements PRs #460, #391 and #390 are not implementation bases or merge candidates.

## Why and what success looks like

Allow a user with a confirmed Equity Bank Kenya account to import a **real, locally verified** Equity statement and examine its own spending, cash movement, months, categories and individual source evidence using the **already approved** Phase 1 Statements design. Keep M-Pesa working unchanged. Establish reusable provider-neutral boundaries so later KCB and other bank adapters are data/configuration additions, not parallel UI/database products.

Phase 2 deliberately does **not** show an all-accounts total: the mirror-transfer and overlapping-source proof needed for that belongs to Phase 3. Do not imply an arbitrary bank PDF is supported when only one real Equity layout has been admitted.

## Findings from the live source

| Layer | Verified current behavior | Consequence |
| --- | --- | --- |
| Account registry | `FinancialInstitutionDirectory` includes `KENYAN_BANK_EQUITY`; `MoneyAccount` supports `BANK`; `EverydayMoneyStore.ownedAccounts()` returns **confirmed-owned** bank accounts. | Reuse the existing account/ownership model. Do not invent a second bank list or automatically link a guessed account. |
| Statements entry | `StatementImportSheet(statementOnly=true)` filters accounts to `MOBILE_WALLET`, says “M-Pesa PDF”, and enables PDF only for wallets. | Equity PDFs **cannot** enter the live Statements archive. Add source-aware account/file controls. |
| Generic CSV import | The non-Statements import uses `StatementCsv.parse()` with strict `date,description,amount` or `debit,credit`; `EverydayMoneyStore.importRows()` writes **Activity** entries in `NEEDS_REVIEW`. | A specially formatted Equity CSV might import into Activity after confirmation, but **not** Equity Statements; a native Equity export has not been validated. |
| Bank-neutral interpretation | `UniversalStatementCsv`, `StatementTableDiscovery`, `UniversalStatementGeometry`, `StatementTableWire`, `UniversalStatementTableInterpreter` and pure JVM tests already exist. | Reuse these bounded, fail-closed components. They are **not** connected to a production bank import or bank archive. |
| Native PDF evidence | `PdfTextReviewService` requires API 35 for position extraction. `OfflineMpesaPdfReader` runs bundled on-device `pdfplumber` through Chaquopy and expects seven M-Pesa-specific cells. | Provide a separate bounded offline table-extraction adapter for Equity and API 24–34. Do not convert bank PDFs through the M-Pesa engine. |
| Archive | `StatementActivityStore` DB version 4 requires owned mobile wallets; `save()` expects M-Pesa `StatementActivityLine` and `StatementActivityProof`. | Preserve old M-Pesa tables/rows; add a versioned, provider-neutral bank source/leg journal. |
| Report | `StatementReportReader.read()` takes one account and returns M-Pesa-shaped `StatementClassified`; `StatementHistoryReport` selects one source per account/month to avoid overlap. | Add a bank source adapter and common report projection. Never concatenate accounts or overlapping PDFs and claim accurate totals. |
| Categories/Activity | `StatementFinancialPolicy` contains M-Pesa-specific description rules; `StatementCategoryBridge` and `StatementReceiptEvidenceReader` use limited confirmed Activity learning. | Preserve M-Pesa rules; use a conservative generic Equity classifier and evidence-backed, ambiguity-safe linking. |
| Backup | `StatementActivityStore.Db` is v4 while `StatementBackupArchive.SCHEMA_VERSION` is **3** and `PesaBackupManager` requires an exact schema match. | Release-blocking compatibility fix is required **before** a v5 statement archive migration. Preserve ability to restore older supported backups. |

**External product fact, not a parser contract:** Equity's official Kenya Online/Mobile pages say users can download account statements. They do not establish the headers, PDF layout, passwords, or file formats of James's particular export. See https://equitygroupholdings.com/ke/pay-send-money/personal/payment-services/online-banking/ and https://equitygroupholdings.com/ke/pay-send-money/diaspora/payment-services/mobile-banking/.

### Evidence added from James's redacted screenshot (28 October)

The image is a screenshot of a **two-page Equity Bank Kenya KES ordinary-account statement**, not an original PDF file. Its visible table uses exactly these six headings, in order: **Date | Value | Particulars | Money Out | Money In | Balance**. A visible page includes several same-day debit rows, a credit row, a separately itemized transaction/SMS charge, and balances printed with a **`Cr` suffix**. It repeats the column headings on the next page and shows **Page Total**, **Grand Total** and **Uncleared Cheques** after the transaction area, followed by a non-transaction information/footer sentence. A provider logo/watermark and a page number are outside the transaction table. Personal identifiers, names, exact monetary figures and reference codes are deliberately *not transcribed* into this document.

**Impact on the generic interpreter:** `Particulars` and `Money Out`/`Money In` are already known aliases and the day-month-year dates fit supported syntax. `Value` is **not** currently a recognized header and must not be guessed to mean amount or value date; its meaning needs evidence from an unmasked structural sample. `Balance` currently expects plain numeric cells and will reject the screenshot's `Cr` notation; a safe debit/credit balance parser is required. Page/grand totals and uncleared-cheque lines must be verified as separate **summary/footer evidence**, not accepted as transaction legs and not silently discarded. Transaction/SMS charges are economic fees, not shopping. The screenshot cannot prove completeness of the first page's redacted rows, PDF selectable-text geometry, password behavior, opening balance or whether totals reconcile.

**Privacy note:** This screenshot still appears to contain some identifying header text despite the visible redactions. Do not quote, copy, commit, OCR into fixtures or expose any of that information. Build only invented synthetic data with the same table grammar.

## Approaches considered

1. **Route Equity through `OfflineMpesaPdfReader` and existing wallet tables.** Low initial code but wrong source structure, wallet ownership restriction and misleading M-Pesa assumptions. **Reject.**
2. **Create a wholly separate Equity-specific database and screen.** Isolates risk but duplicates backup, UI, account selection, categorization and later KCB integration. **Reject.**
3. **Add versioned provider-neutral source/leg tables to the existing statement archive, keeping the old M-Pesa tables intact.** Use one account/provider adapter contract and a shared report projection. Requires careful backup migration but produces one honest, extensible system. **Recommended; pending approval.**

## Proposed data and trust boundaries

1. **Select** a confirmed owned account from the existing institution registry. Show a source label (M-Pesa or Equity) from its actual provider identity. Do not expose an unsaved institution merely because it appears in a catalogue.
2. **Admit** a supported PDF/CSV as a bounded read-only document on the device. Reject unsupported encryption, scanned/image-only pages, unknown currency/financial columns, incomplete geometry, malformed dates and oversized imports. Preserve passwords in memory only, never in logs or storage. For the observed Equity layout, distinguish `Cr`/`Dr` running-balance suffixes from cash movement direction.
3. **Extract** complete source tables with page/row/cell provenance. For PDF, verify header and footer boundaries before handing table cells to `UniversalStatementTableInterpreter`. For CSV, use `UniversalStatementCsv`. Provider layout profiles may add **header aliases** and declarative evidence rules; provider names must not become an expanding `if/else` chain.
4. **Verify** account ownership match, chronology, currency (KES for first Equity format), unique import identity, running-balance arithmetic, coverage and any bank-displayed totals/fees. A nonzero separate fee or adjustment must be reconciled as its own financial leg, not ignored. An unidentified amount or ambiguous debit/credit stops acceptance without partial save.
5. **Preview and consent.** Show source institution/account, date span, row count, money-in/out, uncertain items and totals/verification result. Explicitly save only to Statements; importing into Activity is a separate review action, never a side effect.
6. **Persist** immutable source records and source legs atomically in additive v5 tables (e.g. `statement_sources_v2`, `statement_legs_v2`, `statement_category_labels_v2`) alongside unchanged M-Pesa v4 tables. Source ID includes account, provider, normalized approved financial rows and layout identity; exact re-import is idempotent. Original financial cells are retained only as required evidence and protected by existing app-private storage and encrypted backup contract.
7. **Project** one chosen account and explicit date scope into a common read-only report. Missing months remain unknown, alternative/overlapping source files remain disclosed, and raw money-in/out stays separate from classified income/spending. Extend the current Phase 1 Compose screens using bank-neutral view models; preserve the approved spacing, colors, navigation, and all M-Pesa functions.
8. **Learn safely.** Existing confirmed Activity categorization may suggest a bank category only when a stable counterparty/receipt identity is independently established. Uncertain bank debits remain `UNKNOWN`, not default `EXPENSE`. Own transfers, loan principal, refunds, reversals, charges and withdrawals must never be fabricated as purchases/income. User corrections are source-scoped and replay-safe.

### Two different kinds of transfer proof

- **Within one account:** bank running balances establish direction and cash movement, but not economic purpose.
- **Between accounts (deferred combined totals):** an Equity outflow and M-Pesa inflow can be matched only with compatible **owned account identities, amount/currency, time window, references and unique pairing**. Ambiguous many-to-one candidates remain unmatched. Phase 2 may produce read-only candidate evidence but must not display combined totals until Phase 3 has tested and approved its dedup policy.

## UI contract (protect approved Phase 1 design)

- **Import:** Source institution selector (only actually supported providers); select/create a *confirmed* source account; select PDF/CSV only for provider formats proven by tests; verification preview, blocked reason and explicit save. M-Pesa's existing picker and PDF/password flow remain operational.
- **Statements landing:** Show real imported M-Pesa and Equity account cards when such data exists; no fictitious amounts or “coming soon” bank controls that appear functional. The initial overview is always **one selected account**.
- **Equity detail:** Reuse `StatementOverviewPage`, chart, coverage and drill-down patterns. Receipt page exposes original Equity evidence and provenance, not M-Pesa field labels. Edits/Activity links are optional and verified.
- **Empty/invalid:** Explain unsupported Equity layout, missing account confirmation, password failure, incomplete statement, date/fee ambiguity, and missing months without losing existing imports.

## Scope and acceptance

**Deliver in Phase 2:** One demonstrated Equity statement format (PDF first if that is James's sample, optionally CSV), identity-safe import, read-only archive and account-specific dashboard, existing M-Pesa regression protection, versioned backup/restore, meaningful error states and automated proof tests.

**Do not deliver yet:** KCB parser, generic “upload any bank”, all-account spend sums, silent Activity writes, automatic loans/transfer income, OCR guesses, server processing, new cloud telemetry, or a redesign of Home/Activity/Statements visual language.

**Required acceptance fixtures:** at least one *locally retained*, permissioned Equity statement sample and a structurally faithful synthetic fixture derived from it; variants for split descriptions, mixed fees, credits/debits, dates, duplicate rows, masked accounts, passwords, missing/unknown financial columns and 2-year data. The real statement's private data must **not** be committed or submitted to third-party tools.

**Exit criteria:** After one Equity file is admitted, the visible balance trail, row count, inward/outward sums and date coverage agree with its source; importing it twice is idempotent; M-Pesa reports remain identical; every visible Equity action uses saved evidence; encrypted backup export, v3/v4 compatibility restore and v5 round-trip are verified; Android build/JVM/lint/APK/Android-test-source checks pass. On-device import/UI acceptance remains separate from compile-only CI.

## Decisions required before product implementation

- James to provide a **sanitized, structurally faithful Equity PDF** (or keep a private original for an on-device fixture check), and say whether the export is PDF, CSV or both. A scanned image or an Equity statement from another jurisdiction may need separate scope.
- Confirm that the target is **one Equity Bank Kenya personal account** and source-specific insights first; the all-institutions overview remains Phase 3 until cross-account accounting is proven.
- Approve the additive v5 archive/backup design and the use of a provider registry instead of individual `Equity.kt`/`Kcb.kt` business-logic branches.
