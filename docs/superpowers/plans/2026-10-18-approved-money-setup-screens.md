# Approved Money Setup Screens — Implementation Plan

**Goal:** Implement the five approved onboarding screens from the reviewable HTML design, matching the visual hierarchy in light and dark themes while using only locally detected or explicitly selected financial data.

**Architecture:** Keep orchestration in `SmartOnboardingSetupScreen`; extract focused institution ranking/search, Safaricom service selection, account-detail and completion composables into onboarding UI components. Reuse `FinancialUnderstandingStore`, `FinancialInstitutionDirectory`, existing ownership policies, M-Pesa confirmation, and account-detail validation rather than introducing new financial authorities.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing onboarding stores and JVM/Compose tests.

**Spec:** `outputs/pesatrack-money-setup-design.html` (approved visual flow)

## Global constraints

- Show at most five detected financial institutions, ranked by financial-source message count; an SMS mention never implies ownership.
- M-Pesa and Safaricom products remain grouped separately; only products already identified by the app may be suggested as detected.
- Keep user selections explicit and M-Pesa phone details masked until the user chooses to confirm or edit them.
- Bank names and last-four hints remain optional; last four digits alone never verify ownership.
- Preserve local-only processing, accounting semantics, existing permissions, and the optional salary feature in Plan.
- Preserve light/dark theming and accessibility semantics.

## Tasks

1. Add and test a deterministic presentation policy for top-five institution ranking, Safaricom sub-product grouping, and masking a detected M-Pesa phone suffix.
2. Update the institution review screen with the approved top-five rows, message-frequency context, searchable directory results, explicit selection, selected count, and empty state. Reuse existing store ownership/reconciliation paths for manually chosen institutions.
3. Add the conditional M-Pesa service review step using only detected, supported provider identities, retaining explicit ownership choices and the separate Lock Savings consent.
4. Consolidate selected M-Pesa and bank identity inputs into the approved account-details layout; persist optional account labels and last-four display hints locally, retain existing validation, and allow save/skip.
5. Match the completion screen to the approved concise success state and route directly to the dashboard; salary remains available later from Plan.
6. Run JVM tests, lint, debug APK assembly and Android test compilation in CI; review the diff, merge only after PR checks pass, then verify the successful `main` workflow APK artifact.

## Review focus

- Missing or zero detected sources: show a useful empty state and allow search/continue.
- More than five institutions: use deterministic message-count ordering and avoid losing lower-ranked institutions in search.
- M-Pesa selected versus unselected: show the service step only when selected; never auto-confirm a product.
- Missing/ambiguous M-Pesa number: keep manual entry and skip paths available.
- Optional account data: blank labels or hints must not block progress or change accounting identity.
