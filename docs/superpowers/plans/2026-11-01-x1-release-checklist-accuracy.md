# X1 Release Checklist Accuracy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct the remaining verified stale release-engineering checklist claims without changing build or signing behavior.

**Architecture:** Update only the existing Play release checklist, distinguishing observed signed rolling publication from a Play-store-ready release and from owner-controlled keystore protections.

**Tech Stack:** Markdown; source verification against Git and GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-01-audit-remediation-design.md`

## Global Constraints

- No Kotlin, UI, theme, build, signing, CI workflow, dependency, privacy-flow or APK modification.
- Source and exact-SHA CI evidence establish checked items; unverified Play/device/security settings stay unchecked.
- Do not relabel a signed rolling preview as accepted on a physical device or published on Google Play.

## Review Focus

- Wrapper scripts, properties and JAR must be tracked in current `main` before checking the wrapper item.
- Only signed release APKs are public rolling assets; debug remains a private workflow artifact.
- Signing secrets are working in CI, but an independent offline recovery backup and protected environment are not verified.
- Manual signed AAB for Play and actual Redmi upgrade are still unverified.
- Changes must not claim a new review of current Google Play policies.

---

### Task 1: Correct release-engineering status

**Files:**
- Modify: `docs/PLAY_RELEASE_CHECKLIST.md`
- Create: `docs/superpowers/plans/2026-11-01-x1-release-checklist-accuracy.md`

**Interfaces:** None; documentation only. Read the current `android-ci.yml`, tracked wrapper files and successful main-push signed-publish run.

- [ ] **Step 1:** Verify each stale claim against the current commit, required files and successful signed-publish run.
- [ ] **Step 2:** Correct checkbox states and wording; preserve separate pending phone, Play Console, environment protection and offline custody gates.
- [ ] **Step 3:** Run relevant repository tests, security scan, Markdown path checks and `git diff --check`.
- [ ] **Step 4:** Open one X1 PR against latest main, wait for exact-head Android CI and PR-secrets checks, recheck main, merge if safe and verify the rolling publisher.
