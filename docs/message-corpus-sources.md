# Kenya Message Corpus — Evidence Sources

This file records where PesaTrack Brain v3 message/product knowledge came from.

## Evidence classes

1. **REAL_SANITIZED**
   - Sanitized message text already present in PesaTrack regression tests or supplied from a real device.
   - Highest priority for authoring a concrete Message Pack shape.

2. **OFFICIAL_PRODUCT_RULE**
   - Product mechanics documented by the provider.
   - Suitable for account/product relationships, but not for inventing exact SMS wording.

3. **PUBLIC_MESSAGE_SAMPLE**
   - A published screenshot/report containing a message example.
   - Must be sanitized before becoming a fixture.
   - Treat historical wording as a shape sample, not a promise that current wording is identical.

## Phase 2 sources

### M-Pesa / M-Shwari / Fuliza

Evidence class: **REAL_SANITIZED**

Primary source:
- Existing PesaTrack regression corpora under `app/src/test/java/com/kedevjimmy/pesatrack/domain/interpretation/`.
- Existing real-device M-Shwari samples supplied during development.

The Phase 2 templates for merchant payment, reversal, external account transfer, cash withdrawal, cash deposit, airtime, M-Shwari savings movement and Lock Savings release are authored from these sanitized fixtures.

### KCB M-PESA product model

Evidence class: **OFFICIAL_PRODUCT_RULE**

Official references:
- https://ke.kcbgroup.com/products/kcb-mpesa
- https://ke.kcbgroup.com/products/kcb-mpesa-loan
- https://ke.kcbgroup.com/images/downloads/KCB_MPESA_TERMS_AND_CONDITONS.pdf

Product facts used by PesaTrack:
- KCB M-PESA has a separate deposit/savings account.
- Loan proceeds are credited into the KCB M-PESA Deposit Account before the customer moves funds to ordinary M-PESA.
- The KCB M-PESA loan therefore must not be modeled as a loan whose proceeds account is automatically ordinary M-PESA.

### KCB M-PESA declined repayment sample

Evidence class: **PUBLIC_MESSAGE_SAMPLE**

Public research reference:
- MicroSave Consulting, *Digital Credit in Kenya — Final Report* (2019).
- Public PDF: https://www.microsave.net/wp-content/uploads/2019/09/Digital-Credit-Kenya-Final-report.pdf

The report contains a KCB M-PESA SMS screenshot where a loan repayment is declined for insufficient funds and the KCB M-PESA account balance is shown.

PesaTrack stores only a sanitized fixture. The important semantic rule is generic:
**declined / rejected / failed financial actions do not create ledger movement events.**

### PesaLink

Evidence class: **OFFICIAL_PRODUCT_RULE**

References:
- https://pesalink.co.ke/for-me/how-to-use-pesalink
- https://ke.kcbgroup.com/products/pesalink
- https://ke.kcbgroup.com/images/downloads/KCB_PESALINK_FAQs.pdf

Confirmed product mechanics:
- PesaLink is an inter-bank instant transfer network.
- KCB states that a PesaLink transfer can generate a KCB confirmation and a separate PesaLink confirmation.
- PesaLink/KCB documentation confirms that notifications can contain sender/recipient, amount and bank information.

The official sources above establish PesaLink's role and duplicate-notification behavior.

Phase 2 increment 3 additionally uses the Apache-2.0 public template schema from `peteretelej/sms-transaction-parser` as **PUBLIC_TEMPLATE_SCHEMA** evidence for PesaLink sent/received wording. Those fixtures are synthetic sanitized examples reconstructed from published anchors; they are not represented as current screenshots from a user's device.


### Equity beneficiary / M-Pesa transfer confirmation

Evidence class: **REAL_SANITIZED**

Source:
- Existing PesaTrack Financial Intelligence V2 sanitized regression corpus.

Two variants are retained as golden fixtures:
- a compact `Kshs.` form containing a beneficiary phone/name, bank reference and M-Pesa reference;
- a `Kenya Shillings` form using explicit `Equity Ref` and `M-Pesa Ref` labels.

Both belong to the stable template ID `ke.equity.transfer.credit-to-beneficiary`.

This does **not** imply that every Equity SMS is now supported by the Message Pack. Other Equity transaction families remain on Financial Intelligence V2 fallback until a concrete sanitized shape is available.


### KCB Bank, I&M Bank, PesaLink and Standard Chartered public template schemas

Evidence class: **PUBLIC_TEMPLATE_SCHEMA**

Source:
- Repository: `peteretelej/sms-transaction-parser`
- License: Apache-2.0
- Public Kenya provider templates reviewed on 1 October 2026.

Upstream shapes adapted into the PesaTrack Message Pack:
- `iandmbank_paid_to.json`
- `kcb_account_credit.json`
- `kcb_app_received.json`
- `kcb_card.json`
- `kcb_mpesa_received.json`
- `kcb_mpesa_sent.json`
- `kcb_paid_to.json`
- `kcb_salary_credit.json`
- `kcb_send_to_mpesa.json`
- `kcb_send_to_pesalink.json`
- `pesalink_received.json`
- `pesalink_sent.json`
- `stanchart_card.json`

PesaTrack adapts the published anchors and field boundaries into its own versioned schema. The associated `fixtures-v4.json` messages are synthetic sanitized examples constructed to exercise those published shapes. They must not be described as exact current messages from a customer's device.

Permanent rule: a public template can expand language coverage, but real-device samples should be added later as additional golden fixtures rather than silently replacing the public-source evidence trail.


### DTB Till Moja payment notifications

Evidence classes: **OFFICIAL_PRODUCT_RULE + PUBLIC_MESSAGE_SAMPLE**

Official DTB source:
- https://dtbk.dtbafrica.com/account/till-moja-collection-service-2

Official product behavior used by PesaTrack:
- Till Moja settles mobile-money collections directly into a DTB account.
- DTB provides instant payment alerts to both merchants and customers.
- The product supports payment notifications over SMS.

Published message examples:
- DTB Till Moja FAQ copy mirrored publicly at:
  https://www.scribd.com/document/735178762/DTB-Till-Moja-FAQs-V6

The published FAQ contains examples of:
- a payer confirmation shaped like `you have successfully paid KES ... to ... Ref ...`;
- a merchant receipt shaped like `KES ... has been paid into ... by ... Ref:...`.

PesaTrack stores only sanitized synthetic fixtures based on those published message shapes. The DTB bank identity itself comes from the existing `KenyanBankDirectory`; no new bank-specific parser branch is introduced.

## Phase 6 sources

### Etica, Evopay, Globalpay and Sanlam public template schemas

Evidence class: **PUBLIC_TEMPLATE_SCHEMA**

Source:
- Repository: `peteretelej/sms-transaction-parser`
- License: Apache-2.0
- Public Kenya provider templates and golden fixtures reviewed on 2 October 2026.

Upstream shapes adapted into the PesaTrack Message Pack:
- `etica_payment.json`
- `etica_redemption.json`
- `etica_withdrawal.json`
- `evopay_card_parking.json`
- `evopay_lpn_parking.json`
- `globalpay_card_purchase.json`
- `sanlam_fund_credit.json`
- `sanlam_top_up.json`

PesaTrack stores sanitized synthetic fixtures reconstructed from those published templates/fixtures. These are not represented as current screenshots from a user's device.

Important semantic boundary:
- Evopay parking and Globalpay card-purchase shapes may contribute ordinary PAYMENT candidates when generic extraction is otherwise empty.
- Etica redemption/withdrawal and Sanlam fund-credit/top-up shapes record template provenance using investment-specific event kinds. The Message Pack bridge does not convert those investment shapes into ordinary income/expense/transfer ledger movements. Financial Intelligence V2 and the investment relationship layer remain authoritative.
- Etica's published USD redemption variant is retained only as extraction/provenance coverage. PesaTrack must not silently treat a foreign-currency amount as a KES ledger amount.
