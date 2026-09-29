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

Deploy from the repository root (this matches Cloudflare Git Builds):

```bash
npx wrangler deploy
```

The repository-root `wrangler.jsonc` points to `worker/entity-enrichment/src/index.js`.

You can also deploy from the Worker folder explicitly:

```bash
cd worker/entity-enrichment
npx wrangler deploy --config wrangler.jsonc
```

The Android build then needs the deployed endpoint:

```properties
PESATRACK_ENTITY_ENRICHMENT_URL=https://<worker-host>/v1/entity/resolve
```

Do not commit a private provider key or token to the repository.

## Automatic review policy

The Worker may return a category hint, but the Android app remains the final policy gate. Only high-confidence results in a conservative allow-list of ordinary expense categories may auto-review a transaction. Financial services, loans, transfers, utilities that are not specific enough, and ambiguous results remain in the normal review flow.

## GitHub deployment workflow

The repository includes `.github/workflows/entity-enrichment-deploy.yml` for repeatable manual deployment.

Repository secrets required by that workflow:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

Keep `LOGO_DEV_SECRET_KEY` in Cloudflare itself. Do not copy it into the Android build.

Run **Deploy Entity Enrichment Worker** from GitHub Actions and provide the public Worker base URL, for example:

`https://pesatrack-entity-enrichment.<subdomain>.workers.dev`

The workflow deploys the Worker and verifies:

`GET /v1/entity/health`

After deployment, set the GitHub repository variable:

`PESATRACK_ENTITY_ENRICHMENT_URL=https://<worker-host>/v1/entity/resolve`

The next successful `main` Android build will embed only that public endpoint in the rolling APK.

The health endpoint exposes no provider secret and only reports the service/privacy contract.

