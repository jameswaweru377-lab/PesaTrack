# PesaTrack unified premium UI — design specification

Status: **Owner-approved on 11 November 2026; implementation in progress**

## Intent

Bring all user-facing PesaTrack surfaces into one premium visual language,
following the approved Profile design: warm cream canvas, coffee-brown type and
controls, understated leaf-green accents, restrained imagery, compact
rectangular cards, quiet outlined icons and generous *overall* breathing room
without oversized *internal* padding.

The owner wants one coordinated release, not a succession of inconsistent APKs.
This is a **presentation-only** project: preserve all existing financial
meaning, routes, local data, callbacks, form decisions, privacy protections,
permissions, backups, APK signing and update compatibility.

## What is already true

- The Android app is written in Jetpack Compose. `PesaTrackApp.kt` currently
  declares 34 navigation destinations. Additional nested pages, dialogs,
  bottom sheets and reusable components are not separate top-level routes.
- `PesaDesignTokens.kt` and `PesaComponents.kt` already provide some reusable
  geometry/components, but `Theme.kt` still starts from an indigo/violet light
  Material scheme. Home/Activity, Profile, Plan and More independently override
  their styling; this is why changing one palette cannot standardize the app.
- The merged Profile editorial design provides these existing local colours:
  coffee `#6B381A`, cream `#FCF0E7`, card `#FFF8F3`, sand `#F3E3D6`, border
  `#E3D0C0`, green `#94B95B`. The green is decorative, not a readable
  foreground colour on white.
- Existing approved pages include locally bundled Nairobi, finance and other
  illustration assets. No personal photo, stock image or cloud lookup is
  required for the design system.
- Concurrent PRs may change Accounts, investments and balance presentation;
  final integration must rebase onto the actual latest `main` without
  overwriting their unrelated work.

## Approved design language

### Colour system

| Token | Proposed light value | Intended use |
| --- | --- | --- |
| `canvas` | `#FCF0E7` | App background |
| `surface` | `#FFF8F3` | Cards, sheets, elevated controls |
| `surfaceSoft` | `#F3E3D6` | Neutral inset/secondary surface |
| `border` | `#E3D0C0` | Light strokes and dividers |
| `coffee` | `#6B381A` | Primary identity, icon outlines, selected labels |
| `ink` | `#161310` | Main text and high-contrast actions |
| `muted` | `#625B55` | Secondary text, after contrast verification |
| `leaf` | `#94B95B` | Small decorative strokes/checks only |

Use deeper accessible green for any small green text or controls. Green must not
be used for financial gain/loss by itself, and must not suggest that an
unverified balance is verified. Keep semantic attention, loss/error, success
and neutral signals distinct. Preserve system-readable white/dark contrast
where a coloured image sits behind text. Do not replace official institution
logos with recoloured generic icons.

### Dark and system modes

Respect the existing Appearance/System/Light/Dark selection. The dark variant
will use warm neutral near-black surfaces and readable cream text, with the
same quiet brown/green hierarchy. It must not simply invert images or force
light mode. Exact dark token values are implementation details requiring
contrast checks before release; avoid inventing a new setting.

### Typography

- Use existing Compose typography scaled with Android's font-size setting.
- Reserve editorial serif for a small number of prominent display headings;
  ordinary values, dates, transaction rows, labels and controls use readable
  sans-serif styles and tabular figures where appropriate.
- Sentence case throughout, consistent numerical alignment and type scale.
- No text clipping, ellipsizing of critical financial amounts, or fixed-height
  containers that break at larger system text sizes. Short labels may adapt;
  card rows can wrap or use a responsive layout.

### Geometry and density

- Standard page margins: 16 dp; sections: 12 dp; compact internal gaps: 4/8 dp.
- Compact rectangular cards: gentle shared radius, thin low-contrast border,
  quiet shadow, sensible height driven by content and text scaling. Do not
  introduce oversized square tiles for ordinary links.
- Icons: outlined coffee strokes on pale neutral surfaces; compact *visible*
  icon circles with at least a 48 dp interactive hit target. Avoid solid
  saturated-brown icon bubbles.
- Buttons, menu rows, chips, filters, sheets, dialogs, snackbars, tab selection,
  back arrows and loading/empty/error placeholders use the same component
  family. Selected bottom-navigation geometry stays square/soft rectangular.
- Retain visual hierarchy and imagery where already approved, but remove
  inconsistent excess padding, duplicate headings and uneven card heights.

### Navigation and behaviour

- Preserve the current five tabs exactly: Home, Activity, Plan, Accounts, More.
- Keep routes, deep links, click handlers, local permission prompts, form
  validation, filters, searches, scrolling, state restoration, accessibility
  semantics and every existing call to trusted money presentation unchanged.
- Drilled-in pages keep a clear back arrow and title. The approved Profile back
  arrow remains the chevron-only style, not a shafted arrow.
- Keep the Profile user-selected photo/cover. If none exists, use the existing
  locally bundled city cover and letter initial; do not prefill a stranger's
  portrait. The Profile wellness Nairobi asset remains bundled locally.
- Do not add a remote theme service, AI styling service, telemetry or network
  dependency. No Google Search or Gemini is needed.

## Scope inventory to verify before modifying code

All *reachable* screens and their nested UI must be checked against a
route-and-surface manifest, not only files named `*Screen.kt`:

1. Entry and setup: splash, onboarding, permission/setup, SMS and history
   import, connection and account-discovery flows.
2. Home: hero, balances, account summaries, review banners, quick actions,
   recent Activity, and every home-launched detail/sheet.
3. Activity: list, day groups, filters, search, attention/review, category
   selection, transaction details, note/receipt and evidence drawers.
4. Plan: overview, Details, pay cycle/salary, budgets, bills, reminders,
   savings goals and shopping lists/scanner flows.
5. Accounts: account list, carousel, detail, add/select institution, bank and
   M-Pesa connection, investments, debts and personal receivables.
6. More: Profile and nested identity pages, Statements and imports, Reports,
   export flows, Settings, privacy/security, backup/restore, notifications,
   troubleshooting and Data Health.
7. Cross-cutting: bottom navigation, top/back bars, dialogs, confirmation
   screens, dropdowns, date inputs, text fields, loading/empty/error/partial
   data states, accessibility font scale, system bar insets and dark mode.

## Explicit non-goals

- No new accounting features, parser logic, migration, statement admission or
  reconciliation changes.
- No money amount, balance confidence, fee/refund/transfer classification,
  historic record or persisted choice may change as part of this visual work.
- No removal of routes or controls just to simplify screenshots.
- No replacement of approved decorative imagery or brand marks without a
  separate request; layouts may be compacted while keeping content and actions.
- No instrumentation/emulator test suite added to CI. The owner tests on Redmi.
- No redesign of APK signing, package identity, release workflows or storage.

## Risks and acceptance

The main risks are: conflicts with concurrent changes; a visual migration that
silently hides an important action or status; and unreadable small text when
the phone font scale changes. A global `MaterialTheme` colour swap alone is not
enough, because many screens use local hardcoded colours.

Accept only when:

1. Every reachable route and nested surface has a checked entry in the visual
   manifest, including empty/error/partial and font-scale states.
2. No old violet/indigo remains on an ordinary PesaTrack surface except where
   a genuine provider/asset identity requires it.
3. Core navigation, all existing user actions and accessibility labels remain
   accessible. No required transaction value is visually truncated.
4. The UI diff is restricted to presentation and closely related tests/docs;
   domain, SQLite, SMS import, backup, security and release code are unchanged.
5. Targeted JVM tests, Android lint, compilation and release-candidate APK CI
   succeed on the exact integration head. Android instrumentation is skipped.
6. The owner can install the signed update *in place* on a compatible existing
   installation and manually review the selected screen checklist on Redmi.
   CI cannot claim this last step happened.
7. One integration PR is merged only after review and checks, producing one
   coordinated rolling APK release from the merged commit.

## Approaches considered

1. **Global recolour only:** fastest but incomplete; hardcoded screen palettes,
   padding and icon styles would still differ. Rejected.
2. **Rewrite each screen independently:** visually flexible but duplicates the
   inconsistency and has high regression/conflict risk. Rejected.
3. **One shared visual system, migrated in reviewable internal batches:**
   recommended and approved direction. Shared components and tokens define
   the contract; screen-specific content and imagery stay, all UI lands in one
   final release after whole-app verification.

## Coordinated delivery outline

Freeze this specification, inventory the exact latest routes, extract theme
tokens and reusable primitives, migrate surfaces in dependency order, validate
each area, then integrate/review/publish one APK. See the companion
implementation plan for work packets and verification checkpoints. The owner
approved implementation on 11 November 2026. A single release still requires
every acceptance and verification gate above.
