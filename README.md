# PesaTrack

PesaTrack is a private, local-first Android money companion focused on turning Kenyan financial messages into an understandable money ledger: spending, income, transfers, debt relationships, investments, commitments, budgets and payday context.

## Product principle

PesaTrack is not meant to be another manual expense spreadsheet. Its core question is:

> What is my money doing, what is already committed, and what is actually safe to spend?

## Current architecture

- Kotlin
- Jetpack Compose + Material 3
- Android SQLite as the local source of truth
- provider-agnostic Financial Intelligence V2 for financial-message interpretation
- notification capture plus optional historical SMS import
- deterministic smart rules and teach-once review
- local account, pay-cycle, budget, debt and investment relationship stores
- privacy-safe Cloudflare Worker boundary for optional merchant/entity enrichment
- Android CI with unit tests, Worker contract validation and debug APK assembly

The core ledger does not require a cloud account. Financial message interpretation and durable financial state are kept on-device.

## Privacy boundary

Raw financial messages must not be sent to the enrichment service. Online enrichment is limited to a sanitized merchant/entity descriptor and rejects phone numbers, transaction codes, email addresses, long references and likely personal names.

Provider credentials stay server-side in Cloudflare and are not embedded in the Android APK.

## Current product status

Connected to real local data:

- Home
- Activity and transaction details
- Plan/pay-cycle context
- Accounts and balances
- Debts and relationship review
- Budgets and categories
- smart onboarding, historical import and M-Pesa connection diagnostics

Intentional preview / unfinished areas:

- Goals and Goal Details still use preview values. They must only be connected after deliberate savings-allocation semantics exist; unspent cash must not be treated as automatically saved.
- Community is primarily a presentation/engagement surface and is not part of the financial source-of-truth path.
- Profile editing, backup/restore, security/PIN/biometric settings and some Settings rows are not yet fully wired.

## CI and APK

Android CI runs the Worker contract tests, Android unit tests and debug APK build. Successful `main` builds publish the current debug APK to the rolling private `ci-apk` prerelease while Actions artifact storage remains unsuitable for the project workflow.

See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the detailed implementation history, safety rules and current handoff state.
