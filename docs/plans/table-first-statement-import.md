# Table-first statement import

Baseline: main `a04e8d1e` (PRs #376, #377, #379, #380). Related Work chats were idle. A direct GitHub collection check found eight pre-existing dependency update PRs and no other statement feature PR. This branch does not change their work.

## Observed gap and selected design

PDFBox already supplies bounded word rectangles and verifies M-Pesa receipt groups, summary totals and balances. Its production bridge then formats those cells as text and runs the legacy parser again. V2 already supplies semantic column aliases, an ordered table model, a CSV adapter and an institution-neutral financial interpreter, but assumes the caller has located the table region. These are reusable components, not grounds for a new competing engine.

Extend the V2 table model with optional cell provenance. Discover semantic header bands and retain ordered physical columns before reconstructing rows. Reuse that discovery in the existing PDFBox adapter. Carry a bounded, versioned structured table payload across the isolated service boundary; the PDFBox review path consumes cells directly. Keep document metadata separate and reduce it to validation declarations, excluding names, phone numbers and customer identities. Keep summaries separate from transaction tables. Retain the existing native and CSV imports until replacement compatibility is demonstrated.

The table layer establishes account movement evidence only. Credit/debit signs cannot prove income, expense, own-account transfer or debt semantics. Unsupported financial columns, account perspectives and ambiguous rows must remain blocked or explicitly unresolved; no invented classification.

## Implementation and acceptance

1. Add bounded table discovery and cell provenance, with split/stacked/reordered headers, repeated headers, wrapped descriptions, invalid geometry and unknown-column refusal tests. Every transaction anchor must belong to a reconstructed region; do not silently cut off trailing transaction evidence.
2. Build shared tables from the existing receipt proof and replace the production formatted-text round trip with a strict structured transport and direct table review. Recheck all cells, receipt identities, date limits, currency/title evidence, receipt groups and report counts before exposing candidates. A partial/malformed transport exposes no candidates.
3. Compare extraction tools on invented selectable-text fixtures locally. Benchmark evidence must distinguish synthetic desktop results from Android/device results. No Python dependencies in the application.
4. Run relevant JVM and financial contract tests, then automatic PR CI (Android source compilation, lint and APK). Obtain independent review. No emulator/instrumentation runs or manual workflow dispatch/retry.
5. Merge only the exact reviewed, tested commit after checking concurrent development. Verify main CI and APK publication. Real 26-page/881-row acceptance still requires private local device testing and safe aggregate diagnostics.

## Technology assessment

* Existing Kotlin + PDFBox: already Android-compatible and isolated, and the Redmi probe found 881 possible rows with no word-position issues. That demonstrates coverage potential, not financial correctness. Extending existing extraction minimizes integration risk; memory/dependency logging and cryptography risks remain as documented in `pdfbox-dependency-review.md`.
* PDFBox into shared V2 tables: selected architecture. Preserves geometry and separates discovery, verification and interpretation without a text round trip or additional runtime.
* pdfplumber/Camelot: useful local synthetic benchmarks. Their Python/native/image dependencies require substantial Android packaging and maintenance work. Do not ship them or infer real-statement accuracy from library examples.
* Tabula Java: reusable desktop table algorithms, but its Java/PDFBox stack is not demonstrated Android-compatible. Adding a second PDFBox dependency or desktop dependencies requires an explicit compatibility assessment. No replacement without measured benefits.
* Structured CSV: preferable when available, and V2 already decodes it to the common table model. Preserve production CSV date/amount compatibility; running balances and ownership still need independent proof where absent.

Primary references: [pdfplumber](https://github.com/jsvine/pdfplumber), [Camelot](https://camelot-py.readthedocs.io/en/stable/), [Tabula Java](https://github.com/tabulapdf/tabula-java).

## Implemented evidence and local comparison

The shared table model now retains optional cell rectangles and distinguishes repeated table regions on one page. Generic discovery finds semantic header bands and reconstructs date-anchored cells without fixed column positions. It refuses invalid/unassigned geometry and dates outside discovered regions, and does not guess away footer text. The production M-Pesa adapter reuses its shared physical-line/header discovery while preserving the existing full-document receipt/status coverage gates and configurable semantic aliases. Arbitrary bank/footer and cross-page row continuation acceptance is not established.

The isolated PDFBox service emits a bounded, versioned length-prefixed table transport. No production transaction prose is fed back to the legacy parser. Only ordered cells, proof counts, financial declarations and fixed currency notices cross into review. Rectangles remain transient in the service. The direct review adapter independently rechecks transferred counts, exact minor-unit arithmetic, dates, complete receipt groups, duplicates, totals and endpoints. Counts alone cannot bind a changed oldest debit or uniformly shifted balances to the original statement; retained declarations are required for the corresponding proof flags. A single event needs its observed opening declaration. Customer metadata is not transported.

Local command (optional desktop PDF packages; never use real statements):

```text
python -m tools.python_lab.statement_table_benchmark --out <scratch-directory>
```

Invented, visually checked 26-page/881-row fixtures include reordered debit/credit columns, wrapped masked identities and AES-256 document encryption. On this Windows desktop, pdfplumber recovered 881/881 exact source rows using ruled lines (~2.7 s), found no acceptable rows with its tested borderless text-discovery settings (~4.5 s), and recovered 881/881 using supplied column boundaries (~4.2 s). These are configuration-specific synthetic observations, not a general library ranking, Android speed/memory estimates or real financial acceptance. The fixtures test extraction only; their dates are intentionally repetitive rather than a reconciled financial history. Camelot and Tabula were assessed from primary documentation only and were not executed here. No new application dependency or Python runtime was added.

Targeted regression coverage includes discovery/provenance, common PDF/CSV cell interpretation, changing column order, repeated headers, wrapped descriptions, 26-page/881-row coverage and transport, malformed/truncated wire refusal, altered oldest amounts, shifted balances, single-event opening proof and proof-flag disagreement. Existing receipt-leg, duplicate, currency, summary and legacy regressions remain applicable. Automatic Android CI is required before integration.

## Release gates and limits

Do not default-enable PDFBox, remove existing import options or claim any provider is verified solely from synthetic tests. Bank exports need sanitized representative formats; liability account perspective needs explicit rules before production interpretation. Statement period is currently required as metadata structure, but its date range is not independently validated. Generic discovery refuses incomplete cross-page rows rather than fabricating a continuation. A successful import also needs user ownership confirmation, duplicate preview and the existing atomic ledger transaction. The original private statement must never enter the repository, benchmarks, logs or external tools.
