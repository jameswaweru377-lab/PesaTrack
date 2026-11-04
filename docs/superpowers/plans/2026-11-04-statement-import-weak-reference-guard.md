# Statement Import Weak-Reference Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent generic statement narration from silently suppressing separate legitimate money movements as exact import duplicates.

**Architecture:** Keep the existing `StatementImportMatchIndex` reference map and possible-match index. Normalize stable transaction references identically at index creation and lookup, using the same conservative code shape already used in evidence correlation. Weak labels fall back to the unchanged possible-match route.

**Tech Stack:** Kotlin, JUnit, GitHub Actions Android CI.

**Spec:** `docs/superpowers/specs/2026-11-04-statement-import-weak-reference-guard.md`

## Global Constraints

- Keep financial amounts, categories, balance calculations, database schema and import posting path unchanged.
- Preserve exact match's account, direction, amount and local-day checks.
- No emulator or Android instrumentation execution.
- Use synthetic test data only; draft PR, review and exact-head CI before merge.

## Review Focus

- All-letter `TRANSFER` on matching day/amount must not be an exact duplicate; test weak-reference branch.
- All-letter `PAYMENT` on matching day/amount must not be an exact duplicate; test same branch with both inputs.
- Case and whitespace changes on strong digit-bearing reference must still match; test valid normalizing branch.
- Different account/day/amount/side with valid code must not match; retain existing exact-match regression.
- Genuine ambiguous same-day cash leg must still offer review; test `hasPossibleMatch` for generic references.

---

### Task 1: Narrow exact statement import proof

**Files:**
- Modify: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementImportMatchIndexTest.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/everyday/StatementImportMatchIndex.kt`

**Interfaces:**
- Consumes: `StatementImportMatchIndex.from(List<ExistingLeg>, TimeZone)`, `hasExactReference(String, StatementRow)`, `hasPossibleMatch(String, StatementRow)`.
- Produces: Same public/internal interfaces, but only normalized digit-bearing stable codes enter the exact-reference map.

- [ ] **Step 1: Write failing tests.** Synthetic same-account/day/amount `TRANSFER`, `PAYMENT`, and a punctuation label are not exact; `hasPossibleMatch` remains true. A lowercase, whitespace-padded `qab1234567` remains exact against uppercase stored `QAB1234567`.
- [ ] **Step 2: Run RED.** GitHub PR Android CI `testDebugUnitTest` must fail for the new generic-label regression on old production code.
- [ ] **Step 3: Implement stable reference normalization.** `private fun normalizedReference(value: String?): String?`, uppercase trim, `[A-Z0-9]{8,48}` with at least one digit; call in both map population and exact lookup, otherwise leave all matching untouched.
- [ ] **Step 4: Run GREEN.** Exact-head PR Android CI including JVM suite and secrets check must succeed; no instrumentation.
- [ ] **Step 5: Review and commit.** Independent whole-branch review, verify latest `main`, ready draft and squash merge after green checks; verify resulting main SHA.

**Execution method:** Native inline implementation already approved by James. Use this plan's commits and PR history as the durable progress record.