# PesaTrack UI Consistency — Foundation Specification

**Status:** Approved direction (Option 1); implementation details awaiting plan review.

## Goal

Make future PesaTrack UI changes more consistent without visually altering any approved screen or interfering with concurrently active feature pull requests.

## Current evidence

- The app already defines shared `PesaDesignTokens`, `PesaTypography`, `PesaShapes`, and components such as `PesaCard`, `SectionHeader`, `IconBubble`, and `SmallStatusPill`.
- Existing approved UI uses screen padding `18.dp`, section gap `14.dp`, card radius `22.dp`, and icon bubble `42.dp`; the audit's suggested `16.dp`/`24.dp` replacements are **not** authority to change approved pixels.
- The latest inspected `main` is `61e0dd9` (31 October 2026).
- Open PR #477 owns `DESIGN.md`, `CONTRIBUTING.md`, `README.md`, and audit-design documentation. Open PRs #456, #458, #459, and #460 own various navigation/More/Statements/Settings screens. These files are outside this change.

## Phase 1, intentionally limited

1. Add a lightweight **on-demand** UI source audit to report newly introduced raw hex colors or one-off raw corner radii in changed Kotlin UI lines. It must not block ordinary CI, flag existing unchanged code, report private financial data, or require new dependencies.
2. Move repeated fixed metrics already used in the shared components into `PesaDesignTokens`, keeping each exact value. Update only `PesaComponents.kt` to consume those tokens. Preserve its function signatures, colors, shapes, layout pixels and interactions.
3. Test the audit script against controlled synthetic samples. Verify Kotlin compile, unit tests, lint, debug assembly using existing CI before any merge. Diff-review every UI value to confirm unchanged visuals.

## Out of scope

- No changes to Home, Activity, Plan, More, Profile, Statements, Settings or imports.
- No changes to `PesaTrackApp.kt`, bottom navigation, financial logic, database, networking, backup, CI workflow, signing, releases, or dependency versions.
- No rewrite of `DESIGN.md` or other files owned by open PR #477.
- No global change to 18dp margins, 22dp radii, text sizes, contrast palette, or previously approved screens.
- No mandatory instrumentation or screenshot suite. Device visual acceptance is a later step, not implied by CI success.

## User-visible effect and follow-up

The foundation is **visually neutral**. It establishes a safer reusable basis and audit workflow; consistency migrations of individual screens follow as separate, conflict-checked tasks after their owners finish. A CI pass does not prove screenshots or Redmi behavior.

## Merge safeguards

- Work only on `feat/ui-consistency-foundation-20261031` from fresh `main`.
- Do not merge when main has conflicting changes, a required check is red, or another PR begins editing the same shared files.
- Obtain verified GitHub workflow results for the exact PR head; never alter the rolling signed APK release policy.
