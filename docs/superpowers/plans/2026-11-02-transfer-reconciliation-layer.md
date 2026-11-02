# Transfer reconciliation layer

Status: implemented and verified locally on 2026-11-03; no device acceptance claimed. Owner: Codex. Base: 5864209941b027f44257b12f15af382df8430b81.

## Goal
Explain transfers between explicitly owned bank and wallet accounts without treating time and amount as proof, deleting either account movement, or changing financial totals.

## Architecture and specification
Existing bounded statement/original-message evidence reader -> directional bank narration parser -> pure transfer reconciliation policy -> review metadata store -> Statements reconciliation workspace. Preserve the existing match-review screen for duplicate and shared-reference reviews. Kotlin, SQLite, Compose; no new runtime dependency or database version (Robolectric is test-only).

An observation is an account movement; multiple independently linked observations can describe the same movement. A transfer connects opposite movements of equal integer principal and currency in different explicitly owned accounts. Existing strict same-leg proof may group observations; no approximate deduplication. Stable shared references establish a candidate, not account ownership. Directional exact account identifiers establish route evidence; names, paybill identity, amount and time alone do not. Reference-free route candidates always require explicit review. Missing counterpart means absent from eligible evidence, not a failed payment. Protected loan, reversal, fee, excluded and linked records are not reclassified.

Decisions use the existing match metadata table, a separate versioned fingerprint namespace, and exact evidence/ownership/route binding. Re-read evidence under the canonical lock before writing. Reject stale decisions and overlapping confirmed relationships, including existing match-review decisions. Require account attestation for every new confirmation. Undo only deletes this layer's metadata. No financial write or automatic category change.

The UI distinguishes unreviewed candidates, user-confirmed pairs, rejected pairs and one-sided route clues. No accuracy percentages. Fee handling is deliberately explicit: do not infer fees from a difference or claim zero fees. No combined cash total until the separately developed shared accounting model is integrated and verified.

## Constraints and scope rulings
- No phone access, private-data extraction, installation, instrumentation, production financial mutations, merge or release.
- Existing isolated audit clone is the working checkout; no second worktree needed.
- PR #595 owns shared-accounting presentation; do not edit its policy or match-review implementation.
- Bounded English route grammar initially reads existing verified bank narration only. Other formats remain unresolved. Source-specific SMS route parsing is future work; existing shared-reference evidence remains available.
- Metadata-only scope satisfies the current posting admission hold. User requested implementation; do not stop for a second design approval.
- The existing dashboard exceeds file-size guidance; change only its one navigation call rather than unrelated refactoring.

## Tasks and verification
1. Run baseline policy suites. Add failing synthetic route/reconciliation tests.
2. Implement bounded exact directional route parser and deterministic reconciliation policy. Test missing/shared references, duplicate observations, competing matches, cross-period context, protected roles, currency/sign mismatches, identifier ambiguity and overflow.
3. Add fingerprint and decision validation tests; implement metadata reader/store with legacy-decision conflict checks.
4. Add reconciliation workspace with loading/error/retry, explanations, confirmation/rejection/undo and legacy review navigation.
5. Run focused and existing host tests, compile Android Kotlin and assemble where tooling allows. Do not claim device acceptance.
6. Review changed code for identity, stale evidence, privacy, overflow and unintended accounting writes. Document limits and deliver full changed source files plus patch.

## Review focus
False own-account assertions; duplicate transfer candidates; evidence revisions; confirmation conflicts across both review systems; year-boundary context; source ownership gaps; route text leaking into diagnostics; confirmation dialog state; unchanged accounting outputs.

## Executed result
427 related Gradle tests passed across 68 suites, including 38 new tests (7 Android-backed persistence tests). The overlapping standalone policy runner passed 296 tests. Debug APK assembly, Android lint, the repository security regression script and git whitespace checks passed. Independent review found two Important defects (identifier prefixes and cross-year conflicts); both were reproduced and fixed with passing regressions. No Critical or Important findings remain from that review. No phone data, instrumentation, installation, publication or merge was involved. See docs/TRANSFER_RECONCILIATION.md for supported scope and limitations.
