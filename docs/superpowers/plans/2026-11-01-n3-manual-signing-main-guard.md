# N3 Manual Signing Main Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the unmodified manual signed-release workflow from executing its signing job when dispatched from any ref other than `refs/heads/main`, while retaining the rolling APK publisher.

**Architecture:** Make a small job-level `if` change in the independent signed-release workflow, with a parsed-YAML contract test. Explain that the YAML guard alone is not a trust boundary against someone who can edit a branch's workflow: an owner-managed protected environment, branch protection and secret scoping remain mandatory for strong custody.

**Tech Stack:** GitHub Actions YAML, Python `unittest` and PyYAML.

**Spec:** `docs/superpowers/specs/2026-11-01-audit-remediation-design.md`

## Global Constraints

- Do not change `.github/workflows/android-ci.yml`, its `ci-apk` rolling publisher or APK names.
- Never change, print or regenerate the release keystore, release signing certificate or its four secret values.
- Do not claim environment settings or the offline keystore have been independently verified.
- The existing manual validation jobs must continue on pull requests, and signing must continue on an explicit `main` dispatch after validation.

## Review Focus

- Manual dispatch on a feature branch: signing job must be skipped (tested by the condition asserting `github.ref`).
- Manual dispatch on `main`: signing job still runs after both required validations (tested by unchanged validation terms).
- Pull request touching the release workflow: validation may run, but signing remains ineligible (tested by event guard).
- Rolling main publisher: remains main-push-only and unchanged (tested by workflow snapshot and diff review).
- Branch-editable workflow bypass: document remaining protected-environment/secret-scoping task rather than claim YAML alone solves it (document review).

---

### Task 1: Guard independent manual release signing

**Files:**
- Modify: `tools/test_security_ci_config.py`
- Modify: `.github/workflows/release-build.yml`
- Modify: `docs/RELEASE_PROCESS.md`
- Create: `docs/superpowers/plans/2026-11-01-n3-manual-signing-main-guard.md`

**Interfaces:** Read parsed YAML via existing `load_workflow(name: str)`. Assert the release job condition requires `github.event_name == 'workflow_dispatch'`, `github.ref == 'refs/heads/main'`, and both required validation result checks. No runtime API changes.

- [ ] **Step 1: Write failing `SecurityWorkflowTest.test_manual_signed_release_is_main_only`** asserting the release job condition's branch check and required validations. Add a regression assertion that the rolling publisher remains main-push-only.
- [ ] **Step 2: Run** `python3 -m unittest tools.test_security_ci_config.SecurityWorkflowTest.test_manual_signed_release_is_main_only -v`. **Expected:** FAIL because the branch guard is missing.
- [ ] **Step 3: Add** the exact `github.ref == 'refs/heads/main'` release-job gate, preserving all existing validation conditions and secrets. Document the main-only entry point and the unsolved branch-editable workflow risk.
- [ ] **Step 4: Run** the focused test and `python3 -m unittest tools.test_security_ci_config tools.test_publish_rolling_apks -q`; **expected:** all pass. Run tracker tests, `python3 tools/security_scan.py`, a Git diff check and confirm `android-ci.yml` unchanged.
- [ ] **Step 5: Commit** the four scoped files, open one N3 PR, check exact-head CI and merge only if required checks are green and no path conflict exists.
