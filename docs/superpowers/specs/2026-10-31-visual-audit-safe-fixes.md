# Visual consistency audit — safe corrections (31 October 2026)

## Scope and evidence

Review the screens reachable from Home, Activity, Plan, More, Profile, Statements,
and onboarding, against the approved light/purple PesaTrack designs and
`AGENT_WORKING_METHOD.md`. This is a **source inspection**, not a device screenshot
review: no Android emulator or connected phone is available here.

Keep financial calculations, transaction meaning, privacy, navigation, approved
hero illustrations, and the rolling release process unchanged. Do not touch
shared design tokens/components or files covered by concurrently open PRs.

## Safe implementation scope

1. Activity summary: maintain the compact appearance while making the month
   picker and analytics action each at least 48dp tall/wide as applicable;
   keep the same callbacks and icon sizes.
2. All-statements coverage: keep the twelve colored 16dp bars as visual
   *indicators*, with one accessible, >=48dp-high **account row** that opens
   the same account detail. Individual month bars cannot each be 48dp wide
   on a phone showing twelve columns. Preserve month/year/state information
   in the row's accessible description and the visual legend.
3. Bank statements: make the year-filter chip row horizontally scrollable so
   longer year histories do not clip offscreen chips. Preserve selected-year
   and filter behavior.

These are interaction-size and overflow corrections, not a fresh redesign.
Record broader style drift as findings for owner review. Do not automatically
replace intentional gradient colors or small visual labels inside approved art.

## Verification

- Existing Compose UI tests must assert a >=48dp accessible *account-row*
  target with correct account navigation, the month-state description and
  the Activity summary target sizes. They must run RED→GREEN on an emulator
  only during an expressly scheduled focused instrumentation window.
- Do not substitute source-text grep for actual UI behavior tests or
  activate the hour-long default instrumentation in CI.
- Run `git diff --check`, focused tests and standard Android PR CI.
- Review overlapping PRs and the latest main before considering a controlled
  merge; leave the branch unmerged if checks are missing/failing or there is a
  live conflict.
