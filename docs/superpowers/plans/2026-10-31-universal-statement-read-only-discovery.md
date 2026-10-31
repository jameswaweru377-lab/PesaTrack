# Universal Statement Read-only Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recognize additional unfamiliar selectable-text statement table families and produce bounded, read-only candidate evidence without granting import or ledger authority.

**Architecture:** Extend the existing V2 semantic header discovery by one strictly named signed-amount family. Compose its current discovery and interpreter into a new pure Kotlin read-only multi-page probe. Keep issuer/account/document-wide proof, production PDF routing, Statement archive persistence, M-Pesa and Equity import paths completely unchanged until a separately tested live-admission phase.

**Tech Stack:** Kotlin JVM tests, existing `PdfTextFragment`, `StatementTableDiscovery`, `UniversalStatementTableInterpreter`, JUnit 4. No new dependencies, network, permissions, database, UI, or Android instrumentation.

**Spec:** `docs/superpowers/specs/2026-10-31-universal-statement-safe-auto-import-design.md` (approved 31 October 2026; initially on `design/universal-statement-auto-import-20261031`)

## Global Constraints

- Work from current `main` (`61e0dd94` when this plan was written); recheck active PRs and HEAD immediately before editing. Do not modify another chat's branches.
- Preserve full-document/issuer/account/currency/total proof and the separately isolated Equity limited-assurance archive; this stage exposes no admission or persistence.
- No automatic Activity writes or classification, no partial import, no cloud processing, and no scanned-PDF claims.
- Use only invented data in tests; no private statement, account number or source text in fixture/log/commit.
- Preserve existing M-Pesa, Equity and structured CSV acceptance. Existing API restrictions, backup and signing configuration are unchanged.
- Respect `AGENT_WORKING_METHOD.md`: tests first, new file under 800 lines, new function under 80 lines, focused checks then CI; no instrumentation unless separately approved.

## Review Focus

1. A generic **Amount** column without an explicit direction can mean absolute debits: it must not be treated as signed merely because arithmetic appears to work. Task 1 tests explicit signed labels and generic rejection.
2. A missing PDF page or unreadable last page must never produce a seemingly complete candidate from preceding pages. Task 2 tests duplicate, gapped, empty and invalid page sequences.
3. A correct first-page table plus a malformed second page must return only rejection, never a partial candidate. Task 2 tests all-or-nothing output.
4. A known Equity `unverified_issuer` or wrong-account refusal must not trigger the generic fallback to financial admission. Task 3 tests typed refusal routing.
5. A balance-compatible row with an unknown numeric column or contradictory adjacent balance must be refused. Tasks 1 and 2 test both cases.

---

### Task 1: Explicit signed-amount format family

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/UniversalStatementTableInterpreter.kt` (header aliases only)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementTableDiscovery.kt` (one additional strictly gated schema)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementTableDiscoveryTest.kt`

**Interfaces:** Consume `StatementTableDiscovery.discover(pageNumber, words, profile)` and `UniversalStatementTableInterpreter.interpret(pages, openingBalanceMinor)`. Preserve their public signatures, return types and current accounting logic.

- [ ] **Step 1: Write failing tests** `signedAmountHeadingIsDiscoverableAndBalanceReconciles`, `genericAmountWithoutDirectionIsNotDiscoverable`, and `unknownNumericColumnStillBlocksSignedLayout`. Invent a four-column Date/Description/Signed Amount/Balance table with consecutive movements -10.00, +20.00 and balances 90.00, 110.00 from opening 100.00. Assert the exact minor-unit movements `[-1000, 2000]` and `sourceRows == 2`. Assert no candidate for bare Amount or an extra numeric unknown column.
- [ ] **Step 2: Run the three tests** with `./gradlew :app:testDebugUnitTest --tests '*StatementTableDiscoveryTest' --console=plain`; expect the new signed-family test to fail before implementation, while baseline tests remain unchanged.
- [ ] **Step 3: Implement the smallest change**: recognize normalized heading `SIGNED AMOUNT` as `StatementField.AMOUNT`, and add `(DATE, DESCRIPTION, AMOUNT, BALANCE)` only when the actual observed amount heading is expressly signed. Leave generic `AMOUNT` without `DIRECTION` blocked in physical discovery. Do not change the existing interpreter's direct CSV behavior.
- [ ] **Step 4: Rerun the focused test** and verify all three new cases plus prior discovery/interpreter regressions pass.
- [ ] **Step 5: Commit** only these files with message `feat(statements): discover explicit signed movement tables read only`.

### Task 2: Bounded multi-page read-only probe

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/UniversalStatementReadOnlyProbe.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/UniversalStatementReadOnlyProbeTest.kt`

**Interfaces:**
- `data class StatementProbePage(val pageNumber: Int, val words: List<PdfTextFragment>)`
- `sealed class StatementProbeResult` with `Provisional(val tables: List<StatementTablePage>, val legs: List<StatementLegEvidence>, val sourceRows: Int, val oldestLegUnanchored: Boolean)` and `Blocked(val pageNumber: Int?, val reason: StatementProbeReason)`.
- `enum class StatementProbeReason { INVALID_PAGE_SEQUENCE, UNRECOGNIZED_TABLE, UNVERIFIED_MOVEMENTS }`.
- `object UniversalStatementReadOnlyProbe { fun inspect(pages: List<StatementProbePage>, openingBalanceMinor: Long? = null, profile: StatementFormatProfile = StatementFormatProfile(), now: Long = System.currentTimeMillis()): StatementProbeResult }`.
- Every `Provisional` is **read-only and not archive-admissible**; it contains no issuer/account identity or full-document proof. No method may produce `StatementAdmissionResult.Accepted`.

- [ ] **Step 1: Write failing JUnit tests** for two unrelated invented header families over 2 pages; exact movements and rows; malformed second page; empty/gapped/duplicate/reordered pages; conflicting adjacent balances; unknown amount column; and unanchored first row. Assert blocked results expose only stable enum reason and optional page number.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*UniversalStatementReadOnlyProbeTest' --console=plain`; expect compilation failure or failing assertions before implementation.
- [ ] **Step 3: Implement** the pure probe: require 1–160 contiguous numbered pages, nonempty words, at most 32,000 fragments across the document and at most 5,000 reconstructed rows; call existing `StatementTableDiscovery.discover` on every page, combine tables only after every page succeeds, then invoke `UniversalStatementTableInterpreter.interpret` exactly once over the complete ordered document. Map errors to safe enum reasons; no partial candidate and no sensitive diagnostics.
- [ ] **Step 4: Rerun focused tests** for probe, discovery, interpreter and bank admission. Verify first/last balance chains and no unknown numeric bypass.
- [ ] **Step 5: Commit** the new probe and tests with message `feat(statements): add untrusted multi-page layout probe`.

### Task 3: Typed non-bypass routing policy

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementFallbackProbeGate.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementFallbackProbeGateTest.kt`

**Interfaces:**
- `enum class StatementKnownImportOutcome { UNSUPPORTED_LAYOUT, ISSUER_REJECTED, ACCOUNT_REJECTED, FINANCIAL_PROOF_REJECTED, PASSWORD_REJECTED, UNSUPPORTED_DEVICE, OTHER_FAILURE }`
- `object StatementFallbackProbeGate { fun mayRunReadOnlyProbe(outcome: StatementKnownImportOutcome): Boolean }`
- This policy **only** grants an in-memory probe attempt; it never grants archive admission or rewires the production PDF picker.

- [ ] **Step 1: Write failing test** asserting only `UNSUPPORTED_LAYOUT` returns true. Test issuer/account/financial/password/device/unknown refusals all false, with a named regression `equityUnverifiedIssuerNeverBecomesFallbackAdmission`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*StatementFallbackProbeGateTest' --console=plain`; expect failure before the policy exists.
- [ ] **Step 3: Implement** a small exhaustive typed decision, defaulting closed; never map arbitrary exception strings to unsupported layout.
- [ ] **Step 4: Rerun** its focused tests, with the current `StatementBankAdmissionTest` as regression.
- [ ] **Step 5: Commit** only the policy and test with message `test(statements): gate unknown-layout probing after safe refusals`.

### Task 4: Verification, documentation and safe PR handoff

**Files:**
- Modify: `CURRENT_STATE.md` (report read-only unconnected feature accurately)
- Preserve: `AGENTS.md`, `AGENT_WORKING_METHOD.md`, `StatementImportScreens.kt`, `OfflineBankTableReader.kt`, `StatementBankArchiveStore.kt`, Python bank extractors, backup schemas and workflows.

- [ ] **Step 1: Update current state** to say the new probe understands provisional table families **only** and is not wired to the import picker, ledger, scan/OCR, profile learning or bank archive. Include test/device acceptance limits.
- [ ] **Step 2: Run focused tests**, then repository's standard `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`; record exact failure, pass and environmental restrictions. Use existing CI rather than adding tests to the mandatory workflow.
- [ ] **Step 3: Review diff** for private values, new dependencies, unintended importer/Activity modifications, test fixture authenticity and current-main conflicts. Commit docs only if accurate to verified outcomes.
- [ ] **Step 4: Open one isolated PR** against latest main and let existing CI run on the exact commit. No manual instrumentation, CI retries or release workflow changes. Obtain independent code review; report remaining gates. Merge only with task-specific owner authorization and all required checks passed.

## Scope after this plan

This plan implements **Slice A only**: an isolated proof-of-capability, not user-visible general bank importing. A separate Slice B plan will connect independently proven issuer/document metadata and atomic verified Statements archive with review; it requires tested source contracts and device acceptance before enabling automatic saving. A separate Slice C plan will investigate offline OCR for scans. Do not quietly expand this PR into those risk areas.
