# PesaTrack UI Consistency Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a safe, testable UI consistency foundation without changing approved PesaTrack visuals.

**Architecture:** An opt-in local source guard catches *new* hardcoded colors and one-off corner radii; shared Compose metrics move into the existing token object with identical numeric values. No screen or financial behavior changes.

**Tech Stack:** Python 3 standard library for the local guard; Kotlin and Jetpack Compose for tokens/components; existing Android Gradle CI for final verification.

**Spec:** `docs/superpowers/specs/2026-10-31-ui-consistency-foundation.md`

## Global Constraints

- Work from current `main` in `feat/ui-consistency-foundation-20261031`.
- Preserve approved light-first purple UI, four-tab navigation, existing functions and exact layout metrics.
- No mandatory CI jobs, new dependencies, app permissions, analytics, financial changes or release changes.
- Do not edit files owned by existing UI/documentation PRs; check again before opening/merging.
- Local Android baseline is blocked because this environment has no configured Android SDK. CI must supply Kotlin build/unit/lint/APK evidence; do not claim local Android tests passed.

## Review Focus

1. A removed or unchanged legacy raw color must not count as a new violation — cover in Task 1 tests.
2. New raw hexadecimal colors must be flagged with actionable file/line output — cover in Task 1 tests.
3. New arbitrary raw corner radii must be flagged, while token references remain allowed — cover in Task 1 tests.
4. A missing base revision or malformed diff must fail safely, without reporting a false clean result — cover in Task 1 tests.
5. Numeric token extraction must preserve the existing composables' pixel metrics and semantics — verify mapping and unchanged signatures in Task 2, then CI compile and diff review.

---

### Task 1: On-demand changed-line UI consistency audit

**Files:**
- Create: `tools/ui_consistency_guard.py`
- Create: `tools/test_ui_consistency_guard.py`

**Interfaces:**
- Produces: `scan_added_ui_lines(diff_text: str) -> list[Violation]`, where `Violation` includes file, new line, rule and offending expression.
- CLI: `python3 tools/ui_consistency_guard.py --base origin/main [--strict]`; without `--strict` it reports only; strict mode exits nonzero for violations or invalid diff/base.

- [ ] Write synthetic unittest cases for new `Color(0x...)`, one-off `RoundedCornerShape(21.dp)`, unchanged context, removed lines, token references, malformed diff and invalid base.
- [ ] Run `python3 -m unittest tools/test_ui_consistency_guard.py -v` and observe expected missing-interface failures.
- [ ] Implement a bounded parser of `git diff --unified=0` plus added-line checks for `app/src/main/java/**/ui/**/*.kt`; ignore unrelated files and no privately sourced fixtures.
- [ ] Rerun the unittest file; check valid samples, strict/report-only behavior, and failure paths.
- [ ] Verify `python3 tools/ui_consistency_guard.py --base origin/main --strict` runs on the branch without changing files.
- [ ] Commit only the two guard files.

### Task 2: Preserve existing shared-component metrics through tokens

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/theme/PesaDesignTokens.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/components/PesaComponents.kt`

**Interfaces:**
- Existing `PesaCard`, `BrandWordmark`, `SectionHeader`, `IconBubble` and `SmallStatusPill` signatures stay unchanged.
- Exact previously inline component metrics are named under `PesaDesignTokens`: compact/regular brand icons (`36.dp`, `44.dp`), brand gap (`10.dp`), action chevron (`19.dp`), card outline (`1.dp`), light elevation (`2.dp`), status pill insets (`10.dp`, `5.dp`). `IconBubble` uses the existing `42.dp` `iconBubble` token.

- [ ] Write and manually review a before/after metric mapping from the existing component source; review visual invariants and check for any mixed usages requiring unchanged values.
- [ ] Refactor only the listed literals into existing/new tokens, without touching color, padding, modifier ordering, shape or click behavior.
- [ ] Run the Task 1 guard in strict mode; verify no new raw colors or radii.
- [ ] Review `git diff --check` and `git diff --word-diff` against the captured mapping; no lost content or layout value change.
- [ ] Verify Kotlin build/unit/lint/debug assembly on GitHub CI, since local SDK is absent, and record exact checks and SHA.
- [ ] Commit just the two Kotlin files.

### Task 3: Controlled PR and merge gate

**Files:** No new code files.

**Interfaces:** Consumes the tested branch and current GitHub PR/check status.

- [ ] Recheck the open PR file lists and `main` HEAD for overlap. If overlap exists, pause and coordinate.
- [ ] Push the dedicated feature branch and create a small PR against `main`.
- [ ] Wait for all required checks at the final PR commit (JVM tests, Android lint, debug assembly); inspect failures rather than retry blindly.
- [ ] Confirm the merge base is current, change scope remains two Kotlin files plus guard and plan/spec, and there are no UI or financial side effects.
- [ ] Merge only if authorized and safely green. Do not change release workflows or delete existing assets.

## Self-review

- No whole-app UI migration or mandatory workflow additions.
- No planned changes to open-PR-owned screens/documents.
- Tests target observable guard outputs, not constants or source-text presence alone.
- Visual preservation is explicitly a diff-reviewed requirement, not a claim of device testing.
- Local Android SDK absence is a known verification limitation; the CI gate is mandatory.
