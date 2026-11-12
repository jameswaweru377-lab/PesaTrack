# PesaTrack unified premium UI — coordinated implementation plan

Status: **Owner-approved; implementation underway in isolated worktree**

Design: `docs/superpowers/specs/2026-11-11-unified-premium-ui-design.md`

## Release strategy

Use **one dedicated integration branch and one final PR to `main`**, with
several small, reviewable commits/checkpoints inside that branch. No partial
visual rollout is merged or published while the new theme is inconsistent.
Keep the existing rolling signed full/update APK process untouched. This
fulfils the owner's one-at-once delivery goal without making each change an
unreviewable whole-app rewrite.

Current starting inspection: 11 November 2026, `origin/main` at
`cd07291` in the planning worktree. Recheck the true head and open PR ownership
at execution time: other agents are actively working on balances, investments,
scanner and optimization features. Do not reuse or overwrite their branches,
uncommitted changes, files or metadata.

## Work packet 0 — baseline and complete inventory

**Read-only work.**

- Refresh latest `main`, record head SHA, local dirty-worktree state, active
  owners/open PRs and exact CI baseline.
- Use `PesaTrackApp.kt` routes (34 top-level Compose destinations at planning
  time), scan nested `@Composable` entry points and actual navigation
  callbacks; build a checklist of every reachable page, modal, dialog, sheet,
  input state and tablet/phone responsive variant.
- Label surfaces as shared/system, primary tabs, or nested destination.
  Capture current colors, card density, title hierarchy, accessibility state,
  empty/loading/error/partial appearance, and existing approved imagery.
- Run the existing focused JVM tests and normal build/lint baseline only once,
  using available caches/CI. Record skipped checks explicitly; do not trigger
  instrumentation/emulator workflows.
- Produce a redacted visual checklist without live financial data. Define a
  stable manual Redmi smoke path for each of the five tabs.

**Gate:** scope includes all reachable surfaces, not just visually prominent
tab landing pages; existing failures are known before UI work starts.

## Work packet 1 — one visual source of truth

**Target files:** `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`,
`PesaDesignTokens.kt`, the existing palette adapters, and small shared files
in `ui/components`.

- Define typed premium theme tokens for light/dark/system modes: canvas,
  surface, elevation, border, coffee, readable green, text, semantic statuses,
  spacing, sizes, card/corner geometry, icon and typography roles.
- Keep `MaterialTheme` in sync so default Compose controls do not fall back to
  indigo/violet. Replace old semantic aliases only once callers migrate.
- Add reusable composables for compact editorial card, primary/secondary
  button, outlined icon affordance, quiet pill/chip, section heading, sheet
  scaffold, form field, status, placeholder and accessible back affordance.
- Do not expose raw colour literals or one-off geometry in converted screens.
  Keep component files focused and small.
- Add JVM-level contracts for token selection and pure visual policies where
  possible; do not pretend these prove rendered screenshot fidelity.

**Gate:** the base screen shell builds in light and dark themes; default
control colours and contrast are coherent; existing route behaviour unchanged.

## Work packet 2 — app shell and primary workflows

**Target:** `PesaTrackApp.kt`, `AppScaffold.kt`, navigation/headers and
cross-screen component usages.

- Standardise status/navigation bar surfaces, inset handling, screen margins,
  back chevrons, typography hierarchy, scroll containers and square-ish
  selected bottom navigation state.
- Keep all five tabs, deep links, activity results, saved state, action handlers
  and conditional route availability identical.
- Make loading, no-data, error, partial and review-needed states visually
  distinct without changing their decision logic.

**Gate:** navigate all five tabs and return from nested pages; no missing back
controls, duplicate toolbars or hidden actions.

## Work packet 3 — Home, Activity and transaction details

**Targets:** `HomeScreen`, `ActivityScreen`, transaction-detail components,
category choices, review queues, sheets, receipt/note and filters.

- Translate existing approved information layouts to the shared palette and
  card rhythm; retain coin/receipt imagery and evidence-backed figures.
- Preserve financial totals, labels, posting/review actions, transaction
  filters, source evidence, transfer/refund/fee handling and personal corrections.
- Keep list rows compact; avoid clipping amounts and daily totals at larger
  font scale. Readable semantic loss/error states remain distinct from green.

**Gate:** synthetic known/unknown/needs-review transactions display the same
values and trigger the same handlers as the baseline.

## Work packet 4 — Plan and planning subpages

**Targets:** Plan overview/details, pay cycle, salary, budgets, bills,
savings goals, shopping lists/scanner and every related modal.

- Unify spacing, cards, calendar chips, progress visuals, fields and CTA styles.
- Keep all budget calculations, saved goals, salary dates, recurring reminders,
  forecasts and the Nano/OCR shopping scanner functionality untouched.
- Preserve available/unavailable/partial financial states verbatim; styling may
  explain them, but must not replace uncertainty with estimated numbers.

**Gate:** all planning creation/edit/review flows remain reachable and the
saved outcomes do not differ from baseline.

## Work packet 5 — Accounts, investments and debts

**Targets:** Accounts list, carousel, add/select institution, details, bank and
M-Pesa connection, investment and debt/receivable detail states.

- Apply the same card density, header style, account rows, logo sizing and
  outlines while respecting providers' own local identity marks.
- Preserve swipe/selection semantics, account ownership confirmations,
  account-balance provenance, investment review state and debt actions.
- Reconcile this packet with any recently merged investment/balance PR before
  editing; do not race another chat's active file ownership.

**Gate:** every account/connection action remains reachable; no account
balances or classifications change due to UI refactoring.

## Work packet 6 — More, Profile, Statements and settings

**Targets:** Profile editorial home/nested pages, More, Settings,
Statement activity/import, Reports/PDF export UI, Privacy, app lock,
notifications, data health and backup/restore screens.

- Treat the merged compact Profile as the geometry reference; do not revert its
  rectangular tiles, grouped outlined quick actions, shield-check, Nairobi
  wellness image or chevron-only back affordance.
- Replace black-and-white-only More styling with the approved shared neutral
  palette while keeping high-contrast black actions where appropriate.
- Harmonise document pickers, export feedback and form states. Do not change
  statement admission, file outputs, encryption or security decisions.

**Gate:** Profile edit, custom cover, reports export, statement browsing,
security/backup preferences and settings still work as before.

## Work packet 7 — onboarding, secondary journeys and final sweep

- Convert the remaining reachable secondary and diagnostic screens using the
  route manifest, including older or seldom-used paths.
- Search for old hardcoded indigo/violet, `ProfileVisualColors`/`MoreVisualTokens`
  mismatches and one-off shape or spacing values. Review exceptions instead
  of blindly replacing provider logos or image pixels.
- Inspect all dialog/sheet/keyboard states, screen rotation/layout, font scale,
  talkback labels and minimum 48 dp tap targets. Ensure decorative assets remain
  bundled locally and do not increase privacy/network scope.

**Gate:** each manifest item is marked checked or genuinely unavailable with
reason; no reachable old-theme screen is silently skipped.

## Work packet 8 — integrated verification, review and one merge

- Compare UI-only diff against current `main`, including the changes merged by
  other chats during implementation. Exclude unintended ledger, database,
  security, import, permissions, signing, release and backup edits.
- Run exact-head `:app:testDebugUnitTest`, `:app:lintDebug`, and Android APK
  compilation/CI as configured. Respect a bounded test timeout; do not start
  instrumentation or emulator jobs. Record exact commands, results and SHA.
- Inspect accessibility/font-scale and dark-mode cases using available static
  methods; owner performs physical Redmi screenshots/navigation checks after
  signed APK publication. Do not claim manual tests before they happen.
- Review PR for visible regressions and review comments. Resolve conflicts
  with latest `main` by preserving concurrently approved financial/feature work.
- Merge the single UI integration PR to `main` only after appropriate checks
  pass, then verify its merge SHA, main CI, and the `ci-apk` release's source
  commit, versionCode and two signed Full/Update assets.
- Warn the owner to protect a verified backup and install *in place* only if
  signing compatibility is established. Never recommend uninstalling to force
  the UI upgrade or claim the APK is phone-accepted before testing.

**Gate:** one complete rollout and one owner-visible APK version, not a set of
intermediate UI releases.

## Acceptance checklist by surface

For every surface, record: route/entry, initial/empty/loading/error/partial
states, palette and typography, card geometry, button styles, icons, spacing,
scaling, touch targets, navigation actions, and comparison with its baseline.
Use invented/redacted Kenyan examples only.

## Explicit stop conditions

Stop and request direction if a UI change requires new network egress or
permission, alters money meaning, hides required uncertainty, affects data
deletion/backup compatibility, or conflicts with another agent's owned work
that cannot safely be resolved. The release can be coordinated, but it cannot
be made safe by skipping these gates.

## Definition of done

1. All reachable user-facing surfaces match the unified light/dark premium
   language and respect accessible font scaling.
2. Navigation/actions, financial values, review states and persisted data have
   the same behavior before and after the UI migration.
3. Tests/lint/build pass on the actual final head; skipped device tests are
   declared, not assumed.
4. A single reviewed PR is merged and its exact commit is reflected in the
   rolling signed APK.
5. Physical Redmi acceptance remains the last user verification step.

This plan records the approved coordination approach. Production UI changes
are tracked separately on the isolated implementation branch; no merge occurs
until the full coverage and CI gates pass.
