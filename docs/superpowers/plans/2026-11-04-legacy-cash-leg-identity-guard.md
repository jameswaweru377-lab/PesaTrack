# Legacy Cash-Leg Identity Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans; execute this one task natively with test-first, exact-head CI and independent read-only review.

**Goal:** Prevent the older read-only Statements correlation view from declaring a duplicate cash leg or owned transfer when its shared reference is non-specific or a competing protected movement exists.

**Architecture:** Reuse the pure `StatementEvidenceCorrelationPolicy.reconcile(observations, ownedAccountIds)` boundary. Keep the existing `StatementCrossSourceEventPolicy` and `StatementSourceCoveragePolicy` accounting/source-coverage outputs unchanged. Never post, delete, reclassify, or change an Activity transaction or stored PDF row.

**Tech Stack:** Kotlin/JVM, existing JUnit tests and Android CI.

**Spec:** Owner's approval of next #601 duplicate/movement identity slice on 4 November 2026; N2 #537 non-posting hold.

## Global Constraints

- Preserve all current ledger/account/report calculations, balances, refunds, fees, corrections, schema, backup, UI and local-first privacy.
- No new statement admission or assurance promotion; no Android instrumentation unless separately approved.
- Synthetic invented reference/amount/account IDs only. A reference with no digit is not a stable identity proof, consistent with the existing stricter `StatementCrossSourceEventPolicy`.
- Open draft PR on latest `main`, TDD RED before production fix, exact-head Android CI + secrets GREEN, independent review, protected merge only when safe.

## Review Focus

- A generic 8+ letter-only reference should not prove a same-leg match between SMS and M-Pesa PDF.
- A protected competing bank cash leg with the same reference/currency/magnitude vetoes otherwise unique SMS/PDF same-leg proof.
- A protected cash leg with different reference/magnitude cannot cancel independent unambiguous proof.
- Exact digit-bearing same-leg references and verified owned-transfer reference candidates remain eligible.
- Different amounts/fees and competing references remain distinct; no financial totals are modified.

---

### Task 1: Protect legacy read-only cash-leg identity proof

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementEvidenceCorrelationPolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementEvidenceCorrelationPolicyTest.kt`
- Modify: `CURRENT_STATE.md`

**Interfaces:**
- Consumes: `StatementEvidenceCorrelationPolicy.Observation` and `ownedAccountIds: Set<String>`.
- Produces: existing `Result.uniqueLinks`, `ambiguousIds`, `unlinkedIds`, `missingReferenceIds`; no new API.

- [ ] Step 1: Add invented-data tests `genericReferenceDoesNotProveSmsAndStatementCopy`, `protectedCompetingLegVetoesSameMovementIdentity`, and `unrelatedProtectedLegDoesNotVetoIndependentMatch`. The first two must currently produce false unique links; third is a positive control.
- [ ] Step 2: Run exact-head Android CI against test-only PR; verify the expected two failing tests and no unrelated failures.
- [ ] Step 3: Strengthen `normalizedReference` to require at least one digit; group eligible owned observations before filtering protected rows, and withhold the entire same-reference/currency/amount bucket if any confirmed owned member is protected, reporting those ids ambiguous.
- [ ] Step 4: Re-run focused `StatementEvidenceCorrelationPolicyTest` and Android CI on the fix; require all existing tests, lint, APK assembly and secrets check.
- [ ] Step 5: Review exact PR diff, update CURRENT_STATE, obtain independent read-only code review, recheck latest `main`, and merge only with verified exact-head checks. Record the slice in #601. Do not claim phone acceptance or signed APK publication without evidence.
