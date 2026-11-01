# S5 Person-Like Merchant Descriptor Hardening Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fail closed for additional invented person-like descriptors and expand the existing shared Kotlin/Worker privacy test corpus past 60 cases, without adding any new data egress.

**Architecture:** Extend the shared JSON contract with positive and negative invented cases, observe Worker RED, then add the same narrow person-like first-token reject rule to the two existing policies. Keep public-name and strong business-suffix admission otherwise unchanged. Do not claim the heuristic detects every personal name.

**Tech Stack:** Kotlin/JVM, JavaScript/Node Worker, JSON shared test vectors.

**Spec:** `docs/superpowers/specs/2026-10-29-s5-merchant-privacy-design.md`

## Global Constraints

- Only PAY_BILL and BUY_GOODS currently reach the optional Worker; no change to transaction eligibility.
- Do not add new names to the public-allow list, network calls, permissions, telemetry, identifiers or financial categorization.
- Prefer a missed merchant hint over sending an ambiguous person name off-device.
- Owner review of the changed privacy boundary is mandatory before any merge/deployment. Prepare a DRAFT PR and do not trigger Worker deployment without that review.

## Review Focus

- Mixed-case personal first names followed by shop, bank, pharmacy or limited/company terms must stay local.
- Current public-brand exceptions and synthetically unambiguous commercial business suffixes must still pass.
- Digits, long identifiers, email, Unicode obfuscation and split phone/account references remain rejected.
- Kotlin and JS must use the identical shared corpus; false-rejected unfamiliar merchants remain explicitly disclosed.
- No silent change to PAY_BILL/BUY_GOODS eligibility or money classification.

---

### Task 1: Add shared negative cases and minimal first-token guard

**Files:**
- Modify: `app/src/test/resources/merchant-descriptor-privacy-v1.json`
- Modify: `app/src/test/java/com/kedevjimmy/pesatrack/domain/intelligence/RemoteEntityEnrichmentTest.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/intelligence/RemoteEntityEnrichment.kt`
- Modify: `worker/entity-enrichment/src/index.js`
- Create: `docs/superpowers/plans/2026-11-01-s5-person-name-privacy-hardening.md`

**Interfaces:** Existing `EntityDescriptorPrivacyPolicy.sanitize(String?): String?` and `sanitizeDescriptor(raw): string|null`, driven by the same test-resource JSON.

- [ ] **Step 1:** Add at least 26 invented person-looking/merchant-label cases and commercial controls; increase corpus minimum to 60 in Kotlin and JS tests.
- [ ] **Step 2:** Run `node worker/entity-enrichment/test.mjs`; expected RED for person-like business-suffix descriptors.
- [ ] **Step 3:** Add a conservative person-like initial-token reject to Kotlin and JS, after exact public-name exceptions; preserve all other logic.
- [ ] **Step 4:** Run Worker contract/resource tests, Node syntax, security scan, Python repository tests and diff checks. Full Android JVM/lint/APK verification is required in GitHub CI.
- [ ] **Step 5:** Open a DRAFT PR from latest main, obtain exact-head CI evidence, and leave unmerged pending explicit owner review of residual privacy false accepts/rejects.
