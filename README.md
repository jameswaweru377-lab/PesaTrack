# PesaTrack

PesaTrack is a **private, local-first Android money intelligence app for Kenya**. It turns financial SMS and notification evidence into a trustworthy personal ledger and answers practical questions about cash flow, transfers, balances, debts, salary cycles and commitments.

The product is deliberately not trying to become a bank, social network, investment broker, generic spreadsheet, AI chatbot or cloud-first finance platform.

## Product boundary

The authoritative scope is in [docs/PRODUCT_CHARTER.md](docs/PRODUCT_CHARTER.md). The current engineering snapshot is in [CURRENT_STATE.md](CURRENT_STATE.md).

**PesaTrack is**
- a provider-neutral financial-message understanding engine;
- a canonical local personal ledger;
- account, balance, debt, refund and transfer intelligence;
- salary/pay-cycle, budget, bill and saving-goal context built from trusted ledger state;
- reports and CSV export based on the same Activity ledger;
- explainable review and learning when certainty is insufficient;
- optional privacy-safe merchant/entity enrichment.

**PesaTrack is not**
- a social/community app;
- a stock/crypto trading or payment-initiation app;
- a bank-sync product that invents unavailable data;
- an LLM accounting authority;
- a cloud-first service.

## Current live user surfaces

- Onboarding and guided financial-data setup
- Home
- Activity and transaction details
- Plan / payday context
- Budgets and categories
- Accounts and observed balances
- Debts and relationship review
- Saving goals
- Bills and local reminders
- Reports and CSV export
- Salary setup and manual salary receipts
- Identity review
- Historical financial SMS import
- Financial notification connection status
- Data Health / Message Lab
- Settings, theme, Smart Rules, app lock and encrypted backup/restore

Legacy routes may remain only for compatibility with older navigation state.

## Financial intelligence architecture

Financial messages flow through a provider-neutral pipeline:

`capture -> relevance -> normalization -> candidate extraction -> relationship resolution -> canonical financial records -> ledger/correlation -> review/learning`

Message Packs are bounded deterministic language evidence. They are not allowed to become a provider-by-provider business-logic maze.

Message Lab and Message Inspector are developer/diagnostic tools, not accounting authority. Evidence can suggest parser work, but it cannot silently activate parsing or write ledger state.

## Privacy

SQLite on the device is the source of truth. Raw financial messages are not sent to the optional enrichment service.

Online merchant/entity lookup is **off by default**. If the user enables it, only a sanitized business descriptor may leave the device. The sanitation layer rejects likely phone numbers, transaction codes, email addresses, long numeric references and likely personal names. Amounts, balances, account numbers and full SMS bodies are not sent.

The public enrichment Worker is rate limited, and provider credentials remain server-side.

## Engineering status

- Database schema: **v32**
- Android minSdk: **24**
- target/compile SDK: **36**
- Current CI runs:
  - Worker syntax/contract checks;
  - Android JVM unit tests;
  - Android lint;
  - debug APK assembly;
  - Android instrumentation on API 35.
- The rolling debug APK is published only after both the build and instrumentation jobs pass.
- Canonical ingestion uses an explicit SQLite transaction boundary with rollback coverage.
- Migration coverage includes fresh v32 plus selected historical upgrades including v1, v30 and v31 to v32.
- App lock, biometric unlock and passphrase-encrypted backup/restore are implemented.

## Current hardening priorities

Before public release, the project is prioritizing:
- branch protection and repository hygiene;
- release signing/versioning and reproducible wrapper/tooling;
- explicit privacy and Play Store permission documentation;
- real-device Redmi/Xiaomi acceptance;
- performance work for long-history import and large ledgers;
- decomposition of the largest stores and Compose screens;
- broader API/migration/performance test matrices.

See [CURRENT_STATE.md](CURRENT_STATE.md) for the source-of-truth engineering snapshot and [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for chronological implementation history.
