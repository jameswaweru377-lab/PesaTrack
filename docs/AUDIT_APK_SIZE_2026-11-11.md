# PesaTrack APK size and unused-code audit — 11 November 2026

Baseline: `main@24d4d238076da7f3e14ad0134119b6dbf5be5afc`.
Inspected the public `ci-apk` release `PesaTrack-Full.apk`: **66,999,531 bytes / 63.90 MiB**; the Update APK has the same published size. Results below are from the APK ZIP contents, not estimates derived from source size. The ZIP entry payloads total 66,704,633 bytes; archive structures/signing account for the remaining ~294,898 bytes.

## What occupies the APK

| Category | ZIP-entry payload bytes | Share of payload | Finding |
|---|---:|---:|---|
| Embedded Python/PDF runtime and libraries | 34,339,124 | 51.48% | In active use for offline PDF/statement extraction; preserve |
| Android DEX bytecode | 17,776,715 | 26.65% | Release shrinking not configured; existing PR #549 already proposes R8/resource shrinking |
| Bundled ML Kit OCR runtime/models | 12,336,869 | 18.49% | In active use in shopping photo scanning; preserve |
| Other resources/assets | 2,251,925 | 3.38% | Most small; inspect individually before removal |

The two large offline runtimes together occupy **69.97%** of the ZIP payload. They are not obsolete code. Disabling PDF parsing or shopping OCR merely to reduce APK size would break approved features.

## Findings register

| ID | Severity | Status | Action | Evidence and decision |
|---|---|---|---|---|
| SIZE-01 | P2 | Fixed in branch; build pending | REMOVE | `SelectedAccountsCompass` and its private `HeritageAccountCard` had no call sites outside their definitions; the approved Accounts surface is now static. Removed 127 lines and three exclusively used decorative WebP images (87,876 source bytes). No ledger or storage code touched. |
| SIZE-02 | P2 | Open, separately owned | UPDATE | `app/build.gradle.kts` lacks release `isMinifyEnabled`/`isShrinkResources`. Existing open PR #549 handles this area. Benchmark actual signed APK and test reflection/serialization/Python bridges before merging; do not make an overlapping release change here. |
| SIZE-03 | P2 | Keep | UPDATE | `pdfplumber` through Chaquopy is reached by `OfflineBankTableReader` and `OfflineMpesaPdfReader`, both called by `StatementImportScreens`. The package accounts for substantial size but is actively used. |
| SIZE-04 | P2 | Keep | UPDATE | ML Kit text recognition is called by `ShoppingIntelligencePanels`; offline photo-scanner fallback is approved and in use. |
| SIZE-05 | P3 | Keep | UPDATE | All other `drawable-nodpi` image names were found referenced in application code. No other unused drawable is proven by this static search. |

## Confidence, limitations, and next steps

The removed carousel is an isolated, source-level dead-code finding. This is **not** a claim that every class in the project was proved reachable or unreachable: Kotlin/Compose references, reflection, resource lookup and Python imports require additional checks. This audit inspected dependency configuration, current APK ZIP entries, production Python and OCR call sites, all `drawable-nodpi` references, the replaced Accounts implementation, and current open PRs. It did not inspect each of the hundreds of Kotlin files line by line or run a whole-program dead-code analysis.

Local baseline `./gradlew :app:testDebugUnitTest --offline --console=plain` could not start: Gradle 9.5.0 distribution was absent and the wrapper could not reach `services.gradle.org`. Therefore no Android compilation, unit-test, lint, signed APK size delta, physical phone or in-place upgrade has been verified for this branch. Static reference scans and `git diff --check` are available checks, not substitutes for CI.

1. Run CI unit tests, lint, and release APK assembly for this small removal; compare APK size and retain the approved Accounts layout.
2. Review the separately owned shrinking PR #549 with its own tests and an APK before/after table; do not merge it blindly.
3. Only consider replacing embedded Python/PDF infrastructure after a separate verified design preserving local PDF import accuracy and compatibility. Do not remove functionality for cosmetic size reduction.

Strengths worth preserving: offline statement reading, on-device shopping OCR, local-only financial ledger, the approved static Accounts UI, and source-level asset compression (existing WebP imagery is a comparatively small part of the APK).
