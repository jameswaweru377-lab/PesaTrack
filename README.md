# PesaTrack

PesaTrack is a private, local-first Android money companion focused on turning M-Pesa activity into understandable spending, commitments, savings and goals.

## Product principle

PesaTrack is not meant to be another manual expense spreadsheet. Its core question is:

> What is my money doing, what is already committed, and what is actually safe to spend?

## Technology

- Kotlin
- Jetpack Compose + Material 3
- Room/SQLite (local source of truth; persistence wiring is the next implementation step)
- WorkManager for reliable local background work
- Deterministic M-Pesa parsing and smart rules
- No account or cloud dependency in the core app

## Privacy

The intended production architecture processes M-Pesa messages on-device and stores only the transaction fields the app needs. Raw financial messages should not be uploaded to a server.

## Current bootstrap

The initial application shell includes:

- onboarding/privacy direction
- Home
- Activity
- Plan
- Goals
- light and dark themes
- M-Pesa parser prototype
- smart categorization rule engine
- unit tests
- Android CI with APK artifact generation

Preview amounts currently shown in the UI are explicitly demo data.

See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for implementation status and next steps.
