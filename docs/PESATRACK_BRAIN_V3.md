# PesaTrack Brain v3 — Product and Intelligence Guardrails

This document is the repository handoff for the **PesaTrack Intelligence & Product Redesign Blueprint (1 October 2026)**.

The blueprint is the product direction unless a later documented decision explicitly replaces it.

## Core rule

PesaTrack should learn **financial language, identity and user behavior** without making the accounting engine probabilistic.

Keep authoritative and deterministic:

- ledger writes and money arithmetic;
- Financial Intelligence V2 financial meaning;
- mirror/cross-message correlation;
- debt event chains;
- ownership confirmations and durable user decisions;
- explicit observed balances;
- user-confirmed classifications.

Do not let a template, remote service, ML model or LLM invent an amount, balance, account ownership or transfer direction.

## Build order

1. **Message Pack foundation**
   - versioned data schema + router;
   - template ID/revision provenance;
   - V2 generic fallback;
   - Message Lab / redacted fixture export;
   - Data Health coverage counters.
2. **Kenya core corpus**
   - deep M-Pesa, M-Shwari, Fuliza, KCB M-Pesa and PesaLink coverage;
   - then Tier-A banks;
   - sanitized golden fixtures for each supported shape.
3. **Identity Graph**
   - stable self/person/business/biller/institution/account entities;
   - aliases and own-number/name discovery prompts.
4. **Personal Learning**
   - learn category/type from user reviews;
   - high confidence auto-applies, medium suggests, low asks;
   - user decisions always win.
5. **Product simplification**
   - calmer Home;
   - simpler canonical Activity timeline;
   - one Needs Attention inbox;
   - Plan, Accounts and Data Health hierarchy.
6. **Long tail**
   - remaining banks, SACCOs, digital lenders, investment messages and bank-app notification shapes.

## Permanent anti-drift rules

- A missed provider wording is first a **corpus/template coverage problem**, not automatically a new Kotlin parser branch.
- Provider identity is context, not transaction meaning.
- Product-specific mechanics are allowed as metadata when the product genuinely behaves differently.
- Human names must not become businesses merely because they repeat.
- Raw SMS bodies are local; redacted fixture contribution is opt-in.
- Remote enrichment is one evidence source, never the accounting authority.
- Do not redesign Home to hide uncertainty before coverage/provenance plumbing exists.
- Do not weaken existing V2 regression tests to make a Message Pack template pass.

## Current Phase 1 implementation

Branch milestone: feature/brain-v3-message-pack-v1.

Implemented in this milestone:

- domain/messagepack schema, loader, router and parse provenance;
- resource-backed message-packs/ke/ index and initial M-Pesa/M-Shwari/Fuliza templates;
- V2 remains the fallback and final financial interpretation path;
- ingestion diagnostics persist only template ID/revision and extracted field names, not extracted values;
- Data Health can count template routes, V2 fallbacks, understood-no-ledger-effect and unsupported financial messages;
- Message Lab stores only locally redacted unsupported financial shapes and can export a redacted developer fixture;
- Data Health / Message Lab is accessible under More;
- sanitized golden Message Pack fixtures are additional tests and do not replace the existing Financial Intelligence V2 corpus.

## Next milestone after this phase is green

Do **not** jump to Home redesign.

Continue Phase 2: grow the Kenya core corpus, beginning with message shapes for the M-Pesa ecosystem that produce the most transfer/debt ambiguity. Each supported shape should accumulate multiple sanitized fixtures and expected extraction output.


## Phase 2 — Kenya core corpus, increment 1

Branch milestone: `feature/brain-v3-kenya-core-corpus-v1`.

This increment expands the Message Pack from 11 to 21 active templates.

Evidence-backed additions:

- M-Pesa merchant payment;
- completed reversal;
- reversal with explicit credited amount;
- transfer to another named account;
- cash withdrawal;
- cash deposit;
- airtime purchase;
- M-Shwari credit from M-Pesa wording;
- Lock Savings release back to M-Shwari;
- KCB M-PESA declined loan repayment.

Additional hardening:

- M-Shwari loan-balance template revised to accept `Sh`, `Ksh`, `Kshs` and `KES`;
- every active template must have at least one sanitized golden fixture;
- multiple examples are now required across several core shapes;
- KCB M-PESA savings and loan identities are explicit and separate;
- KCB M-PESA loan proceeds are modeled as funding the KCB M-PESA deposit/savings account, not ordinary M-Pesa;
- provider-agnostic failed/declined/rejected financial actions are understood but cannot create movement/debt ledger events;
- observed balances from a failed action may still be retained.

PesaLink remains intentionally untemplated until an exact sanitized message shape is available. Official documentation is used only for the known dual-notification/correlation behavior.


## Phase 2 — Kenya core corpus, increment 2

Merged on `main` before this branch.

- Added the Equity beneficiary-credit / M-Pesa transfer confirmation shape.
- Retained two sanitized Equity variants as fixtures.
- Message Pack reached 22 active templates.
- Financial Intelligence V2 remained the canonical event engine.

## Phase 2 — bank corpus + candidate bridge, increment 3

Branch milestone: `feature/brain-v3-bank-corpus-kcb-pesalink-v1`.

This increment expands the Message Pack from 22 to 35 active templates using **PUBLIC_TEMPLATE_SCHEMA** evidence from the documented Apache-2.0 Kenyan parser source.

Added shapes:

- I&M Bank paid-to;
- KCB account credit;
- KCB app received;
- KCB card purchase;
- KCB M-Pesa received notice;
- KCB sent-to-M-Pesa confirmation;
- KCB paid-to;
- KCB salary credit;
- KCB send-to-M-Pesa;
- KCB send-to-PesaLink;
- PesaLink received;
- PesaLink sent;
- Standard Chartered card purchase.

### Message Pack -> V2 bridge

A new fallback-only bridge converts explicit Message Pack fields into V2 action candidates **only when the existing generic extractor found no action**.

Allowed bridge facts are narrow and deterministic:

- PAYMENT requires an explicit amount + counterparty;
- RECEIPT requires an explicit amount + counterparty;
- TRANSFER requires an explicit amount + source or destination account label.

Permanent safety rules:

- the bridge never overwrites an action V2 already extracted;
- the bridge never writes to the ledger directly;
- V2 relationship resolution and event classification remain authoritative;
- PesaLink network messages do not invent a PesaLink-owned account merely because a sent/received template matched;
- template spans are re-located against normalized text before becoming evidence;
- a template without enough explicit fields remains provenance only.

The purpose of the bridge is to prevent a dangerous false milestone where Data Health says "template matched" while the app still cannot create a correct canonical event.

### Fixture provenance

`fixtures-v4.json` contains sanitized synthetic examples reconstructed from published upstream template anchors. They are test vectors for the public schema, not claims about exact current customer SMS wording.

Real-device samples should later be added as additional fixtures without deleting this evidence trail.


## Phase 2 — DTB Till Moja corpus, increment 4

Branch milestone: `feature/brain-v3-bank-corpus-dtb-v1`.

This increment expands the Message Pack from 35 to 37 active templates.

Added evidence-backed DTB Till Moja shapes:

- payer-side merchant payment confirmation;
- merchant-side payment receipt.

Both shapes:
- use the existing DTB entry in `KenyanBankDirectory`;
- feed explicit amount/counterparty facts through the existing fallback-only Message Pack candidate bridge;
- still rely on Financial Intelligence V2 for relationship resolution and canonical event classification;
- have sanitized golden fixtures and end-to-end canonical-event tests.

No Co-op, Absa, NCBA or Stanbic generic transaction template is added here because exact message wording has not yet been established strongly enough. Keep those providers on generic V2 fallback until concrete evidence exists.


## Phase 3 — Identity Graph foundation

Branch milestone: `feature/brain-v3-identity-graph-foundation-v2`.

Phase 2 remains evidence-driven: Co-op, Absa, NCBA and Stanbic generic transaction messages stay on V2 fallback until exact message bodies are available. That evidence gap does not block the next architectural milestone.

### Purpose

The Identity Graph separates **what happened** from **who/what the parties are**.

It creates durable local identity knowledge for:

- the user (`SELF`);
- the user's financial accounts;
- people;
- businesses;
- billers;
- financial institutions;
- unresolved counterparties.

### Schema v28

New local tables:

- `identity_nodes`;
- `identity_aliases`;
- `identity_links`;
- `identity_observations`.

The graph stores normalized aliases, evidence source, confidence, user-confirmation state, first/last-seen timestamps and observation counts.

### Foundation behavior

- Every enabled `money_accounts` entry becomes a user-owned financial-account node.
- `SELF -> OWNS -> account` links are created deterministically.
- Confirmed account phone identifiers can seed a SELF phone alias.
- Existing confirmed `financial_entities` contribute provider/name aliases to the corresponding account node.
- Ledger transactions are observed idempotently using transaction ID; rescanning history cannot inflate counts.
- Known entities from the local entity catalogue retain their known business/biller/institution kind.
- Unknown counterparties remain `UNKNOWN`; repetition alone never turns them into businesses.
- Phone evidence has stronger identity value than a name-only cluster.
- The graph can produce review candidates for possible self phone, possible self transaction name, and repeated unknown counterparties needing classification.
- User-confirmed aliases/identity kinds outrank inferred evidence.

### Accounting isolation

Identity Graph enrichment is downstream metadata.

A graph failure must never invalidate or roll back an already-correct ledger write. Canonical transactions trigger bounded graph synchronization only after financial ingestion succeeds.

Data Health can perform a larger historical backfill and reports graph counts and pending identity questions.

### Deliberately not included yet

This foundation does not yet:

- apply identity-based personal categories;
- auto-create Smart Rules;
- auto-decide Person vs Business for unknown names;
- expose the final interactive identity-review inbox;
- change Home/Activity design.

The next Phase 3 increment is the user-facing identity review flow: “Is this your number?”, “Is this your transaction name?”, and safe external identity classification.


## Phase 3 — Identity Review, increment 2

Branch milestone: `feature/brain-v3-identity-review-v1`.

This increment turns Identity Graph questions into explicit user decisions without introducing personal category learning yet.

### Schema v29

New local table:

- `identity_review_decisions`

It stores prompt type, identity ID, decision, evidence count and update time.

### Durable review behavior

Self-identity questions support:

- **Yes, mine** — promotes the observed aliases to SELF using the existing user-confirmed Identity Graph path.
- **Not mine** — permanently suppresses SELF prompts for that identity, but does not guess Person/Business/Biller type.
- **Not sure yet** — defers that exact question at the current evidence count. It can return only after new evidence increases.

Repeated unknown counterparties support explicit:

- Person
- Business
- Biller
- Financial institution
- Not sure yet

Classification answers are user-confirmed and durable. Frequency alone still never determines identity kind.

### UI placement

Identity Review lives under **More → Data Health → Identity Review**.

It is deliberately not promoted to Home/Activity yet. The blueprint requires durable identity decisions before product simplification or personal-learning behavior.

### Next milestone

After this increment is green, Phase 4 can begin with personal learning from reviewed transactions and confirmed identities. High-confidence learning may auto-apply only when evidence is strong; medium confidence suggests; low confidence asks. User decisions remain authoritative.


## Phase 4 — Personal Learning, increment 1

Branch milestone: `feature/brain-v3-personal-learning-v1`.

This increment begins personal learning without weakening deterministic accounting or overriding explicit user choices.

### Schema v30

Two local-first tables are added:

- `identity_category_preferences` stores an explicitly remembered category preference against a stable Identity Graph node;
- `personal_learning_applications` records when a learned preference was auto-applied, including the identity, category, direction, confidence and confirmation count.

No raw message text is stored in either table.

### Confidence and authority

The learning layer sits below explicit Smart Rules and user classifications.

- The existing **Remember** action remains authoritative and still creates the exact-name / exact-phone Smart Rule.
- Remember also attaches the chosen ordinary expense category to the resolved Identity Graph node when one can be resolved safely.
- A user-confirmed or high-confidence identity match can auto-apply the remembered category.
- A medium-confidence identity match only produces a suggestion on Transaction Details.
- A low-confidence match does not change the ledger and continues through normal review.
- **Only this one** does not create an identity preference.
- USER_CLASSIFIED, EXCLUDED, Smart-Rule-controlled, linked/refund, transfer, income, debt, savings, investment and cash records are never overwritten by this learning layer.
- A later explicit user classification removes personal-learning application provenance for that transaction.

### Product behavior

Transaction Details can now explain a medium-confidence category suggestion and requires the user to confirm it before the category changes.

Data Health reports:

- number of identities with an explicit remembered category;
- number of transactions auto-applied through personal learning.

### Boundary

This is not a probabilistic transaction classifier and it does not learn from raw name repetition. It only generalizes an explicit remembered category through the durable Identity Graph.

The next Phase 4 increment should focus on conflict handling, preference correction/revocation, and stronger explanation of why an identity matched before any Home/Activity simplification work begins.


## Phase 4 — Personal Learning, increment 2

Branch milestone: `feature/brain-v3-personal-learning-v2`.

This increment completes the first durable correction/revocation loop required before Product Simplification.

### Correction authority

When a transaction was auto-classified by personal learning and the user changes its category:

- an ordinary expense correction updates the identity's remembered category;
- a structural correction such as Transfer removes the old expense preference rather than trying to learn an accounting state;
- the explicit transaction becomes USER_CLASSIFIED and therefore remains authoritative;
- Smart Rules remain above personal learning and are not rewritten by this layer.

### Reversible provenance

Transaction Details can now explain an auto-applied learned category using:

- the stable identity label;
- the matched alias type when available;
- the evidence source when available;
- the confidence that allowed auto-application;
- the remembered confirmation count.

The user can:

- **Keep this, forget future learning** — preserve the current transaction category while deleting the reusable identity preference;
- **Undo & forget** — remove the reusable preference and reopen the transaction as Needs Review / Other.

Medium-confidence suggestions now explain why the identity matched instead of presenting a category guess without provenance.

### Phase boundary

With correction, revocation, explanation and explicit user authority now present, Phase 4 has the minimum durable loop required by the blueprint.

Next: **Phase 5 — Product Simplification**, beginning with the unified Needs Attention model and a calmer Activity hierarchy before doing the full Home visual rewrite.


## Phase 5 — Product Simplification, increment 1

Branch milestone: `feature/phase5-product-simplification-v1`.

This increment begins the blueprint's product hierarchy cleanup without changing accounting semantics.

### Primary navigation

The bottom navigation now follows the user's money:

- Home
- Activity
- Plan
- Accounts
- More

Community is no longer a primary money tab and is available under More. Historical import is also surfaced under More. Goals are moved into the Plan hierarchy instead of competing with Plan as a separate primary destination.

### Activity

The separate **Review / Duplicates / Links** controls are replaced by one **Needs Attention** surface.

Needs Attention can now collect:

- account-ownership questions;
- identity questions;
- transaction category review;
- possible duplicate observations;
- relationship/link questions;
- unsupported financial message shapes.

The surface phrases each section as a human question instead of exposing parser/reconciliation terminology.

Activity's canonical timeline keeps simple filters:

- All
- Needs review
- Spending
- Income
- Transfers

The technical financial-source feed is no longer exposed from the primary Activity controls. Source evidence remains available through transaction details/Data Health paths.

### Home

The old daily quick-action row for Review / Import / Rules / Goals is removed.

Home can show a compact Needs Attention card when current transaction review is required. Import belongs under More/Data Health, rules remain an implementation detail, and goals live under Plan.

### Boundary

This is structural Product Simplification, not the full Home visual redesign yet. The existing ledger, V2 semantics, debt model, identity graph and personal learning authority remain unchanged.

Next Phase 5 increments should continue simplifying Home into the blueprint order (available this cycle, attention, today, where money went, money picture, recent activity, one useful insight) and break the large Home/Activity composables into smaller feature modules.


## Phase 5 — Product Simplification, increment 2

Branch milestone: `feature/phase5-home-hierarchy-v2`.

This increment rebuilds Home around the blueprint's seven-block information hierarchy instead of the old feature-card stack.

### Home order

Home now presents:

1. **Available this cycle** — the existing trusted pay-cycle amount, with cycle start and estimated days remaining when known.
2. **Needs Attention** — shown only when review work exists; its count includes transaction review, possible duplicates, relationship questions, identity questions and unsupported financial message shapes.
3. **Today** — trusted spending today, safe daily pace when a cycle exists, and the amount/count of still-protected commitments.
4. **Where money went** — the top three trusted spending categories with amount/share; budget management routes into Plan instead of becoming another Home card.
5. **Money picture** — latest observed M-Pesa balance/freshness, confirmed account count and active debt summary. Missing balances remain unknown rather than guessed.
6. **Recent activity** — up to five canonical transactions; unresolved duplicate second-observations are suppressed and Needs Review items carry an Uncertain label.
7. **One useful insight** — the existing insight engine is retained as one final card instead of stacking multiple generic insight surfaces.

Conditional salary/pay-cycle prompts remain available without becoming permanent Home navigation.

### Plan and Accounts follow-through

- Budget is now directly reachable from Plan alongside Goals and recurring commitments.
- The Accounts primary tab now also surfaces active debt and routes into debt details.
- Nested Budget screens remain under the Plan tab; nested Debt screens remain under Accounts.

### Performance / hierarchy cleanup

Home no longer loads monthly budget records merely to render a daily Home budget card. Category spending is derived from the trusted Activity snapshot already loaded for Home.

The new Home cards are implemented in `HomeOverviewComponents.kt` so further Phase 5 work can continue breaking the old monolithic Home file into smaller feature modules.

### Safety boundary

This increment changes presentation and navigation only. It does not alter ledger arithmetic, category authority, refund accounting, debt balances, pay-cycle math or message parsing.


## Phase 5 — Product Simplification, increment 3

Branch milestone: `feature/phase5-home-cleanup-v3`.

This is the consolidation pass after the new Home hierarchy landed.

Removed from the legacy `HomeScreen.kt` implementation:

- the old standalone Debt Overview card;
- the old Wallets & Balances card and wallet row implementation;
- the old Payday Picture card and its local value tiles;
- obsolete helpers/imports that existed only for those replaced surfaces.

The active Phase 5 Home presentation remains in `HomeOverviewComponents.kt`.

This reduces `HomeScreen.kt` from roughly 2,346 lines after the hierarchy migration to roughly 1,600 lines and prevents future work from accidentally updating an old card path that is no longer rendered.

### Phase 5 structural completion

The blueprint's Phase 5 product hierarchy is now present:

- calmer seven-block Home;
- canonical Activity timeline;
- one unified Needs Attention inbox;
- Plan as the home for budget, commitments and goals;
- Accounts as the primary wallet/bank/savings/debt hierarchy;
- Data Health under More for technical coverage and diagnostics.

Further UI decomposition and investment-specific account presentation can continue as maintenance/Phase 6 work, but they are no longer prerequisites for the Phase 5 product hierarchy itself.

## Phase 6 — Long-tail coverage, increment 1

Branch milestone: `feature/phase6-long-tail-public-pack-v1`.

Phase 6 begins with evidence-backed adjacent-finance coverage rather than provider-specific Kotlin branches.

### Added public-template shapes

The Kenya Message Pack grows from 37 to 45 active templates with:

- Etica investment-account payment;
- Etica investment redemption;
- Etica withdrawal to M-Pesa;
- Evopay card parking;
- Evopay number-plate parking;
- Globalpay card purchase;
- Sanlam Money Market Fund credit;
- Sanlam investment top-up.

A new `fixtures-v6.json` corpus provides sanitized golden examples, including the published KES and USD Etica redemption variants.

### Semantic guardrails

- Evopay/Globalpay purchase shapes expose PAYMENT provenance and structured fields without bypassing Financial Intelligence V2 or downstream evidence requirements.
- Investment-specific Etica redemption/withdrawal and Sanlam credit/top-up shapes use event kinds that the generic Message Pack candidate bridge does not convert into ordinary ledger movements.
- This intentionally prevents a fund top-up from becoming ordinary spending or a fund credit from becoming ordinary income merely because a public parser labels it that way.
- Foreign-currency extraction can be recorded as provenance, but it is not silently posted as KES.

### Next Phase 6 increments

Continue with evidence-backed bank/app-notification coverage and then recurring/subscription intelligence. Prefer public or sanitized message evidence; official product mechanics alone are not enough to invent SMS wording.


## Phase 6 — Long-tail coverage, increment 2 — Recurring intelligence

Branch milestone: `feature/phase6-recurring-intelligence-v2`.

This increment adds deterministic recurring-payment intelligence without allowing observed repetition to silently create planning commitments.

### Detection policy

PesaTrack examines trusted ordinary expense history and groups observations by stable identity plus category.

A candidate is considered only when:

- at least three observations exist;
- every observed interval is between 24 and 38 days;
- the latest observation is not more than 60 days old;
- observed amounts remain within 35% of the median amount;
- the transaction is not a transfer, debt, savings, investment, cash, income, Other or Uncategorized event;
- the transaction is not excluded, Needs Review, a reversal, linked relationship event, or already controlled by a Smart Rule;
- SELF and owned financial-account identities are excluded.

Confidence is **HIGH** only for stronger evidence: at least four observations, a 27-33 day median interval, no more than 10% amount deviation and no more than six days of interval spread. Other qualifying monthly patterns remain **MEDIUM**.

### User authority

Detection creates a suggestion only.

Plan shows **Possible recurring payments** with:

- identity/merchant label;
- number of observed payments;
- approximate cadence;
- typical median amount;
- next expected date;
- Strong pattern / Possible confidence.

The user must choose **Add to plan** before PesaTrack creates anything reusable.

Accepting a suggestion creates an exact-name/phone `OTHER_RECURRING` Smart Rule with the median amount as `plannedAmountMinor`. That rule then participates in the existing pay-cycle commitment model.

Choosing **Not recurring** writes a durable local decision. PesaTrack will not ask again on the same evidence and requires at least two additional observations before that pattern can reappear.

### Schema v31

`recurring_pattern_decisions` stores only:

- pattern key;
- decision;
- evidence count at the time of the decision;
- update timestamp.

No new raw-message storage is introduced.

### Guardrails

Recurring intelligence does not:

- auto-create Smart Rules;
- auto-change transaction categories;
- infer transfers/debt/savings/investments as subscriptions;
- overwrite explicit user decisions;
- guess a commitment from highly variable payments;
- change ledger arithmetic.

The next Phase 6 increments remain evidence-backed provider/app-notification coverage and additional long-tail institutions.

## Phase 6 — Long-tail coverage, increment 3 — Bank account alerts

Branch milestone: `feature/phase6-bank-alert-pack-v3`.

This increment extends the Message Pack using concrete public sample implementations from an ISC-licensed Kenyan SMS-finance project.

### Added shapes

The Kenya Message Pack grows from 45 to 48 active templates with:

- Co-operative Bank compact account credit;
- Co-operative Bank compact account debit;
- Equity account credit.

Each template extracts the available structured evidence:

- account reference/tail;
- amount;
- date;
- observed balance;
- transaction/reference ID.

### Authority boundary

These public examples are sample/test formats, not bank-certified wording. PesaTrack keeps matching narrow and labels the evidence in the corpus documentation accordingly.

The new event kinds are `ACCOUNT_CREDIT` and `ACCOUNT_DEBIT`.

They deliberately do **not** route through the generic PAYMENT / RECEIPT / TRANSFER Message Pack bridge because the source examples do not establish a counterparty. Financial Intelligence V2 can still interpret stronger evidence in the message, but the template itself cannot invent who money came from or went to.

### Regression coverage

`fixtures-v7.json` adds sanitized public-sample fixtures for all three templates, and the router test requires every active template to retain at least one golden fixture.

Next Phase 6 work should continue with additional banks, SACCOs, lenders and app-notification wording only when concrete public/sanitized message evidence exists.


## Phase 6 — Long-tail coverage, increment 4 — Bank and SACCO notification corpus

Branch milestone: `feature/phase6-bank-sacco-coverage-v3b`.

After the Co-op/Equity account-alert increment, the Kenya Message Pack expands from 48 to **53 active templates** using a separate public observed-message corpus.

Added:
- Fundilima SACCO account withdrawal;
- generic SACCO member deposit;
- generic bank-to-mobile/M-Pesa credit notification;
- generic confirmed bill payment;
- generic account-credit/balance notification.

A new `fixtures-v8.json` stores sanitized synthetic examples of the observed language.

### Safety model

Long-tail coverage must not turn every recognized notification into a ledger movement.

Therefore:
- `SACCO_WITHDRAWAL`, `SACCO_DEPOSIT`, `BANK_TO_MOBILE_TRANSFER` and `ACCOUNT_CREDIT` remain provenance-only event kinds in the generic Message Pack bridge;
- only the explicit confirmed bill-payment shape uses ordinary `PAYMENT` semantics;
- senderless generic templates require strong, multi-anchor body matches;
- no bank/SACCO ownership is inferred from the notification alone;
- real names, phone numbers, account numbers and references from the public corpus are not copied into PesaTrack fixtures.

Phase 6 now covers adjacent-finance templates, recurring-payment intelligence, Co-op/Equity account alerts, and broader bank/SACCO notification language.


## Phase 6 — Long-tail coverage, increment 5 — Source-aware app notifications

Branch milestone: `feature/phase6-source-aware-message-pack-v4`.

This increment rebases the already-green source-aware notification work onto the current Phase 6 corpus.

### Message Pack source constraints

Templates may optionally declare:

- `sourcePackages` — exact Android package IDs allowed to produce the shape;
- `sourceKinds` — ingestion channels such as `financial_notification` or `historical_financial_message`.

Existing SMS templates omit these fields and remain backward compatible.

A source-constrained template does not match if required package/channel evidence is absent or different. Source-constrained matches receive stronger routing specificity than sender-only templates, and parse provenance explains when source-package/source-channel evidence participated.

### Ingestion and Message Lab

The existing notification listener passes source package/channel through the ingestion coordinator into the Financial Message Interpreter and Message Pack router.

Unsupported relevant shapes retain safe package/channel metadata locally alongside the redacted sender/body. Source metadata contributes to unsupported-shape fingerprints so identical text from different apps does not collapse incorrectly.

Data Health surfaces source context for unsupported shapes and developer fixture export retains the safe source metadata.

### Guardrail

Source-aware routing is evidence plumbing, not permission to invent bank-app wording. Provider-specific app templates still require a real sanitized/public notification example before being added.


## Phase 6 — Long-tail coverage, increment 5 — Financial institution identities

Branch milestone: `feature/phase6-financial-identity-pack-v5`.

This increment expands stable financial-institution identity after the 53-template bank/SACCO corpus landed on main.

### New canonical identities

Local entity intelligence now knows:

- Co-operative Bank of Kenya;
- NCBA Bank Kenya;
- Absa Bank Kenya;
- Stanbic Bank Kenya;
- Diamond Trust Bank Kenya;
- Family Bank;
- Stima DT Sacco;
- Mwalimu National Sacco.

A new `EntityKind.SACCO` maps into the Identity Graph as `FINANCIAL_INSTITUTION`.

### Guardrails

All new bank/SACCO identities:

- have no default transaction category;
- are not eligible for automatic review/category application;
- keep `allowMessageMatch = false`;
- require counterparty/descriptor identity evidence instead of matching a brand name anywhere in raw message text.

Identity coverage answers **who is this organization?** Transaction interpretation still requires message and relationship evidence.

Evidence is documented in `docs/entity-catalog-sources.md`.

Next Phase 6 work: make unsupported/app-notification learning source-aware so future app-only templates can be constrained by transport/package evidence without uploading raw inbox content.


## Phase 6 — Long-tail coverage, increment 7 — Verified app-package identity

Branch milestone: `feature/phase6-app-package-identity-v7`.

This increment connects the source-aware notification infrastructure to stable institution identity without changing transaction semantics.

### Verified package registry

A new `FinancialAppSourceRegistry` maps current verified Android banking-app package IDs to existing Kenyan bank identities for:

- KCB;
- Equity;
- Co-operative Bank;
- NCBA;
- Absa;
- Stanbic;
- Diamond Trust Bank.

The evidence trail is recorded in `docs/entity-catalog-sources.md`.

### How it is used

`FinancialMessageIngestionCoordinator` now checks verified package identity when creating source diagnostics and unsupported Message Lab labels.

This means a relevant notification can be attributed to the correct bank even when the notification title is generic or inconsistent.

### Authority boundary

Package identity does **not**:

- claim the bank account belongs to the user;
- alter the notification title used by the semantic interpreter;
- bypass the financial relevance gate;
- create an income/expense/transfer/debt event;
- assign a category;
- create a Smart Rule;
- teach personal learning.

Unknown package IDs fail closed and remain unknown.

This prepares the next long-tail step: provider-specific app-notification templates can use package constraints only after concrete sanitized/public notification wording is available.


## Phase 6 — Long-tail coverage, increment 8 — Coverage by source

Branch milestone: `feature/phase6-source-coverage-v8`.

This increment turns local ingestion diagnostics into an actionable coverage map so long-tail work can be driven by real device evidence instead of guessing which institution to implement next.

### Source coverage analysis

A new pure `FinancialCoverageAnalysis` groups recent financial observations by:

- source channel;
- Android package when available;
- otherwise stable source display identity/title.

For each source it reports:

- observation count;
- understood count/percentage;
- Message Pack template count;
- unsupported financial count;
- latest observation time.

SMS/imported observations and app notifications remain separate coverage groups even when they belong to the same institution because their message shapes can differ.

### Data Health

Data Health now contains **Coverage by source** below the overall coverage summary.

Sources with unsupported financial observations sort first, followed by observation volume and recency. This makes the next Message Pack gap visible without exposing raw message content.

Examples of the intended workflow:

1. receive/import real messages;
2. open Data Health;
3. find a bank/SACCO/lender with unsupported observations;
4. inspect its locally redacted Message Lab fixture;
5. add a narrow evidence-backed template;
6. verify that source coverage improves.

### Guardrail

Coverage analysis is observational only. It does not change transaction meaning, identity, account ownership, category, Smart Rules, recurring commitments or ledger arithmetic.

## Phase 6 — Long-tail coverage, increment 9 — Digital-lender app source identity

Branch milestone: `feature/phase6-lender-app-source-v9`.

This increment extends verified Android source identity beyond banks to the two existing conservative lender identities, Branch and Tala.

### Verified app sources

Added:

- Branch — `com.branch_international.branch.branch_demo_android`;
- Tala — `com.inventureaccess.safarirahisi`.

The evidence comes from current Google Play listings and is recorded in `docs/entity-catalog-sources.md`.

### Registry generalization

`FinancialAppSourceRegistry` no longer assumes that every known financial app must map through `KenyanBankDirectory`.

Bank profiles still resolve exactly as before to BANK/ASSET source references.

Non-bank financial apps may now define a direct provider source identity with explicit conservative hints.

Branch and Tala resolve as:

- `AccountType.OTHER`;
- `AccountOwnershipHint.UNKNOWN`;
- `FinancialAccountNature.UNKNOWN`.

This is intentional because package identity tells PesaTrack which app emitted the notification, not what financial event occurred.

### Product effect

Relevant unsupported Branch/Tala notifications can now be labeled by institution in:

- ingestion diagnostics;
- Message Lab;
- Coverage by source / Data Health.

This makes their real unsupported notification shapes discoverable on-device without inventing message templates.

### Guardrail

No Branch/Tala loan wording is added in this increment. No loan drawdown, repayment, balance, offer or debt account is inferred from the package alone.

The next evidence-backed Branch/Tala template should come from a concrete sanitized/public notification or SMS shape observed in the corpus workflow.

## Phase 6 — Long-tail coverage, increment 10 — Bank and SACCO app source identity

Branch milestone: `feature/phase6-bank-sacco-app-source-v10`.

This increment continues evidence-driven source coverage using current Google Play package identity.

Added verified Android sources:

- I&M OTG Personal KE — `com.imbank.digital.retail.app`;
- MSTIMA — `com.mode.stima`;
- MwalimuHELA — `skyworld.mwalimu`.

I&M maps through the existing Kenyan bank directory.

Stima DT Sacco and Mwalimu National Sacco use direct provider source identities with conservative `AccountType.OTHER`, unknown ownership and unknown financial nature because their apps can expose multiple product types.

### Product effect

Relevant unsupported notifications from these apps now group under the correct institution in local diagnostics / Coverage by source even when their visible notification title is generic.

This reduces future corpus work from “which app is this?” to “what exact redacted financial shape is still unsupported?”

### Guardrail

Package identity is not message interpretation.

No SACCO savings, loan, transfer, bill payment or withdrawal is created from app identity alone. App-specific templates still require concrete message evidence.

## Phase 6 — Long-tail coverage, increment 11 — More verified bank app sources

Branch milestone: `feature/phase6-more-bank-app-sources-v11`.

Verified package-source coverage now also includes:

- Standard Chartered Kenya / SC Mobile — `com.scb.breezebanking.ke`;
- Family Bank / PesaPap — `com.mode.familykenya.ui`;
- Ecobank Mobile — `com.ecobank.mobileapp5`;
- SBM Bank Kenya / Mfukoni — `com.mode.chasetest.ui`.

All four map through the existing `KenyanBankDirectory` identity path.

### Effect

Relevant app notifications from these packages can now be attributed to the correct institution in local ingestion diagnostics, Message Lab and Coverage by source even when the visible notification title is generic.

### Guardrail

This is source identity only.

No account ownership, transaction category, transfer direction, loan, payment or balance is inferred from package identity. App-specific notification templates still require concrete message evidence.


## Phase 6 — Long-tail coverage, increment 9 — Source drill-down

Branch milestone: `feature/phase6-source-drilldown-v9`.

This increment closes the local evidence loop introduced by **Coverage by source**.

### Coverage -> Message Lab

A source card with unsupported financial observations is now actionable.

Tapping it filters Message Lab to only the redacted unsupported shapes that belong to the same stable source key. The source key uses:

- source channel;
- Android package when available;
- otherwise the stable source display identity.

This keeps app-notification and SMS/imported evidence separate even for the same bank.

### Source fixture export

When a source filter is active, Data Health exposes **Copy redacted source fixtures**.

The export is a JSON array of up to 25 locally redacted unsupported fixtures for that source, retaining safe package/channel metadata when available. Individual fixture copy remains available.

### Stability

`FinancialCoverageAnalysis.sourceKey(...)` is now a public pure normalization function shared by diagnostics and Message Lab filtering. Regression tests cover package/channel normalization and display-name fallback.

### Guardrail

This feature does not upload, classify or reinterpret messages. It only makes already-local redacted evidence easier to inspect and contribute. The next Message Pack template should still be created only from concrete reviewed evidence.


## Phase 6 — Long-tail coverage, increment 12 — Message Lab auto-resolution

Branch milestone: `feature/phase6-message-lab-resolution-v12`.

This increment closes the stale-gap problem in the local corpus workflow.

### Stable redacted shape identity

A new pure `MessageLabShapeIdentityFactory` derives a privacy-safe shape key from:

- normalized source channel;
- normalized Android package when available;
- redacted sender;
- redacted message shape.

Sensitive value changes such as amount, date, time, phone/account numbers and salutation names therefore do not create a different gap when the structural message format is the same.

App notifications and SMS/imported observations remain different shape identities.

### Automatic retirement

When a relevant message remains unsupported, Message Lab stores the shape exactly as before.

When a later message with the same redacted shape becomes recognized or semantically understood, PesaTrack now:

- recomputes the same privacy-safe shape identity;
- removes the stale unsupported Message Lab entry;
- marks prior diagnostics carrying that same shape ID as `RESOLVED_BY_CURRENT_COVERAGE`;
- increments a local retired-occurrence counter.

New financial diagnostics persist only the privacy-safe shape ID, never the raw body. Coverage-by-source therefore stops counting those resolved observations as unsupported and can converge as the corpus improves.

No raw-message archive is created.

### Data Health

Data Health now reports both **Resolved by current coverage** diagnostics and **Resolved Message Lab occurrences** so corpus progress is visible.

This does not rewrite historical diagnostics or ledger history. It only keeps the active unsupported-shape inbox aligned with what the current interpreter can understand when that shape is observed again.

### Guardrail

Auto-resolution requires a new live/imported observation of the same redacted shape. PesaTrack does not replay placeholder-redacted fixtures through amount/counterparty extractors because doing so could produce false coverage claims.

## Phase 6 — Long-tail coverage, increment 13 — Lender and loan-product corpus

Branch milestone: `feature/phase6-lender-corpus-v13`.

This increment expands lender/product coverage from a user-supplied structured Kenyan SMS research dictionary.

### Message Pack growth

The Kenya Message Pack grows from **53 to 62 active templates**.

Added:
- M-Shwari loan received with facility fee / M-Pesa proceeds;
- Timiza loan disbursement;
- Tala disbursement to M-Pesa;
- Tala payment-due notice;
- Branch disbursement to M-Pesa;
- Branch payment-due notice;
- Zenka disbursement to M-Pesa;
- OKash disbursement;
- Hustler Fund split disbursement + savings allocation.

`fixtures-v9.json` provides sanitized synthetic golden examples for every new template.

### Debt authority

The new templates deliberately use debt-specific event kinds:

- `DEBT_DRAWDOWN`;
- `DEBT_DRAWDOWN_WITH_SAVINGS`;
- `DEBT_DUE_NOTICE`.

The generic Message Pack candidate bridge still only promotes PAYMENT / RECEIPT / TRANSFER into ordinary action candidates.

Therefore:
- a lender reminder cannot become an expense or repayment merely because it contains “loan payment” and an amount;
- a drawdown template can expose principal, fee, repay amount and date as provenance while Financial Intelligence V2 remains responsible for canonical debt semantics;
- Hustler Fund can expose total loan, M-Pesa proceeds and savings allocation separately without collapsing savings into spending.

### Evidence level

The source is a project-supplied internal parsing dictionary with parameterized placeholders. It is useful structured evidence, but it is not represented as current bank-certified live wording. Matching remains narrow and the evidence class is documented as **USER_RESEARCH_REFERENCE**.

Continue Phase 6 from real Coverage-by-source / Message Lab gaps and other concrete sanitized/public message evidence.


## Phase 6 — Long-tail coverage, increment 15 — Zenka and Timiza app sources

Branch milestone: `feature/phase6-zenka-timiza-app-sources-v15`.

Verified Android source identity now also covers:

- Zenka Loan App Kenya — `com.zenkafinance.microloans`;
- Timiza — `ke.co.absa.digitalwallet`.

Zenka resolves to a conservative direct lender source identity. Timiza resolves to a neutral financial-app source identity rather than a debt account because its official app exposes loans, transfers, payments, insurance and savings.

Both keep `AccountType.OTHER`, UNKNOWN ownership and UNKNOWN financial nature at package-identity level.

This improves Coverage by source and Message Lab attribution only. It does not create a loan event, debt account, category, Smart Rule or ownership claim from package identity.


## Phase 6 — Long-tail coverage, increment 14 — Lender debt bridge

Branch milestone: `feature/phase6-lender-debt-bridge-v14`.

This increment makes the 62-template lender corpus useful to the existing debt engine without turning reminders or ambiguous split products into ledger movements.

### Stable lender liabilities

The Financial Provider Registry now has explicit liability identities for:

- Timiza Loan;
- Tala Loan;
- Branch Loan;
- Zenka Loan;
- OKash Loan;
- Hustler Fund Loan.

Timiza also has a stable owned Timiza account identity for messages that explicitly say loan proceeds were disbursed to **your Timiza account**.

Cash-disbursement behavior is configured conservatively:

- Tala / Branch / Zenka -> M-Pesa only because their current template wording explicitly says the proceeds went to M-Pesa;
- Timiza -> Timiza account because the template explicitly says so;
- OKash remains destination-unknown because the current template does not state where proceeds landed;
- Hustler Fund has a provider identity but its split drawdown is not yet bridged into the single-proceeds debt model.

### Message Pack -> debt candidates

For event kind `DEBT_DRAWDOWN`, Message Pack evidence may now fill missing generic debt fields:

- principal amount;
- lender liability identity;
- explicit fee;
- observed repay / total-due amount as outstanding-after evidence;
- proceeds account only when the message explicitly states it.

If the generic debt extractor already found a drawdown, Message Pack evidence only fills missing fields. It does not replace stronger generic evidence.

### Guardrails

- `DEBT_DUE_NOTICE` remains provenance-only and creates no debt movement.
- `DEBT_DRAWDOWN_WITH_SAVINGS` remains provenance-only until the canonical debt model can preserve both proceeds and mandatory savings allocation.
- No lender reminder becomes a repayment.
- No proceeds destination is inferred for OKash.
- Existing Financial Intelligence V2 / DebtStore authority remains unchanged.


## Phase 6 — Long-tail coverage, increment 16 — Investment semantics

Branch milestone: `feature/phase6-investment-semantics-v16`.

This increment moves the existing Etica/Sanlam Message Pack from simple provenance toward the provider-neutral investment engine without turning investment cash messages into ordinary spending or income.

### Explicit executed investment evidence

A matched financial Message Pack is itself sufficient evidence to pass the generic relevance gate; unknown text still fails closed through the normal relevance screen.

A matched `INVESTMENT_CREDIT` may become a `SemanticInvestmentFact` only when the template exposes all of:

- product/instrument label;
- unit quantity;
- monetary value;
- KES-compatible evidence.

The current Sanlam Money Market Fund credit shape satisfies that requirement, so it becomes a HIGH-confidence **EXECUTED BUY** observation. Units are stored as quantity microunits, the fund name becomes the stable instrument identity, and the credited value becomes investment value. The existing `InvestmentStore` remains authoritative for persistence, settlement linking and observed position delta.

This is an observed investment event, not an authoritative fund balance.

### Provenance-only investment movements

Amount-only investment shapes remain understood without changing a position:

- Sanlam top-up;
- Etica redemption;
- Etica withdrawal to M-Pesa;
- foreign-currency Etica redemption.

For these shapes PesaTrack suppresses accidental PAYMENT / RECEIPT / TRANSFER semantics caused by generic wording such as “sent” or “credited”, emits no ordinary money-movement event, blocks legacy transaction fallback, and keeps the Message Pack provenance as an understood investment confirmation.

USD evidence is never converted into KES.

### Legacy-fallback guardrail

`FinancialMessageInterpretation` now carries an explicit legacy-fallback block. Provenance-only investment shapes and provenance-only debt shapes such as due notices / split Hustler Fund drawdowns can therefore stop older transaction parsing even when they intentionally create no ordinary ledger event.

The ingestion coordinator preserves this decision, and historical import distinguishes “handled safely with no ledger movement” from a duplicate transaction.

No raw-message archive, provider-specific investment parser branch or inferred unit quantity is introduced.


## Phase 6 — Long-tail coverage, increment 17 — Core corpus depth

Branch milestone: `feature/phase6-core-fixture-depth-v17`.

This increment turns fixture depth into a measurable corpus-quality constraint instead of relying only on template count.

### Core ambiguity floor

The ten highest-risk Kenya shapes now have at least **three golden fixtures each**:

- M-Pesa send money;
- M-Pesa receive money;
- M-Pesa PayBill;
- M-Shwari -> M-Pesa;
- M-Pesa -> M-Shwari;
- M-Shwari loan approval/disbursement;
- M-Shwari loan balance;
- Fuliza drawdown;
- Fuliza partial repayment;
- Fuliza full repayment.

`fixtures-v10.json` adds 15 sanitized value/date/spacing variants using wording structures already represented by the supported templates. It does not invent a new provider message shape.

### Corpus health tests

`MessagePackCorpusHealthTest` now enforces:

- every active Kenya template has at least one golden fixture;
- fixture IDs are unique;
- every fixture references an active template;
- the ten core ambiguity shapes above cannot fall below three fixtures.

The full router/extraction regression suite also loads `fixtures-v10.json`, so every added fixture must route to the expected template and extract its declared fields.

### Boundary

The blueprint recommends 3-10 fixtures per shape. This increment deliberately applies that floor first to the flows most likely to create duplicate-accounting, transfer or debt mistakes. Long-tail templates remain evidence-gated and should gain additional fixtures only from supported/sanitized wording evidence rather than fabricated provider text.


## Phase 6 — Long-tail coverage, increment 18 — Tier-A bank fixture depth

Branch milestone: `feature/phase6-tier-a-fixture-depth-v18`.

This increment extends the blueprint's **3-10 sanitized fixtures per supported shape** target from the core M-Pesa/M-Shwari/Fuliza ambiguity flows to the currently supported Tier-A bank shapes.

### Added regression depth

`fixtures-v11.json` adds **33 sanitized value/date/reference variants** using wording structures that were already supported by the existing templates.

The following 17 supported bank shapes now have at least three golden fixtures each:

- Co-operative Bank account credit;
- Co-operative Bank account debit;
- DTB Till Moja payer payment;
- DTB Till Moja merchant receipt;
- Equity account credit;
- Equity transfer credit-to-beneficiary;
- I&M paid-to;
- KCB account credit;
- KCB salary credit;
- KCB app received;
- KCB card purchase;
- KCB M-Pesa received;
- KCB M-Pesa sent;
- KCB paid-to;
- KCB send-to-M-Pesa;
- KCB send-to-PesaLink;
- Standard Chartered card purchase.

### Corpus-health rule

`MessagePackCorpusHealthTest` now fails CI if any of those supported Tier-A bank templates drops below the three-fixture floor.

The existing core ambiguity floor remains unchanged.

### Evidence boundary

No new provider wording is introduced here.

Every new fixture is a sanitized amount/date/reference/name/account variant of an already-supported message structure. This improves extraction and routing regression coverage without pretending that a bank currently emits an unverified message shape.

Phase 6 remains evidence-driven: new long-tail templates still require real sanitized/public wording evidence.


## Phase 6 — Long-tail coverage, increment 19 — Adjacent-finance fixture depth

Branch milestone: `feature/phase6-adjacent-fixture-depth-v19`.

This increment extends the three-fixture regression floor to the supported adjacent-finance shapes that now participate in guarded savings, debt or investment semantics.

### Added regression depth

`fixtures-v12.json` adds **33 sanitized variants** using wording structures already supported by the current Message Pack.

The following 17 shapes now have at least three golden fixtures each:

- M-Shwari Lock Savings deposit;
- M-Shwari Lock Savings release to M-Shwari;
- M-Shwari credit from M-Pesa;
- M-Shwari fee-bearing loan receipt;
- Etica account payment;
- Etica redemption;
- Etica withdrawal to M-Pesa;
- Sanlam Money Market Fund credit;
- Sanlam investment top-up;
- Branch drawdown;
- Branch payment-due notice;
- Tala drawdown;
- Tala payment-due notice;
- Timiza drawdown;
- Zenka drawdown;
- OKash drawdown;
- Hustler Fund split drawdown/savings.

### Why this matters

These shapes include cases where confusing one field can corrupt debt, savings or investment interpretation. The extra fixtures therefore exercise amount, fee, repay amount, savings allocation, unit amount, date, account and counterparty extraction across more value combinations.

### Corpus-health rule

CI now fails if any of these adjacent-finance templates drops below three golden fixtures.

No new provider wording is introduced. The new fixtures only vary sanitized values inside already-supported message structures.


## Phase 6 — Long-tail coverage, increment 20 — Full corpus fixture floor

Branch milestone: `feature/phase6-full-corpus-fixture-floor-v20`.

This increment completes the blueprint's recommended minimum regression depth across the entire active Kenya Message Pack.

### Full-corpus floor

`fixtures-v13.json` adds **36 sanitized variants** for the final 18 supported shapes that previously had only one golden example.

Those remaining shapes cover:

- M-Pesa merchant payment, cash deposit, cash withdrawal, airtime, external-account transfer and two reversal forms;
- KCB M-Pesa repayment-declined;
- PesaLink sent/received;
- Evopay parking card/LPN payments;
- Globalpay card purchase;
- Fundilima SACCO withdrawal;
- generic SACCO deposit;
- generic bank mobile credit;
- generic bank bill payment;
- generic account credit/balance notice.

After v13, **all 62 active Kenya templates have at least three golden fixtures**.

### Corpus-health invariant

CI now includes a universal rule:

> every active Kenya Message Pack template must have at least three golden fixtures.

The earlier focused core-ambiguity, Tier-A bank and adjacent-finance fixture floors remain in place as explicit regression intent.

### Evidence boundary

No new provider wording is introduced.

All new examples are sanitized value/reference/date/name/account variants of message structures that were already supported. This raises regression depth without fabricating new bank, SACCO, wallet or payment-provider messages.

Phase 6 can now treat three-fixture coverage as a baseline invariant and use real Coverage-by-source / Message Lab evidence for any future new template.


## Phase 6 — Long-tail coverage, increment 21 — Brain v3 acceptance gates

Branch milestone: `feature/phase6-brain-v3-acceptance-gates-v21`.

This increment turns the blueprint's recommended intelligence targets into executable CI gates instead of leaving them as prose.

### Supported-corpus relevance gate

`BrainV3AcceptanceGateTest` evaluates every sanitized Message Pack fixture through the same system boundary used by production:

- a deterministic Message Pack match counts as accepted financial evidence;
- otherwise the provider-agnostic Financial Message Relevance Gate must accept it.

The supported financial corpus must remain at or above **95% acceptance**, matching the blueprint target.

The test also retains explicit non-financial controls from the canonical financial fixture corpus and requires those controls to remain rejected.

### Structured extraction gate

For every declared `expectedFields` value across fixture corpora v1-v13, the matched template extraction is compared with the expected structured value.

The aggregate declared-field accuracy must remain at or above **90%**, matching the blueprint target.

Existing router tests remain stricter and still fail on individual route/field regressions. The new gate exists to make the product-level target measurable and visible.

### Current corpus baseline

The active Kenya Message Pack remains:

- 62 active templates;
- at least three sanitized golden fixtures per active template;
- source-aware app/SMS diagnostics;
- guarded lender/investment semantic bridges;
- evidence-driven Message Lab coverage workflow.

Phase 6 should no longer add new provider wording merely to increase template count. Future template additions should be selected from real Coverage-by-source / Message Lab gaps and must preserve these quality gates.


## Phase 6 — Long-tail coverage, increment 22 — Duplicate-accounting safety gate

Branch milestone: `feature/phase6-duplicate-accounting-gate-v22`.

This increment turns the blueprint's duplicate-accounting target into an explicit regression acceptance gate.

### Strong known mirrors must auto-link

The gate covers representative high-evidence mirror cases:

- exact shared external reference;
- same transaction code;
- complementary owned-account path;
- shared stable counterparty identity across distinct sources;
- complementary transfer/income direction;
- coded + codeless copies inside the very-close safety window.

Every known mirror fixture must auto-link.

### Known distinct movements must stay separate

The gate also protects against false merging:

- conflicting amounts even with a shared reference;
- two real transactions with different strong codes;
- same amount outside the correlation window;
- reversals;
- codeless time-only similarity from the same source;
- ultra-close codeless observations from distinct sources.

Every known distinct fixture must remain unlinked.

### Ambiguity stays reviewable

A codeless same-amount pair from distinct sources inside the review window is required to surface through `PossibleDuplicatePolicy` while remaining **not auto-linked** by `MirrorTransactionMatcher`.

This keeps weak evidence in Needs Attention instead of silently collapsing or silently accepting two observations as one.

### Boundary

This is a regression gate around the existing correlation engine, not a change to matching thresholds or ledger arithmetic.

The target is practical near-zero known duplicate-accounting regressions: known strong mirrors must correlate, known distinct movements must not, and ambiguous pairs must remain review-only.

The same integration pass also consolidates the useful readiness checks from the overlapping readiness branch into the existing `BrainV3AcceptanceGateTest`:

- relevance accuracy is now measured against labeled Foundation + canonical financial corpora rather than only positive Message Pack fixtures;
- every supported Message Pack fixture must produce deterministic TEMPLATE provenance with the expected template ID and a non-empty human-readable reason;
- the canonical financial fixture corpus must have zero legacy transaction-parser dependency.

This avoids maintaining two competing Phase 6 readiness frameworks.
