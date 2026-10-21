# Phase 2 performance — cross-statement receivables search

## Why this increment

PesaTrack already has revision-scoped in-memory caches in the Home, Activity,
Plan and Accounts Compose screens. Replacing those wholesale could accidentally
show stale financial totals. Performance work proceeds in small, testable steps.

The separate PDF Statement Activity archive previously indexed
`(batch_id, occurred_at DESC)`, which helps when reading **one** statement,
but not when "Owed to Me" searches and sorts records **across every saved PDF**.

## Implemented

- A new index on `statement_activity_lines(occurred_at DESC, batch_id, line_number)`
  can stream newest rows before applying the bounded name/phone and date filters.
- Statement archive schema version 3 → 4 adds **only an index**. No row,
  category, balance, personal debt or statement proof is rewritten.
- The v1/v2 migration path still creates category tables before adding the new
  index; the v3 migration is a straightforward index-only change.
- Android integration tests verify that simulated v3 statements, classification
  labels, and Owed-to-Me search results remain identical across migration.
- A synthetic host SQLite test checks that the search plan uses the new index
  and that dropping the index does not change results.

## Scope and limitations

This improves the chronological scan opportunity, **not** substring matching:
`LIKE '%name%'` may still examine many rows to find a rare counterparty.
There is no claim of a measured Android-device speedup; run emulator/device
benchmarks to quantify results. This index adds a small write/storage cost
to new statement imports.

## Next targeted performance increments

1. Measure Home → Activity/Plan/Accounts cold and warm route timings with
   synthetic datasets and the existing `PesaTrackMeasurement` instrumentation.
2. Refine background preloading and revision-gated stale-snapshot handling
   without showing outdated balances, and add no-flicker UI tests.
3. For exceptionally large archives, consider a reviewed and migration-tested
   normalized name/phone search table or FTS design. Do not allow that
   index to infer loans or alter transaction provenance.
