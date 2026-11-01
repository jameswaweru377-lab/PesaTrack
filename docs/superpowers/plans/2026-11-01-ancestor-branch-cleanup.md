# Guarded Ancestor Branch Cleanup Implementation Plan

> **For agentic workers:** Use the existing repository test-first workflow; implement each checkbox in order.

**Goal:** Add a separate owner-authorized lane for branches with no commits outside current main.

**Architecture:** Reuse existing inventory and lease-deletion functions. Add live GitHub compare proof at candidate selection and at final recheck; keep existing strict merged-PR defaults.

**Tech Stack:** Python 3.13 unittest, GitHub Actions YAML and REST API.

**Spec:** `docs/superpowers/specs/2026-11-01-ancestor-branch-cleanup-design.md`

## Global constraints

- Default and monthly behavior must remain exact-merged-PR-only.
- Ten maximum deletions; never delete open, protected, moved or uncertain refs.
- No app/release/signing changes.

## Review focus

- GitHub compare direction and diverged statuses.
- An open PR head with unknown repository identity.
- Changed main or branch between preview and recheck.
- Malformed API responses and pagination caps.
- Owner-command guard must not match unrelated comments.

## Task 1: Test and implement opt-in ancestry candidates

- [ ] Add failing tests for branch-to-main compare evidence, diverged rejection and existing default behavior.
- [ ] Run the focused tests and confirm expected failure.
- [ ] Implement opt-in selection and fresh recheck in `tools/tracker/branch_cleanup.py`.
- [ ] Run focused tests and full tracker tests.

## Task 2: Safely expose the owner command

- [ ] Add failing workflow contract test for exact owner command and default-branch gating.
- [ ] Update `.github/workflows/branch-cleanup.yml` for opt-in mode.
- [ ] Update tracker docs and run YAML, secrets and diff validation.
- [ ] Open a PR against fresh main and inspect exact-head CI and review before merge.
