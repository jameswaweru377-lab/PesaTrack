# PDFBox statement-review bridge — experimental opt-in

## Scope

Owner-approved connection of the previously validated independent **on-device PDFBox** extractor to actual PesaTrack statement **candidate review**, not to automatic ledger insertion. Previous Redmi PDFBox diagnostics showed **26 pages, 881 possible receipt rows, 881 Completed labels and 0 word-position problems**. Those counts are not proof of money/cell accuracy. The older Android reader continued to reject page 1 native geometry.

## What this branch implements

1. **PDFBox word-coordinate extraction in isolated PDF worker.** The existing Android PDFBox dependency is reused. `PdfBoxStatementExtractor` privately opens password-protected documents via `PDDocument.load`, extracts page text and `TextPosition` glyph geometry for individual words, normalizes direction-adjusted rectangles, and enforces 8 MiB / 160 page / 500,000 characters / 32,000 word bounds plus the existing service's 90-second timeout. There is no OCR, network or extra permission.
2. **Source-column-preserving row reconstruction.** `PdfBoxStatementGeometry` groups words by **physical Y line**, identifies receipt/date/time boundaries, collects descriptions **to the left of the unique Completed status**, and collects exactly 2–3 financial cells **to its right**. Wrapped descriptions, masked numbers and dashed references never become monetary cells. Every original receipt and Completed marker must be accounted for on every page; missing word positions, extra amount tokens or ambiguous rows fail the complete document.
3. **Existing full financial validation remains authoritative.** The geometry bridge sends a bounded, complete, normalized M-Pesa text preview over the existing isolated Binder text chunk protocol; the ordinary `PdfStatementReview.read` validates full official Kenya statement structure/currency, receipt-leg identity, amounts, zero receipts, chronological balance proofs and row integrity. No unproven row is silently posted. Existing duplicate/selection/atomic import validation remains.
4. **Explicit experiment on the device.** Statement Import offers **Select M-Pesa PDF using PDFBox (review first)**. If complete PDFBox geometric/financial validation succeeds, the same normal transaction review and confirmation screen is shown. It is **separate** from the old M-Pesa reader and from the PDFBox count-only diagnostic, so the trusted existing production behavior is not switched over automatically. The source remains entirely local and the password is not logged/stored.
5. **Tests.** Synthetic JVM geometry/negative/atomic tests; compile-only synthetic encrypted Android integration test; no instrumentation or real-user samples uploaded.

## Financial and security constraints

- The 881 `Completed` diagnostic markers are a **target to account for**, not proof that 881 cash movements exist. Real statement includes 5 zero-value informational receipts and multi-leg receipts; those must not become fabricated income/expenses.
- A ledger write still requires explicit review, owned wallet selection and user confirmation. Unknown transfers/loans/fees remain reviewable and must not become automatically classified as earned income.
- PDFBox-Android version `2.0.27.0` is based on an older PDFBox release. Dependency/security advisories and APK size/memory usage remain release review gates before this replaces the legacy extractor.
- Source file, password, receipt text and account details are never uploaded to GitHub, remote processing, telemetry or test assets. All examples are synthetic.
- The PDFBox parser currently supports the known M-Pesa table family; it **does not** claim support for arbitrary Equity/KCB/Co-op PDFs, OCR/scans or all rotated/complex layouts.
- **This is not yet a verified fix for the original 26-page source.** The device must run the exact APK and confirm every original receipt row and totals. If the new experimental route fails closed, preserve the failure evidence and improve geometry; do not relax financial checks.

## Verification and rollout

Exact-head PR CI must pass Android JVM tests, lint, debug APK, Android test-source compilation and Kotlin/Python financial evaluations. Instrumentation stays skipped absent new explicit owner authorization. Once passing, merge only if the owner authorizes; then verify rolling main APK publication. Ask for a screenshot of the **new PDFBox review result**, not another raw statement or password. Do not import if totals/balances are unverified.


## Follow-up: PDFBox currency/structure handoff (13 October 2026)

The real Redmi review-first path opens the PDF using PDFBox and reaches financial verification, but then stops with **"KES could not be verified from a currency declaration or the full M-Pesa Kenya statement structure."** No transactions were added.

The count-only PDFBox probe previously reported 26 pages, 881 possible receipt rows, 881 Completed labels and no word-position issues. These are structural observations, not proof of the financial columns or of currency.

**Code finding:** The geometry bridge reconstructs transaction rows from physical word positions, but previously copied only the *linear PDFBox text prefix* before the first detected receipt. PDFBox's text-stream ordering can be different from physically displayed header ordering. Thus a structurally recognizable PDF can lose its visible M-Pesa title or required table headings before the strict currency check, even when those headings exist in the extracted positioned words.

**Targeted repair:** Preserve both pieces of **original source evidence** before receipt rows: the PDFBox text prefix and the bounded physical word-lines above the first receipt. This is not a synthesized `Currency: KES` declaration. The unchanged financial validator still requires either an explicit recognized KES declaration or all official Kenyan statement headings, and still refuses foreign or conflicting currency declarations. New all-synthetic tests require recovery of genuinely positioned document headings, rejection when the original M-Pesa title is absent, rejection of explicit USD, and refusal to use a transaction description as document-authenticity evidence. The error now reports missing heading **names only**, never private text.

**Outstanding evidence:** Android CI must pass on the exact branch head; an actual Redmi test must show whether this fixes the 26-page source. Even if currency checks succeed, all receipts, individual paid-in/withdrawn cells, multi-leg balance groups and duplicate checks must still pass before any candidate can be approved. Do not relax checks or import partially. This repair does not activate PDFBox automatically for other banks or sources.
