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

Both sides use a conservative business-evidence check. A narrow list of exact
public business names and descriptors ending with a meaningful business-type
word may pass; unknown single-word names and ambiguous generic labels such as
`Mary Mobile`, `Jane Services`, or `John Kamau Properties` stay on device.
The entire descriptor must be plain ASCII without digits; this deliberately
blocks whitespace-separated phone numbers, reference fragments, and Unicode
disguises before any normalization. Unknown ten-letter words are also rejected;
explicitly recognized public names are exceptions. One shared, synthetic corpus under
`app/src/test/resources/merchant-descriptor-privacy-v1.json` checks exact
Android/Worker agreement. Legitimate merchants not recognized by this policy
may simply receive no online enrichment; local accounting is unaffected.
All numeric-bearing descriptors are conservatively rejected, even when
combined with an otherwise recognized merchant name. The business-type suffix
remains a heuristic, not identity verification: a contrived human-name-plus-
business-word descriptor may still be ambiguous and requires owner approval
of this residual tradeoff before merging or deploying.

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
- the Android app generates a random install-scoped UUID and sends it only as `X-PesaTrack-Client-Id` for rate-limit partitioning;
- the identifier is not an authentication credential and must never contain a phone number, account identifier, SMS content, transaction code, or other ledger data;
- `ENTITY_RESOLVE_CLIENT_RATE_LIMITER` allows 60 requests per 60 seconds per valid install ID in each Cloudflare edge location;
- old or malformed clients fall into a shared `anonymous` client bucket rather than bypassing the limiter;
- `ENTITY_RESOLVE_EDGE_RATE_LIMITER` adds a larger 600 requests per 60 seconds route-wide safety cap per Cloudflare edge location;
- HTTP 429 responses include `Retry-After: 60`;
- the resolve route fails closed with HTTP 503 if either limiter binding is missing or limiter evaluation fails;
- the health route is not rate limited;
- IP addresses are deliberately not used as limiter keys because mobile/NAT users may share them;
- keep `LOGO_DEV_SECRET_KEY` only in Cloudflare;
- never weaken descriptor privacy checks in order to make rate limiting easier;
- do not send raw SMS, phone numbers, transaction codes, account numbers, or likely personal names.

The install ID improves fairness but is client-generated and therefore not trusted identity. The larger edge-wide cap remains necessary to limit abuse from clients that rotate IDs. These rate limits protect provider quota and abuse exposure; they are not authentication and are not accounting controls. Cloudflare's Worker limiter is permissive/eventually consistent and local to an edge location, so monitoring upstream usage still matters.

Audit S4 remains open: rotating client IDs can evade per-install throttling and
the shared edge bucket can be exhausted. Adding IP throttling requires an
explicit decision about shared-NAT/mobile-user false positives; this privacy
filter change does not modify rate limiting or deploy Cloudflare rules.

If enrichment is throttled or unavailable, PesaTrack must continue with local ledger truth and local entity intelligence.

## Resource limits

After rate-limit evaluation, resolve requests are read with a 4 KiB byte cap before JSON parsing. Oversized bodies return HTTP 413, including streamed bodies without a Content-Length header. Provider responses have a 64 KiB byte cap and a 5-second application deadline covering both fetch and response-body reading; failures return the existing HTTP 503 fallback. The Android client independently caps successful responses at 64 KiB and uses a monotonic 12-second request deadline with a disconnect watchdog. Descriptor privacy checks and online lookup consent remain required.

Run both Worker suites with `npm test` in this directory, or directly with `node test.mjs` and `node resource-test.mjs`. Android JVM resource tests are under `app/src/test/java/com/kedevjimmy/pesatrack/data/remote`. Without an Android build installation, the production Java reader can be checked from the repository root with JDK 17:

```text
javac -encoding UTF-8 -d build/resource-check app/src/main/java/com/kedevjimmy/pesatrack/data/remote/BoundedEnrichmentResponse.java tools/EnrichmentResourceBoundsCheck.java
java -cp build/resource-check EnrichmentResourceBoundsCheck
```

This standalone check covers byte and elapsed-time handling; it does not compile the Android/Kotlin integration or replace device verification.
