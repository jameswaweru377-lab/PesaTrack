# Universal Statement Engine V2 — verified automatic import for unfamiliar layouts

**Design date:** 31 October 2026  
**Status:** Option 2 approved by owner; native Slice A implementation authorized. Live archive admission and OCR are separately gated and not yet approved for rollout.
**Selected product behavior:** Option 2 — archive a fully verified statement automatically, while leaving uncertain transaction meaning for review.

## Intent

James wants to import legitimate bank and financial-institution statements without commissioning a separate parser fix each time their PDF column arrangement changes. After selecting a statement and its confirmed owned account, PesaTrack should recognize supported format families, prove completeness, and archive the source with minimal intervention. A transaction's raw cash movement may be verified without its economic meaning being known. Review is for meaning, not permission to guess financial facts.

Existing M-Pesa and Equity import routes must remain functional. The ongoing unverified Equity document-admission error is a separate defect; this design does not claim to fix it.

## Goals

1. Reuse the existing local-first V2 ordered table, semantic header discovery, geometry, document proof and evidence archive rather than creating another parser/database.
2. Accept previously unseen **layouts of already understood financial column families** when independent, document-wide verification succeeds.
3. Preserve exact money-in, money-out, date, account, currency, source row and running-balance evidence. Never infer income, expense, loan, transfer or fee from cash direction alone.
4. Automatically archive the entire verified source after the user explicitly selects a file for a confirmed owned account. Show an import result with source coverage and classification review counts.
5. Put unclassified or ambiguous economic meanings into Statements Needs Review; use existing reviewed Activity evidence only where its source linkage is independently trustworthy.
6. Remain wholly on-device; no uploaded statements, external AI, new permissions or telemetry. Preserve duplicate awareness, legacy imports, backups and update continuity.

## Non-goals and anti-goals

- No promise that every PDF, provider, image or arbitrary financial format can be understood.
- No saving of a partial or unbalanced source, even if some transaction rows look correct.
- No silent Activity ledger posting, balance changes, cross-account money totals or spending/income counting merely because a PDF was parsed.
- No guessed bank/account ownership, synthesized missing rows, ignored nonzero fees, inferred signs, or false success from synthetic tests.
- No scanned/OCR production admission in the first rollout slice. On-device OCR is a separately gated extension after its extraction reliability, APK impact and physical device performance are measured.
- No large framework, Room replacement, cloud model, or new parallel bank source-of-truth.

## Architecture and approaches

### Selected: deterministic format-family fallback on the existing V2 pipeline

Keep existing proven provider-specific adapters first. A new read-only unknown-layout fallback uses the shared PDFBox coordinate/structured-table path for selectable-text PDFs, and the existing V2 CSV path for structured CSV. It finds header bands, row anchors and financial column roles; constructs complete source tables; then runs strict document-wide validation. Only after passing does the existing source archive atomically save evidence. This is format recognition, not an unbounded AI prediction.

Use bounded, locally remembered layout signatures only to **suggest** column mapping for later documents; every new document must pass the same independent proofs. Provider label alone never decides column meaning.

### Alternatives considered, not selected

1. **Individual bank templates only:** cheap and predictable for known layouts but would require recurring code changes for KCB/Co-op/I&M revisions and not answer the owner's request.
2. **Automatic vision/LLM interpretation of every page:** may handle more formats but is harder to verify, may strain the Redmi or require remote processing, and can confidently invent financial rows. Unacceptable as a posting authority.
3. **Partial-row import:** saves recognizable rows from an incompletely extracted document; destroys source completeness and could misstate cash totals. Rejected.

## User journey

1. User chooses Import statement, selects an already confirmed owned account/institution, and picks a PDF or CSV. File selection is explicit import intent. No unspecified financial file is sent anywhere.
2. Existing supported route gets first attempt. For an unrecognized format, a read-only V2 fallback attempts extraction and header/column discovery. A password is requested only when the file is actually protected, retained in memory only.
3. The importer checks **all** pages and financial regions, dates, currency, account identifier, unique rows and financial columns; it audits printed totals, opening/closing balances, every adjacent running-balance transition, nonzero fees, page breaks and source coverage using the strict evidence requirements supported by that format. An unproven boundary must remain unproven, not become a passing heuristic.
4. If any critical extraction or document verification check fails, **nothing is saved**. Show a private-data-safe reason and an actionable next step. A detected scanned/image-only PDF says that OCR support is not yet available rather than pretending to import.
5. If the document passes and ownership is confirmed, atomically archive all original account-leg observations in Statements without an extra Save step. Show an import receipt: institution/account label, covered dates, count, cash in/out, verification status, number needing classification review. Do not show an unsupported combined account total.
6. Classify only when existing deterministic, trusted source policy establishes meaning. Keep other imported observations in Statements Needs Review and do not duplicate them in Activity. User corrections remain authoritative and should be replay-safe.
7. Reimport of the same source is idempotent. Overlap with another statement or SMS/Activity creates link candidates, never duplicate cash or an automatically inferred self-transfer.

## Financial boundaries

There are two distinct kinds of confidence:

- **Source certainty:** all financial rows are extracted with immutable provenance and check out against source document totals/balances. This is a hard pass/fail gate for archive admission; percentages alone are not enough.
- **Meaning certainty:** whether a verified cash movement is income, expense, internal transfer, loan, repayment, fee, refund, reversal or unknown. Unresolved meaning does not invalidate a complete bank source, but does prevent misleading classification or downstream ledger posting.

Do not use a self-learned layout to override hard checks. Unknown numeric columns, negative/DR/CR sign ambiguity, repeated receipt legs, cross-page continuation, document-level exclusions, adjusted/unavailable opening balances, and conflicting account identifiers must be handled explicitly or rejected. Preserve fully observed zero/informational rows according to existing evidence rules; never silently discard unknown financial rows.

If archive persistence needs new layout-signature or review-state fields, use one bounded local store with migration, encrypted backup/restore coverage, and tests. Do not introduce a second transaction ledger.

## Implementation boundaries

- Read-only discovery: extend the existing UniversalStatementGeometry, table/header discovery, bounded PDFBox structured transport, and UniversalStatementTableInterpreter where warranted. Preserve current M-Pesa and Equity adapters unchanged until comparison tests demonstrate safe compatibility.
- Admission: introduce a narrow format-family fallback and proof coordinator using existing StatementDocumentProof/assurance and verified account identity; retain whole-document atomicity in the existing statement source archive.
- Meaning: connect admitted observations to existing Statements review semantics; guard Activity linking under the separate evidence-correlation and duplicate rules. No automatic spending/salary/debt posting in this feature.
- UI: reuse the approved Statements import, result, and Needs Review style. Provide specific unsupported-format / partial-document / password / account / reconciliation statuses without leaking source text or private amounts into diagnostics.
- Performance: streaming/bounded page processing, background dispatch, progress, cancellation and memory caps appropriate to Redmi Note 14. Do not enable instrumentation or add slow mandatory CI steps without separate authorization.

## Safe delivery slices

**Slice A — read-only recognition and proof:** Support at least two invented unfamiliar selectable-text layout families using the current shared V2 model. Produce only an in-memory candidate and reason codes; no persistence or changes to existing live imports. Validate cross-page rows, fees, credits, re-ordered headings, statement totals, and refusal cases.

**Slice B — atomic Statements archive and review:** Once Slice A passes, wire verified fallback evidence through existing account admission and statement archive, plus an import-result UI. Add source idempotency, overlap protection, backup/restore, migration and review-state tests where applicable. First default-enabled supported family requires tested full-document proof and explicit owner acceptance on the Redmi. Existing M-Pesa/Equity routes remain as fallback if the new route is not equivalent.

**Slice C — optional on-device OCR for scans:** Investigate PdfRenderer plus an offline ML Kit OCR model, retaining text coordinates and reconstructing tables through V2. Compare against invented scanned images; measure time, battery, memory, bundle size and missing-digit rates. Never enable financial admission from OCR without equivalent document-wide proof and real-device acceptance. If the scan cannot be proven, show a clear unsupported reason.

## Acceptance evidence

- Pure JVM financial tests: multiple invented format families, reordered columns, wrapped and repeated headers, CR/DR balances, exact integer minor-unit arithmetic, printed subtotals, closing/opening balance chain and all source-row coverage.
- Adversarial refusal tests: missing page/row, truncated transport, unknown amount column, changed balance, duplicate or altered receipt, ambiguous fee, unsupported status, account mismatch, wrong currency, invalid dates, password error, image-only source and competing format signature. **Zero persistence** on every failed proof.
- Archive tests: atomic insert/rollback, source idempotency, overlapping period/source handling, duplicate SMS candidate linkage, backup/restore of review states and user corrections.
- App checks: focused tests first, then existing Android unit/lint/debug build/CI on the exact branch head. No auto-triggered instrumentation if still paused.
- Device: local physical Redmi trial against a consenting real bank statement with only privacy-safe aggregate results reported. A green CI build or synthetic document never substitutes for this acceptance.
- Compare old M-Pesa/Equity behavior on the exact same synthetic fixtures and avoid changing their known valid results.

## Coordination and release gates

The repository currently has multiple parallel feature and documentation PRs. Work in an isolated branch starting from current main; recheck open PR ownership immediately before writing shared import/archive/UI files. The separate Equity issuer-verification failure should be fixed and accepted independently; do not hide it behind a generic fallback.

This document is only design. After owner approval, write a small implementation plan and stage read-only proof before any live admission change. Merge/release only with task-specific authorization, successful current-head CI, privacy review and required device evidence. Never publish untested import functionality as general bank support.

## Clarifications from independent builder review

1. **No validation bypass:** The universal path is only a fallback for unsupported layout recognition. It must **not** override an already recognized source's failed issuer, account-ownership, password, currency, authenticity, duplicate-conflict, or financial-reconciliation check. In particular, an Equity document refused as unverified_issuer remains refused; a generic successful balance chain cannot rehabilitate it. Route decisions and validation failures must remain typed and independently testable.
2. **Independent boundary evidence:** A candidate must account for the earliest and latest financial rows, not merely the observed middle. Require appropriate original source declarations such as printed totals and anchored opening/closing balances under the current document-assurance contract. Missing, conflicting or ambiguous anchors are a reason to abstain, never to lower the admission standard.
3. **Supported-device gate:** Current native coordinate extraction is API-sensitive and partly opt-in. First release must feature-detect its availability and state exactly what is unsupported. No automatic substitution with a less-proven extractor. API 24–34 and scanned PDFs remain unpromised until their own verified adapter and device tests exist.
4. **Overlapping verified archives:** It is acceptable to retain two independently verified statements covering an overlapping period as separate source evidence. Existing reports must still choose the appropriate source for each account/month and disclose overlap; do not add both copies or duplicate Activity cash. Inconsistent overlaps are flagged for review.
