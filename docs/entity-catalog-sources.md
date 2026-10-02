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

## Phase 6 verified Android digital-lender app packages

Evidence class: **OFFICIAL_APP_STORE_PACKAGE_IDENTITY**

Reviewed 2 October 2026 from current Google Play listings.

Package identity added:

- Branch: Loans & Mobile Banking — `com.branch_international.branch.branch_demo_android` — Branch International Financial Services Limited.
- Tala: Fast & Safe Pesa Loan — `com.inventureaccess.safarirahisi` — Tala Mobile / Inventure Capital Corporation.

Guardrails:

- These package IDs establish **notification source identity only**.
- Branch currently exposes loans, wallet/savings, transfers and payments, so package identity alone cannot determine whether a particular notification represents an asset, liability, transfer, payment or loan event.
- Tala package identity likewise does not prove a notification is a drawdown, repayment, offer, reminder or account-status message.
- Both therefore resolve with `AccountType.OTHER`, `AccountOwnershipHint.UNKNOWN` and `FinancialAccountNature.UNKNOWN`.
- No loan transaction is created from package identity.
- No debt account is created from package identity.
- No message wording is inferred from the app-store descriptions.
- Provider-specific Branch/Tala notification templates still require concrete sanitized/public notification wording.

Current Google Play evidence URLs:

- https://play.google.com/store/apps/details?id=com.branch_international.branch.branch_demo_android
- https://play.google.com/store/apps/details?id=com.inventureaccess.safarirahisi

## Phase 6 verified I&M and SACCO Android app packages

Evidence class: **OFFICIAL_APP_STORE_PACKAGE_IDENTITY**

Reviewed 2 October 2026 from current Google Play listings.

Package identity added:

- I&M OTG Personal KE — `com.imbank.digital.retail.app` — I&M Bank Limited.
- MSTIMA — `com.mode.stima` — Stima Sacco DT.
- MwalimuHELA — `skyworld.mwalimu` — published as Mwalimu National Mobile Banking Application with Mwalimu National support contact.

Guardrails:

- I&M maps through the existing `KenyanBankDirectory` bank identity and therefore retains the existing BANK/ASSET source-reference convention, with ownership still UNKNOWN.
- MSTIMA exposes savings, transfers, payments and loan functions, so package identity alone cannot establish asset/liability semantics. It resolves as OTHER/UNKNOWN.
- MwalimuHELA likewise exposes transaction/account services and resolves as OTHER/UNKNOWN.
- Neither SACCO package creates an account, debt or transfer merely because a notification came from that app.
- No notification wording is inferred from the app-store listings.
- Provider-specific app templates still require concrete sanitized/public notification wording.

Current Google Play evidence URLs:

- https://play.google.com/store/apps/details?id=com.imbank.digital.retail.app
- https://play.google.com/store/apps/details?id=com.mode.stima
- https://play.google.com/store/apps/details?id=skyworld.mwalimu

## Phase 6 verified Standard Chartered and Family Bank Android app packages

Evidence class: **OFFICIAL_APP_STORE_PACKAGE_IDENTITY**

Reviewed 2 October 2026 from current Google Play listings.

Package identity added:

- SC Mobile Kenya — `com.scb.breezebanking.ke` — Standard Chartered Bank PLC.
- PesaPap, Family Bank — `com.mode.familykenya.ui` — Family Bank.

Guardrails:

- Both packages map through the existing `KenyanBankDirectory`.
- Package identity establishes the institution that emitted a notification; ownership remains UNKNOWN.
- No account, transfer, card purchase, bill payment, loan, savings movement or category is inferred from the package alone.
- No app-notification wording is inferred from Play Store product descriptions.
- Provider-specific app templates still require concrete sanitized/public notification wording.

Current Google Play evidence URLs:

- https://play.google.com/store/apps/details?id=com.scb.breezebanking.ke
- https://play.google.com/store/apps/details?id=com.mode.familykenya.ui
