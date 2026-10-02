# PesaTrack entity-catalog sources

This file records evidence used for stable organization identity only. It does **not** establish transaction meaning, message wording, account ownership, or category by itself.

## Phase 6 financial-institution identity pack

Evidence class: **OFFICIAL_OR_REGULATORY_IDENTITY**

Reviewed 2 October 2026.

Organizations added as local canonical identities:

- Co-operative Bank of Kenya — official bank website `co-opbank.co.ke`.
- NCBA Bank Kenya — official group/bank material and Kenyan regulatory listings; domain `ke.ncbagroup.com`.
- Absa Bank Kenya — official bank website `absabank.co.ke`.
- Stanbic Bank Kenya — official bank website `stanbicbank.co.ke`.
- Diamond Trust Bank Kenya — official bank website `dtbk.dtbafrica.com`.
- Family Bank — official bank website `familybank.co.ke`.
- Stima DT Sacco — official Sacco website `stima-sacco.com`; the official contact page also documents the sender labels `StimaSacco` and `StimaDTS`.
- Mwalimu National Sacco — legal entity name cross-checked against public Kenyan regulatory/gazette material.

Guardrails:

- These identities have no default spending category.
- They are not auto-review eligible.
- They do not match from arbitrary raw message text; `allowMessageMatch` remains false.
- Identity matching therefore requires a counterparty/descriptor that already names the institution.
- A recognized bank or Sacco name is evidence about **who the organization is**, not proof that a transaction is income, expense, savings, debt, or an own-account transfer.
- No SMS or app-notification wording is inferred from these identity sources.
