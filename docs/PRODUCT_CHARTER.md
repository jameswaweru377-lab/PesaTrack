# PesaTrack Product Charter

Last reset: 2026-10-04

## One-sentence product

PesaTrack is a private Android money companion that understands Kenyan financial messages and turns them into a trustworthy local picture of cash flow, balances, debts, transfers, commitments and reviewable uncertainty.

## The user promise

PesaTrack must prefer **being uncertain over being wrong**.

A polished card is not useful if the underlying money meaning is fabricated. Every amount shown as financial fact must come from:
1. trusted local ledger/account state;
2. an explicit user entry/decision; or
3. deterministic evidence with enough confidence to persist safely.

## Core jobs

PesaTrack exists to do five jobs well:

1. **Understand financial messages**
   - M-Pesa, banks, lenders, savings/investment products and other Kenyan financial sources.
   - Interpret the financial language, not merely memorize provider names.

2. **Build a correct local ledger**
   - income;
   - expense/payment;
   - transfer;
   - cash movement;
   - fee;
   - balance observation;
   - debt drawdown/repayment;
   - reversal/refund;
   - savings/investment movement where semantics are safe.

3. **Understand relationships**
   - owned accounts;
   - self-transfers;
   - mirror/duplicate observations;
   - lender and debt balances;
   - counterparties and identities;
   - refunds and linked event chains.

4. **Explain the user's current money**
   - balances;
   - spending;
   - money in/out;
   - debts;
   - commitments;
   - payday context;
   - “available after commitments”.

5. **Ask for user help only when the user can resolve the uncertainty**
   - Needs Attention;
   - identity review;
   - ambiguous duplicates;
   - uncertain categories.

Unsupported parser/message-shape coverage belongs in Data Health and the developer evidence workflow. It must not inflate ordinary user attention counts.

## Explicit non-goals

Do not add these unless the product charter is deliberately changed:

- Social/community feed.
- Public profiles, comments, likes or sharing a private ledger.
- Fake demo goals mixed with live money.
- Gamified savings balances that are not backed by actual allocations/accounts.
- Stock/crypto trading or investment execution.
- Loans, credit scoring or lending decisions.
- Sending money or initiating bank/mobile-money payments.
- Full cloud synchronization of raw financial records.
- Uploading raw SMS to a server or LLM.
- Provider-specific `if/else` business logic as the default parsing strategy.
- A chatbot as the primary product interface.
- Generic “AI insights” that cannot point to ledger evidence.
- Feature-count growth for its own sake.

## Product surfaces

### Tier 1 — core
These must remain reliable before new scope is added:
- onboarding/data access;
- Home;
- Activity;
- transaction details;
- Accounts;
- Debts;
- Plan/payday;
- Budgets;
- review queues.

### Tier 2 — trust and diagnostics
- Data Health;
- Message Inspector;
- Message Lab;
- identity review;
- history reprocessing;
- parser provenance and regression checks.

These are valuable, but developer-facing diagnostics must not overwhelm the ordinary user experience.

### Tier 3 — optional enrichment
- remote merchant/entity enrichment;
- logos/category hints.

Failure of Tier 3 must never corrupt Tier 1 financial truth.

## Accounting authority

The canonical ledger is authoritative.

The following are **not** authoritative by themselves:
- Message Pack match;
- user-facing label text;
- merchant logo/category enrichment;
- Message Lab semantic label;
- parser draft;
- implementation work packet;
- recurring-pattern guess;
- identity repetition count.

They may provide evidence, not truth.

## Parser rules

New message support must follow this order:

`real evidence -> sanitize -> label meaning -> confirm recurrence -> review mechanics -> implement deterministic parser/grammar -> fixtures -> negative tests -> duplicate/reconciliation tests -> history reprocess -> device verification`

Never auto-activate a parser merely because:
- one pasted message exists;
- a provider name is known;
- a regex can be written;
- an LLM guessed the meaning.

## UX rules

- No fake financial values on navigable production screens.
- Preview/demo screens must be developer-only or absent.
- “Coming soon” functionality must be labelled as unavailable, not shown as active.
- Needs Attention should group uncertainty instead of scattering review work across many pages.
- Home should summarize; it should not become a diagnostic dashboard.
- Data Health can be technical.
- “Available after commitments” must be derived from trusted balances and known commitments, not from wishful budgeting.

## Privacy rules

- Local-first by default.
- Raw SMS stays on-device.
- Remote enrichment gets sanitized descriptors only.
- Never upload full transaction history for convenience.
- Secrets never ship in the APK.
- Any future backup/sync feature requires explicit encryption, restore semantics and user control before it is exposed in UI.

## Architecture rules

- UI screens should not become application layers.
- No new 2,000–5,000 line files.
- Split large stores/screens by responsibility before major feature expansion.
- New persistence should have a repository/domain boundary rather than adding unrelated SQL to giant stores.
- One app-scoped database owner should coordinate access to the canonical SQLite file; do not keep multiplying independent `SQLiteOpenHelper` instances.
- A financial write that changes canonical accounting state must have an explicit atomicity contract. Related canonical writes should either commit together or be safely replayable after interruption.
- `@Synchronized` is not a substitute for a database transaction.
- New schema versions require migration tests.
- Core financial logic must have deterministic JVM tests.
- User journeys require Android/instrumentation coverage as the project matures.
- Optional online services must fail closed or degrade gracefully.

## Release gate

A feature is not “done” because it compiles.

For money-affecting changes, require:
- unit tests;
- relevant corpus fixtures;
- negative safety cases;
- duplicate/mirror safety where applicable;
- history reprocess behavior;
- CI green;
- real-device validation for user-facing flow changes.

## Retired product surfaces

The following preview/superseded screens were removed from source after quarantine and CI verification:
- Community;
- Goals / Goal Details with fabricated preview values;
- older superseded onboarding screens.

Compatibility redirects for restored navigation state are allowed, but these retired features must not be reintroduced without deliberately changing this charter.
