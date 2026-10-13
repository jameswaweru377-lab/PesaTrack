# PesaTrack — on-device PDFBox evidence probe

**Implementation branch:** `feature/pdfbox-local-evidence-probe`  
**Scope:** A real second local PDF extraction engine with an opt-in, **read-only** diagnostic in Statement Import. This is **not** a replacement live parser and is not permission to import a PDF automatically.

## Why

The Redmi's native Android API35+ `PdfRenderer` sometimes yields a `PdfPageTextContent` with no bounding rectangles on page 1. Earlier attempts at regex corrections addressed the fallback's page-3 issue, but did not solve Android's missing coordinate evidence. The engine must independently extract document text and its physical positions rather than substitute guessed money fields.

## Implementation

- Dependency: `com.tom-roush:pdfbox-android:2.0.27.0` (Apache-2.0). This older port is used **only in diagnostic mode** until a security and maintenance review is complete.
- `PdfBoxEvidenceProbe` uses `PDFBoxResourceLoader.init()`, `PDDocument.load(inputStream,password)`, and a `PDFTextStripper` extension recording `TextPosition` word rectangles (direction-adjusted top-left coordinates, finite bounds and per-page dimensions). Page ordering and count-based coverage are derived independently of Android `PdfRenderer`. No OCR or image extraction is promised.
- PesaTrack's existing `:pdf_review` **isolated Android service** handles the probe on its worker thread. The already-selected file passes through the existing bounded, temporary, private local document-copy process; the document and password are not uploaded, persisted to the ledger or placed in diagnostic output.
- The probe mode, reachable by **Statement Import → Test PDFBox extraction (read-only)**, uses the same password dialog and cancellation/timeout. It returns only page counts, possible receipt markers, `Completed` marker counts, positioned words and coordinate-problem counts. Those are **not** verified transaction counts.
- IPC transmits **seven bounded integer fields per page**, never text, identifier, password, amounts, coordinates or a PDF preview. The UI cannot pass the report through `PdfStatementReview` or `EverydayMoneyStore.importRows`.
- Existing **Select M-Pesa PDF**, payroll and CSV import behavior remain unchanged. The read-only probe clears previously selected financial import candidates before inspecting a new document.
- Guardrails: original file up to 8 MiB; at most 160 pages and 500,000 extracted characters; max 100,000 probed positioned words; one 90-second overall timeout and isolated-process teardown/cancellation; a full-page count transfer with no partial successes.

## Verification

- Deterministic JVM tests cover count-only IPC, impossible/negative counts, incomplete transfers, no financial authority, bounded rectangles, NaN/infinite/negative/out-of-bounds glyph positions.
- Two **synthetic encrypted PDF** Android tests construct a two-page document locally, confirm correct password and text-position extraction, and reject wrong passwords.
- **Do not execute instrumentation tests** until the user explicitly reauthorizes them. Their source may be compiled in CI; unrun tests must be disclosed.
- Require exact-branch-head Android JVM/lint/debug APK, test-source compilation, local financial regression checks and artifact upload.
- **Real Redmi device trial required:** open the owner's original 26-page file in the diagnostic, observe per-page text/position counts, compare Android native engine output. Do not share the statement or password, or treat matching counts alone as sufficient proof of correct debit/credit/balance cells.

## Release concerns and next gate

PDFBox-Android `2.0.27.0` is based on PDFBox 2.0.27 and is older than current desktop PDFBox. Audit its transitive dependencies, reported CVEs, Logcat behavior, APK-size delta and memory/latency before moving beyond a prototype. PDFs are untrusted input; parsing stays in the isolated process. Do not route PDFBox words to financial statement validation or canonical ledger until end-to-end synthetic fixtures and real-device balance checks prove full table semantics.

For the future production adapter, normalize PDFBox word/character coordinates to a shared page/cell geometry model, prove complete receipt and amount column coverage, validate opening/closing balances and different account perspectives, and keep unsupported scanned or bank layouts unimportable. This follows [Universal Statement Engine V2](universal-statement-engine-v2.md) and [tracking issue #374](https://github.com/jameswaweru377-lab/PesaTrack/issues/374).

**Not implemented in this PR:** arbitrarily accurate bank PDF imports, reconciliation of all 881 real rows on the device, automatic SMS mirroring, OCR, debt/transfer assignment, or a new published APK.
