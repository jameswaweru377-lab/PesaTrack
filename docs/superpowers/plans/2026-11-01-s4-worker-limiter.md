# S4 Worker Rate-Limit Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop rotating client UUIDs and rejected client requests from exhausting the shared enrichment Worker budget as easily, without adding an account, telemetry or new financial data flow.

**Architecture:** Keep the existing per-install 60/min and edge-location 600/min limiters. Add a *supplementary* Cloudflare-connection-IP limiter at 300/min per location, applied after per-client and before the shared bucket, so denied clients do not spend the shared allowance. Treat the IP as an abuse-control hint, not a stable identity; use a conservative shared fallback key when missing.

**Tech Stack:** Cloudflare Workers JS, Wrangler ratelimit bindings, Node contract tests.

**Spec:** Verified audit S4 at main `37743e3`, tracked in `docs/tracker/AUDIT_MANIFEST.json`; no new outbound request or merchant data class.

**Tracking:** issue #525, chat owner `CHAT-AUDIT-S4`.

## Global Constraints

- Public IPs must not be logged, persisted in app data or mixed into merchant/provider request payloads; use synthetic documentation-only IP addresses in tests.
- Real users behind mobile carrier NAT can share an address; 300/min is deliberately above the per-install bucket, but is still a trade-off to review from real traffic.
- This is **not** a globally strict budget; per-location Cloudflare counters remain approximate. Deployment needs separate explicit approval and provider-spend monitoring.
- Fail closed if any of the three limiter bindings is unavailable; keep health endpoint unthrottled.
- No changes to Kotlin online eligibility, descriptor privacy rules, banking SMS, cloud sync, permissions or financial classification.

## Review Focus

1. 301 distinct valid UUIDs from one trusted IP in a minute: 301st denied before the edge bucket.
2. 61 requests from one UUID: 61st denied **without** incrementing shared edge quota.
3. Two different IPs behind the same edge: separate IP buckets but still one 600/min edge protection.
4. Missing `CF-Connecting-IP`: shared conservative fallback bucket; no caller-supplied UUID can circumvent it.
5. Missing/throwing IP limiter: 503 fail closed, no upstream provider call.

---

### Task 1: Failing rate-limit contract tests

**Files:** Modify `worker/entity-enrichment/test.mjs`.

**Interfaces:** Consumes current Worker `fetch(request, env)`, ratelimit `.limit({key})`; produces a 300/min IP-bound contract and correct limiter ordering.

- [ ] Add an in-memory counting limiter fixture and synthetic `CF-Connecting-IP` header to invented test requests.
- [ ] Test rotating 301 UUIDs and fixed 61 UUIDs with recorded client/IP/edge calls; test missing binding and separate IPs.
- [ ] Run `node worker/entity-enrichment/test.mjs`. Expected **FAIL** on old code because rotating identities avoid the client limiter and edge is counted before client denial.

### Task 2: Minimal Worker binding/logic and docs

**Files:**
- Modify: `worker/entity-enrichment/src/index.js` (`enforceResolveRateLimit` only).
- Modify: `worker/entity-enrichment/wrangler.jsonc` (new independent namespace 300/min).
- Modify: `worker/entity-enrichment/resource-test.mjs` (new binding fixture).
- Modify: `worker/entity-enrichment/README.md` and `docs/PRIVACY_POLICY.md` (limited ephemeral IP use and NAT caveat).

**Interfaces:** Consumes Cloudflare-supplied `CF-Connecting-IP` or `ip:unknown`, plus existing client and edge keys. Produces a 429 with `retry-after: 60` for the new abuse cap; 503 if required limiters fail.

- [ ] Add fail-closed binding check and evaluate **client → IP → edge**, returning the existing JSON error shapes and no sensitive diagnostics.
- [ ] Set `ENTITY_RESOLVE_IP_RATE_LIMITER` at 300/60 with its own namespace, preserving client and edge thresholds.
- [ ] Update both docs narrowly; no new telemetry, network call or permission.
- [ ] Run `node worker/entity-enrichment/test.mjs` and `node worker/entity-enrichment/resource-test.mjs`. Expected PASS and eight existing resource checks.
- [ ] Run `node --check worker/entity-enrichment/src/index.js`, `python3 tools/security_scan.py`, `git diff --check`, and full available offline JS tests. Expected all exit 0.
- [ ] Commit focused S4 files; create a **draft PR** linked to its canonical issue. Do **not** deploy the Worker or merge based only on mock limiters.
