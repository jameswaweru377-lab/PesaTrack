# PDFBox: full table proof vs decorative statement section heading

## Redmi device evidence

After merging #375/#376, PDFBox read 26 pages locally and reported **881 possible receipt rows, 881 Completed markers and zero word-position issues** in the read-only check. The opt-in review-first path proceeded past native Android's unbounded-geometry issue, but failed before row financial validation with the exact fixed-category message:

`KES could not be verified ... Missing statement headings: detailed statement section.`

PR #377 preserved original positioned visual headings in the page metadata, but the owner's next Redmi screenshot still showed the single missing detailed-section label. This establishes that the updated extractor metadata lacks that exact heading, **not** that the file is invalid or all money columns are verified.

## Bounded alternative proof

A PDF's decorative "Detailed Statement" heading is not itself a currency declaration or wallet amount. The app can accept **full-document, independently reconstructed PDFBox table geometry** as alternative evidence of *having a detailed financial section*, provided **all other** official Kenya statement elements are present:

- Original `M-Pesa Statement` document title and statement period
- Original receipt number, completion time, status, Paid In, Withdrawn and Balance column headings
- No explicit foreign or conflicting currency declaration
- Every original receipt/status located, correct money columns separated by physical coordinates, all pages accounted for (the existing PDFBox geometry bridge must finish before setting its `pdfBoxStatementReady` signal)
- All existing row parsing, signed-minor money, repeated-reference identity, receipt-group balance checks, duplicate checks and atomic user-selected import validation
- Explicit mobile-wallet account and user confirmation of **KES**, dates, references, balances and amounts before posting

Only the PDFBox **review-first route** passes `completePdfBoxGeometry=true` into `PdfStatementReview.read`. Android's native text fallback and CSV parsing remain unchanged and still require their original document structure. The checked full-table contract is not permission to manufacture a currency header, infer an account balance, or automatically import.

## Negative and positive synthetic regression

Tests omit only the decorative heading from an otherwise physically reconstructed table. The default parser must refuse; the independently proven PDFBox route may offer review candidates with a conspicuous warning that neither currency nor statement authenticity has been established. Other tests omit the title, period or money headings, or include a USD declaration: all must fail even in PDFBox mode.

The real private PDF/password are **not** included in repo, fixtures, logs, diagnostics or uploads.

## Remaining real-device acceptance

The user's 26-page document has **not yet** passed full on-device candidate/amount/balance verification. A new Redmi test is required to identify any *subsequent* failing stage. Do not merge or publish an unverified parser path until exact-head Android CI passes. Instrumentation stays paused; use synthetic JVM, lint, Android test-source compilation, debug APK and financial Python/Kotlin evaluation. If the review opens, verify all source receipt rows, zero-value exclusions, transfers, fees, and each unresolved balance transition before importing.
