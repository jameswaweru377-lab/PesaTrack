# Rolling Two-APK Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically publish only Full and Update APK attachments for the current accepted PesaTrack build, enabling in-place Android upgrades when signing identity matches.

**Architecture:** Preserve Android CI's current validation job. A main-only publish job materializes the existing protected release keystore, assembles a monotonically versioned release APK, verifies compatibility with any previously published signed update, and replaces the rolling release assets.

**Tech Stack:** GitHub Actions, bash, Android Gradle Plugin, Android SDK `apksigner`/`aapt`/`zipalign`, GitHub CLI, Python validation tests.

**Spec:** `docs/superpowers/specs/2026-10-29-rolling-two-apk-design.md`

## Global Constraints

- APK ID: `com.kedevjimmy.pesatrack`.
- Only rolling assets: `PesaTrack-Full.apk` and `PesaTrack-Update.apk`.
- Never use debug signing for the user-facing rolling update channel.
- Never delete phone data or claim on-device upgrade proof from CI.
- Keep optional instrumentation opt-in.

## Review Focus

- Missing/invalid signing secrets must fail without modifying the existing rolling release.
- A previous signed update from another certificate must block publication.
- A version downgrade or repeat must block publication.
- Old commit-specific assets must be pruned only *after* successful signed upload.
- A rerun and a concurrent main push cannot roll back the published version.

---

### Task 1: Checked publication script

**Files:**
- Create: `tools/publish_rolling_apks.sh`
- Test: `tools/test_publish_rolling_apks.py`

**Interfaces:**
- Consumes: `APK_PATH`, `PESATRACK_VERSION_CODE`, `PESATRACK_VERSION_NAME`, `GITHUB_SHA`, `GH_TOKEN`, `GH_REPO`, `ANDROID_HOME`.
- Produces: verified signed rolling release with exactly two APK assets, or nonzero exit before unsafe publication.

- [ ] Write synthetic executable-tool test covering signing mismatch, version downgrade, and asset pruning.
- [ ] Run test; confirm expected failure from missing implementation.
- [ ] Implement shell release checks and publication.
- [ ] Run tests, `bash -n`, and review failures.
- [ ] Commit code and tests.

### Task 2: Main-only signed assembly and documentation

**Files:**
- Modify: `.github/workflows/android-ci.yml`
- Modify: `AGENTS.md`, `docs/RELEASE_PROCESS.md`, `CURRENT_STATE.md`.
- Test: `tools/test_publish_rolling_apks.py` plus YAML shape assertions.

**Interfaces:**
- Consumes: existing four release-signing secrets and successful `build` job at the source SHA.
- Produces: versioned signed APK passed to Task 1, then two rolling attachments.

- [ ] Write YAML invariants test for main-only signed release and two stable names.
- [ ] Run test; verify red until workflow updated.
- [ ] Implement signed build, secret preflight, explicit versioning and verified publish call.
- [ ] Update agent/release process and current-state documentation.
- [ ] Re-run script tests, parse YAML and inspect diff.
- [ ] Commit; open PR, verify CI and do not deploy until signing preconditions are confirmed.
