# N2 Statement Feature Admission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record an unambiguous temporary hold on new statement capabilities until actual real-device and persistence-test evidence permits them to resume.

**Architecture:** A single linked admission checklist defines what is held, what correctness fixes may continue, and the exact evidence needed. The product charter, contributor instructions and current engineering state point to that checklist; no application or workflow logic changes.

**Tech Stack:** Markdown repository policies and acceptance records.

**Spec:** `docs/superpowers/specs/2026-11-01-audit-remediation-design.md`

## Global Constraints

- Preserve PesaTrack's existing M-Pesa and Equity behavior, financial meaning, privacy, release signing, Android 7 minimum and UI themes.
- No new required CI job, emulator dispatch, live-device action, network flow or source-code change.
- Never mark PASS from a test plan, successful compilation or an unrelated older commit.
- Preserve active draft PR ownership; this process gate is not permission to merge unverified work or rewrite another branch.

## Review Focus

- Existing-import safety fixes remain permitted with their own evidence; new providers and automatic ledger admission do not slip through as fixes.
- R1 and T1 must both actually PASS for the same final candidate SHA, not merely have a checklist or compiled tests.
- M-Pesa, Equity, backup/restore, app-lock and in-place upgrade need real physical-device results.
- Emulator must execute migration, atomic ingestion, replay, debt and money aggregation tests instead of only compiling instrumentation.
- No present-day PASS or automatic feature approval is claimed while evidence is NOT RUN.

---

### Task 1: Define the statement-admission evidence gate

**Files:**
- Create: `docs/acceptance/N2_STATEMENT_FEATURE_GATE.md`
- Modify: `docs/PRODUCT_CHARTER.md`
- Modify: `CONTRIBUTING.md`
- Modify: `CURRENT_STATE.md`
- Create: `docs/superpowers/plans/2026-11-01-n2-statement-feature-admission.md`

**Interfaces:** The R1 record at `docs/acceptance/R1_DEVICE_ACCEPTANCE_TEMPLATE.md` is the physical-device contract. The existing T1 emulator suite and its canonical task/CI record are the persistence evidence. No API or test interface changes.

- [ ] **Step 1:** Write a concise checklist separating blocked expansion from allowed fixes, with exact R1/T1 evidence and fail-closed PASS criteria.
- [ ] **Step 2:** Link the gate in the product charter and contribution instructions; record N2 as blocked/pending in CURRENT_STATE.
- [ ] **Step 3:** Verify all relative links and exact wording against current code/docs; run Python repository tests, security scan and `git diff --check`.
- [ ] **Step 4:** Commit one documentation PR from fresh main, wait for exact-head CI and PR secrets, then merge only if no newer overlapping change and required checks pass.
