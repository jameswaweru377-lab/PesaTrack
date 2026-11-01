# R1 Physical-Device Acceptance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give PesaTrack a truthful, repeatable, non-destructive physical-phone acceptance record for M-Pesa PDF, Equity PDF, encrypted backup/restore, app lock and signed in-place upgrade.

**Architecture:** Pure documentation/process first. A reusable blank evidence template and a release-process link provide one source of truth for pass/fail criteria. A device result can only be entered by a person who actually ran the journey; automation will be addressed independently under T1.

**Tech Stack:** Markdown release documentation and existing GitHub Actions (no runtime changes in R1).

**Spec:** `docs/superpowers/specs/2026-11-01-audit-remediation-design.md`

**Tracking:** GitHub issue #523; chat owner `CHAT-AUDIT-R1`.

## Global Constraints

- Invented disposable financial inputs only; no personal statements, SMS or credentials in source control.
- No real financial restore onto the owner's only active phone. Stop at signing-certificate mismatch or missing independent backup.
- App ID, Android API24 support, signing, versionCode, SQLite, import and backup behavior unchanged.
- Every recorded PASS must name physical device/OS, test date, commit SHA, signing fingerprint and actual observed evidence. `NOT RUN` is the default.
- No branch merge or public release is authorized by a documentation PASS template alone.

## Review Focus

1. A debug-signed installed app cannot be overwritten by an unrelated release certificate: checklist must stop before uninstall/data loss.
2. A partial/unreconciled PDF must fail without producing canonical money.
3. Re-import must preserve exact-once ledger rows and debt/transfer totals.
4. An Equity limited source must remain visible but not become verified Activity income/spending.
5. An interrupted or wrong-password restore must not erase the pre-existing disposable ledger.

---

### Task 1: Reusable release-device acceptance template

**Files:**
- Create: `docs/acceptance/R1_DEVICE_ACCEPTANCE_TEMPLATE.md`

**Interfaces:**
- Consumes: signed candidate SHA/APK metadata and existing import/backup/lock journeys.
- Produces: a single device result record, default `NOT RUN`, with explicit pass/fail criteria and no sensitive inputs.

- [ ] **Step 1:** Read current `docs/RELEASE_PROCESS.md`, `docs/RELEASE_GATES.md` and relevant `CURRENT_STATE.md` acceptance notes to avoid contradicting them.
- [ ] **Step 2:** Write a template with metadata, preflight, four tested journeys, signed in-place upgrade, negative cases, test evidence and final PASS/FAIL/NOT RUN conclusion. No fabricated results.
- [ ] **Step 3:** Verify by reading the complete template and checking each Review Focus input has an explicit stop/fail outcome. Expected: five of five cases represented.
- [ ] **Step 4:** Run `git diff --check` and the existing repository security script. Expected: exit 0, no secrets or device-private content.

### Task 2: Link release policy without weakening existing automation

**Files:**
- Modify: `docs/RELEASE_PROCESS.md` (physical-device policy, before public Play release)
- Modify: `docs/RELEASE_GATES.md`
- Modify: `CURRENT_STATE.md` (release acceptance status only)

**Interfaces:**
- Consumes: Task 1 template path.
- Produces: a release checklist that distinguishes automated, emulator and physical-phone evidence.

- [ ] **Step 1:** Add a documented R1 release gate pointing to the template, with `NOT RUN` and explicit final-candidate-SHA requirements.
- [ ] **Step 2:** Keep rolling `ci-apk` publication described as *preview/available*, not as verified physical acceptance; do not change workflow triggers in this PR.
- [ ] **Step 3:** Re-read changed paragraphs for consistency with the signing/debug migration process and verify Markdown relative links.
- [ ] **Step 4:** Run `git diff --check`, `python3 tools/security_scan.py`, `python3 -B -m unittest discover -s tools/tracker/tests -q`. Expected: all exit 0; docs-only change is not an Android build.
- [ ] **Step 5:** Review diff against `main`, commit only these R1 files and create a **draft PR** with result/evidence/unverified/device-gate notes. Do not merge until owner review.
