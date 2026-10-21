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
- CSV statement import with duplicate/date review, plus local Android15+ PDF password unlock and extraction. Supported M-Pesa text rows offer editable confirmed import; labelled payroll fields offer reviewed journal entry. Arbitrary layouts, scanned OCR and older-Android PDF extraction remain unsupported; device acceptance remains pending.
- Salary setup and manual salary receipts
- Dedicated Paycheck Analysis linked from Activity, Salary and Reports: privately reviewed payroll journal with gross/net, deductions and reported SACCO figures.
- Long-text PDF imports: bounded 8 MiB / 160 pages / 500,000 extracted characters / 5,000 statement rows; native Android reader transfers safe small IPC chunks. Payroll outflow abbreviations and rounding carry-forward are reviewed without copying computed tax-relief amounts into deductions. CSV files retain their 2 MiB cap; scanned and unfamiliar PDF layouts remain unsupported.
- Payslip review pre-fills recognised employer/month/gross/net/deductions when supported; gross cash earnings and taxable pay remain distinct. Unknown deduction rows from explicitly marked deduction tables need review. Gross-to-net mismatches must be resolved manually, never fabricated.
 Android15+ local PDF selection has a 3 MiB cap; raw PDF text is never shown. Earnings breakdowns are preview-only. Payroll never proves cash receipt.
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

- Database schema: **v40**
- Android minSdk: **24**
- target/compile SDK: **36**
- Current CI runs:
  - Worker syntax/contract checks;
  - Android JVM unit tests;
  - Android lint;
  - debug APK assembly;
- Android instrumentation on API 35 is paused and only runs through an explicitly requested manual review with run_instrumentation enabled. PR updates and main publication use build checks without automatic emulator execution.
- Pushes to main publish the rolling development debug APK after build, lint and unit tests pass, without requiring emulator tests. Manual review runs retain artifacts but do not replace the rolling release. Signed release runs also default to build-only validation; enable run_instrumentation only when the user explicitly requests emulator review.
- Canonical ingestion uses an explicit SQLite transaction boundary with rollback coverage.
- Migration coverage includes fresh v38 plus selected historical upgrades including v1, v30, v31/v32 and populated v37 to v38; encrypted legacy/current restore is covered.
- App lock, biometric unlock and passphrase-encrypted backup/restore are implemented.

## Release and security documents

- [Current engineering state](CURRENT_STATE.md)
- [Security policy](SECURITY.md)
- [Contributing rules](CONTRIBUTING.md)
- [Privacy policy source — pre-release review](docs/PRIVACY_POLICY.md)
- [Terms of Service — pre-release draft](docs/TERMS_OF_SERVICE_DRAFT.md)
- [Legal, ODPC and SMS permission release gate](docs/LEGAL_RELEASE_GATE.md)
- [Google Play release checklist](docs/PLAY_RELEASE_CHECKLIST.md)
- [Signed release process](docs/RELEASE_PROCESS.md)

## Current hardening priorities

Before public release, the project is prioritizing:
- branch protection and stale-branch cleanup;
- release signing/versioning and a committed Gradle wrapper;
- publishing the privacy policy and completing Play Console declarations;
- real-device Redmi/Xiaomi acceptance;
- performance work for long-history import and large ledgers;
- decomposition of the largest stores and Compose screens;
- broader API/migration/performance test matrices.

See [CURRENT_STATE.md](CURRENT_STATE.md) for the source-of-truth engineering snapshot and [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for chronological implementation history.

## Owed to me history review

Personal receivables use a separate local journal. Find in transactions searches imported M-Pesa and bank Activity history by name or phone. Separately saved PDF statement archive matches are visible as read-only evidence; they do not create a cash movement or a debt unless the user follows the controlled Activity import and individual loan review flow. Loans and possible repayments require explicit user confirmation; gifts/not-loan decisions are remembered per person. Bulk selection includes only eligible displayed outgoing rows and all selected records are previewed before an atomic save. Shared bills preserve the user's remaining expense category and fees; original cash movements are never duplicated. Activity exposes Owed to me/Repayments filters and debt labels. Schema 37 adds personal_receivable_review_exclusions; encrypted backups from schema 34–36 are bridged with missing tables empty.

Implementation verification is tracked in the history-review PR. Android instrumentation and device acceptance remain unrun unless explicitly requested. Build/lint/unit evidence does not prove physical-device migration or user journeys.
