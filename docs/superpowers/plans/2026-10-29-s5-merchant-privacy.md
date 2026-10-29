# S5 Merchant Privacy Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Android and Worker reject ambiguous personal descriptors and accept clearly public merchant descriptors consistently.

**Architecture:** Keep both existing pure sanitizers. Add one synthetic corpus under Android's JVM test resources, read it from Node and Kotlin, and narrow each sanitizer to a matching documented privacy policy. No new runtime dependency or network path.

**Tech Stack:** Kotlin, JUnit4, Gson already in app tests, JavaScript/Node built-ins, Cloudflare Worker.

**Spec:** `docs/superpowers/specs/2026-10-29-s5-merchant-privacy-design.md`

## Global Constraints

- Optional enrichment remains off by default and available only to PAY_BILL / BUY_GOODS.
- Never log or upload raw SMS, amounts, account data or personal identifiers.
- No APK signing, CI workflow, cloud deployment or financial ledger changes.
- PR is intentionally unmerged pending user review.

## Review Focus

- 10-letter plain business words must not be misclassified as transaction codes.
- Alphanumeric transaction references with digits must remain blocked.
- Single-name personal descriptors must remain local.
- Weak generic category words must not turn personal-looking labels into businesses.
- Both Android and Worker must agree on exact normalized accepted strings.

---

### Task 1: Capture privacy regressions in a shared corpus

**Files:**
- Create: `app/src/test/resources/merchant-descriptor-privacy-v1.json`
- Modify: `app/src/test/java/com/kedevjimmy/pesatrack/domain/intelligence/RemoteEntityEnrichmentTest.kt`
- Modify: `worker/entity-enrichment/test.mjs`

**Interfaces:**
- Consumes: existing `EntityDescriptorPrivacyPolicy.sanitize(raw)` and Worker route.
- Produces: a table of `input` and `expected` strings or null, checked by both implementations.

- [ ] Write failing regression examples for three missed business names and three privacy leaks; retain positive and negative legacy samples.
- [ ] Run `npm test` from `worker/entity-enrichment` and confirm the expected regression failure.
- [ ] Have the Kotlin JUnit test read the same JSON with its existing Gson dependency.

### Task 2: Narrow both sanitizers and verify

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/intelligence/RemoteEntityEnrichment.kt`
- Modify: `worker/entity-enrichment/src/index.js`
- Modify: `worker/entity-enrichment/README.md`, `CURRENT_STATE.md` where behavior changed.

**Interfaces:**
- Consumes: synthetic shared corpus from Task 1.
- Produces: sanitized public descriptors or null; Worker remains fail-closed.

- [ ] Correct transaction-reference discrimination and apply conservative business allow rules.
- [ ] Run `npm test`, the security scan, Worker syntax, and the existing deployment dry-run check if available.
- [ ] Run JVM/Android CI for Kotlin test, lint and APK assembly; inspect CI before review.
- [ ] Review the diff for privacy-expanding behavior, keep PR unmerged, and report unverified device/deployment behavior.
