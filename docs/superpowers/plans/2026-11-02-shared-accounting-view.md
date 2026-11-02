# Shared Accounting View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Connect Activity/statement coverage and confirmed cash-leg evidence through one reusable, read-only accounting model.

**Architecture:** Project existing `CrossSourceTotalsPreviewPolicy.Preview` and `StatementReconciledLegProjectionPolicy.Projection` into an immutable `SharedAccountingViewPolicy.View`. Expose it on the existing matching snapshot and render from that shared value.

**Tech Stack:** Kotlin, JUnit 4, Compose.

**Spec:** `docs/superpowers/specs/2026-11-02-shared-accounting-view-design.md`

## Global Constraints

- No canonical money, database, archive, account, backup, security or category writes.
- No instrumentation or emulator tests.
- No combined totals, financial posting or source assurance promotion.
- Use invented financial data in tests and PR descriptions.

## Review Focus

- An owned transfer is not new economic income.
- Withheld statement coverage is not zero.
- An absent Activity snapshot cannot be replaced by statement cash.
- Undo changes preview information only.
- Equality of unrelated totals is not evidence they can be combined.

---

### Task 1: Shared domain projection

**Files:** `domain/statements/SharedAccountingViewPolicy.kt` and `SharedAccountingViewPolicyTest.kt`.

- [ ] Write tests first and observe expected red failure.
- [ ] Implement guarded immutable projection with separate values and `postingAllowed=false`.
- [ ] Run focused and broad unit tests.

### Task 2: Connect existing read model and screen

**Files:** `data/local/StatementMatchDecisionStore.kt`, `ui/screens/StatementMatchReviewPage.kt`.

- [ ] Build shared value once in `Snapshot` from validated current evidence.
- [ ] Render Activity and Statement values through the shared view, retaining approved labels and screen composition.
- [ ] Verify no new SQL writes or new report total consumers.

### Task 3: Integration

- [ ] Verify latest `main` compatibility and exact-head CI/security.
- [ ] Confirm instrumentation skipped.
- [ ] Merge only the review-only candidate when all applicable checks pass.
