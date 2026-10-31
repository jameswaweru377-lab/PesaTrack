# Restricted Auto-Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically merge only narrowly scoped, explicitly approved PesaTrack docs/test additions without risking stale code.

**Architecture:** A Python policy layer evaluates a bounded GitHub snapshot and returns the first eligible PR; a separate apply path rechecks main/PR/head/approval before a SHA-locked merge. A trusted-main GitHub Action runs it without checking out PR code.

**Tech Stack:** Python standard library, GitHub REST API, unittest, GitHub Actions YAML.

**Spec:** `docs/superpowers/specs/2026-10-31-restricted-automerge-design.md`

## Global Constraints

- Preserve tracker read-only workflow unchanged and avoid Android runtime code.
- Opt-in requires a matching exact-head owner approval comment plus label.
- Fail closed on any incomplete CI, approvals, PR list or changed-file list.
- Merge no more than one PR per run; no force-push, branch deletion or status fakery.
- Only permit new `.md` files under `docs/tracker/` or new `.py` files under `tools/tracker/tests/`; exclude Android test code in stage one.
- Require a separate repository-scoped GitHub App token so merges still trigger the signed-APK `main` push workflow. The default `GITHUB_TOKEN` must never perform a merge.

## Review Focus

- Owner approval for an old head SHA must not authorize a new commit.
- A clean PR whose base is behind current main must not merge.
- An overlapping open PR or unbounded list must block a merge.
- A green Android CI run for a different SHA or an older rerun must not count.
- A missing Github API response or permission must not trigger a write.
- An absent owner enable flag or GitHub App credential must keep the automation off.

---

### Task 1: Pure eligibility gate

**Files:**
- Create `tools/tracker/restricted_merge.py`
- Create `tools/tracker/tests/test_restricted_merge.py`

**Interfaces:**
- `evaluate_candidate(pr, *, main_sha, approvals, issue, files, open_pr_files, runs) -> tuple[bool, str]`
- Returns a short stable reason code and never writes.

- [ ] Write behavior tests for approved addition-only docs/test PR, stale head, missing approval/label, changed files, outdated main, missing CI, overlap and malformed data.
- [ ] Run tests to confirm failure from missing implementation.
- [ ] Implement the smallest pure gate.
- [ ] Run focused tests and the existing tracker suite.

### Task 2: Bounded GitHub evaluation and guarded merge

**Files:**
- Modify `tools/tracker/restricted_merge.py`
- Extend `tools/tracker/tests/test_restricted_merge.py`

**Interfaces:**
- `run(repository, token, *, apply=False, client=None) -> list[dict]`
- `main(argv=None) -> int`

- [ ] Add tests using an in-memory HTTP boundary with realistic GitHub responses, checking that no merge request occurs for any invalid input.
- [ ] Observe the tests fail before adding HTTP handling.
- [ ] Read current main, bounded PRs/files, issue, owner comments and exact-head actions; select at most one PR.
- [ ] Re-fetch current main/PR/approval before an expected-head merge; capture only bounded statuses in Action summary.
- [ ] Run focused tests and existing tracker suite.

### Task 3: Install opt-in workflow and owner documentation

**Files:**
- Create `.github/workflows/restricted-safe-automerge.yml`
- Modify `docs/tracker/README.md`
- Extend `tools/tracker/tests/test_restricted_merge.py`

- [ ] Add a workflow contract test ensuring trusted main checkout, limited permissions, no untrusted PR code execution, and no change to the read-only tracker.
- [ ] Observe contract test fail before adding workflow.
- [ ] Add workflow for successful CI completion, hourly fallback and manual dispatch, with disabled-by-default GitHub App activation.
- [ ] Document exact approval string/label, exclusions, manual dry-run, blocked status, and no historical backfill.
- [ ] Run tests and `git diff --check`.
- [ ] Review changed files, commit to the isolated branch, and submit PR for current-head CI. Merge only when safe and fully verified.
