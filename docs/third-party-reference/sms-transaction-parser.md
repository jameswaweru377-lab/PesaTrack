# Message Pack architecture reference

PesaTrack Message Pack v1 was designed independently from the PesaTrack blueprint, but the implementation approach was evaluated against:

- Repository: peteretelej/sms-transaction-parser
- License: Apache-2.0
- Public architecture reviewed on 1 October 2026.

Useful design patterns observed there:

- provider/message shapes stored as JSON data;
- an index of bundled templates;
- stable template IDs and revisions;
- active/retired template behavior;
- golden fixtures;
- a fallback path when no template matches.

PesaTrack does **not** replace Financial Intelligence V2 with that library and does not copy its parser engine. PesaTrack keeps its own schema because it must preserve richer concepts such as owned-account identity, debt products, mirror observations, event chains, canonical ledger movements, and explicit user decisions.

The initial PesaTrack JSON templates were authored from PesaTrack's existing sanitized regression corpus and real-device regression cases.


## Phase 2 bank-corpus use

PesaTrack later used the upstream Kenya JSON templates as Apache-2.0 **public template schema evidence** for a limited set of KCB Bank, I&M Bank, PesaLink and Standard Chartered shapes.

PesaTrack does not import the upstream parser runtime. The JSON shapes are adapted into PesaTrack's own schema, IDs, product identities, provenance, V2 candidate bridge and regression fixtures.

The upstream files consulted are listed in `docs/message-corpus-sources.md`. Synthetic PesaTrack fixtures based on those anchors are explicitly marked as public-template-derived rather than real-device messages.
