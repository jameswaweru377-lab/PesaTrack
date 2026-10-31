# PesaTrack Development Command Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Track PesaTrack task ownership, audit findings, PR states and verified test evidence without interfering with concurrent Android development.

**Architecture:** GitHub Issues are canonical; a GitHub Project presents them. A small Python standard-library reconciler reads GitHub REST responses and emits a guarded report; GitHub Actions triggers it on PR/CI events and hourly. A dedicated ChatGPT project chat queries this durable evidence.

**Tech Stack:** GitHub Issues/Projects, GitHub Actions YAML, Python 3 standard library, existing repository test conventions.

**Spec:** `docs/superpowers/specs/2026-10-31-development-command-center-design.md`

## Global Constraints

- No Android application logic, database, signing, release, permissions, or dependency changes.
- No auto-merge, branch deletion, forced push, or automatic closing of tasks when a PR merges.
- No new mandatory Android CI job; no automatic instrumentation. Run tracker independently and lightweight.
- No public ChatGPT conversation URLs, financial information, credentials or raw private PDF/SMS content.
- Task status, PR status, CI status, and phone acceptance are distinct truths.
- Existing task ownership and concurrent branches must be preserved; historical imports are idempotent and initially untriaged.

## Review Focus

1. Two PRs report the same task: test that the issue lists both and warns rather than overwriting one.
2. Missing or failed CI: test that the status is unknown/failed, never passed.
3. Merged PR without phone acceptance: test that the issue remains open and needs acceptance.
4. Concurrent claim: test warning with no automatic reassignment or merge.
5. Repeated event/hourly run: test stable report/comment identity without duplicate issues or noisy updates.

---

### Task 1: Tracking schema and safe adoption instructions

**Files:**
- Create: `docs/tracker/README.md`
- Create: `docs/tracker/CHAT_OWNERSHIP.md`
- Create: `.github/ISSUE_TEMPLATE/pesatrack-task.yml`
- Create: `.github/PULL_REQUEST_TEMPLATE.md`
- Modify: `AGENTS.md` (one concise tracker-check instruction)

**Interfaces:** Issues are `PT-<issue number>`; PR body includes `Tracks #<number>` and `Chat owner: CHAT-<TOPIC>`; no private chat URL.

- [ ] Document issue lifecycle, accepted evidence, privacy and claim/release commands in `docs/tracker/README.md`.
- [ ] Define owner aliases and a claimed-task handoff format; never infer ownership from a chat title alone.
- [ ] Add templates that make task source, owner, CI, device acceptance and affected paths explicit.
- [ ] Add a one-paragraph preflight to `AGENTS.md` (check issue ownership, open PRs and latest `main` before editing).
- [ ] Validate YAML and links using an offline test; run `git diff --check`, then commit scoped docs.

### Task 2: Reconciler policy with offline red/green tests

**Files:**
- Create: `tools/tracker/reconcile.py`
- Create: `tools/tracker/models.py`
- Test: `tools/tracker/tests/test_reconcile.py`

**Interfaces:** `reconcile(snapshot: dict) -> dict` consumes invented GitHub Issues/PR/check-run fixtures; produces findings with `task_id`, `owner`, `prs`, `ci`, `merged`, `device_acceptance`, `warnings`, `checked_at`.

- [ ] Write failing tests for the five Review Focus scenarios and status transitions.
- [ ] Run `python3 -m unittest discover -s tools/tracker/tests -v`; confirm the intended failures.
- [ ] Implement pure parsing/joins and conservative statuses; no remote writes in the policy module.
- [ ] Re-run focused tests; add invalid metadata/untrusted PR-body test.
- [ ] Commit only tracker policy and tests.

### Task 3: Read-only GitHub evidence collection and event workflow

**Files:**
- Create: `tools/tracker/github_snapshot.py`
- Create: `tools/tracker/cli.py`
- Create: `.github/workflows/development-tracker.yml`
- Test: `tools/tracker/tests/test_github_snapshot.py`
- Test: `tools/tracker/tests/test_workflow_contract.py`

**Interfaces:** `fetch_snapshot(token: str, repository: str) -> dict`; CLI defaults to read-only `--dry-run` and writes only a GitHub Actions summary unless a separately gated `--sync-task-metadata` mode is enabled.

- [ ] Write tests for bounded pagination, unavailable checks, API throttling, unsanitized descriptions and event filtering.
- [ ] Implement least-privilege, timeout-bounded GitHub REST reads with no secret printing.
- [ ] Add triggers: PR changes, completion of relevant CI, `17 * * * *`, `workflow_dispatch`; do not run Android Gradle.
- [ ] Run unit and workflow-contract tests, YAML syntax check and `git diff --check`.
- [ ] Commit tracker CLI/workflow independently; keep it advisory until live outputs are reviewed.

### Task 4: Historical import preview and idempotent sync

**Files:**
- Create: `docs/tracker/AUDIT_SOURCES.md`
- Create: `tools/tracker/bootstrap.py`
- Test: `tools/tracker/tests/test_bootstrap.py`

**Interfaces:** `plan_import(audits: list[dict], existing: list[dict]) -> dict` lists create/link/review actions; actual mutations require explicit `--apply` and matching source IDs.

- [ ] Inventory October 30 architecture/release audit, October 31 visual findings, existing issues and active PRs.
- [ ] Write tests that repeated imports never duplicate existing source IDs and never mark inferred fixes verified.
- [ ] Implement a preview report, then guarded issue creation/synchronization without touching unrelated issues.
- [ ] Run the dry run and review target identities before any GitHub writes.
- [ ] Commit import manifest, preview and focused tests; apply only after reviewer checks the preview.

### Task 5: GitHub Project board, acceptance and ChatGPT handoff

**Files:**
- Create: `docs/tracker/COMMAND_CENTER_STARTER.md`
- Modify: `docs/tracker/README.md` (verified Project URL and owner setup notes)

**Interfaces:** GitHub Project `PesaTrack Development Tracker` presents canonical Issues and project fields for area, priority, chat owner, task status and last review.

- [ ] Check for an existing Project to avoid duplicate boards; use supported UI route if connector lacks Projects administration.
- [ ] Create/verify one board and auto-add labeled Issues; if a permission is needed, stop and ask only for the specific owner click.
- [ ] Apply validated audit import; link known PRs and explicitly preserve unverified device gates.
- [ ] Run a read-only live reconciliation on recent PRs including #478, #489 and #501, inspect actual CI/merge evidence.
- [ ] Save a simple Command Center opening message; advise James to create one chat in the existing ChatGPT project, not move existing chats.
- [ ] Verify the tracker action did not trigger Android builds, publish APKs, close tasks automatically or alter existing feature branches.

## Completion gate

The work is complete only after the Project board is accessible, test fixtures pass, live PR evidence is correct, overlapping/unknown work is flagged, and the owner receives the board link and new-chat starter. If GitHub Projects permission blocks board creation, keep the issue tracker usable and report that single remaining owner action; do not claim full completion.
