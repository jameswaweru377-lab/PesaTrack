# Unattended Command Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove per-PR manual approval for safely bounded tracker documentation changes and automate only evidence-complete acceptance and exception reporting.

**Architecture:** Extend the existing guarded merge evaluator with a separate autonomous documentation lane, retaining the legacy signed owner-approved lane. Introduce conservative automated acceptance in the read-only reconciler. Keep all GitHub mutations within the existing App-token merge path and Project status mirror.

**Tech Stack:** Python 3 standard library, GitHub REST, GitHub Actions, `unittest`.

**Spec:** `docs/superpowers/specs/2026-11-01-unattended-command-center-design.md`

## Global Constraints

- No app, finance, signing, release, database, privacy or phone behavior changes.
- No automatic merge for existing-file edits, forks, outdated bases, drafts, overlaps, failed or unknown exact-head checks.
- One eligible merge maximum per workflow run, with a second exact-state check.
- Keep explicit Issue ownership and existing manual approval for test additions.
- Unattended documentation merges require two separately configured repository
  opt-ins and a dedicated GitHub App token; both opt-ins default to off.
- Do not treat CI alone as Android-device or financial acceptance.

## Review Focus

- Same-repo non-owner PR with apparent task claim: refuse without owner approval.
- Owner-authored docs addition plus unrelated unsafe file: refuse.
- Stale branch or another PR touching the same new file: refuse.
- Merged docs PR with older successful run and later failed rerun: withhold Verified.
- Closed Issue with an open newer linked PR, paused status or incomplete snapshot: withhold Verified.

---

### Task 1: Autonomous merge lane for safe tracker documentation

**Files:** Modify `tools/tracker/restricted_merge.py`; test `tools/tracker/tests/test_restricted_merge.py`.

**Interfaces:** Existing `evaluate_candidate(pr, ..., approvals, ...) -> (bool, str)`, `run(..., apply=False)`.

- [ ] Step 1: Add failing tests: owner-authored added tracker Markdown with matching Issue, green checks and no manual label/comment is eligible; non-owner stays in the original approval lane; executable test addition still requires owner opt-in; unchanged unsafe/stale/overlap guards reject.
- [ ] Step 2: Run `python3 -B -m unittest tools.tracker.tests.test_restricted_merge -v` and observe RED for no-label/no-comment eligible case.
- [ ] Step 3: Add a docs-only autonomous eligibility gate based on trusted PR author and safe files; scan owner-authored canonical task PRs in addition to manually opted-in PRs. Fetch the approval comments only when needed and retain existing conservative bounded API collections.
- [ ] Step 4: Run the test file and full tracker suite to GREEN.

### Task 2: Evidence-based documentation acceptance and exception reasons

**Files:** Modify `tools/tracker/reconcile.py`; test `tools/tracker/tests/test_reconcile.py`.

**Interfaces:** `reconcile(snapshot: dict) -> dict`, preserving `tasks[].status` and adding `tasks[].acceptance` with `mode`, `reason`, `action`.

- [ ] Step 1: Add failing tests: merged tracker-docs-only PR(s) with exact-head CI+secrets, complete snapshot, unambiguous links and no explicit device requirement reach Verified; failed/missing runs, changed files, paused tasks, conflicting links and truncated history never do. Test action reasons for repair, stale and missing evidence.
- [ ] Step 2: Run `python3 -B -m unittest tools.tracker.tests.test_reconcile -v`, observe the new failures.
- [ ] Step 3: Add smallest fail-closed acceptance policy, with clear `action` for non-eligible tasks. Preserve legacy explicit Final verification handling.
- [ ] Step 4: Run focused and full tracker suites, ensuring historical Project-sync safeguards still pass.

### Task 3: Wire reporting, documentation and review

**Files:** Modify `tools/tracker/cli.py`, `docs/tracker/README.md`, `docs/tracker/COMMAND_CENTER_STARTER.md`; test `tools/tracker/tests/test_cli.py`.

**Interfaces:** Render acceptance mode/reason/action in the existing report, no new GitHub Issue mutation.

- [ ] Step 1: Add failing CLI report test for a blocked evidence status and its next action.
- [ ] Step 2: Run the focused CLI test to RED.
- [ ] Step 3: Render the reason/action and document automatic lane boundaries, permission setup and the still-unverified device/repair-agent work.
- [ ] Step 4: Run `python3 -B -m unittest discover -s tools/tracker/tests -q`, `python3 tools/security_scan.py` and `git diff --check`.
- [ ] Step 5: Self-review changed file scopes, confirming only the restricted merge workflow was modified and no Android/release settings changed; then prepare a PR from the latest main. Do not merge without exact-head checks.
