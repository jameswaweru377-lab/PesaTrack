# Implement strong-reference guard for statement source coverage

Spec: `docs/superpowers/specs/2026-11-06-strong-reference-statement-coverage.md`
Issue: #601 — owner CURRENT FINANCIAL-INTEGRITY CONVERSATION
Base: latest main on 2026-11-06 (recorded before branch).

## Task 1: Regression gate (RED)
**Files:** `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementSourceCoveragePolicyTest.kt`
1. Add synthetic bank PDF evidence from two source documents with the same time, movement and generic narration (`TRANSFER`, `WITHDRAWAL`, and normalized casing/whitespace).
2. Assert `NEEDS_REVIEW`, two retained observations, zero confirmed repeats, two unresolved candidates and null safe subtotals.
3. Verify one-document `TRANSFER` retains a safe source-only observation.
4. Open draft PR and run Android CI against the test-only commit.
**Expected:** Regression fails because the old policy incorrectly treats `TRANSFER` as a verified repeat.

## Task 2: Narrow policy correction (GREEN)
**Files:** `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementSourceCoveragePolicy.kt`
1. Restrict `referenceOf` to current alphanumeric format **and at least one digit**; do not change grouping or posting.
2. Update `CURRENT_STATE.md` and `README.md` to explain the conservative source-only subtotal status.
3. Rerun GitHub CI on the exact final PR head.
**Expected:** New regression and existing full JVM suite green, lint/build/security green; generic-label pairs retain both rows without unsafe total.

## Task 3: Review, integrate and verify
1. Inspect PR file diff, paths, latest main and concurrent work.
2. Require passing exact-head checks. Skip emulator/instrumentation.
3. Merge after safe verification and check postmerge main CI and `ci-apk` rolling two-assets release.
**Expected:** One focused merge, no ledger/UI/schema change, release status and device limitations reported.