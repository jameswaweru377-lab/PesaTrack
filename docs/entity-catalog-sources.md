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

## Phase 6 verified Android banking-app packages

Evidence class: **OFFICIAL_APP_STORE_PACKAGE_IDENTITY**

Reviewed 2 October 2026 from current Google Play listings.

Package identity added:

- KCB Mobile — `com.kcb.mobilebanking.android.mbp` — KCB BANK GROUP.
- Equity Mobile — `ke.co.equitygroup.equitymobile` — Equity Group.
- Co-op Bank Mobile Banking App — `com.mcoopcash.retail6` — Co-operative Bank of Kenya Ltd.
- NCBA NOW — `com.nicbank.android` — listing states the app is owned by NCBA Bank.
- Absa Kenya — `com.absa.ke.mobile.android.ui` — Absa Group Limited.
- Stanbic Bank Kenya — `com.cfcstanbicbank.app` — Standard Bank / Stanbic Bank.
- DTB 24/7 — `com.dtb.app` — Diamond Trust Bank.

Guardrails:

- Package identity is evidence about **which institution emitted an Android notification**.
- It does not prove the user owns a bank account.
- It does not classify the notification as income, spending, transfer, debt or savings.
- It does not bypass financial-language relevance screening.
- It does not create a transaction when the message wording is unsupported.
- Unknown package IDs remain unknown.
- A package is only added after an official/current app-store listing can be verified.

Current Google Play evidence URLs:

- https://play.google.com/store/apps/details?id=com.kcb.mobilebanking.android.mbp
- https://play.google.com/store/apps/details?id=ke.co.equitygroup.equitymobile
- https://play.google.com/store/apps/details?id=com.mcoopcash.retail6
- https://play.google.com/store/apps/details?id=com.nicbank.android
- https://play.google.com/store/apps/details?id=com.absa.ke.mobile.android.ui
- https://play.google.com/store/apps/details?id=com.cfcstanbicbank.app
- https://play.google.com/store/apps/details?id=com.dtb.app


## Phase 6 verified digital-finance app packages

Evidence class: **OFFICIAL_APP_STORE_PACKAGE_IDENTITY**

Reviewed 2 October 2026 from current Google Play listings.

Package identity added:

- Branch — `com.branch_international.branch.branch_demo_android` — Branch International Financial Services Limited.
- Tala Kenya — `com.inventureaccess.safarirahisi` — Tala Mobile.
- Timiza — `ke.co.absa.digitalwallet` — Absa Group Limited.

Guardrails:

- Package identity establishes only **which app emitted an Android notification**.
- Branch and Timiza are treated as financial platforms rather than automatically as loan/debt sources because the apps expose multiple financial functions.
- Tala is labeled as a lender source, but the package alone still does not prove that a specific notification is a loan drawdown, repayment or balance.
- No account ownership, category, debt balance, transaction direction or ledger movement is inferred from package identity.
- Provider-specific notification wording still requires a concrete sanitized/public message example.
- Unknown package IDs fail closed.

Current Google Play evidence URLs:

- https://play.google.com/store/apps/details?gl=KE&hl=en&id=com.branch_international.branch.branch_demo_android
- https://play.google.com/store/apps/details?hl=en&id=com.inventureaccess.safarirahisi
- https://play.google.com/store/apps/details?id=ke.co.absa.digitalwallet
