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
