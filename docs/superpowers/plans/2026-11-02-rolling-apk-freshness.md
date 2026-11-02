# Rolling APK Freshness Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans. Implement one tested change at a time.

**Goal:** Prevent an outdated or unverifiable `main` build from replacing PesaTrack's two signed rolling APK assets.

**Architecture:** Keep the existing Android CI, signing identity, version progression and two-asset contract. Add a fail-closed remote `main` SHA check in the existing publisher immediately before release mutations and before uploading. Use the already existing synthetic GitHub CLI harness for regression tests.

**Tech stack:** Bash, GitHub CLI, Python `unittest` (no new dependencies).

**Spec:** User-approved CI-efficiency request and `docs/RELEASE_PROCESS.md`; tracked by GitHub Issue #588.

## Global constraints

- No financial, app, signing-key, backup or APK format changes; existing app ID, API24 floor, ARM64 filter and versionCode monotonicity remain intact.
- Do not edit `.github/workflows/android-ci.yml`: active other-owned PRs overlap it.
- Instrumentation remains explicitly opt-in. Any documentation-only publication skip needs a later separately claimed workflow PR, keeping required status checks runnable.
- This is a concurrency safeguard, not a guarantee of atomic GitHub Release updates if `main` moves inside the upload operation.

## Alternatives

- Skip **all** Android CI on docs-only commits: rejected because required checks could remain pending and unit/secret validation would be lost.
- Skip only the signed publication job for docs-only commits: preferred follow-up after current workflow-file overlaps clear, retaining normal PR checks.
- Check remote `main` in the existing publisher: **this batch**; independent, small and enforceable before most release mutations.

## Review focus

- Stale main SHA: old run must leave both assets unchanged.
- Unavailable/malformed GitHub main ref: fail closed, leave assets unchanged.
- Main moves during pre-publication checks: check again just before upload.
- Valid current main: retain two identical signed assets and prune historical attachments.
- Existing certificate and non-increasing versionCode refusals: preserve current tests.

## Task 1: Test and enforce remote-main freshness

**Files:** Modify `tools/test_publish_rolling_apks.py`; modify `tools/publish_rolling_apks.sh`.

- [ ] Add a synthetic `gh api repos/<repo>/git/ref/heads/main --jq .object.sha` stub and test current, stale, malformed, API-error and changed-between-checks cases. Require no release mutation on refusal.
- [ ] Run `python3 -B -m unittest tools.test_publish_rolling_apks -v`; observe new tests fail against the original publisher.
- [ ] Add `require_current_main()` to publisher: retrieve and validate exact 40-character SHA, compare to `GITHUB_SHA`, fail closed; call before release creation and immediately before upload.
- [ ] Run publisher tests again; require every case pass, including previous signer/version safeguards.
- [ ] Run `bash -n tools/publish_rolling_apks.sh`, security scan, `git diff --check`.

## Task 2: Documentation, review and release gate

**Files:** Modify `docs/RELEASE_PROCESS.md` to describe freshness refusal, concurrent-main limit, no unrequested instrumentation and deferred doc-only optimization.

- [ ] Verify doc matches current code, and record the remaining workflow-file overlap constraint.
- [ ] Commit only the publisher, tests, plan and release docs on the scoped branch.
- [ ] Open a PR with `Tracks #588` and `Chat owner: CHAT-CI-PUBLISH-FRESHNESS`, run exact-head Android CI/PR Secrets, independently review diff, and merge only if latest-main and required checks pass.
