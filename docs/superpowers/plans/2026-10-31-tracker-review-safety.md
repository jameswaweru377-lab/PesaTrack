# Tracker Review Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct two P2 tracker review findings without silent Project status regressions.

**Architecture:** Preserve the existing read-only GitHub adapter and reconciler.
Normalize documented human status names in the parser; preserve capped history
as a separate advisory flag and protect per-task stronger Project evidence
without stopping unrelated safe updates. No new services.

**Tech Stack:** Python standard library and `unittest`.

**Spec:** `docs/superpowers/specs/2026-10-31-tracker-review-safety-design.md`

## Global Constraints

- Existing canonical GitHub Issues remain authoritative.
- Project synchronization remains owner-controlled; incomplete full snapshots
  fail closed, while bounded history protects individual uncertain downgrades.
- No new Android checks, network access, secrets, or financial behavior.

## Review Focus

- Mixed-case `Paused` with an open passing PR must still be `Paused`.
- Duplicate status lines with equivalent spelling must remain one state.
- Two different status lines must remain ambiguous.
- A full closed-history page cap must refuse individual uncertain downgrades.
- A non-capped closed-history response must preserve normal reporting.
- A capped page must not block independent safe progress or manual pauses.
- Current failed CI or withdrawn device acceptance for a known merged PR must
  revoke an earlier `Verified` status even when historical pages are capped.

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

- [x] Initial failing fake-API tests: capped closed history was incorrectly trusted, uncapped remained complete.
- [ ] Run focused test and confirm the expected assertion failure.
- [x] Record capped closed history separately from the full incomplete signal.
- [x] Add test-first per-task Project guard: preserve stronger evidence on capped history, while allowing unrelated updates, CI failures, and human pauses.
- [x] Update the Project mirror's status-plan result to surface withheld task numbers without mutating them.
- [ ] Run focused tests and then full tracker `unittest` suite.
- [ ] Run `python3 tools/security_scan.py` and `git diff --check`.
- [ ] Commit the narrow changes, open a scoped PR linked to #513, and inspect exact-head GitHub CI/review before merging.
