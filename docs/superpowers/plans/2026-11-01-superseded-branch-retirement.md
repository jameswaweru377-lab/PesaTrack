# Superseded Branch Retirement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable owner-only, at-most-30 manifest-driven retirements of closed/unmerged PR branches whose patches are fully present in merged successors.

**Architecture:** Reuse the existing inventory, fresh branch/PR checks and exact-head Git lease. Extend candidate classification only for explicit manifest entries; reprove all facts against GitHub before deleting.

**Tech Stack:** Python 3.13 standard library, GitHub Actions, unittest.

**Spec:** `docs/superpowers/specs/2026-11-01-superseded-branch-retirement-design.md`

## Global constraints

Never touch app code or active UI; keep Equity/instrumentation held. Fail closed on incomplete GitHub data. Keep ordinary cleanup schedule and cap unchanged.

## Review focus

- Wrong/moved old branch: exact SHA excludes it.
- Open PR added after snapshot: live open PR pagination excludes it.
- Successor merged then reverted or non-main: current-main ancestor proof excludes it.
- Missing/truncated patch: no deletion.
- Unlisted merged branch: superseded mode never includes it.

## Task 1: Proof and candidate selection

- [x] Write tests for complete line inclusion, missing patches, explicit manifest only, changed SHA and open PR.
- [x] Confirm missing proof implementation fails test.
- [x] Add `_patch_lines_contained`, `_confirmed_superseded`, and scoped `build_plan(..., superseded_pairs=...)`.
- [x] Run focused tests.

## Task 2: Execution gate and command

- [x] Reuse existing live exact-tip, open-PR, protected and lease checks; verify successor merge commit in current main.
- [x] Verify old PR paths have unchanged Git blobs on current main since successor merge; defer if current code evolved.
- [x] Add `--superseded-manifest`, capped 30, and owner-only `/pesatrack-cleanup-superseded-apply-30` workflow command.
- [ ] Run full tracker tests and security scan.
- [ ] Submit and merge guarded workflow PR only after exact-head checks.
- [ ] Trigger owner-only Issue #528 command; inspect deletion run and final branch count.
