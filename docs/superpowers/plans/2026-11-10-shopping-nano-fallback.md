# Nano-first Shopping Scanner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use Gemini Nano for optional local interpretation of scanned shopping lists, with today's offline OCR as a reliable fallback.

**Architecture:** Existing ML Kit OCR always reads the photo first. A small Android-only service checks AICore availability and conditionally produces a bounded structured draft from the OCR text; a pure Kotlin gate verifies and normalizes AI draft lines before the unchanged explicit review/save flow. No model is downloaded automatically.

**Tech Stack:** Kotlin, Compose, bundled ML Kit OCR, ML Kit GenAI prompt beta4, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-10-shopping-nano-first-design.md`

## Global Constraints

- Keep minSdk 24 and existing offline OCR unchanged for unsupported devices.
- No new permission, telemetry, receipt upload, direct ledger write or backup format change.
- Model output is untrusted and must remain a user-editable proposal.
- Do not automatically download a Gemini Nano model.

## Review Focus

- Unsupporting phone/API 24: no Nano access; OCR-only draft displayed.
- Inference throws or times out: OCR-only draft survives.
- Model returns prose, invented items or prices: do not show a trusted AI draft.
- OCR produces empty text: do not prompt Nano with empty input.
- Switching shopping lists mid-scan: never save into the wrong list.

### Task 1: Pure guarded Nano draft format

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/planning/ShoppingNanoDraftPolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/planning/ShoppingNanoDraftPolicyTest.kt`

**Interfaces:**
- `prompt(ocrText: String): String` — asks for bounded canonical item lines, no guessed prices.
- `reviewableDraft(ocrText: String, modelText: String): String?` — accepts only useful, bounded, grounded candidate lines; returns null to select raw OCR.

- [ ] Write tests for grounded items, footer rejection, blank/malformed text and fabricated prices.
- [ ] Run focused JVM test and observe expected failure.
- [ ] Implement pure policy and rerun tests.

### Task 2: Local Nano availability and OCR fallback

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ShoppingNanoInterpreter.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ShoppingIntelligencePanels.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- `suspend fun propose(ocrText: String): NanoProposal` — status-aware, safe, no automatic download; never throws to UI.
- `NanoProposal` — text and explanation; raw OCR when Nano is unavailable or fails.

- [ ] Test fallback selection using a fake interpreter where feasible.
- [ ] Implement Nano availability check (only Android API >=26) and bounded inference.
- [ ] Show Nano vs standard source in existing editable review; preserve explicit confirmation.
- [ ] Run targeted and broader JVM tests, lint and debug APK assembly.

### Task 3: Documentation and delivery

**Files:** `CURRENT_STATE.md`, `docs/PRIVACY_POLICY.md`, `README.md`

- [ ] Explain no uploads, no automatic model download, unsupported-device fallback and model limitations.
- [ ] Check diff for privacy, money, package identity and APK impact.
- [ ] Open separate PR; report unavailable local build/test checks accurately and request Redmi acceptance.
