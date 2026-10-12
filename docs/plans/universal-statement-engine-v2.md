# Universal Statement Engine V2 — staged architecture and acceptance gates

Date: 2026-10-12. Approved scope: implement an extensible **local-first** statement document engine; retain existing M-Pesa and payslip import until proven replacement. This document records what is proposed and what is implemented in the first PR, not a release claim.

## Decision

Build one shared, issuer-neutral document-to-ledger-evidence pipeline, with tested **format-family** profiles rather than institution-name if/else chains:

1. **Input and geometry**: CSV rows and, in a subsequent gated step, Android PDF text boxes (page, x/y, width/height) become a lossless in-memory table evidence representation. Scanned/OCR files and arbitrary bank PDFs remain unsupported until separately verified.
2. **Header and structure recognition**: map semantically identified date, details, credit, debit, signed amount, direction, balance, reference and status columns; permit reordered headings, optional informational columns and multiple recognized layouts. Ambiguous monetary columns abstain. Never use bank identity as the sole format selector.
3. **Balance and completeness verification**: require every proposed row to have complete data, coherent date order, exact integer-cent movements, strictly evaluated states and correct adjacent balances, including page boundaries. Opening balances prove oldest row where known; otherwise surface unanchored proof explicitly. Known fees that may be extra money fail closed pending validated accounting semantics. Never drop unrecognised rows, duplicate-looking financial legs, unknown money cells or zero records silently.
4. **Interpretation**: normalized movements are *account-leg evidence* only. They are not automatically income, expense, self-transfer, debt disbursement, repayment, refund, fee or savings. Feed only separately reviewed/linked evidence into the existing canonical ledger and owned-account matching.
5. **Privacy**: in-memory local diagnostics report safe page/row/reason, not amounts, names, receipt IDs, source file text or raw PDFs. No telemetry, third-party uploads, new permissions, learning uploads or cloud services.

## First delivered slice (this PR)

A deterministic **JVM-only, read-only table interpretation core** with dynamic header names/positions and injected bounded format profiles. It supports data that is already a structured table (e.g. from a checked CSV adapter, or future PDF column reconstruction). It has no persistence, import button or routing to the ledger, so existing native PDF, payslip and SMS flows remain unchanged.

Synthetic tests must prove:
- two different header styles and three financial shape families (credit/debit, negative-withdrawn, signed-amount plus direction);
- swapped columns across pages, extra non-financial columns and wrapped description values;
- strict adjacent and optional opening-balance checks;
- refusals on unknown numeric columns, nonzero unallocated fees, malformed dates, invalid/missing money, date disorder, unverified zero records, unknown statuses, bad balances and duplicate complete rows;
- repeated receipt references may represent separate statement legs and are not blindly discarded.

## Next independently gated slices (not implemented by this PR)

1. Preserve all PDF text fragments and coordinates in the existing isolated Android extraction service and reconstruct real table rows using header x-ranges and receipt y-anchors, including page headers/footers and multi-line details. Compare old M-Pesa parser and V2 outputs read-only first. Do not remove old parser.
2. Add conservative mapping and diagnostics in Data Health, plus user-only local field confirmation, validated layout signatures and strict no-partial-import checks; continue to reject unproven formats.
3. Extend structured CSV parsing and explicitly validated bank/loan format profiles; integrate with canonical evidence-linking under separate ledger invariants and device acceptance.
4. Verify supported bank format families against legally sourced, sanitized sample statements; run a real-device trial of the user's original 26-page Safaricom PDF **on the user's phone**. The original statement has never been available in the repository, so success cannot be asserted.

## Mandatory gates

Before merge: full JVM tests, lint, Android test-source compilation, Python/Kotlin financial regressions, debug APK and evidence artifact uploads, no instrumentation; inspect CI for the exact HEAD. Never manually dispatch/retry workflows. Before replacing current live parser or enabling general bank PDF imports: static-format and adversarial fixture coverage, complete statement reconciliation, local-first privacy review, controlled UI preview, and **physical Redmi device acceptance**. User's funds, historical SMS and private PDF must never be transmitted to external providers for testing.
