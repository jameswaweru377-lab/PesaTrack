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

`https://pesatrack.<subdomain>.workers.dev`

The workflow deploys the Worker and verifies:

`GET /v1/entity/health`

The verified public production endpoint is:

`https://pesatrack.jameswaweru399.workers.dev/v1/entity/resolve`

Android CI embeds this public endpoint directly into successful `main` APK builds. No GitHub Actions repository-variable write is required.

The health endpoint exposes no provider secret and only reports the service/privacy contract.
Wrangler is configured with `keep_vars: true` so dashboard-managed Worker variables are not removed by later code deployments. The provider credential should still be stored as a Cloudflare **Secret** named `LOGO_DEV_SECRET_KEY` on the exact `pesatrack` Worker.



## Abuse and quota protection

The Android app must not embed a reusable secret to authenticate to this Worker. APK secrets are extractable and would not create a meaningful trust boundary.

Because `/v1/entity/resolve` is publicly callable, the Worker uses Cloudflare's native Rate Limiting binding before it parses the request body or calls the upstream provider.

Current repository policy:
- `ENTITY_RESOLVE_RATE_LIMITER` is configured in the root `wrangler.jsonc`;
- the resolve route is capped at 60 requests per 60 seconds for the stable route key in each Cloudflare edge location;
- HTTP 429 responses include `Retry-After: 60`;
- the resolve route fails closed with HTTP 503 if the limiter binding is unexpectedly missing;
- the health route is not rate limited;
- IP addresses are deliberately not used as limiter keys because mobile/NAT users may share them;
- keep `LOGO_DEV_SECRET_KEY` only in Cloudflare;
- never weaken descriptor privacy checks in order to make rate limiting easier;
- do not send raw SMS, phone numbers, transaction codes, account numbers, or likely personal names.

This rate limit protects provider quota and abuse exposure; it is not authentication and is not an accounting control. Cloudflare's Worker limiter is permissive/eventually consistent and local to an edge location, so monitoring upstream usage still matters.

If enrichment is throttled or unavailable, PesaTrack must continue with local ledger truth and local entity intelligence.
