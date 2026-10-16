# Read-only PDF vector-grid feasibility probe (research only)

## Purpose
Determine privately on the owner's Android device whether the **source PDF** contains
long orthogonal **stroked vector** paths that could support a future Tabula-style
table detector. A screenshot cannot determine whether green borders are real PDF
vector paths, raster pixels, or thin filled rectangles.

This probe is **not** a new statement importer and cannot determine financial
accuracy, currency, receipt completeness, or percentage reliability.

## Existing vs proposed
- Existing PDFBox read-only probe counts positioned words and possible receipt markers.
- This isolated mode traverses path commands using the existing Android PDFBox
  dependency and counts long horizontal/vertical strokes, joins short collinear fragments, also recognizes narrow filled rectangles, and measures their intersections.
- The report contains seven bounded integers per page: page ordinal, distinct
  horizontal count, distinct vertical count, intersections, possible-grid bit,
  thin filled horizontal-rule count, and thin filled vertical-rule count.
- Only fixed safe summary text leaves the isolated service; no text, coordinates,
  balances, identifiers, drawing colors, screenshots, or passwords are logged,
  exported, or transported.
- Original PDF remains in the local isolated worker; preexisting temporary-file
  cleanup, password handling, 8MiB/160-page bounds and 90-second timeout apply.
- No new permissions, telemetry, network service, OCR or dependencies.
- The scanner has per-page path and segment limits and refuses resource abuse.

## How to test after a reviewed Android build (not performed here)
1. On a Redmi Note 14, open **Activity → Statement Import**.
2. Choose **Test PDF vector table lines (read-only)**.
3. Choose the original PDF only on the device; supply the password locally if asked.
4. Read the page-count summary and report **only** the number of pages with possible
   vector grids and the count breakdown for insufficient intersections, one-direction-only lines,
   no long lines and thin filled rules. Share no document
   contents, passwords, raw logcat, real account data, or receipts.
5. Compare a synthetic ruled file (should indicate grid) and a synthetic
   borderless file (should not). Test incorrectly encrypted input and cancellation.
6. Do **not** use these counts to enable financial posting.

## Interpretation and important limits
- A "possible grid" means only that vector line intersections were observed.
  It does **not** prove text belongs to cells, every row is present, or the grid
  describes a financial statement.
- Zero grid pages does not mean the PDF has no visible lines: rasterized grids,
  nonrectangular filled shapes, unusually short strokes, curved/rotated paths
  and other drawing techniques may be missed. The new segment joining and
  thin-rectangle detection narrow this uncertainty but do not eliminate it. PDF form/rotation handling must be verified.
- A ruled synthetic grid is not the owner's original 26-page PDF. No private
  statement has been uploaded or tested.
- This intentionally does not change PesaTrack's existing statement geometry
  parser, metadata/KES validation, verified-table review, classification or ledger.
- The existing PDFBox dependency's logging/security maintenance gate remains.
  Do not collect raw upstream PDFBox Logcat.

## Acceptance gates
- JVM positive/negative geometry model, fragmented-line joining, thin filled
  rules and malformed IPC tests pass.
- Android source, synthetic androidTest *compilation*, lint, APK and financial
  regression checks pass against the exact draft PR head.
- Physical device test confirms 26-page completion and safe category-only
  outputs, without recording any customer statement values.
- Do not run Android instrumentation tests automatically while paused.
- Do not merge, publish or claim the 95% goal on the basis of vector counts.

## Follow-up decision
If vectors are present on the relevant pages: evaluate actual line-to-cell extraction
on synthetic fixtures as a separate reviewed proposal. Otherwise prefer a text
alignment strategy and separately investigate raster/filled-line cases. In either
case, exact financial reconciliation remains independently mandatory.
