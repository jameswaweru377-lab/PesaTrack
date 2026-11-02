# Owner-Approved Old Branch Retirement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely retire the owner's exactly 42 oldest named branches in one bounded, recoverable batch.

**Architecture:** Commit a pinned name/SHA/date manifest. Use a separate owner-only Issue #528 workflow to preview and then recheck/archive/delete with Git leases. Fail closed on open PRs, protection, changed heads and archive failures.

**Tech Stack:** Python 3.13, GitHub Actions, Git, unittest.

**Spec:** `docs/superpowers/specs/2026-11-01-owner-approved-old-branch-retirement.md`

## Constraints

Do not touch app code or money logic. Retain Android 7, Equity and instrumentation ownership, and approved UI. No force-updating refs; only exact-lease branch deletions after tag archival.

## Review focus

- Stale head: skip, do not delete.
- Protected/open PR branch: skip, never delete.
- Tag creation rejected: stop before branch deletion.
- Tag pointing at wrong SHA: stop before branch deletion.
- Partial progress: report already archived/deleted branches, no unbounded retries.

## Tasks

- [x] Capture 42 oldest live branch tips and commit dates in explicit manifest.
- [x] Write failing tests for manifest, protected/open/moved skipping, archive-before-delete and archive failure.
- [x] Implement preview and guarded apply.
- [x] Add owner-only workflow and recoverable archive tags.
- [ ] Run full tracker regression suite and security check.
- [ ] Review and merge PR after exact-head CI.
- [ ] Trigger owner-only Issue #528 command, confirm actual deletions and recovery tags.
