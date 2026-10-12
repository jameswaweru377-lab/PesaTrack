# Kenyan M-Pesa Statement PDF — structure-based import hardening

Date: 12 October 2026. User supplied three **screenshots**, not an original PDF. Treat visual observations as a layout reference, not as evidence of what Android returns from PdfRenderer text blocks. Never commit identifying details, real receipts, account numbers, original verification code, or raw source PDF.

## Root cause confirmed on main before patch
- `PdfStatementReview` rejected a PDF unless an explicit `Currency: KES` or equivalent field was printed. The pictured Safaricom M-Pesa statement visibly has the title, statement period, summary, detailed rows and column headings but no visible currency declaration.
- The signed withdrawn column is displayed as a negative amount, with Paid In or Withdrawn sometimes blank. Prior parser assumed 3 numeric columns with unsigned amounts or hyphens.
- Flattened Android PDF text may not preserve table columns. Header matching and 8 MiB/160 pages/500k text limits from #350 do not themselves establish row integrity.

## Safeguarded adapter changes
- Accept a missing explicit KES declaration **only** when the full Kenya M-Pesa statement structure is present: M-PESA STATEMENT title, statement period, DETAILED STATEMENT heading, receipt/completion/status/paid-in/withdrawn/balance columns, and no foreign-currency marker in that metadata. An explicit non-KES currency declaration still rejects.
- Show an **inferred-currency review warning** and require user to verify KES before importing. This validates structure, **not** a document's authenticity.
- Parse validated 3-cell rows and compact signed-movement + running-balance rows. Compact rows require at least one explicit negative debit and a mathematically consistent running balance at every adjacent receipt before *any* import candidates are offered. If the text order, sign direction, receipt number, amount or balance is uncertain, fail closed with a concrete layout error. No partial imports or invented deposits.
- Strip the statement verification/disclaimer footer from row extraction while never persisting original PDF content.
- Dates, receipts, duplicate handling, account type, money validation, user selection and final confirmation remain unchanged.

## Current limits and remaining work
- Screenshot indicates 26 pages, which is below the reader's 160-page cap; file size and extracted character count are **not known** from screenshots.
- Textual row-order parser remains dependent on native Android PdfRenderer extracted text. If a PDF yields column-major or differently segmented fragments, this adapter intentionally refuses import. **Next step for true cross-layout support** is on-device coordinate-aware extraction from PdfPageTextContent bounds with synthetic geometry tests. The screenshots alone cannot prove text order or bounding-box behavior.
- Actual end-to-end acceptance on a device needs a personal copy of the PDF. Do not request or upload an unredacted financial statement by default; favor voluntary limited metadata and redacted/test samples.
- Never silently reclassify credits as withdrawals, skip unknown rows, or treat balances as separate income/expenses.

## Verification
- Synthetic JVM tests: branded missing-currency header, explicit foreign override, signed compact rows across two pages, running-balance mismatch blocks whole preview, footer exclusion, legacy 3-cell statements.
- Automatic Android CI JVM tests, lint, APK build and financial contracts. **Do not run instrumentation**. Device trial remains necessary.
