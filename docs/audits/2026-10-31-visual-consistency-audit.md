# PesaTrack — cross-screen visual consistency audit

**Date:** 31 October 2026 (Africa/Nairobi)
**Baseline:** `origin/main` `b8fc54f` at start; isolated audit branch
`feat/visual-audit-20261031`
**Method:** route map + targeted Jetpack Compose source inspection, approved
Home/Activity reference, saved 28 October UI/UX handover and historical 10
October visual audit. **No APK rendering, screenshots from this exact commit,
emulator, TalkBack or device measurement.** Treat visual outcomes as risks until
checked on a phone. No personal statement or ledger data was inspected.

## Summary

The four-tab Home / Activity / Plan / More shell, light-first theme, branded
header, and core Home/Activity visuals provide a strong coherent base. Profile,
Settings, and More use recognizable grouped-card navigation. The main risks now
cluster in **touch target sizes, tiny supporting labels, long monetary figures,
secondary page alignment, and one system-dark-mode mismatch**. Intentionally
rich gradients and dark introductory/security artwork are not defects.

This audit sampled the relevant code in **106 screen Kotlin files** (about
50,000 lines in that directory), inspected the important route and component
call sites, and cross-checked current PR ownership. It does **not** mean that
every conditional Compose state or every device size was visually tested.

### Screen inventory

| Area / route | Observed implementation | Source-level judgment | Device evidence still needed |
|---|---|---|---|
| Global shell/nav | One scaffold, branded top bar and four-tab bottom bar | **Consistent**; don't restore historical five-tab artwork | Safe-area/gesture, TalkBack |
| Home | Approved gradient hero, three money columns, upcoming, recent Activity | **Mostly aligned**; small month link and 9sp money labels need attention | Long KES, 320dp width, large fonts |
| Activity, five views | Shared filter + donut summary + grouped ledger, progressive bottom loader | **Mostly aligned**; month and analytics targets small, summary text may shrink | 5 modes, search, dark OS/light app, scrolling |
| Transaction details/category | Dedicated detail and source evidence screens | **Requires screenshot comparison**, do not disturb live transaction logic | Source/metadata and long merchant names |
| Plan overview | Approved colorful cards and real planning values | **Mostly aligned**; small sublabels may compromise font scaling | Tight cards, dynamic text, empty plan |
| Budgets | Cards, editing and progress indicators | **Needs device review**; old October recording no longer reflects all updates | Long category names, 3-column grid |
| Goals | Vibrant goal cards and progress bars | **Needs device review**; older low-contrast complaint not reverified on current UI | Text on goal illustrations, large savings |
| Bills | Vibrant list and month filter | **Correction candidate**: month pill may be under 48dp | Font scaling and month picker |
| Salary / Pay Cycle | Salary source/receipt tabs, loading and error states | **Functional state coverage visible**; generic TabRow differs visually from vibrant Plan | Tab width, keyboard and errors |
| Accounts / Debt / Receivables | Confirmed-only cards and separate debt history | **Mostly sound**; small clickable section link | Number overflow, zero debt, review states |
| More | Grouped cards, expandable money tools | **Aligned structurally**; another active PR owns styling | Long subtitles and background spacing |
| Profile | Purple identity banner; grouped settings rows >=70dp | **Aligned** with approved Design 2 direction | User photo/name/email scaling |
| Settings / privacy / security | Settings rows >=64dp, focused pages, app-lock sheets | **Aligned structurally**; privacy copy should be readability-tested | Consent and text scaling |
| Backup/restore | Separate Create/Restore bottom-sheet flow | **New approved flow**; leave cryptography/restore semantics untouched | Small device/keyboard/auth overlays |
| Statements summary | Unified year/account filters, cash totals, coverage, account rows | **Highest-priority correction area**: tiny month targets and possible truncated totals | Empty/limited/verified states, long values |
| Bank statement & coverage | Year chips, chart, month grid and saved evidence | **Correction candidate**: chips can overflow without horizontal scroll | Multiple years and banks, mixed currencies |
| Import workspace | Full-screen dialog, three-step rail, review flows | **Aligned with local-first intent**; step chips may truncate | Keyboard, PDF errors and large text |
| Data Health / diagnostics | Dense technical inspection page | **Purposefully advanced**, but visual hierarchy needs human inspection | Long status messages, errors and actions |
| Onboarding/permissions | Dark intro art, source selection and permission pages | **Dark intro intentional**; tiny permission copy is a readability risk | Android restricted-settings prompts |
| Reports / insights | Graph components and statement evidence views | **Not visually validated on current APK**; October zero-bar finding historical | Zero-value bars and truthful empty states |

## Findings register (source-backed unless marked runtime-only)

P1 = important before calling visual QA complete; P2 = next polish cycle;
P3 = low-risk cleanup. `OPEN` means **not fixed in this audit**. Statuses here
are deliberately honest about source-only evidence.

| ID | Severity / status | Evidence | Finding and action | Concurrent ownership |
|---|---|---|---|---|
| VC-01 | P1 / OPEN | `ui/screens/StatementUnifiedOverviewPage.kt:264-295` | **UPDATE:** Each of twelve coverage bars is a separately clickable **16dp-tall** box with a **9sp** single-letter month legend. At phone width each column is also much narrower than 48dp. Make the **whole account-coverage row** the accessible >=48dp target, and retain all twelve colored bars as visual indicators with equivalent state descriptions. | Clear of listed active PR files at audit start |
| VC-02 | P1 / OPEN | `ui/screens/HomeReferenceDesign.kt:238-244` | **UPDATE:** month range is an 11sp clickable `Text` without an explicit 48dp target. Add an accessible container, preserving the Home reference visually. | PR #456 touches this file; **defer** |
| VC-03 | P1 / OPEN | `ui/screens/HomeReferenceDesign.kt:258-276,364-372` | **UPDATE:** 3-column money figures may ellipsize; upcoming salary/bill amounts are **9sp**. Test long KES amounts and accessible alternatives before editing calculations/labels. | PR #456; **defer** |
| VC-04 | P1 / OPEN | `ui/screens/StatementUnifiedOverviewPage.kt:377-389` | **UPDATE:** statement Money in/out amounts use 20sp single-line ellipsis; a large verified amount could be hidden. Decide a wrapping/scaling layout after rendered checks; never silently abbreviate money. | Clear of listed active PR files |
| VC-05 | P1 / OPEN | `MainActivity.kt:151-160`, `ui/screens/ActivityScreen.kt:1869-1888` | **UPDATE:** the app explicitly sets `useDark = false`, but the Activity discovery card chooses colors/shadow using **OS** `isSystemInDarkTheme()`. On a dark-OS/light-app phone, card styling diverges. Use actual app theme semantics after testing; do not alter account review logic. | Shared Activity core; isolated future patch |
| VC-06 | P1 / OPEN | `ui/screens/ApprovedPrivacyConsentPage.kt:100-120,186-193` | **UPDATE:** some permission/consent explanations are 10–11sp. Audit readability at large font sizes and contrast before adjusting approved privacy wording. | Sensitive consent UX; separate review |
| VC-07 | P2 / OPEN | `ui/screens/StatementBankOverviewPage.kt:161-174` | **UPDATE:** bank-year `FilterChip`s are placed in a fixed `Row`, with one chip per historical year; many years can overflow horizontally. Use a horizontal scroll container without changing filtering. | Clear of listed active PR files |
| VC-08 | P2 / OPEN | `ui/screens/ActivityReferenceSummary.kt:166-183` | **UPDATE:** Activity month selector has no minimum height and analytics `Surface` has a 20dp icon + 6dp each side (about 32dp). Enlarge hit areas independently of visuals. | Clear of listed active PR files |
| VC-09 | P2 / OPEN | `ui/screens/BillsReferenceScreen.kt:192-227` | **UPDATE:** month picker pill has 7dp vertical padding and small label/icon; likely shorter than recommended 48dp target. | Clear of listed active PR files |
| VC-10 | P2 / OPEN | `ui/screens/AccountsAndDebtsScreens.kt:317-326` | **UPDATE:** clickable “Debt overview / See debts” heading has no minimum hit height. A full-width 48dp row can keep the visual text. | Clear of listed active PR files |
| VC-11 | P2 / OPEN | `ui/screens/StatementImportWorkspace.kt:105-135` | **UPDATE:** three equally weighted stage labels have 4dp horizontal padding and single-line text; `1 Account` / `2 Document` / `3 Review` may truncate under large fonts. | Adjacent to PR #459 import changes; **defer** |
| VC-12 | P2 / OPEN | `ui/theme/PesaDesignTokens.kt:9-19`; `ui/screens/{ProfileScreen.kt:134-137,MoreScreen.kt:59-68,SalaryScreen.kt:144-145,StatementUnifiedOverviewPage.kt:51-55}` | **REVIEW:** token says 18dp screen horizontal / 14dp section gap, but these screens use 16dp and gaps 10–14dp. Not automatically wrong; compare screenshot before normalization. | Shared foundation branch and More PR; **defer** |
| VC-13 | P2 / OPEN | `ui/screens/{StatementUnifiedOverviewPage.kt:50-78,StatementBankOverviewPage.kt:35-66,StatementImportWorkspace.kt:39-91}` | **REVIEW:** Statements transitions between branded cards, generic bank detail and full-screen Material import; confirm page headers, padding and primary action alignment across actual navigation. | PRs #459/#460; **defer** |
| VC-14 | P2 / OPEN | `ui/screens/SmartOnboardingPermissionComponents.kt:152-171` | **UPDATE:** 10–11sp descriptions and button copy on permissions; recheck with HyperOS dialogs and 1.3x font scaling. | Onboarding owner review |
| VC-15 | P2 / OPEN | `ui/screens/ActivityReferenceSummary.kt:230-255` | **REVIEW:** donut center number can shrink to 10sp for large values; prove no clipped or unreadable financial figures on narrow devices. | Clear of listed active PR files |
| VC-16 | P2 / OPEN | `ui/screens/StatementEmptyLanding.kt` (search `M-Pesa`, `CSV`) | **UPDATE:** older source-restriction guidance should be checked against current Equity PDF support. Do not rewrite import verification rules. | PR #459; **defer** |
| VC-17 | P3 / OPEN | `ui/components/AppScaffold.kt:58,113-135` | **REMOVE (if confirmed):** `toolsOpen` sheet appears unreachable; only remove after inspecting current call path. | PR #456; **defer** |
| VC-18 | P3 / OPEN | screen-directory `Color(0x...)` / custom-radius usage | **REVIEW, not bulk replace:** 300 direct hex colors across 27 files, 261 explicit rounded shapes across 47. Many are intentional gradients/illustrations. The shared token foundation branch is already addressing reuse. | Parallel visual foundation branch |
| VC-19 | P2 / NEEDS RUNTIME EVIDENCE | `ui/screens/HomeScreen.kt:861-916`, `ui/screens/ActivityScreen.kt:4080-4122` | **TEST:** Home has a startup placeholder, Activity has a bottom incremental loader. Source does not prove or disprove reported tab-switch flicker; profile with device or controlled emulator before changing refresh logic. | Broad data/cache logic; separate task |
| VC-20 | P1 / SOURCE-CONFIRMED, OPEN | `ui/screens/StatementReportVisuals.kt:153-181,191-214`; saved *Independent UI/UX Audit*, 10 Oct 2026, p. 2 | **UPDATE:** Statement monthly charts give a zero-valued imported month a **10dp purple bar**, and trend bars give an observed zero a **12dp purple bar**. Thus a zero can look like a positive amount, matching the historical report's warning. Make observed zero bars invisible or zero-height, retaining a distinct grey placeholder only for **no evidence**. Test zero vs missing vs positive before changing. Other historical complaints still require device revalidation. | No conflict in listed active PR files |
| VC-21 | P1 / MEASURED SOURCE CONTRAST, OPEN | `ui/screens/StatementUnifiedOverviewPage.kt:330-345` | **UPDATE:** review badge text `#B96008` on `#FFF0DF` measures **3.96:1** at 10sp; review banner text `#C96510` on `#FFF2E5` measures **3.57:1** at 13sp. Both are below 4.5:1 small-text guidance. Choose darker approved warning ink; verify on-device for actual compositing. | Clear of listed active PR files |
| VC-22 | P2 / MEASURED SOURCE CONTRAST, OPEN | `ui/screens/StatementUnifiedOverviewPage.kt:236-251`, `ui/screens/ActivityReferenceSummary.kt:292-308` | **UPDATE/REVIEW:** orange status icon `#E78316` on `#FFF0DF` is about **2.45:1**; Activity orange icon `#FF7100` on `#FFE5CC` is about **2.28:1**, under 3:1 non-text guidance if these are standalone status indicators. Confirm nearby label conveys meaning and test actual icon compositing. | Isolated widgets |

## Existing strengths to preserve

- `AppScaffold.kt:100-106`, `HomeReferenceDesign.kt:93-117`: one four-tab bar;
  Accounts is still a route but **not** a fifth bottom tab. No extra bottom
  nav was found in the Statements overview files inspected.
- `ui/theme/Theme.kt`: a light-first purple brand palette. The dark gradient
  intro (`OnboardingScreen.kt:25-86`) and app lock illustration are deliberate.
- `ProfileScreen.kt:275-299` and `SettingsScreen.kt:137-159`: full-width
  tappable rows have >=64–70dp minimums already; preserve this pattern.
- `ActivityScreen.kt:4080-4122`: progressive loader lives with transaction
  placeholders, not a persistent top overlay.
- `StatementUnifiedOverviewPage.kt:205-233`: cash-movement scope is clearly
  described; lower-assurance rows are kept out of verified totals.
- `BackupRestoreFlow.kt`: Create and Restore have separated steps and guarded
  operations; UI visual audit does not modify encryption or restore.

## Safe sequence, not a blanket redesign

1. **Next focused UI-test window (isolated):** correct observed-zero bars,
   enlarge Activity and statement *row* hit targets, darken review-text colors,
   and make bank-year chips scroll. Preserve visual bars and callbacks,
   colors and approved graphics. These are the least overlapping fixes.
2. **After #456 and UI foundation merge:** review Home month link, tiny
   financial labels, Activity dark-OS card, and shared token alignment on
   actual screenshots.
3. **After #458/#459/#460 settle:** compare More, Statements, Import and
   adjacent detail pages as one journey; solve overflow and header differences.
4. **Dedicated accessibility/device pass:** 320dp width, Redmi Note 14,
   1.0x/1.3x/1.5x fonts, light app under dark OS, TalkBack, contrast, long
   KES values, zero states, keyboard and system insets. Capture screenshots
   and approve before declaring visual perfection.

## Verification and concurrency

- Baseline local Android build attempt could **not start** because Gradle
  wrapper cache under `/root/.gradle` is read-only. Android `adb` is absent.
- This repository intentionally leaves instrumentation manual/opt-in; an
  Android UI test that compiles is not evidence of rendered tap sizes or color.
- At audit start, active/overlapping PRs #456, #458, #459, #460, #477,
  #489 and #492 were identified. The local parallel UI-foundation branch
  additionally touches design tokens/components. Keep this report/fixes in a
  unique branch and **never merge blind over their work**.
- Findings are source-verified risks, not evidence of current device failure
  unless explicitly stated. No pixel-accuracy or accessibility certification.

### Exact acceptance scenarios for a later controlled device run

| Scenario | Expected behavior / measurement |
|---|---|
| Dark system, PesaTrack light | Activity review cards remain visually light and match the app theme |
| Statements: one account, 12 months | A full-width account coverage row is one >=48dp-tall target; colored month indicators do not create twelve unreachable tiny tap targets; the row still announces imported/review months |
| Statements: 0, unknown and positive month | Observed **KES 0** has no purple-positive bar; **no statement** uses the defined grey placeholder; positive months scale by data |
| Statements: KES 9,999,999,999.99 | The full amount is readable or available through a clearly accessible detailed view; no silent ellipsis implying another value |
| Bank history: 2021 through 2026 | Every year filter remains reachable by horizontal scrolling, with selected state clear |
| Activity month/analytics and Bills month | Each tap area measures >=48dp without enlarging decorative icon art unnecessarily |
| Home on 320dp phone with 1.5x fonts | Month and amount controls remain identifiable; no hidden key monetary amounts or clipped bottom bar |
| Review and warning color | >=4.5:1 for regular text and >=3:1 for meaningful non-text graphic elements (measure actual rendered colors) |
| Profile/More/Settings | Large-finger tap targets, correct back behavior and same four-tab shell on re-entry |
| Full import flow with keyboard | Full-screen import stepper, error and save confirmation remain reachable above IME; no old nested bottom bar |

## Audit decision log

- **Ruling:** keep four bottom tabs; old five-tab design references are stale.
- **Ruling:** do not remove raw hex colors en masse; many define intentional
  artwork and financial status semantics.
- **Ruling:** do not touch current financial amounts, transaction processing,
  data permissions, backups or release workflows during visual cleanup.
- **Ruling:** do not run default hour-long emulator instrumentation without a
  separate, specifically requested device-validation window.
- **Ruling:** text/icon contrast ratios are computed from source RGB hex
  values (standard sRGB relative luminance), not sampled screenshots; validate
  actual rendered overlays in a later device pass.
