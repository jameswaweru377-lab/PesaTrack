# Reconciliation Account Uniqueness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A shared reference cannot misidentify an owned-account transfer when account records duplicate the same full financial identifier.

**Architecture:** Add a small pure account-identity filter and apply it before the existing transfer proof and route matching. Preserve all original rows and non-posting review decisions; duplicate identities become withheld evidence.

**Tech Stack:** Kotlin domain policy, JUnit 4, existing Android CI.

**Spec:** `docs/superpowers/specs/2026-11-03-reconciliation-account-uniqueness-design.md`

## Global Constraints

- Latest `main`, issue #601, separate focused PR; no UI, database, backup or statement-ledger writes.
- No emulator or instrumentation. Source-independent amounts stay integer minor units.
- User-owned distinct accounts retain the existing review behavior.

## Review Focus

- Bank full-number duplicates presented with formatting differences: no candidate.
- Kenyan wallet local/international aliases: no candidate.
- Identifiers shared across bank and wallet types: do not conflate types.
- Missing/malformed identity: do not generate a positive identity proof.
- Unique complete bank-to-wallet identifiers with a shared reference: existing candidate remains.

### Task 1: Withhold ambiguous account identity candidates

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementOwnedAccountUniquenessPolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/TransferReconciliationPolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/TransferReconciliationPolicyTest.kt`
- Document: `CURRENT_STATE.md`

**Interfaces:**
- Consumes: `Map<String, StatementMatchDecisionPolicy.Ownership>` and `OwnAccountTransferDetector.normalizeKenyanPhone()`.
- Produces: `StatementOwnedAccountUniquenessPolicy.unambiguous(ownership): Map<String, StatementMatchDecisionPolicy.Ownership>`.

- [ ] Add failing shared-reference tests for duplicate bank digits (including punctuation), duplicate wallet aliases, and distinct-type preservation.
- [ ] Run the new tests at the test-only commit and verify they fail specifically because an unsafe candidate is returned.
- [ ] Add the pure policy and apply it to the `owned` map in `TransferReconciliationPolicy.reconcile()`; leave rows unchanged.
- [ ] Run the focused JVM tests, then full Android CI/lint/debug APK and synthetic money fixtures on the exact final PR head; confirm only this scope changed.
- [ ] Update docs, keep N2 posting hold, and report lack of device acceptance in #601.
