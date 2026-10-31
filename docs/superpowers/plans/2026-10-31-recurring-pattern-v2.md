# Recurring-pattern v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a tested, explainable, non-mutating recurring-pattern v2 domain detector.

**Architecture:** Preserve the live monthly `RecurringPatternPolicy.detect` and add a distinct `detectV2` pure-domain path with immutable candidates. No Android UI, store, bill schedule or ledger change is made in this PR.

**Tech Stack:** Kotlin/JVM, JUnit 4, `java.util.Calendar` in the Africa/Nairobi timezone.

**Spec:** `docs/superpowers/specs/2026-10-31-recurring-pattern-v2-design.md`

## Global Constraints

- All data stays on-device; no new dependencies, permission, telemetry or network call.
- Existing amount tolerance is 35%; HIGH/MEDIUM remain. At least 3 observations.
- One missed gap is tolerated only with enough regular evidence to identify cadence.
- No source file above 800 lines and no changed function above 80 lines.
- Do not change `TransactionStore.kt`, `ActivityScreen.kt` or live categorization.

## Review Focus

- Same merchant visited irregularly within one month → no recurring suggestion.
- A single long gap with otherwise weekly evidence → exactly one skipped week.
- Last day of February → March 31, not March 28.
- Fixed fifth of the month with 1–2 day jitter → next fifth, not drift.
- Termly events with variable month lengths → about four calendar months, not an exact 120-day guess.

---

### Task 1: Define the v2 contract and red tests

**Files:**
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/domain/recurring/RecurringPatternModels.kt`
- Create `app/src/test/java/com/kedevjimmy/pesatrack/domain/recurring/RecurringPatternPolicyV2Test.kt`

**Interfaces:** `RecurringPatternPolicy.detectV2(observations, nowEpochMs): List<RecurringPatternCandidate>`; candidates carry cadence and a human-readable reason.

- [ ] Write invented fixed-day, month-end, weekly, fortnightly, termly, one-missed, and negative test cases.
- [ ] Run the focused JVM test and observe a missing `detectV2` method failure.

### Task 2: Implement the pure policy and pass tests

**Files:**
- Create `app/src/main/java/com/kedevjimmy/pesatrack/domain/recurring/RecurringPatternV2Policy.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/domain/recurring/RecurringPatternPolicy.kt` with a delegated `detectV2` entry point only.

**Interfaces:** The existing production `detect` remains byte-for-byte untouched.

- [ ] Implement cadence classification with local calendar anchoring and amount/staleness checks.
- [ ] Run the focused JVM tests; verify exact expected cadences, dates and reason strings.
- [ ] Run the project-wide JVM tests, Android lint and debug assembly in CI.

### Task 3: Document, review and open PR

**Files:**
- Modify `CURRENT_STATE.md`; add this plan and design file.

- [ ] Document domain-only activation limit, no data transfer and next integration gate.
- [ ] Review the PR diff for unintended live financial effects.
- [ ] Open a separate PR and do not merge it.
