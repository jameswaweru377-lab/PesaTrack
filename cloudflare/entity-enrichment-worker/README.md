# PesaTrack entity enrichment proxy

This Worker is an optional privacy boundary between the Android app and an external merchant-enrichment provider.

## Privacy contract

The Android app only sends a sanitized merchant/bill descriptor such as `KPLC PREPAID` or `NAIVAS`.

The app never sends the raw SMS body, transaction code, phone number, amount, balance, account number, or a person-to-person `SENT_MONEY` counterparty to this endpoint.

The Worker repeats the safety validation and rejects descriptors that appear to contain phone numbers, M-PESA-style reference codes, amounts, or multiline message text.

## Upstream

The first adapter uses Logo.dev's transaction enrichment endpoint. The secret stays in the Worker:

```bash
wrangler secret put LOGO_DEV_API_KEY
```

The Worker returns only:

- canonical merchant name
- domain
- HTTPS logo URL when available
- provider category
- confidence
- source

The Android app maps provider categories into PesaTrack categories locally. Finance/bank/lending results are never converted into expenses merely because of the company identity.

## Android configuration

Set the Worker base URL at build time:

```bash
PESATRACK_ENTITY_API_BASE_URL=https://your-worker.workers.dev ./gradlew :app:assembleDebug
```

If the value is absent, PesaTrack remains fully offline and skips online enrichment.

The external provider can be replaced later without changing the Android contract as long as `POST /v1/entities/resolve` keeps the same response shape.
