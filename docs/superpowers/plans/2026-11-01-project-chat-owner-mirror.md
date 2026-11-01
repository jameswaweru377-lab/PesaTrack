# Project chat-owner mirror implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make explicit canonical Issue chat ownership visible in the existing PesaTrack Project without manual re-entry.

**Architecture:** Add an optional owner-text plan to `tools/tracker/project_sync.py`, sharing the existing verified Project snapshot and exact Issue-card map. Keep the established status mirroring and all human fields intact. An additional workflow variable is the explicit activation gate.

**Tech Stack:** Python `unittest`, GitHub GraphQL Project v2, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-01-project-chat-owner-mirror-design.md`

## Global Constraints

- Only existing `Chat owner` text and `Tracker status` select fields may be written.
- No new secret, dependency, Android code, release or signing modification.
- Refuse incomplete evidence and ambiguous Project identity or Issue mapping.

## Review Focus

- A malformed/duplicated field must refuse owner mutations.
- Foreign/archived/duplicate Issue cards must not receive an owner.
- A missing or unclaimed Issue owner must never become an invented chat alias.
- Unset owner opt-in must preserve current status-only behavior.
- An owner update must use GraphQL `text`, not a status option mutation.

### Task 1: Test and implement owner planning

**Files:** `tools/tracker/project_sync.py`, `tools/tracker/tests/test_project_sync.py`

- [ ] Add failing tests for owner planning and Project snapshot field extraction.
- [ ] Run focused tests and confirm expected failures.
- [ ] Read the existing Project `Chat owner` text field in bounded GraphQL queries; make a separate strictly validated plan for exact Issue owner aliases / `Unclaimed`.
- [ ] Run focused and all tracker tests; confirm zero failures.

### Task 2: Test and implement guarded application

**Files:** `tools/tracker/project_sync.py`, `tools/tracker/tests/test_project_sync.py`, `.github/workflows/development-tracker.yml`

- [ ] Add failing tests for opt-in disabled, owner-text GraphQL mutation and status coexistence.
- [ ] Run focused tests and confirm expected failures.
- [ ] Add preview and gated owner updates to the existing mirror; never change legacy status mutations or Project auto-add.
- [ ] Run focused and all tracker tests; inspect the scoped diff.

### Task 3: Document handoff and verify CI

**Files:** `docs/tracker/README.md`, `docs/tracker/CHAT_OWNERSHIP.md`

- [ ] Explain the Project owner field's read-only preview, activation variable, canonical Issue authority and remaining unknowns.
- [ ] Run Python tests, security scan, `git diff --check`; push a linked PR using `Tracks #513` and `CHAT-COMMAND-CENTER`.
- [ ] Verify exact-head Android CI and PR Secrets; do not enable extra writes or merge before a real Project preview confirms the field.
