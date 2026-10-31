# Tracker Review Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct two P2 tracker review findings without silent Project status regressions.

**Architecture:** Preserve the existing read-only GitHub adapter and reconciler.
Normalize only documented human status names in the parser; propagate bounded
closed-history truncation to the existing global incomplete gate. No new services.

**Tech Stack:** Python standard library and `unittest`.

**Spec:** `docs/superpowers/specs/2026-10-31-tracker-review-safety-design.md`

## Global Constraints

- Existing canonical GitHub Issues remain authoritative.
- Project synchronization stays disabled by default and fails closed on incomplete evidence.
- No new Android checks, network access, secrets, or financial behavior.

## Review Focus

- Mixed-case `Paused` with an open passing PR must still be `Paused`.
- Duplicate status lines with equivalent spelling must remain one state.
- Two different status lines must remain ambiguous.
- A full closed-history page cap must refuse Project synchronization.
- A non-capped closed-history response must preserve normal reporting.

---

### Task 1: Canonical human task state

**Files:** `tools/tracker/models.py`, `tools/tracker/tests/test_reconcile.py`

**Interface:** `explicit_task_status(body: object) -> str | None`

- [ ] Add failing synthetic tests for mixed/lowercase pauses with an open passing PR, repeated equivalent states, and distinct conflicting states.
- [ ] Run focused test and confirm the expected assertion failures.
- [ ] Canonicalize only recognized regex matches before deciding uniqueness.
- [ ] Run focused tests and confirm passing.

### Task 2: Historical coverage completeness

**Files:** `tools/tracker/github_snapshot.py`, `tools/tracker/tests/test_github_snapshot.py`

**Interface:** `fetch_snapshot(token: str, repository: str, *, opener=None, max_pages: int = 3) -> dict`

- [ ] Add failing fake-API tests: exactly capped closed history means `truncated: true`, uncapped history remains complete.
- [ ] Run focused test and confirm the expected assertion failure.
- [ ] Include closed-history cap in the existing incomplete evidence signal, retaining its advisory display flag.
- [ ] Run focused tests and then full tracker `unittest` suite.
- [ ] Run `python3 tools/security_scan.py` and `git diff --check`.
- [ ] Commit the narrow changes, open a scoped PR linked to #513, and inspect exact-head GitHub CI/review before merging.
