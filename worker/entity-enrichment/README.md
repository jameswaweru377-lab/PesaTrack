# PesaTrack entity enrichment Worker

This Worker is the privacy boundary between the Android app and the external merchant-enrichment provider.

## Privacy contract

The Android app sends only a sanitized merchant/entity descriptor such as `KPLC PREPAID` or `ACME HARDWARE LTD`.

The app and Worker reject descriptors that look like:

- Kenyan phone numbers
- M-PESA-style 10-character transaction codes
- email addresses
- long account/reference numbers
- likely personal names

The Worker must never receive a full SMS body, transaction amount, balance, phone number, transaction code, or account number.

## Provider

The Worker calls Logo.dev's transaction enrichment endpoint using a server-side secret. Never put that secret in the Android APK.

Set the secret:

```bash
npx wrangler secret put LOGO_DEV_SECRET_KEY
```

Deploy:

```bash
cd worker/entity-enrichment
npx wrangler deploy
```

The Android build then needs the deployed endpoint:

```properties
PESATRACK_ENTITY_ENRICHMENT_URL=https://<worker-host>/v1/entity/resolve
```

Do not commit a private provider key or token to the repository.

## Automatic review policy

The Worker may return a category hint, but the Android app remains the final policy gate. Only high-confidence results in a conservative allow-list of ordinary expense categories may auto-review a transaction. Financial services, loans, transfers, utilities that are not specific enough, and ambiguous results remain in the normal review flow.
