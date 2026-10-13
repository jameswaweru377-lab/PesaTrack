# PDFBox production evidence gates

Date: 2026-10-13. Task authorization: continue the existing offline PDF pipeline,
implement and verify safe changes, open PRs and merge verified work. Existing
design: [Universal Statement Engine V2](universal-statement-engine-v2.md).

## Verified starting point

Main at `498eb58a` includes #375 (count-only probe), #376 (opt-in PDFBox to
review bridge) and #377 (physical source headings). #379 is active work on the
decorative section heading and must not be modified by this branch. Its files
are the legacy review adapter and import UI. Do not duplicate that work.

## Design

Keep PDFBox in the isolated service and reuse the existing review, duplicate
and atomic write pipeline. Strengthen the production geometry adapter: derive
column roles from the shared V2 heading aliases on each page, assign every
body fragment to exactly one column, and retain original amount rectangles in
local in-memory evidence. Blank monetary cells may be represented by `-` only
after column ownership is proved; signs alone do not establish column roles.
Reject unknown, overlapping or incomplete layouts. Normalize validated debit
and credit columns into the existing M-Pesa review format without inventing
currency or source headings.

Audit every extracted leg, including zero records, with checked minor units
and the existing receipt-group balance audit before returning a document to
review. Any unresolved boundary prevents PDFBox import candidates. Compare
recognized statement summary totals and opening/closing balances when present;
missing summary proof stays explicitly unverified, never invented. A single
event needs an independently observed opening balance. Financial meaning
remains a separate review concern; this branch does not alter ledger semantics.

## Implementation and verification

- [ ] Add synthetic failing tests for wrong-column amounts, reordered headings,
  wrapped/masked details, repeated headings and missing/overlapping columns.
- [ ] Implement shared heading geometry and retain local per-cell evidence in
  the PDFBox bridge; update existing invented fixtures to use real headings.
- [ ] Add strict document financial proof with negative balance/summary tests,
  zero records, repeated financial legs and a synthetic 26-page/881-row case.
- [ ] Connect the proof to the existing isolated production extractor and add
  compile-only encrypted PDF regression coverage. No emulator execution.
- [ ] Run local pure JVM regressions, Python financial checks and exact-head
  automatic Android CI (JVM, lint, APK, Android-test compilation, financial
  evaluations). Review dependency/security and size evidence. Review diff.
- [ ] Open a focused PR. Merge only exact tested safe head, after checking
  concurrent main changes; verify automatic main APK publication.

## Acceptance limits

No real statement, password, account, receipt or transaction data may leave the
phone. Matching 881 markers does not establish 881 correct financial rows.
Physical-device encrypted extraction, memory/latency and final reconciliation
remain unverified until the owner retries privately on Redmi Note 14.
Keep the engine selection unchanged until that acceptance; arbitrary bank PDF
layouts and automatic transfer/loan classification are not established here.
