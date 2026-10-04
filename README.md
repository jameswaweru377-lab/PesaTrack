# PesaTrack

PesaTrack is a **private, local-first Android money intelligence app for Kenya**. It turns financial SMS/notification evidence into a trustworthy personal ledger and answers a small set of practical questions:

- What money came in?
- What money went out?
- Which movements were transfers rather than spending?
- What do I owe, and what changed in my debts?
- What balances and owned accounts can be trusted?
- What is already committed before the next payday?
- Which transactions still need my attention?

The product is deliberately not trying to become a bank, social network, investment broker, generic budgeting spreadsheet, or cloud finance platform.

## Product boundary

The authoritative product scope is in [docs/PRODUCT_CHARTER.md](docs/PRODUCT_CHARTER.md).

The short version:

**PesaTrack is**
- a financial-message understanding engine;
- a local personal ledger;
- account, balance, debt and transfer intelligence;
- budgeting and payday context built from trusted ledger state;
- explainable review/learning when certainty is insufficient;
- optional privacy-safe merchant/entity enrichment.

**PesaTrack is not**
- a social/community app;
- a fake savings-goals simulator;
- a stock-trading/investment execution app;
- a bank-sync product that pretends to have data it cannot access;
- an AI chatbot;
- a cloud-first service;
- a place for preview cards that look like live financial facts.

## Current live user surfaces

- Onboarding and guided financial-data setup
- Home
- Activity
- Transaction details
- Plan / payday context
- Budgets and categories
- Accounts and balances
- Debts and relationship review
- Identity review
- Historical SMS import
- Financial notification connection status
- Data Health
- Settings / theme / Smart Rules

Goals and Community source files remain in the repository for now, but they are **quarantined from live navigation** because they contain preview or fabricated values and are not part of the trustworthy ledger product.

## Financial intelligence architecture

Financial messages flow through a provider-agnostic pipeline:

`capture -> relevance -> normalization -> candidate extraction -> relationship resolution -> canonical financial records -> ledger/correlation -> review/learning`

Message Packs are bounded deterministic language evidence. They are not allowed to become a provider-by-provider business-logic maze.

Message Lab and Message Inspector are **developer/diagnostic tools**, not accounting authority. Evidence can suggest parser work, but it cannot silently activate parsing or write ledger state.

## Privacy

SQLite on the device is the source of truth. Raw financial messages are not sent to the optional enrichment service.

The Cloudflare enrichment boundary accepts only a sanitized merchant/entity descriptor and rejects likely:
- phone numbers;
- transaction codes;
- email addresses;
- long numeric references;
- likely personal names.

Provider credentials remain server-side.

## Engineering status

Current main schema: **v31**.

Current CI on `main`:
- Worker syntax/contract checks;
- Android JVM unit tests;
- Android lint;
- debug APK assembly;
- rolling private `ci-apk` prerelease publication.

Important remaining engineering debt:
- no Android instrumentation/UI test suite yet;
- no automated old-schema-to-v31 migration test matrix yet;
- database ownership is fragmented across many stores and multi-store financial writes do not yet have a proven crash-atomic transaction boundary;
- several files have grown too large and must be decomposed before substantial new feature work;
- legacy/preview source should be deleted only after a clean quarantine period and CI confirmation.

See [docs/AUDIT_2026-10-04.md](docs/AUDIT_2026-10-04.md) for the current repository audit and [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for detailed implementation history.
