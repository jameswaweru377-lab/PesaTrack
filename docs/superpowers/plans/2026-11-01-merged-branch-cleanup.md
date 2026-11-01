# Merged Branch Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically delete only unprotected, non-default branch refs whose current head exactly matches a PR merged into the repository's current default branch and which have no open same-repository PR.

**Architecture:** Add a small read-only inventory/classifier and a fail-closed apply path in a new tracker module. Add a separate monthly GitHub Actions workflow with a read-only preview job and a narrowly scoped apply job; keep the hourly Development Tracker and restricted merge worker unchanged.

**Tech Stack:** Python standard library, GitHub REST API, GitHub Actions YAML, existing `unittest` and workflow-contract test conventions.

**Spec:** `docs/superpowers/specs/2026-11-01-merged-branch-cleanup-design.md`

## Global Constraints

- The current default branch is resolved from repository metadata and is never deleted.
- A candidate must exactly match a merged PR head SHA on the current default branch; age or name is never sufficient.
- A protected branch, any branch with an open same-repository PR, and any incomplete or changing state is skipped.
- Closed-unmerged and no-PR branches are never deleted.
- Only the apply job receives `contents: write` and `pull-requests: read`; no Issues, Projects, PR write, or merge permissions are added.
- Manual dispatch previews by default; manual apply requires the owner on the default branch. The monthly schedule applies the same strict predicate.
- No Android, release, signing, financial, PR-close, PR-merge, or task-owner changes.

## Review Focus

1. **Branch moved after the initial inventory:** recheck current SHA immediately before deletion; test that no delete request occurs.
2. **Open PR or branch protection changes during the run:** recheck both states before deletion; test that the candidate is skipped.
3. **Pagination or malformed API data omits evidence:** fail closed without deleting any branch.
4. **Slash-containing branch names:** encode the ref path correctly and delete only the expected branch.
5. **Manual dispatch from an untrusted ref or actor:** preview only; test apply-job conditions and permissions.

---

### Task 1: Read-only inventory and guarded branch deletion

**Files:**
- Create: `tools/tracker/branch_cleanup.py`
- Create: `tools/tracker/tests/test_branch_cleanup.py`

**Interfaces:**
- `collect_inventory(repository: str, token: str, *, opener=None, max_pages: int = 20) -> dict` returns the current repository default branch, all branch rows, and all PR rows; incomplete pagination raises `RuntimeError`.
- `build_plan(inventory: dict) -> dict` returns `candidates` and per-reason `skipped` counts. A candidate contains `branch`, `sha`, and `merged_pr`.
- `apply_plan(repository: str, token: str, inventory: dict, *, opener=None) -> dict` rechecks each candidate's current branch SHA, default branch, protection, matching merged PR, and open-PR status, then deletes only `refs/heads/<branch>`; the result reports `deleted` and `skipped` rows.
- `main(argv=None) -> int` supports `--repository`, `--apply`, and testable token injection through `GITHUB_TOKEN`; without `--apply`, it only prints the plan.

- [ ] **Step 1: Write failing classifier tests** for exact merged-head eligibility and each exclusion in Review Focus items 2–3; assert no deletion call for any excluded state.
- [ ] **Step 2: Run the focused tests** with `python3 -B -m unittest tools.tracker.tests.test_branch_cleanup -q` and confirm they fail because the module/interface is missing.
- [ ] **Step 3: Implement the pure plan builder and inventory validation** using bounded page-number pagination; fail closed if the repository, branch, PR, SHA, or page data is invalid or truncated.
- [ ] **Step 4: Add tests** for exact SHA mismatch, non-default base PR, foreign-repository PR, default branch, protected branch, open PR, closed-unmerged PR, and no associated PR; confirm all produce no candidates.
- [ ] **Step 5: Add failing recheck and ref-path tests** for branch movement, new open PR, changed protection/default branch, API failure, and slash-containing names.
- [ ] **Step 6: Implement the apply path** to re-fetch eligibility immediately before each deletion, call only the Git ref delete endpoint, and report safe branch/PR identifiers without response bodies or credentials.
- [ ] **Step 7: Run the focused test suite** and confirm all inventory, classification, fail-closed, recheck, and delete-path tests pass.

### Task 2: Separate monthly workflow and current documentation

**Files:**
- Create: `.github/workflows/branch-cleanup.yml`
- Create: `tools/tracker/tests/test_branch_cleanup_workflow.py`
- Modify: `docs/tracker/README.md`
- Modify: `CURRENT_STATE.md`

**Interfaces:**
- The preview job runs the module without `--apply` under `contents: read` and `pull-requests: read`.
- The apply job runs only on the default branch for the monthly schedule or an owner-requested manual apply; it has `contents: write` and `pull-requests: read` only.
- The manual `apply` input defaults to `false`; a non-owner or non-default-branch dispatch cannot reach the apply job.
- Workflow summaries show candidate/deletion/skip counts and branch/PR identifiers, with no Issue or Project writes.

- [ ] **Step 1: Write failing workflow-contract tests** for monthly schedule, preview-by-default dispatch, default-branch and owner apply gates, and separate read/write job permissions.
- [ ] **Step 2: Run the focused workflow tests** and confirm they fail because the cleanup workflow does not exist.
- [ ] **Step 3: Add the workflow** with concurrency protection, a monthly schedule, read-only preview, and owner-only apply from the default branch; preserve the existing tracker and merge workflows unchanged.
- [ ] **Step 4: Run workflow-contract and branch-cleanup tests**; confirm the apply job is the only job with `contents: write` and no PR-trigger path exists.
- [ ] **Step 5: Update tracker documentation and `CURRENT_STATE.md`** to describe the monthly merged-only cleanup, preview behavior, exclusions, and exact permission boundary.
- [ ] **Step 6: Run the full tracker test suite, workflow contract tests, security scan, and `git diff --check`; inspect the final diff for any unrelated files.**
- [ ] **Step 7: Open a focused PR linked to #528, require exact-head CI and PR Secrets, and merge only when the branch is current and every required check is green.**

### Task 3: First live run and verified cleanup

**Files:** No additional source files.

**Interfaces:** Uses the merged workflow from Task 2 and its `workflow_dispatch` inputs.

- [ ] **Step 1: Dispatch preview on the merged default branch** and record the candidate count and criteria in the Actions summary.
- [ ] **Step 2: Dispatch owner-authorized apply** using the approved strict predicate; the workflow independently re-fetches and rechecks every candidate before deletion.
- [ ] **Step 3: Verify the run result** against a fresh live branch and open-PR inventory; confirm all open, protected, unmatched, and unmerged branches remain.
- [ ] **Step 4: Record the exact run and deletion counts** on Issue #528 without changing any PR or branch outside the strict candidate list.
