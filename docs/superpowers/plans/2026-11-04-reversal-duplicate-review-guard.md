# Reversal Duplicate Review Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep genuine reversal movements out of weak cross-source "possible duplicate" review suggestions while preserving every existing ledger operation.

**Architecture:** The current `TransactionStore.buildPossibleDuplicateGroups` consumes the pure `PossibleDuplicatePolicy.isCandidate`, and the strong `MirrorTransactionMatcher` already refuses REVERSAL legs. Apply the same refusal to weak review candidates at that pure policy boundary, with a synthetic regression test.

**Tech Stack:** Kotlin/JVM JUnit4, GitHub Actions Android CI.

**Spec:** `docs/PRODUCT_CHARTER.md` financial truth/non-posting rule, `docs/PESATRACK_BRAIN_V3.md` weak evidence review contract, and existing `MirrorTransactionMatcher` reversal exclusion.

## Global Constraints

- Issue #601 owner; no edits to `TransactionStore.kt`, `MirrorTransactionMatcher.kt`, active ingestion/fingerprint PRs #563/#569, schemas, backups, statement posting, UI or financial totals.
- Do not infer a duplicate solely from equal amount/time when either type is REVERSAL.
- Keep ordinary codeless duplicate-review candidates intact.
- Invent all test identifiers; no real financial data.
- PR must stay draft until red-green tests, full Android CI, secrets, independent review and current-main compare pass.
- No emulator or instrumentation execution.

## Review Focus

1. Reversal vs ordinary codeless same-amount, different-source pair within seconds: never a duplicate candidate.
2. Ordinary codeless same-amount, different-source pair within seconds: remains a review candidate.
3. Reversal vs reversal pair across sources: never a duplicate candidate.
4. Coded/codeless weak pair without a reversal: unaffected.
5. Existing protected review states: unaffected.

### Task 1: Pure weak-duplicate reversal guard

**Files:**
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/PossibleDuplicatePolicyTest.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/rules/PossibleDuplicatePolicy.kt`

**Interfaces:**
- Consumes `PossibleDuplicateObservation.type` (actual `StoredTransaction.type`).
- Preserves `PossibleDuplicatePolicy.isCandidate(first, second): Boolean`.

- [ ] **Step 1: Write failing synthetic regression.** Parameterize test helper `sample(..., type: String = "OTHER")`. Assert a normal codeless EXPENSE from SMS and same-amount REVERSAL from a second source within five seconds are `false` in either argument order. Assert both REVERSAL inputs are `false`. Keep preexisting ordinary codeless `true` test.
- [ ] **Step 2: Verify RED.** Open draft PR containing only test and plan; Android CI should fail exactly new assertion(s), no unrelated failure.
- [ ] **Step 3: Minimal production change.** Early return `false` when `first.type.equals("REVERSAL", ignoreCase = true)` or likewise `second.type`.
- [ ] **Step 4: Verify GREEN.** Android CI exact-head JVM unit, lint, debug build, Python financial contract checks, secrets; instrumentation skipped.
- [ ] **Step 5: Independent review + safe merge.** Verify no direct ledger modification, compare latest main, merge only with green exact-head checks. Verify post-merge main, report signed release separately without assumption.
