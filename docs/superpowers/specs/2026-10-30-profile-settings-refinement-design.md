# PesaTrack Profile and Settings Refinement — Approved Design

Date: 30 October 2026. Approved by James in the Profile design conversation.

## Intent

Keep the existing light, purple PesaTrack Profile identity while making personal details, security, reminders and help genuinely useful. This is a targeted refinement, not a new navigation system or a financial-dashboard project.

## Approved visual direction

- Retain the violet gradient profile hero, rounded white cards, pastel semantic icon bubbles, navy text, subtle lavender atmosphere and compact spacing of the approved Home/Activity/Profile system.
- Profile hero shows a circular local photo (or existing emoji fallback), name, optional email and edit action. Keep the local-storage note.
- Profile destinations: Personal information, App lock & security, Notifications, Appearance, Help & support, About PesaTrack. Remove the Accounts/Budgets/Goals mini-stat row, Financial overview route and duplicated More settings card from Profile. Financial information remains available through existing Home/Plan/Accounts routes.
- Personal information supports Android photo selection, an interactive square/circular crop preview with zoom/reposition, local save, replace and remove. Preserve editable name, optional email and emoji fallback. Do not introduce account sign-in or cloud image uploads.
- Security uses the existing PIN and encrypted-backup actions, exposes the existing biometric capability as a clearly controlled preference when available, and offers an explicit lock-after-background interval. Preserve the existing 30-second interval as the default for existing users. Privacy/data and financial-message access keep their existing routes.
- Notifications retains all currently implemented categories and timing semantics; use high-contrast branded switches with accessible labels and disabled states, no new silent alert behavior.
- Help and troubleshooting use searchable task-oriented topics and read-only safety checks, with guided links to existing permissions, source selection, import, account review, backup and security screens. Advanced diagnostics stay reachable but no repair/reprocessing runs without the existing explicit confirmation and safety gates.

## Product and security boundaries

- Kotlin/Jetpack Compose on Android minSdk 24, light theme; preserve four bottom tabs elsewhere.
- Local-only sensitive photo and personal data. Use Android's photo picker with no broad storage permission; limit decoded image dimensions and file size. Never transmit selected photos, SMS or ledger information.
- Keep screenshot/recording protection, `FLAG_SECURE`, biometric/PIN lockouts, existing backup format, migrations and financial calculations unchanged except for approved settings preferences.
- Visible controls must work; where a capability is unavailable (e.g. biometrics on a device), explain that and disable the control.
- Small-screen, large text, screen-reader labels, error/empty states and interrupted file operations must be handled.
- The six visual mockups in the conversation are design proposals, not exact existing APK captures or claims of backend readiness.

## Approaches considered

1. **Focused reuse of existing Compose screens (selected):** Retain routes and security services, add small photo and help components, reuse existing palette and cards. Lowest compatibility risk.
2. New Profile/navigation module: More freedom to redesign, but unnecessary route/state migration and greater risk to current workflows.
3. Appearance-only patch: Fast, but leaves photo editing and troubleshooting functionality missing.

## Acceptance

- Users can add, crop, save, reopen, replace and remove a local profile photo; name/email remain editable and emoji fallback remains available.
- Profile contains no duplicate financial overview/stats or More settings card. Current four-tab navigation remains unchanged.
- App-lock settings reflect actual PIN/biometric availability and timing choices; current 30-second behavior is preserved by default; screenshot protection stays intact.
- Notification switches clearly indicate on/off and persist using existing reminder stores.
- Help topics lead to real actions or contextual instructions; safe checks never mutate the financial ledger. Advanced diagnostics remain accessible.
- Focused tests and the existing Android CI build/unit/lint/APK checks pass before claiming completion. Emulator/physical-device verification is recorded as unverified unless explicitly requested.
