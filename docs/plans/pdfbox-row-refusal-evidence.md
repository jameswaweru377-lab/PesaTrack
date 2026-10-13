# Identify the private-device row refusal

Baseline: main `d182c2d6` (#381). The user supplied a safe import-screen screenshot: PDFBox refused page 1, receipt row 1 with the generic TABLE_EVIDENCE message. No private statement content is present in the screenshot, and no original PDF was requested or used.

## Investigation

A row-numbered TABLE_EVIDENCE rejection originates inside the receipt-row reconstruction loop, after page geometry, heading discovery and source receipt/Completed/anchor coverage checks. Existing branches reject column ownership, reference/time disagreement, status, description length, ambiguous monetary parts and invalid/empty amounts. The extractor collapsed these distinct failures into the same message and discarded the reason at the service boundary. The screenshot cannot distinguish them. No evidence justifies choosing one and relaxing its financial rule.

Synthetic repros demonstrate that timestamp disagreement, extra source clocks, an oversized description, split monetary cells and straddling rectangles produced the same broad refusal before this change. These are policy reproductions; none establishes the actual device cause.

## Production change and acceptance

Preserve a fixed enum check and seven whitelisted column roles across the existing failure channel. Render authored explanations and bounded page/row numbers only. Do not include original text, descriptions, dates, receipt references, phone numbers, passwords, amounts, coordinates or native exception messages. Unknown check names/column masks cannot be rendered as source text. There is no separate diagnostic mode, file export, logging or network transmission.

Keep every import acceptance rule unchanged. Distinguish individual credit/debit/balance failures without joining split numeric fragments, guessing column boundaries or truncating descriptions. Global financial reconciliation refuses the document without falsely assigning page 1. Existing native/CSV/payslip/SMS pathways and the atomic ledger remain unchanged.

Validate synthetic check selection, privacy-safe formatting, failure transport encode/decode and existing acceptance regressions. Use automatic Android CI for full JVM, lint, APK, financial regressions and Android test-source compilation; no emulator/instrumentation dispatch. Obtain exact-head independent review before merging under the user's existing authorization.

After publication, the user retries the normal PDFBox review option and shares only the fixed Check/Columns message and page/row. That supplies the missing evidence for a targeted reconstruction correction. The real statement remains unverified and no import fix is claimed by this change.
