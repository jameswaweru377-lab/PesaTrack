# Retired UI and first-load flash audit — 22 November 2026

Branch baseline: main at 7074f7ca. Scope: current app UI sources and
references from main, JVM and Android test code. This is a code-reference
audit, not proof that every runtime route has been exercised.

| ID | Severity | Finding | Status | Action |
| --- | --- | --- | --- | --- |
| UIR-01 | P1 | Home displayed retired GreetingBlock and an old loading card before approved coffee components | Fixed in source | REMOVE |
| UIR-02 | P2 | Plan displayed a separate text-only loading page on a first financial read | Fixed in source | REMOVE |
| UIR-03 | P2 | Activity displayed an old full-screen “Loading your activity…” message on month reads | Fixed in source | REMOVE |
| UIR-04 | P2 | Former Home B, reference, monochrome and overview layouts had no reachable production callers | Fixed in source | REMOVE |
| UIR-05 | P1 | Statements, Activity, Plan and investment dialogs still depend on helpers stored inside retired Home files | Fixed in source | UPDATE |
| UIR-06 | P2 | Other screens retain legitimate progress indicators for actual operations (imports, exports, photo saves, long-list paging) | Retained | KEEP |
| UIR-07 | P1 | First-load background and transitions still require real Redmi testing with local data | Open | VERIFY |

Retained exact behaviour: shared light/dark palette values, the
evidence-only investment dialog, future bill data type and amber bill pill.
The deleted HomeReferenceLayoutTest rendered the retired layout and could
not validate current Home.

The screen-loading refresh jobs and ledger revision cache rules were not
changed. No asset, APK signing, database, permissions, accounting or workflow
changes are included. The active onboarding illustration cleanup PR and
startup splash PR were deliberately left untouched.

Verification: the new scripts/check_retired_ui_entries.sh caught the original
Home issue before the fix. Run it, existing UI source contracts, diff checks,
JVM tests, lint and APK build before acceptance. Local Gradle could not
download version 9.5.0 in this environment; CI and physical Redmi checks
remain separate gates.
