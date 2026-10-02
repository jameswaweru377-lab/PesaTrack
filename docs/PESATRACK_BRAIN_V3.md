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
