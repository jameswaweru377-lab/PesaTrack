# M-Pesa native-PDF geometry coverage diagnostic (13 October 2026)

## Device evidence

The user's updated Android screenshot reports two independent failures:

1. The optional positioned table reader rejects **page 1: incomplete geometry**.
2. The legacy text fallback independently rejects **page 3, receipt row 8** as an ambiguous description/amount reconstruction.

No transactions are imported. This is the intended failure-closed outcome for incomplete evidence, but it prevents a real valid statement from being reviewed.

## Verified source cause and bounded fix

`PdfTextReviewService.extract` previously set a page-global `complete=false` when **any** native text item had blank text, no bounding boxes, invalid rectangles, overlong text, more than 24 line bounds, or a position budget overrun. The positioned reader then refused the entire PDF on the *first* incomplete page, even if the missing item contained no financial information. Official Android API 35 `PdfPageTextContent.getBounds()` permits an empty rectangle list for unbounded text.

A **private, local desktop extraction** of the original 26-page PDF contains four whitespace-only spans on page 1; desktop extraction cannot confirm what exact fragments the Redmi API 35 renderer exposes.

This PR:

- Retains all original text in the isolated local PDF reader and existing legacy fallback.
- Treats **strictly blank-only native PDF text** as non-financial. It may be excluded from geometry without marking a transaction page incomplete.
- Permits narrowly enumerated document-only static headings to have missing geometry; unsupported narrative, receipt, status, date, money, account identifier, and unknown block text remain incomplete/fail closed.
- Reports aggregate per-page reasons (`missing_bounds`, `invalid_bounds`, `too_many_line_bounds`, `oversized_content`, `unsafe_text`, `total_fragment_limit`) via fixed-count Binder payload. The UI displays a local-only human summary alongside the existing error. **No PDF text, transaction data, identifiers, password, raw geometry, money or SMS is logged or transmitted.**
- Preserves strict per-page native content completion for anything potentially financial; receipt/status counts and balance validation in the existing positioned parser are unchanged.
- Adds JVM negative and positive tests for whitelist boundaries, mandatory refusal of monetary content without bounds, multiline bounded fragments and exact sanitized IPC counter transfer.

## Remaining acceptance gates

- Exact-head Android JVM suite, lint, Android test-source compilation, sanitized Python/Kotlin financial regression and debug APK build. Instrumentation remains paused.
- On-device trial with the privately held source PDF. The new code **does not establish** whether Android's actual incomplete-geometry cause is a blank span, an unbounded non-financial header, oversized multi-line content, malformed bounds, or native text grouping.
- If on-device shows another incomplete page, use only the fixed category counts, not a screenshot of private receipts, to isolate the specific extractor limitation. Don't disable completeness checks just to force import.
- If the positioned reader passes but row reconstruction fails later (e.g., split native receipt text across boxes), that must receive a separate structure-safe adapter and corresponding synthetic tests.
- Do not merge into main or claim restored imports until exact CI and device checks are completed.

The original statement and password are not in the repository or CI fixtures.
