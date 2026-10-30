# Performance-First Security Scanners Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add useful repository and APK security checks without extending PesaTrack's existing Android CI/release build jobs.

**Architecture:** Keep the existing `tools/security_scan.py` and all release workflows unchanged. Add independent GitHub Actions entrypoints: fast PR secrets detection, scheduled/manual CodeQL Kotlin compilation, and manual MobSF scan of the already-published signed APK.

**Tech Stack:** GitHub Actions, Gitleaks CLI v8.30.1, CodeQL v4, Android Gradle/Kotlin, MobSF static API, Python unittest + PyYAML.

**Spec:** Owner approved the performance-first Option 1 on 30 October 2026. Separate jobs must not gate or mutate the current APK publication and instrumentation controls.

## Global Constraints

- Do not edit `.github/workflows/android-ci.yml`, `.github/workflows/release-build.yml`, signing, or Android app source.
- Do not run instrumentation or publish a new APK.
- Use immutable action SHAs and a pinned MobSF Docker image digest.
- Security findings are review suggestions, never automatic code fixes.
- Do not pass live financial data to scanners.
- Open a separate PR; do not merge without fresh CI evidence and approval.

## Review Focus

- Gitleaks accidentally scanning release secrets: ensure read-only permissions and no secret inputs.
- Gitleaks findings leaking via PR comments/artifacts: disable comments and upload.
- Gitleaks upstream action only scanning 30 PR commits: use the pinned CLI with explicit full base-to-head Git range.
- CodeQL not extracting Kotlin: specify `java-kotlin`, `manual`, and a Kotlin compilation task.
- MobSF rebuilding or changing release assets: download the existing rolling signed APK only.
- MobSF exposing its API over a public interface: bind the Docker port to `127.0.0.1` and use a per-run key.
- MobSF raw report exposing potential security weaknesses in a public repo: make artifact upload an explicitly warned, disabled-by-default manual choice.
- Default MobSF scan losing all useful findings after runner teardown: print allowlisted aggregate counts and heuristic score only.

---

### Task 1: Static safety regression tests

**Files:** `tools/test_security_ci_config.py`

- [x] Write assertions for each workflow's trigger, permissions, scanner mode, and release isolation.
- [x] Run the test and observe missing-workflow failures.
- [x] Confirm tests become green after writing the workflow files.

### Task 2: Independent secrets and Kotlin analysis

**Files:** `.github/workflows/security-gitleaks.yml`, `.github/workflows/security-codeql.yml`

- [x] Pin actions to verified commit SHAs and Gitleaks CLI to a checked SHA-256 release archive.
- [x] Keep Gitleaks on PRs only and cap its runtime.
- [x] Schedule CodeQL weekly and permit manual runs, compiling Kotlin independently.
- [ ] Verify the first GitHub-hosted Gitleaks and CodeQL executions and review errors.

### Task 3: Manual signed-APK scan

**Files:** `.github/workflows/security-mobsf.yml`

- [x] Download only existing `PesaTrack-Full.apk` from the `ci-apk` release.
- [x] Pin the scanner image, use loopback and ephemeral credentials.
- [x] Generate aggregate score/count indicators; raw artifact is opt-in with a public-disclosure warning and three-day retention.
- [ ] Verify a manually dispatched MobSF run with an actual APK.

### Task 4: Owner-facing usage and PR

**Files:** `docs/SECURITY_SCANNING.md`

- [x] Document timings as limits, not guarantees, and clearly distinguish scanner failures from findings.
- [ ] Verify YAML, privacy guardrails, repository diff, and new branch base.
- [ ] Open PR without merging; capture the run IDs and any unverified items.
