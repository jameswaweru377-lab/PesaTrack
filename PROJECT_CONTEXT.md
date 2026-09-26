# PesaTrack Project Context

Updated: 2026-09-26

## Purpose

PesaTrack is a personal Android finance companion designed around Kenyan M-Pesa usage. It is intended for private sideloading, not Play Store distribution.

The application should automatically turn M-Pesa activity into a reliable local money ledger, learn deterministic recipient/category rules, track monthly budgets and commitments, show savings/goal progress, and surface useful context such as budget pace and safe-to-spend amounts.

## Product direction already agreed

The app should not behave like a judgmental expense tracker. Spending that follows the user's plan is not inherently bad. The core experience should distinguish current money, committed money, savings/reserves, genuinely free money, and safe-to-spend pace.

Signature concepts planned:

1. Available to Live / genuinely free money.
2. Safe today amount based on remaining cycle and protected commitments.
3. Smart recipient rules learned from explicit user corrections.
4. M-Pesa-native transaction types: Send Money, Buy Goods, PayBill, airtime, cash withdrawal, reversal, fees and transfers.
5. Goals with progress and impact simulation.
6. Monthly/annual insights that explain what changed, rather than dumping charts.
7. Local-first privacy and optional biometric protection.

## Technical choices

- Native Android.
- Kotlin + Jetpack Compose.
- Package: `com.kedevjimmy.pesatrack`.
- Minimum SDK: 24.
- Target SDK: 36.
- Compile SDK: 36.
- Compose BOM: 2026.09.00.
- Material 3 stable.
- Room and WorkManager are dependencies; database/background ingestion wiring is still pending.
- Source of truth must remain deterministic local data, not AI.
- Any future AI should explain already-derived facts, never invent transaction truth.

## Design system

Light mode uses warm ivory, clean white surfaces, deep navy text, emerald primary, soft mint containers and restrained amber attention states.

Dark mode uses a near-black blue/green background, lifted dark teal surfaces, mint primary, warm amber secondary and off-white text.

Typography currently uses Android's bundled sans-serif family with a strong weight/size hierarchy so the app stays offline and does not depend on downloadable fonts.

## Navigation

Primary tabs:

- Home
- Activity
- Plan
- Goals

Insights are intentionally embedded where they matter instead of becoming a fifth analytics dump.

## Current implementation

Branch: `bootstrap/android-foundation`

Bootstrap scope:

- Android/Gradle structure.
- Compose Material 3 shell.
- light/dark theme foundations.
- onboarding/privacy screen.
- Home preview screen.
- Activity preview screen.
- Plan preview screen.
- Goals preview screen.
- bottom navigation.
- deterministic `MpesaParser`.
- `SmartRuleEngine`.
- parser/rule unit tests.
- Android CI.
- debug APK artifact upload on successful CI.

The UI uses preview/demo numbers only. It is not yet connected to real SMS or persistent transactions.

## Build rule

Do not call an implementation successful merely because code was written.

For every implementation phase:

1. Make a narrow, understandable change.
2. Run the relevant build/test.
3. If it fails, inspect the exact failure.
4. Fix the actual cause before continuing.
5. Only mark the implementation complete when the relevant check passes.

## Immediate next phase after bootstrap CI is green

1. Add Room entities/DAO/database and repository layer.
2. Persist parsed transactions using integer minor units (cents), not floating point.
3. Add an importer abstraction so ingestion is not coupled directly to SMS APIs.
4. Prototype SMS access on the real Redmi Note 14.
5. Add notification-listener ingestion as a future-transaction fallback.
6. Add duplicate detection using transaction code plus normalized fingerprint.
7. Handle reversals and transfers correctly before counting spending.
8. Replace preview Activity data with repository-backed data.
9. Add smart-rule creation/editing UI.
10. Then connect Home totals, budgets and goals to real stored data.

## Important constraints

- Raw SMS content should not leave the phone.
- Never silently classify uncertain transactions as expenses; uncertain items go to Needs Review.
- Transfers between the user's own stores of money are not spending.
- Cash withdrawal is a transfer to Cash until cash is explicitly spent.
- Reversals must neutralize the original transaction rather than become fake income.
- Fees should be tracked separately.
- Do not fabricate account balances or financial insights.
- UI must remain readable on the Redmi Note 14 and adapt cleanly to larger Android form factors.
