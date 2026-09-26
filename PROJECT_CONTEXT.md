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
- Compose BOM: 2026.04.01 (Compose 1.11 generation, chosen for stable API 36 compatibility).
- Material 3 stable.
- Room and WorkManager are planned for the next phase and will be added when their persistence/background code is introduced.
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

## Reference UI implementation

The full visual alignment pass based on the approved PesaTrack concept screens is merged into `main` and verified by Android CI.

Reference-aligned scope:

- refined light/dark palette and typography hierarchy
- custom PesaTrack shield/track app icon and in-app brand mark
- reusable landscape hero treatment and premium card language
- redesigned onboarding
- redesigned Home
- redesigned Activity
- redesigned Plan
- redesigned Goals
- Transaction Details inner page
- Goal Details inner page
- Activity -> Transaction Details navigation
- Goals -> Goal Details navigation

All financial values in these screens remain preview data until the local data layer is connected. The reference UI passed Android CI on the PR branch and again on merged `main` (run #11).

## Redmi responsiveness and appearance correction

A second visual pass was implemented from real Redmi Note 14 screenshots after the first reference-UI build exposed narrow-screen and dark-mode problems.

Implemented on this pass:

- phone-width metric areas switch from four squeezed columns to 2x2 layouts where appropriate
- Home summary cards use vertical content hierarchy so values and status pills do not collide
- Activity transaction rows prioritize recipient + amount and move category/time into a secondary line
- Plan budget rows use full-width progress bars instead of three competing horizontal columns
- Transaction Details uses a 2x2 metadata grid on phone widths and removes the decorative sun behind the amount
- Goal Details stacks its actions on narrow phones
- semantic green/amber/blue/violet/red containers now have dedicated dark-mode surfaces and foregrounds
- hero landscape artwork automatically follows the active theme
- added Profile screen
- added Settings screen
- added persistent System / Light / Dark theme preference stored locally on-device
- Profile/Settings remain inner pages and do not pretend to be one of the four primary money tabs

The correction was validated with Android unit tests and debug APK assembly on CI before merge.

## M-Pesa ingestion foundation

The first real data-ingestion slice is implemented using Android Notification Listener access rather than requiring restricted SMS permissions.

Implemented:

- `MpesaNotificationListenerService` receives newly posted notifications after the user explicitly grants Notification Access.
- Notification bodies are filtered locally; unrelated notifications are ignored.
- M-Pesa confirmation text is parsed into structured fields using the deterministic `MpesaParser`.
- Person-to-person counterparty parsing prioritizes the recipient/sender name and strips trailing full or masked phone tokens.
- Parsed transactions are stored in an app-private SQLite ledger.
- Raw M-Pesa notification/SMS bodies are **not persisted**.
- Transactions are deduplicated by transaction code when available and a deterministic SHA-256 fingerprint as fallback.
- Stored fields include transaction code, type, amount, fee, counterparty name, optional balance, observed time and ingestion source.
- If a confirmation includes a new M-Pesa balance, PesaTrack stores it as a **last known observed balance** with timestamp. It is not treated as continuously live.
- Settings now includes an M-Pesa Connection screen with Notification Access controls and local diagnostics.
- The Android app still declares no Internet permission; cleartext traffic is explicitly disabled.
- Unit tests cover parser name cleanup, M-Pesa notification detection and transaction fingerprint stability.

Why Notification Access first:

- Modern Android treats `READ_SMS` and `RECEIVE_SMS` as restricted permissions whose availability depends on installer/platform policy.
- Notification Listener access is explicitly granted by the user in Android Settings and is suitable for testing real-time M-Pesa confirmation capture on the Redmi Note 14.
- Historical SMS import remains a separate follow-up experiment after the real-time path is proven on-device.

The first code-complete ingestion build passed Android CI run #17.

## Current implementation

Baseline: `main` after the verified reference UI merge

Bootstrap scope:

- Android/Gradle structure.
- Compose Material 3 shell.
- light/dark theme foundations.
- reference-aligned onboarding/privacy screen.
- reference-aligned Home screen.
- reference-aligned Activity screen.
- reference-aligned Plan screen.
- reference-aligned Goals screen.
- Transaction Details screen.
- Goal Details screen.
- custom PesaTrack app icon/brand mark.
- Activity and Goals inner-page navigation.
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

## Immediate next phase after M-Pesa ingestion is proven on-device

1. Install the debug APK on the Redmi Note 14 and grant PesaTrack Notification Access.
2. Trigger a real M-Pesa transaction and verify the Connection screen captures the correct name, amount, fee, code and optional balance.
3. Collect only anonymized format examples for any transaction type the parser misses; never commit real private transaction content.
4. Add explicit reversal linking and own-account transfer rules before using captured data as spending.
5. Connect Activity to the local ledger.
6. Connect Home totals and last-known balance semantics to the local ledger without fabricating unavailable balances.
7. Connect smart categorization rules to persisted transactions.
8. Add budgets/plan calculations on top of real categorized transactions.
9. Prototype historical SMS import only if the Redmi/installer can legitimately grant the restricted SMS permissions.
10. Add biometric/PIN protection before exposing sensitive historical financial detail at scale.

## Important constraints

- Raw SMS content should not leave the phone.
- Never silently classify uncertain transactions as expenses; uncertain items go to Needs Review.
- Transfers between the user's own stores of money are not spending.
- Cash withdrawal is a transfer to Cash until cash is explicitly spent.
- Reversals must neutralize the original transaction rather than become fake income.
- Fees should be tracked separately.
- Do not fabricate account balances or financial insights.
- UI must remain readable on the Redmi Note 14 and adapt cleanly to larger Android form factors.
