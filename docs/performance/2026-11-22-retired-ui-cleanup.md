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
| UIR-08 | P2 | Home still computed unused spending slices and today's total for retired cards, and referenced a deleted row type | Fixed in source after CI compiler error | REMOVE |
| UIR-09 | P2 | Former concept Bills, Goals and account-detail screen family has no production navigation or callers | Fixed in second pass | REMOVE |
| UIR-10 | P2 | Old Activity Design 3 and reference summaries have no callers, but their shared KES label is used in current Activity | Fixed in second pass; shared formatter retained | REMOVE / UPDATE |
| UIR-11 | P2 | Retired onboarding salary, permissions, savings review, welcome and setup compositions have no callers, while three permission helpers remain active | Fixed in second pass; permission helpers retained | REMOVE / UPDATE |
| UIR-12 | P3 | `onboarding_analysis_hero.webp` has no production reference | Fixed in second pass | REMOVE |
| UIR-13 | P2 | Release build has no explicit code/resource shrinking; no measured release APK breakdown is available | Open; keep current release settings unchanged | VERIFY |
| UIR-14 | P2 | Current splash is a separate, intentional 700 ms cold-start brand reveal; transitions and fonts need Redmi confirmation | Open | VERIFY |
| UIR-15 | P3 | Some historical component files contain both active shared widgets and unused local UI | Open; do not delete entire mixed-use files | REVIEW |

Retained exact behaviour: shared light/dark palette values, the
evidence-only investment dialog, future bill data type and amber bill pill.
The deleted HomeReferenceLayoutTest rendered the retired layout and could
not validate current Home.

The screen-loading refresh jobs and ledger revision cache rules were not
changed. No asset, APK signing, database, permissions, accounting or workflow
changes are included. The active onboarding illustration cleanup PR and
startup splash PR were deliberately left untouched.

CI compile feedback on PR #719 identified the remaining HomeSpendingItem
reference. Its derived variable and the upstream spending-slice and
spent-today calculations had no remaining consumers. Removing that dead
display-only graph preserves the authoritative ledger and current summaries
while avoiding wasted per-screen work. The follow-up source contract now
detects those orphaned values.

Verification: the new scripts/check_retired_ui_entries.sh caught the original
Home issue before the fix. Run it, existing UI source contracts, diff checks,
JVM tests, lint and APK build before acceptance. Local Gradle could not
download version 9.5.0 in this environment; CI and physical Redmi checks
remain separate gates.

## Second source-cleanup pass (main baseline `ed058a48`)

The first pass landed as PR #719 at `93a0ec86` after Android CI passed.
The newer main includes a separately approved daily splash and PIN lock;
those active surfaces were deliberately kept.

Call-site checks across production, JVM tests and Android test sources found
11 retired UI source files with no current production consumer. Removed the
inactive concept Bills/Goals/account-detail family, old Activity summary
cards, onboarding preview/setup pages, and the orphaned illustration.
Preserved `activityKesLabel` for active Activity summaries, along with SMS
and notification-listener access helpers for active onboarding/Settings.
The Android test for the removed optional-salary choice was retired; tests
for detected account ownership, institution search and M-Pesa confirmation
remain. All 63 bundled financial-message templates are indexed by the runtime
loader and remain unchanged. No Python PDF extraction, accounting, database,
permissions, backup, release signing, build settings or CI workflow changed.

The app contains no bundled custom font assets: global type uses system
sans-serif, while approved coffee headings still request serif/cursive.
Remaining font differences and first-load transitions need device review.
Resource/code shrinking needs a separate APK composition measurement and
regression-gated rollout; a 122 KB image removal is not a meaningful explanation
for the approximately 83 MB installed/downloaded APK.

The source-contract check was deliberately run red with the retired files
present, then green after deletion. Local Gradle remains blocked before build
configuration by a Gradle 9.5 distribution download failure. New Android CI
and physical-device acceptance are required for this second pass.
