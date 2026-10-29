# PesaTrack Phase 3 — Source-neutral Statements reconciliation and reporting

Date: 29 October 2026  
Base: `main`, after Equity PDF Phase 2 and the phone diagnostic follow-up.

## Product boundary

- **Activity** is the canonical, independently managed record of SMS/user money events. Statement import must never create, delete or silently reclassify Activity transactions.
- **Statements** is a collection of independently verified historical financial documents, their immutable source legs and explicit source-scoped corrections. A repeated source is not new money.
- **Statements overview** will summarize only proven statement evidence, with per-account/date coverage and clear uncertainty. It is **not** the eventual cross-product personal **Insights** view that joins Activity, plans, debts, salary and Statements.
- **Insights (later)** may display a single financial picture, but it must consume reconciled read-only evidence with provenance rather than sum Activity and Statements records together.

## Confirmed architecture before Phase 3

- The M-Pesa archive uses `StatementActivityStore`/`StatementReportReader`; Equity uses additive `StatementBankArchiveStore`/`StatementBankReportReader`. Both are separate from the canonical `TransactionStore`.
- Each existing monthly report chooses one source document when multiple PDFs cover a month. This avoids false double-counting but can exclude valid rows from complementary or partly overlapping files. Selection alone does **not** prove complete coverage.
- M-Pesa receipt references are structured, but the sample-admitted Equity table has no reference column. Its bank legs usually have `null` reference. Descriptions, matching amounts and close times **alone cannot authenticate an own-account transfer**.
- `StatementCategoryBridge` and `StatementCrossCategoryRepository` already support strictly corroborated Activity/M-Pesa category learning. They must not be replaced by a generic fuzzy name matcher.
- A confirmed owned-account relationship is required before comparing bank and wallet observations. Statement archive v5 and existing encrypted backups remain unchanged unless a later reviewed task needs migration.

## Safest architecture

1. **Neutral observation contract:** immutable `(source kind, source id, row id, account id, provider, currency, time, signed cash movement, exact reference, confirmed ownership, protected financial role)` from existing readers. Do not copy source PDF text into a new database or transmit it anywhere.
2. **Read-only reconciliation policy:** one-to-one, exact-reference-based correlation with strictly matching account/direction/currency/amount and bounded date evidence. A reference may identify multiple distinct cash legs (e.g. principal and fee), so a receipt reference alone is **never** a unique transaction ID. Ambiguous and unmatched observations remain independent and explicitly unresolved.
3. **Coverage and provenance:** report the actual verified imported months/accounts, represented source documents and overlap warnings. Missing months are unknown, not zero. Avoid displaying an authoritative all-account spending/income number while any relevant overlap or own-transfer relation is unresolved.
4. **Category bridge:** allow per-row statement corrections and read-only confirmed learning from Activity with exact corroboration. Do not infer a purchase, salary, loan repayment or personal transfer from an Equity narration without additional proof.
5. **Presentation:** retain approved light/purple Statements designs. Add an all-sources landing page only when reconciliation/coverage tests justify its figures; each source remains independently browsable. Keep future cross-product Insights separate.
6. **Future bank adapters:** admit KCB or others only after a sample-backed parser profile, whole-file totals and account identity verification. Avoid bank-specific domain-logic `if/else` chains.

## Implementation slices and gates

### 3A — Safe evidence-correlation foundation (first implementation)

- Introduce a pure source-neutral observation model and deterministic one-to-one match policy. Match only independently confirmed accounts and exact nontrivial references, amounts, currencies, directions and bounded dates; protect charges, refunds, loans, reversals and manual corrections.
- Return **confirmed links versus unresolved candidates**, never write to either archive. Do not expose combined totals or change UI yet.
- Tests: same-leg Activity/Statement corroboration, owned transfer with two cash sides, wrong ownership, amount/reference/date mismatch, repeated references/fees, ambiguous many-to-one, unreferenced Equity rows, multi-source provenance, no mutation.

### 3B — Actual read adapter and coverage

- Read M-Pesa and bank statement legs through their existing stores (not through Activity imports). Materialize source-neutral observations for the requested period and confirmed account set, with bounded loading.
- Compare competing same-account source documents by verified leg identity. Never sum overlapping documents; disclose uncertain partial coverage. Test complementary, identical and contradictory document sets plus second-import idempotency.

### 3C — Category decisions and Activity evidence boundary

- Reuse receipt-based Activity verification; prevent source-bank or payee-name guesses from propagating categories. A proposed cross-source category link must be previewable and user-confirmed where ambiguous.
- Ensure reversing/correcting a category does not mutate immutable statement evidence or transaction balance/history.

### 3D — Premium unified Statements view

- After the above proof gates, add an all-account **Statements** landing, per-account cards, year/month scope, source coverage, evidence drill-down and clearly marked unresolved transfer/double-count candidates.
- Do not add a second bottom navigation or redesign the approved Home, Activity and M-Pesa pages. Test small screens, accessibility and state restoration.
- Combined cash-flow figures require documented limits and verified pair handling. Spending/income classifications are reported separately from raw cash in/out.

### 3E — Additional providers and later Insights

- Add source profiles one at a time from real bank format examples; do not present unsupported banks as importable. Build the separate cross-product **Insights** design/architecture later, after Statement reconciliation is reliable.

## Acceptance and privacy

- One source observation contributes once only when unique corroboration exists; unresolved candidates never suppress or double-count money silently.
- Exact re-imports stay idempotent. Overlap between different PDFs is not assumed deduplicated by document hash.
- Every proof and failure is deterministic, source-scoped and unit-tested with invented data; never commit real statements, passwords, account identifiers or private transactions.
- Existing Android build, lint, Kotlin tests, M-Pesa UI, Equity import, Activity balances, v3/v4/v5 backup and local-first architecture must remain intact.
- On-device backup/restore and duplicate-import acceptance from Phase 2 remain tracked separately; real Equity phone import succeeded but those tests were not completed at this handoff.

## Deferred account-number UX

During import, allow the user to complete/confirm a missing full account identifier **locally** before strict account matching. Do not weaken the existing verified ownership rule or save a mismatched PDF. This UX correction is not part of 3A.

## First implementation status

- Added a pure, bounded `StatementEvidenceCorrelationPolicy` with mutually unique same-leg and owned-transfer links, explicit ambiguous candidates and no monetary aggregation or persistence.
- Added `StatementEvidenceReadModel` to map confirmed, saved M-Pesa and bank archive legs into source-neutral read-only observations for a requested time window. It deliberately does **not** read canonical Activity transactions yet; that cross-product projection belongs to the separate Insights boundary.
- Added invented-data correlation tests covering independent sources, mirrored accounts, missing references, account ownership, protected roles, amount/date/currency mismatches, fee/principal separation and ambiguous many-to-one candidates.
- **Not complete:** detailed overlap-aware source coverage, bank category proof/learning, all-sources UI, additional bank admission, and on-device read-adapter instrumentation. These must not be represented as implemented by the 3A foundation.
